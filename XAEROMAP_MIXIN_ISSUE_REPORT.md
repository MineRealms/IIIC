# XaerosWorldMap Mixin 集成问题报告

## 问题概述

XaerosWorldMap 污染叠加层的 Mixin 集成无法正常工作。Mixin 配置已正确设置，代码已实现，但在游戏运行时完全没有效果，且没有任何日志输出。

## 环境信息

- **Minecraft 版本**: 1.20.1
- **Forge 版本**: 47.4.20
- **Mod 版本**: integratedindustrialcraft-1.0.0.jar
- **目标 Mod**: XaerosWorldMap 1.39.12
- **构建时间**: 2026-05-05 10:33
- **部署位置**: `G:\MinecraftGames\CTNH-BaopuEdition\.minecraft\versions\GregTech Odyssey\mods\`

## 症状

1. **游戏中无效果**
   - 打开世界地图（按 M 键）后没有污染叠加层
   - 没有任何按钮或 UI 变化
   - 功能完全不工作

2. **日志完全静默**
   - 没有 `IIC-MixinPlugin` 的任何日志输出
   - 没有 "Checking XaerosWorldMap mixin" 日志
   - 没有 "Successfully applied mixin" 日志
   - 没有任何错误或警告信息

3. **Mod 被正确加载**
   - 日志显示: `Found mod file integratedindustrialcraft-1.0.0.jar`
   - Mod 文件存在且时间戳正确
   - 其他 mod 的 mixin 配置有日志输出（如 phantasm.mixins.json）

## 已完成的修复

### 1. 移除 GuiMapAccessor（早期加载问题）

**问题**: `GuiMapAccessor` 接口在编译时引用 `xaero.map.gui.GuiMap`，导致目标类提前加载。

**修复**:
- 删除 `GuiMapAccessor.java`
- 删除 `PollutionOverlayRenderer.java`（依赖 GuiMapAccessor）
- 从 mixin 配置中移除 `xaeromap.GuiMapAccessor`

**验证**:
```bash
unzip -l integratedindustrialcraft-1.0.0.jar | grep GuiMap
# 结果: 只有 GuiMapMixin.class，没有 GuiMapAccessor.class ✓
```

### 2. 修正字段 Shadow 配置

**问题**: `width` 和 `height` 继承自混淆的 Minecraft `Screen` 类，需要单独指定重映射。

**修复**:
```java
@Shadow(remap = true)
public int width;

