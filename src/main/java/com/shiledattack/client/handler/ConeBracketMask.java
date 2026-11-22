package com.shiledattack.client.handler;

import com.mojang.blaze3d.platform.NativeImage;
import com.shiledattack.ShiledAttackMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 括号贴图解析，移植自 {@code cortes-combat-rainbow} 的 {@code BracketMask}。
 *
 * <p>把 PNG 读成一组水平矩形，再用 {@code fill} 绘制，而不是 {@code blit}：
 * {@code blit} 不走 GUI 管线，染色对它无效，括号会一直保持不透明。
 * 用 {@code fill} 后括号与进度条的 alpha 语义一致。</p>
 */
final class ConeBracketMask {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ShiledAttackMod.MODID, "textures/gui/cone_bracket.png");

    /** 低于该值视为无墨迹。 */
    private static final int ALPHA_FLOOR = 16;
    /** 无墨迹时锚点的取值。 */
    static final int NO_INK = -1;

    record Run(int leftX, int rightX, int y, int width, int alpha) {
    }

    record Anchors(int left, int right) {
    }

    private record Sprite(List<Run> runs, int width, int height, int peak) {
        static final Sprite MISSING = new Sprite(List.of(), 0, 0, 0);
    }

    private static final Sprite SPRITE = load();

    private ConeBracketMask() {
    }

    static int height() {
        return SPRITE.height();
    }

    static List<Run> runs() {
        return SPRITE.runs();
    }

    /** 左右括号墨迹在贴图中的列位置；右侧由左侧镜像得到。 */
    static Anchors anchors() {
        int first = Integer.MAX_VALUE;
        int last = Integer.MIN_VALUE;
        for (Run run : SPRITE.runs()) {
            first = Math.min(first, run.leftX());
            last = Math.max(last, run.leftX() + run.width() - 1);
        }
        if (first > last) {
            return new Anchors(NO_INK, NO_INK);
        }
        int left = (first + last) / 2;
        return new Anchors(left, SPRITE.width() - 1 - left);
    }

    /** 墨迹 alpha 相对贴图最亮像素的比例，0..1。 */
    static float normalised(int textureAlpha) {
        int peak = SPRITE.peak();
        if (peak <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, textureAlpha / (float) peak);
    }

    private static Sprite load() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return Sprite.MISSING;
        }
        Resource resource = minecraft.getResourceManager().getResource(TEXTURE).orElse(null);
        if (resource == null) {
            return Sprite.MISSING;
        }
        try (InputStream in = resource.open();
             NativeImage image = NativeImage.read(in)) {
            List<Run> runs = runs(image);
            return new Sprite(runs, image.getWidth(), image.getHeight(), peakOf(runs));
        } catch (IOException e) {
            return Sprite.MISSING;
        }
    }

    private static int peakOf(List<Run> runs) {
        int peak = 0;
        for (Run run : runs) {
            peak = Math.max(peak, run.alpha());
        }
        return peak;
    }

    /** 只读左半边再镜像，避免两半被读两次而出现一像素偏差。 */
    private static List<Run> runs(NativeImage image) {
        int width = image.getWidth();
        int axis = (width - 1) / 2;
        List<Run> runs = new ArrayList<>();
        for (int y = 0; y < image.getHeight(); y++) {
            int start = -1;
            int strength = 0;
            for (int x = 0; x <= axis; x++) {
                int alpha = x < axis ? alphaOf(image, x, y) : 0;
                if (alpha >= ALPHA_FLOOR) {
                    if (start < 0) {
                        start = x;
                    }
                    strength = Math.max(strength, alpha);
                } else if (start >= 0) {
                    runs.add(new Run(start, width - x, y, x - start, strength));
                    start = -1;
                    strength = 0;
                }
            }
        }
        return List.copyOf(runs);
    }

    private static int alphaOf(NativeImage image, int x, int y) {
        return (image.getPixelRGBA(x, y) >>> 24) & 0xFF;
    }
}
