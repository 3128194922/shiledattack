package com.shiledattack.dual;

import com.google.common.collect.Multimap;
import com.shiledattack.Config;
import com.shiledattack.ShiledAttackMod;
import com.shiledattack.mixin.LivingEntityTickerAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 双持武器的服务端结算。
 *
 * <p>流程：主手攻击发起时登记候选目标，主手真正造成伤害后再排入一次副手追击，
 * 延迟若干 tick 用副手武器重放一次 {@code Player#attack}。
 * 两次攻击都走原版攻击管线，因此附魔、暴击、击退、耐久消耗与音效都与原版一致。</p>
 *
 * <p>副手追击期间：</p>
 * <ul>
 *   <li>临时移除主手武器的属性修饰符、挂上副手武器的，保证伤害来自副手；</li>
 *   <li>沿用主手那一击的攻击冷却比例，既不因延迟而衰减，也不给连点白送伤害；</li>
 *   <li>结束后恢复冷却计时，让双持挥击不会额外拖慢攻击节奏；</li>
 *   <li>锥形扩散与屏幕抖动不在副手这一击重复结算。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID)
public final class DualWieldHandler {

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    /** 本次挥击的候选目标，只有真正造成伤害后才转成副手追击。 */
    private static final Map<UUID, Candidate> CANDIDATES = new HashMap<>();

    private record Candidate(int targetId, long gameTime, ItemStack main, ItemStack off,
                             float strengthScale) {
    }

    private record Pending(ServerPlayer player, int targetId, ResourceKey<Level> dimension,
                           long dueTick, ItemStack main, ItemStack off, float strengthScale) {
    }

    private DualWieldHandler() {
    }

    /**
     * 记录本次挥击的候选目标。
     *
     * <p>{@code AttackEntityEvent} 只表示“发起攻击”，目标仍可能被无敌帧、
     * 伤害事件或其它模组取消，因此这里先不排入追击。</p>
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        // 副手追击自身也会触发该事件，必须跳过，否则会无限递归
        if (DualWieldState.isOffhandAttackInProgress()) return;
        if (event.isCanceled()) return;

        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        if (!(player instanceof ServerPlayer server)) return;
        if (!Config.dualWieldEnabled) return;

        ItemStack main = server.getMainHandItem();
        ItemStack off = server.getOffhandItem();
        if (!DualWield.isDualWieldMain(main) || !DualWield.isOffhandWeapon(off)) return;

        CANDIDATES.put(server.getUUID(), new Candidate(target.getId(),
                server.level().getGameTime(), main.copy(), off.copy(),
                server.getAttackStrengthScale(0.5F)));
    }

    /**
     * 主手真正造成伤害后才排入副手追击。
     *
     * <p>只认同一 tick、同一目标、同一对武器的记录，避免陈旧数据误触发。</p>
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (DualWieldState.isOffhandAttackInProgress()) return;
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getEntity() == player) return;

        Candidate candidate = CANDIDATES.get(player.getUUID());
        if (candidate == null) return;
        // 一次挥击只结算一次，无论后面是否成功排入追击
        CANDIDATES.remove(player.getUUID());

        // 被格挡或被其它模组取消时不算命中
        if (event.isCanceled() || event.getAmount() <= 0.0F) return;
        if (candidate.gameTime() != player.level().getGameTime()) return;
        if (candidate.targetId() != event.getEntity().getId()) return;
        if (!ItemStack.isSameItem(player.getMainHandItem(), candidate.main())) return;
        if (!ItemStack.isSameItem(player.getOffhandItem(), candidate.off())) return;
        if (!DualWield.isDualWieldMain(candidate.main())
                || !DualWield.isOffhandWeapon(candidate.off())) {
            return;
        }

        long due = player.level().getGameTime() + Math.max(0, Config.dualWieldOffhandDelayTicks);
        PENDING.put(player.getUUID(), new Pending(player, candidate.targetId(),
                player.level().dimension(), due, candidate.main(), candidate.off(),
                candidate.strengthScale()));
    }

    /**
     * 蓄力攻击直接调用 {@code hurt}，不经过 {@link AttackEntityEvent}，
     * 因此命中后由蓄力结算主动调用这里排入副手追击。
     *
     * @param strengthScale 本次蓄力攻击对应的攻击冷却比例，副手沿用同一比例
     */
    public static void scheduleAfterConfirmedHit(ServerPlayer player, LivingEntity target,
                                                 float strengthScale) {
        if (!Config.dualWieldEnabled || player == null || target == null) return;
        if (!player.isAlive() || player.isSpectator()) return;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (!DualWield.isDualWieldMain(main) || !DualWield.isOffhandWeapon(off)) return;

        long due = player.level().getGameTime() + Math.max(0, Config.dualWieldOffhandDelayTicks);
        PENDING.put(player.getUUID(), new Pending(player, target.getId(),
                player.level().dimension(), due, main.copy(), off.copy(), strengthScale));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;

        List<Pending> due = new ArrayList<>(2);
        Iterator<Map.Entry<UUID, Pending>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next().getValue();
            ServerPlayer player = pending.player();
            if (player.hasDisconnected() || player.isRemoved()) {
                iterator.remove();
                continue;
            }
            if (!player.level().dimension().equals(pending.dimension())) {
                iterator.remove();
                continue;
            }
            if (player.level().getGameTime() >= pending.dueTick()) {
                iterator.remove();
                due.add(pending);
            }
        }