@Shadow(remap = true)
public int height;
```

### 3. 调整 Mixin 配置严格性

**修改**: `integratedindustrialcraft.mixins.json`
```json
{
  "required": true,        // 从 false 改为 true
  "injectors": {
    "defaultRequire": 1    // 从 0 改为 1
  }
}
```

**目的**: 让 Mixin 失败时报错而不是静默失败。

## 当前配置状态

### mods.toml
```toml
[[mixins]]
config="integratedindustrialcraft.mixins.json"
```
✓ 正确引用了 mixin 配置

### integratedindustrialcraft.mixins.json
```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "cn.minerealms.iic.mixin",
  "compatibilityLevel": "JAVA_17",
  "refmap": "integratedindustrialcraft.refmap.json",
  "plugin": "cn.minerealms.iic.mixin.MixinPlugin",
  "mixins": [],
  "client": [
    "enhancedvisuals.VisualManagerMixin",
    "xaeromap.GuiMapMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```
✓ 配置语法正确

### MANIFEST.MF
```
MixinConfigs: integratedindustrialcraft.mixins.json,integratedindustrial
 craft.mixins.compat.json,integratedindustrialcraft.mixins.gunmod.json
```
✓ Mixin 配置已在 manifest 中声明

### GuiMapMixin.java
```java
@OnlyIn(Dist.CLIENT)
@Mixin(value = xaero.map.gui.GuiMap.class, remap = false, priority = 2000)
public abstract class GuiMapMixin {
    @Shadow public double cameraX;
    @Shadow public double cameraZ;
    @Shadow public double scale;
    @Shadow(remap = true) public int width;
    @Shadow(remap = true) public int height;
    @Shadow public abstract Minecraft getMinecraft();
    
    @Inject(method = "m_88315_", at = @At("HEAD"), remap = false)
    private void iic$checkDimensionChange(...) { ... }
    
    @Inject(method = "m_88315_", at = @At("TAIL"), remap = false)
    private void iic$renderPollutionOverlay(...) { ... }
}
```
✓ Mixin 代码语法正确

### MixinPlugin.java
```java
public class MixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("IIC-MixinPlugin");
    
    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.info("[IIC-MixinPlugin] Loading mixin plugin for package: {}", mixinPackage);
    }
    
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(".xaeromap.")) {
            boolean present = isClassPresent(XAEROS_WORLDMAP_CLASS);
            LOGGER.info("[IIC-MixinPlugin] Checking XaerosWorldMap mixin: {} -> Present: {}", 
                mixinClassName, present);
            return present;
        }
        return true;
    }
}
```
✓ MixinPlugin 代码正确，应该输出日志

## 关键发现

### 1. MixinPlugin 完全未被调用
- `onLoad()` 方法没有执行（没有 "Loading mixin plugin" 日志）
- `shouldApplyMixin()` 方法没有执行（没有 "Checking XaerosWorldMap mixin" 日志）
- 这表明 **Mixin 系统根本没有加载我们的配置文件**

### 2. 其他 Mod 的 Mixin 正常工作
日志中可以看到其他 mod 的 mixin 配置被处理：
```
[10:33:59] [main/ERROR]: Mixin config phantasm.mixins.json does not specify "minVersion" property
[10:33:59] [main/ERROR]: Mixin config solclassic-sb-compat.mixins.json does not specify "minVersion" property
```

但完全没有 `integratedindustrialcraft.mixins.json` 的任何信息。

### 3. XaerosWorldMap 已加载
```
[10:33:48] [main/INFO]: Found mod file XaerosWorldMap_1.39.12_Forge_1.20.jar
```

### 4. 玩家已打开地图
- 日志显示玩家在游戏中移动
- 用户确认已按 M 键打开世界地图
- 但仍然没有触发 Mixin 加载

## 可能的原因分析

### 理论 1: Mixin 配置未被 Forge 识别
**可能性**: 高

**证据**:
- 日志中完全没有 `integratedindustrialcraft.mixins.json` 的任何提及
- 其他 mod 的 mixin 配置有日志输出
- MixinPlugin 的 `onLoad()` 从未被调用

**可能原因**:
1. `mods.toml` 中的 mixin 配置格式不正确
2. Forge 版本与 Mixin 配置不兼容
3. Mod 加载顺序问题
4. 缺少某个必需的依赖或配置

### 理论 2: Mixin 配置被静默跳过
**可能性**: 中

**证据**:
- `"required": false` 时完全没有日志
- 改为 `"required": true` 后仍然没有日志
- 这表明配置可能在更早的阶段就被跳过了

**可能原因**:
1. `compatibilityLevel` 不匹配
2. `minVersion` 要求不满足
3. Mixin 系统版本问题

### 理论 3: 类加载器隔离问题
**可能性**: 低

**证据**:
- 其他 mod 的 mixin 正常工作
- JAR 文件结构正确

**可能原因**:
1. MixinPlugin 类无法被加载
2. 包名或类路径问题

### 理论 4: Forge 47.4.20 的特殊要求
**可能性**: 中

**证据**:
- 用户使用的是 Forge 47.4.20（较新版本）
- 可能有新的配置要求或限制

**需要验证**:
1. 是否需要额外的 Forge 配置
2. 是否需要特定的 Mixin 版本
3. 是否有新的 mods.toml 格式要求

## 需要进一步调查的方向

### 1. 检查 Forge Mixin 加载机制
- Forge 47.4.20 如何加载 mixin 配置
- `mods.toml` 中 `[[mixins]]` 的正确格式
- 是否需要额外的 manifest 属性

### 2. 对比工作的 Mixin 实现
- 查看其他成功的 Forge mod 的 mixin 配置
- 对比 GregTech CEu 的 XaerosWorldMap mixin 实现
- 检查是否有遗漏的配置步骤

### 3. 启用 Mixin 调试日志
- 添加 JVM 参数: `-Dmixin.debug=true`
- 添加 JVM 参数: `-Dmixin.checks=true`
- 添加 JVM 参数: `-Dmixin.env.disableRefMap=true`

### 4. 验证 MixinGradle 配置
- 检查 `build.gradle` 中的 mixin 配置
- 验证 MixinGradle 插件版本
- 确认 annotation processor 配置

### 5. 测试简化版本
- 创建一个最小化的测试 mixin
- 移除 MixinPlugin，直接应用 mixin
- 测试是否是 MixinPlugin 的问题

## 测试建议

### 测试 1: 添加静态初始化块
在 `GuiMapMixin` 中添加：
```java
static {
    System.out.println("[IIC-DEBUG] GuiMapMixin class loaded!");
}
```

如果这个输出出现，说明类被加载了但 mixin 没有应用。

### 测试 2: 移除 MixinPlugin
临时修改配置，移除 plugin 行：
```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "cn.minerealms.iic.mixin",
  "compatibilityLevel": "JAVA_17",
  "refmap": "integratedindustrialcraft.refmap.json",
  // "plugin": "cn.minerealms.iic.mixin.MixinPlugin",  // 注释掉
  "mixins": [],
  "client": [
    "xaeromap.GuiMapMixin"
  ]
}
```

如果这样能工作，说明问题在 MixinPlugin。

### 测试 3: 创建简单的测试 Mixin
创建一个针对 Minecraft 原版类的简单 mixin：
```java
@Mixin(Screen.class)
public class TestMixin {
    @Inject(method = "init", at = @At("HEAD"))
    private void test(CallbackInfo ci) {
        System.out.println("[IIC-TEST] Mixin working!");
    }
}
```

如果这个能工作，说明 mixin 系统本身没问题，问题在于 XaerosWorldMap 的特殊性。

### 测试 4: 检查 Mixin 版本兼容性
在 `build.gradle` 中明确指定 Mixin 版本：
```gradle
dependencies {
    annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'
    implementation 'org.spongepowered:mixin:0.8.5'
}
```

## 文件清单

### 需要检查的文件
- `src/main/resources/META-INF/mods.toml`
- `src/main/resources/integratedindustrialcraft.mixins.json`
- `src/main/java/cn/minerealms/iic/mixin/MixinPlugin.java`
- `src/main/java/cn/minerealms/iic/mixin/xaeromap/GuiMapMixin.java`
- `build.gradle`

### 已删除的文件
- `src/main/java/cn/minerealms/iic/mixin/xaeromap/GuiMapAccessor.java` ✓
- `src/main/java/cn/minerealms/iic/client/PollutionOverlayRenderer.java` ✓

### JAR 文件内容验证
```bash
# 检查 mixin 配置
unzip -p integratedindustrialcraft-1.0.0.jar integratedindustrialcraft.mixins.json

# 检查 mixin 类
unzip -l integratedindustrialcraft-1.0.0.jar | grep GuiMap

# 检查 manifest
unzip -p integratedindustrialcraft-1.0.0.jar META-INF/MANIFEST.MF

# 检查 mods.toml
unzip -p integratedindustrialcraft-1.0.0.jar META-INF/mods.toml
```

## 参考资源

- [Mixins on Minecraft Forge - SpongePowered Wiki](https://github.com/SpongePowered/Mixin/wiki/Mixins-on-Minecraft-Forge)
- [Forge Mixin Tutorial by TelepathicGrunt](https://gist.github.com/TelepathicGrunt/3784f8a8b317bac11039474012de5fb4)
- [Modding Tutorials - Mixins](https://moddingtutorials.org/mixins/)
- [Mixin Introduction - Forge](https://darkhax.net/2020/07/mixins)

## 下一步行动

1. **让其他 AI 分析**：提供此报告给其他 AI 进行独立分析
2. **对比参考实现**：查看 GregTech CEu 的 XaerosWorldMap mixin 实现
3. **启用调试日志**：添加 Mixin 调试 JVM 参数
4. **简化测试**：创建最小化的测试用例
5. **社区求助**：在 Forge 论坛或 Discord 寻求帮助

## 总结

核心问题是 **Mixin 配置完全没有被 Forge 加载**，导致 MixinPlugin 从未被调用，mixin 从未被应用。所有配置文件语法正确，代码实现正确，但整个 mixin 系统对我们的配置视而不见。这可能是 Forge 47.4.20 的特殊要求、配置格式问题，或者某个关键步骤被遗漏。
