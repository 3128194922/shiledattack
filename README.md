# Shield Auto Attack

一个 Minecraft Forge 模组，提供盾牌自动收放与长按自动攻击功能。

## 参考MOD
https://github.com/Revvilo/responsive-shields/tree/1.18.x-1.20.4?tab=License-1-ov-file

## 环境要求

| 项目 | 版本 |
|------|------|
| Minecraft | 1.20.1 |
| Forge | 47.4.6+ |
| Java | 17 |

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

## 配置

配置文件位于 `config/shiledattack-common.toml`：

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enabled` | boolean | `true` | 启用盾牌自动收放功能 |
| `holdAttackButton` | boolean | `true` | 启用长按自动攻击功能 |
| `sneakAutoShield` | boolean | `true` | 启用潜行自动举盾功能 |

三个功能相互独立，可单独开关。

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
├── Config.java                           # Forge 配置定义
└── client/
    └── handler/
        └── ShieldAutoHandler.java        # 客户端核心逻辑
```

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/`。
