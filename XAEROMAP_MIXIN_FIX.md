# XaerosWorldMap Mixin 修复报告

## 问题描述

XaerosWorldMap 污染叠加层集成的 Mixin 代码已实现，但在运行时未生效。经检查发现以下问题：

### 1. Mixin 未注册
**问题**: `GuiMapMixin` 和 `GuiMapAccessor` 已实现，但未在 `integratedindustrialcraft.mixins.json` 中注册。

**原因**: Mixin 配置文件中缺少对 XaerosWorldMap mixin 的引用，导致 Mixin 系统不会加载这些类。

### 2. 字段 Shadow 配置不当
**问题**: `width` 和 `height` 字段继承自 Minecraft 的 `Screen` 类（已混淆），但 Shadow 注解未指定 `remap = true`。

**原因**: XaerosWorldMap 是未混淆的 mod (`remap = false`)，但 `width` 和 `height` 来自混淆的 Minecraft 代码，需要单独指定重映射。

## 修复方案

### 修复 1: 注册 Mixin

**文件**: `src/main/resources/integratedindustrialcraft.mixins.json`

**修改前**:
```json
{
  "required": false,
  "minVersion": "0.8",
  "package": "cn.minerealms.iic.mixin",
  "compatibilityLevel": "JAVA_17",
  "refmap": "integratedindustrialcraft.refmap.json",
  "mixins": [],
  "client": [
    "enhancedvisuals.VisualManagerMixin"
  ],
  "injectors": {
    "defaultRequire": 0
  }
}
```

**修改后**:
```json
{
  "required": false,
  "minVersion": "0.8",
  "package": "cn.minerealms.iic.mixin",
  "compatibilityLevel": "JAVA_17",
  "refmap": "integratedindustrialcraft.refmap.json",
  "plugin": "cn.minerealms.iic.mixin.MixinPlugin",
  "mixins": [],
  "client": [
    "enhancedvisuals.VisualManagerMixin",
    "xaeromap.GuiMapMixin",
    "xaeromap.GuiMapAccessor"
  ],
  "injectors": {
    "defaultRequire": 0
  }
}
```

**关键变更**:
1. 添加 `"plugin"` 字段，指向 `MixinPlugin`（用于条件加载）
2. 在 `"client"` 数组中添加 `"xaeromap.GuiMapMixin"` 和 `"xaeromap.GuiMapAccessor"`

### 修复 2: 修正字段 Shadow

**文件**: `src/main/java/cn/minerealms/iic/mixin/xaeromap/GuiMapMixin.java`

**修改前**:
```java
@Shadow
public int width;

@Shadow
public int height;
```

**修改后**:
```java
@Shadow(remap = true)
public int width;

@Shadow(remap = true)
public int height;
```

**原因说明**:
- `GuiMap` 继承自 `ScreenBase`，后者继承自 Minecraft 的 `Screen` 类
- `Screen` 类是混淆的，字段名在运行时是 `f_96543_` (width) 和 `f_96544_` (height)
- 虽然 Mixin 目标类设置了 `remap = false`，但继承的混淆字段需要单独指定 `remap = true`
- 这样 Mixin 系统会正确地将 `width` 映射到 `f_96543_`，`height` 映射到 `f_96544_`

## 技术细节

### Mixin 条件加载

`MixinPlugin` 已正确实现条件加载逻辑：

```java
@Override
public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
    // Check if XaerosWorldMap mixins should be applied
    if (mixinClassName.contains(".xaeromap.")) {
        boolean present = isClassPresent(XAEROS_WORLDMAP_CLASS);
        LOGGER.info("[IIC-MixinPlugin] Checking XaerosWorldMap mixin: {} -> Target: {} -> Present: {}",
            mixinClassName, targetClassName, present);
        return present;
    }
    return true;
}
```

- 只有当 XaerosWorldMap 存在时才应用 mixin
- 避免在没有 XaerosWorldMap 的环境中加载失败

### 混淆与非混淆混合

这是一个典型的混淆/非混淆混合场景：

| 类/字段 | 来源 | 混淆状态 | Mixin 配置 |
|---------|------|----------|------------|
| `GuiMap` | XaerosWorldMap | 未混淆 | `remap = false` |
| `cameraX`, `cameraZ`, `scale` | GuiMap | 未混淆 | `@Shadow` (默认) |
| `width`, `height` | Screen (Minecraft) | 已混淆 | `@Shadow(remap = true)` |
| `getMinecraft()` | GuiMap | 未混淆 | `@Shadow` (默认) |

### 注入点验证

通过反编译 XaerosWorldMap 源码验证注入点：

```java
// xaero/map/gui/GuiMap.java
public void m_7856_() {  // init 方法
    // ...
}

public void m_88315_(GuiGraphics guiGraphics, int scaledMouseX, int scaledMouseY, float partialTicks) {  // render 方法
    // ...
}
```

我们的 Mixin 注入：
- `@Inject(method = "m_88315_", at = @At("HEAD"))` - 在渲染开始时检查维度变化
- `@Inject(method = "m_88315_", at = @At("TAIL"))` - 在渲染结束时绘制污染叠加层

## 验证步骤

### 1. 编译验证
```bash
./gradlew build --no-daemon
```

预期结果：BUILD SUCCESSFUL

### 2. 运行时验证

启动游戏后检查日志：

```
[IIC-MixinPlugin] Checking XaerosWorldMap mixin: cn.minerealms.iic.mixin.xaeromap.GuiMapMixin -> Target: xaero.map.gui.GuiMap -> Present: true
[IIC-MixinPlugin] Applying mixin: cn.minerealms.iic.mixin.xaeromap.GuiMapMixin to xaero.map.gui.GuiMap
[IIC-MixinPlugin] Successfully applied mixin: cn.minerealms.iic.mixin.xaeromap.GuiMapMixin to xaero.map.gui.GuiMap
```

### 3. 功能验证

1. 按 `M` 键打开世界地图
2. 污染叠加层应自动显示（如果 `enablePollutionMapOverlay = true`）
3. 鼠标悬停在污染区块上应显示污染值
4. 地图缩放和移动时叠加层应正确跟随

## 参考实现

GregTech CEu 的 XaerosWorldMap 集成采用了类似的方法：

**文件**: `com/gregtechceu/gtceu/core/mixins/xaeroworldmap/GuiMapMixin.java`

```java
@Mixin(value = GuiMap.class, remap = false)
public abstract class GuiMapMixin extends ScreenBase implements IRightClickableElement {
    // 通过继承 ScreenBase 获得对 width/height 的访问
    // 这是另一种解决方案，但需要导入更多 XaerosWorldMap 类
}
```

我们的实现选择了更轻量的方案：
- 不继承 ScreenBase（避免导入额外类）
- 直接 Shadow 需要的字段（更精确的控制）
- 使用 `remap = true` 处理混淆字段（更明确的意图）

## 总结

修复涉及两个关键点：

1. **Mixin 注册**: 在配置文件中正确注册 mixin 类
2. **字段重映射**: 对继承自混淆类的字段使用 `@Shadow(remap = true)`

这两个修复确保了 Mixin 系统能够：
- 找到并加载 mixin 类
- 正确映射混淆字段名
- 成功注入到目标方法

修复后，XaerosWorldMap 污染叠加层集成应能正常工作。