        for (Pending pending : due) {
            execute(pending);
        }
    }

    private static void execute(Pending pending) {
        ServerPlayer player = pending.player();
        if (!player.isAlive() || player.isSpectator() || player.isUsingItem()) return;

        Entity entity = player.level().getEntity(pending.targetId());
        if (!(entity instanceof LivingEntity target) || target == player
                || !target.isAlive() || target.isRemoved()) {
            return;
        }

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        // 两次命中之间换过武器就放弃，避免用错武器结算
        if (!ItemStack.isSameItem(main, pending.main())) return;
        if (!ItemStack.isSameItem(off, pending.off())) return;
        if (!DualWield.isDualWieldMain(main) || !DualWield.isOffhandWeapon(off)) return;
        // 目标在延迟期间跑出攻击距离则不打
        if (!player.canReach(target, 1.5D)) return;

        performOffhandAttack(player, target, main, off, pending.strengthScale());
    }

    private static void performOffhandAttack(ServerPlayer player, LivingEntity target,
                                             ItemStack main, ItemStack off, float strengthScale) {
        Multimap<Attribute, AttributeModifier> mainMods = main.getAttributeModifiers(EquipmentSlot.MAINHAND);
        Multimap<Attribute, AttributeModifier> offMods = off.getAttributeModifiers(EquipmentSlot.MAINHAND);
        AttributeMap attributes = player.getAttributes();
        LivingEntityTickerAccess ticker = (LivingEntityTickerAccess) (Object) player;
        int savedTicker = ticker.shiledattack$getAttackStrengthTicker();

        DualWieldState.begin(player, off, strengthScale);
        try {
            // 主副手武器共用同一组属性修饰符 UUID，必须先摘主手再挂副手
            attributes.removeAttributeModifiers(mainMods);
            attributes.addTransientAttributeModifiers(offMods);
            // 主手那一击已经把无敌帧拉到 20，这里清零才能让副手伤害真正落下
            target.invulnerableTime = 0;

            float healthBefore = target.getHealth();
            float absorptionBefore = target.getAbsorptionAmount();
            player.attack(target);
            boolean landed = target.getHealth() < healthBefore - 1.0E-4F
                    || target.getAbsorptionAmount() < absorptionBefore - 1.0E-4F;
            if (!landed) {
                // 兜底：部分模组会在 AttackEntityEvent 里取消攻击，或由物品自行处理点击，
                // 导致原版攻击管线没有真正掉血。这里用副手武器属性直接补一次伤害。
                applyFallbackDamage(player, target, off, strengthScale);
            }
        } finally {
            attributes.removeAttributeModifiers(offMods);
            attributes.addTransientAttributeModifiers(mainMods);
            DualWieldState.end();
            // 恢复冷却进度，避免延迟命中把整轮攻击节奏往后拖
            ticker.shiledattack$setAttackStrengthTicker(savedTicker);
        }

        // 副手武器碎裂的广播已由 LivingEntityBreakEventMixin 重定向，这里不再重复播放
        DualWieldSwings.broadcast(player);
    }

    /**
     * 副手一击的伤害兜底，只在原版攻击管线没有真正掉血时调用。
     *
     * <p>公式与原版 {@code Player#attack} 保持一致：攻击力 × 冷却倍率 + 附魔加成。
     * 调用时主手属性仍被替换为副手，因此读到的攻击力就是副手武器的。</p>
     */
    private static void applyFallbackDamage(ServerPlayer player, LivingEntity target,
                                            ItemStack off, float strengthScale) {
        if (!target.isAlive() || target.isDeadOrDying()) return;

        float scale = Mth.clamp(strengthScale, 0.0F, 1.0F);
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        damage *= 0.2F + scale * scale * 0.8F;
        damage += EnchantmentHelper.getDamageBonus(off, target.getMobType()) * scale;
        if (damage <= 0.0F) return;

        target.invulnerableTime = 0;
        target.hurt(player.damageSources().playerAttack(player), damage);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
        CANDIDATES.remove(event.getEntity().getUUID());
    }
}
