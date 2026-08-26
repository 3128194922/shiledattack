# Shield Auto Attack

一个 Minecraft Forge 模组，提供盾牌自动收放、长按自动攻击、盾牌瞬时格挡与盾反功能。本mod由AI编写。

## 参考MOD
- [responsive-shields](https://github.com/Revvilo/responsive-shields/tree/1.18.x-1.20.4?tab=License-1-ov-file)：盾牌举起延迟 Mixin 实现参考
- [combat-nouveau](https://github.com/fuzs/combat-nouveau)：长按攻击机制参考

## 环境要求

| 项目 | 版本 |
|------|------|
| Minecraft | 1.20.1 |
| Forge | 47.4.6+ |
| Java | 17 |
| KubeJS（可选） | 2001.6.5+，用于盾反回传事件 `ParryEvents.parried` |

## 功能介绍

### 1. 盾牌自动收放

当玩家举盾状态下按下攻击键时，模组会自动：

1. 瞬间放下盾牌（`stopUsingItem`）
2. 执行攻击
3. 在同一 tick 结束时，如果右键仍在按下状态，自动重新举起盾牌

整个过程对玩家而言是无缝的，实现「举盾同时攻击」的体验。盾牌识别使用 `forge:tools/shields` 物品标签，自动检测主手/副手的盾牌位置。

### 2. 长按自动攻击

模仿自 [combat-nouveau](https://github.com/fuzs/combat-nouveau) 的长按攻击机制。按住攻击键时，当武器攻击冷却恢复满（`getAttackStrengthScale >= 1.0`）且准星对准实体时，自动发起攻击。

**攻击间隔完全由武器本身的攻击速度（Attack Speed）属性决定**，无需手动配置：

| 武器类型 | 攻击速度 | 约每 N ticks 攻击一次 |
|---------|---------|---------------------|
| 剑 | 1.6 | ~12.5 ticks |
| 斧 | 1.0 | ~20 ticks |
| 镐 | 1.0 | ~20 ticks |
| 锹 | 1.0 | ~20 ticks |

> 仅在准星对准实体时触发，不影响方块挖掘。

### 3. 自动挥动武器（Auto Swing Tag）

通过物品标签 `shiledattack:auto_swing` 标记的武器，在长按攻击键时，攻击冷却恢复满后**立即挥动**，无需准星对准实体。

- 默认 tag 为空，玩家/整合包作者可自行添加武器
- 适用于挥动本身具有特殊效果的模组武器（如挥动触发技能、发射投射物等）

**兼容机制：**

| 场景 | 行为 |
|------|------|
| 准星对准实体 | 正常攻击（触发 `hurtEnemy`） |
| 空挥（无实体） | 触发 `PlayerInteractEvent.LeftClickEmpty` 事件 |

空挥时通过 Forge 事件总线发送 `LeftClickEmpty` 事件，兼容 Cataclysm 的 `ILeftClick` 接口机制：
1. 客户端触发 `LeftClickEmpty` → Cataclysm 检测 `ILeftClick` 物品 → 发送 `MessageSwingArm` 包
2. 服务端调用 `onLeftClick` → `launchTornado` → 生成 `Sandstorm_Projectile` 旋风

Tag 文件位置：`data/shiledattack/tags/items/auto_swing.json`

添加武器示例：

```json
{
  "replace": false,
  "values": [
    "cataclysm:ancient_spear",
    "minecraft:diamond_sword",
    "mymod:skill_sword"
  ]
}
```

> 注：`cataclysm:ancient_spear` 加入 tag 后，长按即可自动空挥召唤旋风。

也可通过 KubeJS、CraftTweaker 等工具动态添加。

### 4. 潜行自动举盾

玩家潜行时自动举起盾牌（副手优先，其次主手）。右键按下时自动放下盾牌释放主手右键事件，右键松开时自动重新举盾，实现无缝切换。

**核心机制（边沿触发 + keyUse 状态模拟）：**

| 场景 | 行为 |
|------|------|
| 潜行且未按右键 | `raiseShield()` 发包举盾 → `keyUse.setDown(true)` 防止原版释放 |
| 潜行中右键按下瞬间 | `stopUsingItem()` 放盾 → 原版 `handleKeybinds` 处理主手右键 |
| 潜行中右键持续按住 | 不干预，让原版处理主手物品或副手盾牌 |
| 潜行中右键松开 | `raiseShield()` 重新举盾 → `keyUse.setDown(true)` |
| 潜行中攻击 | 放盾攻击 → `Phase.END` 重新举盾 |
| 停止潜行 | `stopUsingItem()` 放下自动举起的盾牌 |

**技术要点：**
- **`gameMode.releaseUsingItem()` 而非 `stopUsingItem()`**：`stopUsingItem()` 是纯本地操作，不通知服务端，会导致服务端仍认为玩家在举盾，主手物品（如弓）的右键事件被服务端忽略。`releaseUsingItem()` 发送 `ServerboundPlayerActionPacket[RELEASE_USE_ITEM]` 数据包，确保服务端同步释放盾牌状态
- **`keyUse.setDown(true)` 防释放**：原版 `handleKeybinds()` 在 `isUsingItem()=true` 且 `keyUse.isDown()=false` 时会调用 `releaseUsingItem()` 释放盾牌。在 `Phase.START` 设置 `keyUse.setDown(true)` 阻止释放，在 `Phase.END` 恢复 `keyUse.setDown(false)`
- **`Phase.START` 在 `handleKeybinds()` 之前**：可在原版处理前调整状态
- **右键按下瞬间放盾**：仅在 `!wasRealKeyUseDown` 边沿触发，避免持续按住时反复放盾举盾
- **`gameMode.useItem()` 发包**：确保服务端同步举盾状态，减伤判定生效

### 5. 盾牌瞬时格挡（Mixin 取消举起延迟）

原版 Minecraft 在 `LivingEntity.isBlocking()` 中硬编码了 5 tick 的盾牌举起延迟：右键举起盾牌后，必须经过 5 tick 才能真正格挡攻击。本模组通过 Mixin `@ModifyConstant` 将该硬编码常量替换为可配置值，默认设为 0，实现盾牌举起后**立即格挡**。

**原版判定逻辑：**

```java
// LivingEntity.isBlocking()
return item.getUseDuration(this.useItem) - this.useItemRemaining >= 5;
```

**Mixin 替换：**

```java
@Mixin(LivingEntity.class)
public class MixinTicksConst {
    @ModifyConstant(method = "isBlocking", constant = @Constant(intValue = 5))
    private int shiledattack$setShieldUseDelay(int constant) {
        if (Config.shieldDelayOverride) {
            return Config.shieldRaiseDelay;
        }
        return constant;
    }
}
```

**延迟取值参考：**

| `shieldRaiseDelay` | 效果 |
|--------------------|------|
| 0（默认） | 瞬间格挡，右键即生效，适合高节奏战斗 |
| 1-2 | 兼顾手感与平衡，保留少量反应窗口 |
| 5 | 等同原版 |

> 服务端侧生效：`isBlocking()` 在 `LivingEntity.hurt()` 中被调用以判定是否减伤，Mixin 注入两侧（无 `client`/`server` 分离声明），确保单机与多人联机一致。

### 6. 盾反（Shield Parry）

举盾后短时间内成功格挡攻击，可触发「盾反」，实现低成本弹攻击者的操作反馈。

**触发条件：**

- 玩家举起盾牌后的 **`parryWindowTicks`（默认 10 tick）内**，成功触发 Forge 的 `ShieldBlockEvent` 事件（即格挡判定成立）
- 当前不处于盾反冷却（冷却为自定义计时，**不占用原版物品冷却**）
- 受损的伤害类型不携带 `shiledattack:unparryable` tag（见下文「无法盾反的伤害类型」）

**盾反效果：**

| 效果 | 说明 |
|------|------|
| 不消耗盾牌耐久 | 通过 `event.setShieldTakesDamage(false)`，本次格挡不扣耐久、不会碎裂 |
| 击退攻击者 | 水平力 = `伤害 ÷ 碰撞体积(宽×高) × parryKnockbackMul`，体型越大越难被推开，并附加 `parryKnockUp` 上抛力度 |
| 播放铁砧音效 | `block.anvil.hit`，音量由 `parrySoundVolume` 控制 |
| 进入冷却 | 冷却时长由 `parryCooldownSeconds`（单位秒）控制，期间无法再次盾反 |

**冷却就绪提示：**当盾反冷却结束后（CD 由满到就绪的那一刻），为玩家本地播放**原版按钮音效** `ui.button.click`（音量同样受 `parrySoundVolume` 控制）作为「可以再次盾反」的提示。

**HUD 指示：**盾反就绪时，在玩家**副手栏左侧**绘制一个护盾图标；处于冷却期间图标隐藏。图标锚定副手栏位渲染，跟随左右手配色自动定位，仅在副手持有盾牌且就绪时显示。

**无法盾反的伤害类型（数据驱动 tag）：**

仿照原版 `minecraft:is_projectile` 这类 damage_type tag 的做法，本模组使用**数据驱动 tag `shiledattack:unparryable`** 决定哪些伤害类型无法被盾反。判定方式为**直接读取伤害来源的 damage_type 是否携带该 tag**（`DamageSource.is(tag)`），而不是从 config 数组读取。这样整合包作者、数据包、KubeJS 都能在运行时动态增删。

Tag 文件位置：`data/shiledattack/tags/damage_type/unparryable.json`

```json
{
  "replace": false,
  "values": [
    "#minecraft:is_explosion"
  ]
}
```

- `values` 中每项是一个 `damage_type` 的 ID，或一个以 `#` 开头的 tag（表示「所有属于该 tag 的伤害类型」）
- 默认阻止爆炸类伤害（`#minecraft:is_explosion`），可自行改删
- 例如想连投射物也无法盾反，追加 `"#minecraft:is_projectile"`
- 留空 `"values": []` 则所有可格挡伤害都能被盾反

**KubeJS 回传事件（可选）：**

安装 [KubeJS](https://modrinth.com/mod/kubejs) 后，本模组会在每次成功盾反时向脚本回传 `ParryEvents.parried` 服务端事件：

```js
ParryEvents.parried(event => {
    // event.player       盾反玩家（ServerPlayer）
    // event.attacker     被盾反击退的攻击者实体（可能为 null）
    // event.damageSource 被格挡的伤害来源
    // event.blockedDamage 被盾反格挡的伤害量
    console.info(`Player ${event.player?.getName()} parried ${event.attacker?.getName()} for ${event.blockedDamage}`);
});
```

> 事件类由模组提供的 KubeJS 插件注册，KubeJS 通过 `kubejs.plugins.txt` 懒加载该插件；未安装 KubeJS 时模组仍可正常运行，只是不触发回调。

## 配置

配置文件位于 `config/shiledattack-common.toml`：

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enabled` | boolean | `true` | 启用盾牌自动收放功能 |
| `holdAttackButton` | boolean | `true` | 启用长按自动攻击功能 |
| `sneakAutoShield` | boolean | `true` | 启用潜行自动举盾功能 |
| `shieldDelayOverride` | boolean | `true` | 启用盾牌举起延迟覆盖（Mixin） |
| `shieldRaiseDelay` | int | `0` | 盾牌举起延迟 tick 数（0=瞬间格挡，5=原版） |
| `parryEnabled` | boolean | `true` | 启用盾反功能 |
| `parryWindowTicks` | int | `10` | 盾反判定时间（单位 tick，举盾后该时长内可触发盾反） |
| `parryCooldownSeconds` | double | `3.0` | 盾反冷却时间（单位秒） |
| `parryKnockbackMul` | double | `1.0` | 盾反击退力倍率（伤害 ÷ 碰撞体积 × 该值） |
| `parryKnockbackMin` | double | `0.4` | 盾反击退的最小水平速度 |
| `parryKnockbackMax` | double | `4.0` | 盾反击退的最大水平速度 |
| `parryKnockUp` | double | `0.35` | 盾反时施加给攻击者的上抛力度 |
| `parrySoundVolume` | double | `1.0` | 盾反铁砧音效音量 |

前四个功能相互独立，可单独开关。`shieldRaiseDelay` 仅在 `shieldDelayOverride=true` 时生效。
盾反相关配置仅需 `parryEnabled=true` 即生效；`parryWindowTicks` 单位为 tick，`parryCooldownSeconds` 单位为秒。

## 技术实现

### 盾牌检测

通过 `forge:tools/shields` 物品标签识别盾牌，兼容各类模组添加的盾牌：

```java
TagKey<Item> SHIELD_TAG = TagKey.create(Registries.ITEM,
    new ResourceLocation("forge", "tools/shields"));
```

### 事件驱动

使用 `ClientTickEvent` 的 `START` 与 `END` 两个阶段实现跨阶段的状态传递：

- **Phase.START**：检测攻击键按下，执行盾牌释放与自动攻击
- **Phase.END**：检测使用键仍按下，重新举起盾牌

通过 `shieldWasReleased` 标志位在同一 tick 内传递状态，保证收盾→攻击→举盾的无缝衔接。

### 长按攻击逻辑

每 tick 检查攻击冷却进度，冷却满且对准实体时调用 `MultiPlayerGameMode.attack()` 发送攻击数据包：

```java
if (mc.player.getAttackStrengthScale(0.5F) >= 1.0F) {
    if (mc.hitResult instanceof EntityHitResult entityHitResult) {
        mc.gameMode.attack(mc.player, entityHitResult.getEntity());
        mc.player.swing(InteractionHand.MAIN_HAND);
        mc.player.resetAttackStrengthTicker();
    }
}
```

## 项目结构

```
src/main/java/com/shiledattack/
├── ShiledAttackMod.java                  # 模组主类，注册配置
├── Config.java                           # Forge 配置定义（含盾反配置）
├── ShiledAttackTags.java                 # 数据驱动 damage_type tag（unparryable）
├── ParryHandler.java                     # 服务端盾反逻辑（触发判定/击退/冷却/音效）
├── ShieldUtil.java                       # 盾牌物品 tag 检测工具
├── event/
│   └── ShieldParriedEvent.java           # 盾反触发时广播的自定义 Forge 事件
├── kubejs/
│   ├── KubeJSParryPlugin.java            # KubeJS 插件（注册事件组并转发 Forge 盾反事件）
│   ├── ParryEvents.java                  # KubeJS 事件组（ParryEvents.parried）
│   └── ShieldParriedEventJS.java         # KubeJS 事件对象（player/attacker/damageSource/blockedDamage）
├── client/
│   └── handler/
│       ├── ShieldAutoHandler.java        # 客户端核心逻辑（自动收放/长按攻击/潜行举盾）
│       └── ParryHudRenderer.java         # 客户端 HUD 盾反就绪图标渲染
├── network/
│   ├── ParryNetwork.java                 # SimpleChannel 网络通道与包注册
│   ├── ParryCooldownMessage.java         # 冷却剩余时间同步包
│   └── ClientParryState.java             # 客户端冷却状态缓存
└── mixin/
    └── MixinTicksConst.java              # 取消盾牌举起 5 tick 延迟（@ModifyConstant）

src/main/resources/
├── shiledattack.mixin.json               # Mixin 配置（声明 Mixin 类与 refmap）
├── kubejs.plugins.txt                    # 声明 KubeJS 插件类（懒加载，可选）
├── data/shiledattack/tags/damage_type/
│   └── unparryable.json                  # 无法盾反的伤害类型 tag（数据驱动）
├── assets/shiledattack/textures/gui/
│   └── parry_shield.png                  # 盾反 HUD 图标
└── META-INF/
    └── mods.toml                         # Forge 模组元数据（含可选的 kubejs 依赖）
```

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/`。

> **KubeJS 编译依赖（可选）**：`kubejs.*` 相关类只在编译时需要，模组核心运行逻辑不依赖 KubeJS。`libs/` 下需放置官方映射（official 1.20.1）的 `kubejs-forge` 与 `rhino-forge` jar 作为 `compileOnly`，它们不会被打进最终产物，运行时由真实安装的 KubeJS 模组提供。

## Mixin 集成

本项目使用 [MixinGradle](https://github.com/SpongePowered/MixinGradle) + SpongePowered Mixin 0.8.5 实现字节码注入。关键集成点：

- **build.gradle**：`buildscript` 引入 `mixingradle:0.7-SNAPSHOT`，`apply plugin: 'org.spongepowered.mixin'`，`mixin {}` 块声明 refmap 与 config
- **annotationProcessor**：`org.spongepowered:mixin:0.8.5:processor` 编译期生成 refmap（Mojmap → SRG 映射）
- **jar manifest**：`TweakClass=org.spongepowered.asm.launch.MixinTweaker` + `MixinConfigs=shiledattack.mixin.json`
- **refmap 验证**：`isBlocking` → SRG `m_21254_()Z`，确保生产环境 Mixin 命中目标方法
