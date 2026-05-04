# Gun Mod Mixin 应用验证指南

**日期**: 2026-05-04  
**JAR**: integratedindustrialcraft-1.0.0.jar (578KB)

---

## Mixin 配置检查 ✅

### 1. Mixin配置文件

```bash
# 检查JAR中的Mixin配置
unzip -l build/libs/integratedindustrialcraft-1.0.0.jar | grep gunmod
```

**结果**:
```
cn/minerealms/iic/mixin/gunmod/
cn/minerealms/iic/mixin/gunmod/GunModMixinPlugin.class
cn/minerealms/iic/mixin/gunmod/ServerPlayHandlerMixin.class
cn/minerealms/iic/mixin/gunmod/WorkbenchBlockEntityMixin.class
cn/minerealms/iic/mixin/gunmod/WorkbenchBlockMixin.class
cn/minerealms/iic/mixin/gunmod/WorkbenchScreenMixin.class
integratedindustrialcraft.mixins.gunmod.json
```

✅ 所有Mixin类和配置文件都已打包

### 2. Mixin配置内容

```json
{
  "required": false,
  "minVersion": "0.8",
  "package": "cn.minerealms.iic.mixin.gunmod",
  "compatibilityLevel": "JAVA_17",
  "refmap": "integratedindustrialcraft.refmap.json",
  "plugin": "cn.minerealms.iic.mixin.gunmod.GunModMixinPlugin",
  "mixins": [
    "WorkbenchBlockEntityMixin",
    "WorkbenchBlockMixin",
    "ServerPlayHandlerMixin"
  ],
  "client": [
    "WorkbenchScreenMixin"
  ],
  "injectors": {
    "defaultRequire": 0
  },
  "verbose": true,
  "priority": 900
}
```

**关键配置**:
- ✅ `required: false` - Mixin配置是可选的
- ✅ `plugin: GunModMixinPlugin` - 使用条件加载插件
- ✅ `defaultRequire: 0` - 所有注入都是可选的
- ✅ `verbose: true` - 启用详细日志
- ✅ `priority: 900` - 较高优先级

### 3. mods.toml配置

```toml
[[mixins]]
config="integratedindustrialcraft.mixins.json"

[[mixins]]
config="integratedindustrialcraft.mixins.gunmod.json"
```

✅ Gun Mod Mixin配置已注册

---

## Mixin目标类检查 ✅

### 使用@Pseudo和targets

所有Mixin都使用`@Pseudo`注解和`targets`直接指向真实的Gun Mod类：

#### ServerPlayHandlerMixin
```java
@Pseudo
@Mixin(targets = "com.mrcrayfish.guns.common.network.ServerPlayHandler", remap = false)
```
**目标**: `com.mrcrayfish.guns.common.network.ServerPlayHandler`

#### WorkbenchBlockEntityMixin
```java
@Pseudo
@Mixin(targets = "com.mrcrayfish.guns.blockentity.WorkbenchBlockEntity", remap = false)
```
**目标**: `com.mrcrayfish.guns.blockentity.WorkbenchBlockEntity`

#### WorkbenchBlockMixin
```java
@Pseudo
@Mixin(targets = "com.mrcrayfish.guns.block.WorkbenchBlock", remap = false)
```
**目标**: `com.mrcrayfish.guns.block.WorkbenchBlock`

#### WorkbenchScreenMixin
```java
@Pseudo
@Mixin(targets = "com.mrcrayfish.guns.client.screen.WorkbenchScreen", remap = false)
```
**目标**: `com.mrcrayfish.guns.client.screen.WorkbenchScreen`

---

## 运行时验证方法

### 方法1: 检查日志（推荐）

启动游戏后，在日志中搜索以下内容：

#### 1. Gun Mod检测
```
[IIC Gun Mod Integration] Gun Mod detected: true/false
```

#### 2. Mixin应用日志
如果Gun Mod存在：
```
[IIC Gun Mod Integration] Applying ServerPlayHandlerMixin to com.mrcrayfish.guns.common.network.ServerPlayHandler
[IIC Gun Mod Integration] Applying WorkbenchBlockEntityMixin to com.mrcrayfish.guns.blockentity.WorkbenchBlockEntity
[IIC Gun Mod Integration] Applying WorkbenchBlockMixin to com.mrcrayfish.guns.block.WorkbenchBlock
[IIC Gun Mod Integration] Applying WorkbenchScreenMixin to com.mrcrayfish.guns.client.screen.WorkbenchScreen
```

如果Gun Mod不存在：
```
[IIC Gun Mod Integration] Skipping ServerPlayHandlerMixin (Gun Mod not loaded)
[IIC Gun Mod Integration] Skipping WorkbenchBlockEntityMixin (Gun Mod not loaded)
[IIC Gun Mod Integration] Skipping WorkbenchBlockMixin (Gun Mod not loaded)
[IIC Gun Mod Integration] Skipping WorkbenchScreenMixin (Gun Mod not loaded)
```

#### 3. Mixin成功应用
```
[IIC Gun Mod Integration] Pre-apply: ServerPlayHandlerMixin -> com.mrcrayfish.guns.common.network.ServerPlayHandler
[IIC Gun Mod Integration] Successfully applied: ServerPlayHandlerMixin -> com.mrcrayfish.guns.common.network.ServerPlayHandler
```

### 方法2: 游戏内测试

如果Gun Mod已安装：

1. **放置Gun Mod工作台**
2. **打开工作台GUI**
   - 应该看到能量条（底部青色条）
   - 应该显示 "Energy: 0 / 10000 EU"
3. **尝试制作物品**
   - 如果没有能量，应该无法制作
   - 如果有能量，应该看到进度条（绿色）
   - 应该显示剩余时间 "X.Xs"

### 方法3: 使用Mixin调试

在JVM参数中添加：
```
-Dmixin.debug.verbose=true
-Dmixin.debug.export=true
```

这会在`run/.mixin.out/`目录下生成混淆后的类文件，可以反编译查看Mixin是否应用。

---

## 预期行为

### 场景1: Gun Mod已安装

1. **启动阶段**:
   - ✅ GunModMixinPlugin检测到Gun Mod
   - ✅ 所有4个Mixin被应用
   - ✅ 日志显示成功应用的消息

2. **游戏内**:
   - ✅ 工作台GUI显示能量条和进度条
   - ✅ 制作需要消耗能量
   - ✅ 制作显示进度和剩余时间
   - ✅ 能量不足时无法制作

### 场景2: Gun Mod未安装

1. **启动阶段**:
   - ✅ GunModMixinPlugin检测不到Gun Mod
   - ✅ 所有4个Mixin被跳过
   - ✅ 日志显示跳过的消息
   - ✅ 游戏正常启动，无错误

2. **游戏内**:
   - ✅ 没有Gun Mod工作台
   - ✅ 其他功能正常工作

---

## 故障排查

### 问题1: Mixin未应用

**症状**: 日志中没有看到Mixin应用的消息

**检查**:
1. 确认`integratedindustrialcraft.mixins.gunmod.json`在JAR中
2. 确认`mods.toml`中注册了Gun Mod Mixin配置
3. 检查Gun Mod的mod ID是否为`cgm`

**解决**:
```bash
# 检查Gun Mod的mod ID
unzip -p mods/cgm-*.jar META-INF/mods.toml | grep modId
```

### 问题2: 目标类找不到

**症状**: 日志中显示"Target class not found"

**原因**: Gun Mod的类名可能不同

**解决**:
1. 反编译Gun Mod JAR
2. 查找正确的类名
3. 更新Mixin中的`targets`

### 问题3: 方法签名不匹配

**症状**: 日志中显示"Method signature mismatch"

**原因**: Gun Mod版本不同，方法签名改变

**解决**:
1. 检查Gun Mod版本
2. 反编译查看正确的方法签名
3. 更新Mixin中的方法参数

---

## 验证清单

在发布前，请确认以下所有项：

- [ ] JAR中包含所有Gun Mod Mixin类
- [ ] JAR中包含`integratedindustrialcraft.mixins.gunmod.json`
- [ ] `mods.toml`中注册了Gun Mod Mixin配置
- [ ] 所有Mixin使用`@Pseudo`注解
- [ ] 所有Mixin使用`targets`指向真实类
- [ ] 所有Mixin使用`remap = false`
- [ ] 所有Mixin使用`require = 0`
- [ ] GunModMixinPlugin正确检测Gun Mod
- [ ] 启动日志显示Mixin应用状态
- [ ] Gun Mod存在时功能正常
- [ ] Gun Mod不存在时游戏正常启动

---

## 测试环境

### 推荐测试配置

1. **有Gun Mod**:
   - Minecraft 1.20.1
   - Forge 47.1.3
   - MrCrayfish's Gun Mod (cgm)
   - IntegratedIndustrialCraft 1.0.0

2. **无Gun Mod**:
   - Minecraft 1.20.1
   - Forge 47.1.3
   - IntegratedIndustrialCraft 1.0.0

### 测试步骤

#### 测试1: 有Gun Mod
1. 安装Gun Mod和IIC
2. 启动游戏
3. 检查日志中的Mixin应用消息
4. 进入游戏，放置工作台
5. 打开工作台GUI，验证能量条显示
6. 尝试制作物品，验证进度系统

#### 测试2: 无Gun Mod
1. 只安装IIC
2. 启动游戏
3. 检查日志中的Mixin跳过消息
4. 验证游戏正常启动，无错误
5. 验证其他IIC功能正常

---

## 日志示例

### 成功应用（有Gun Mod）

```
[main/INFO] [IIC Gun Mod Integration]: Gun Mod detected: true
[main/INFO] [IIC Gun Mod Integration]: Mixin targets: [com.mrcrayfish.guns.common.network.ServerPlayHandler, com.mrcrayfish.guns.blockentity.WorkbenchBlockEntity, com.mrcrayfish.guns.block.WorkbenchBlock, com.mrcrayfish.guns.client.screen.WorkbenchScreen]
[main/INFO] [IIC Gun Mod Integration]: Applying ServerPlayHandlerMixin to com.mrcrayfish.guns.common.network.ServerPlayHandler
[main/INFO] [IIC Gun Mod Integration]: Pre-apply: ServerPlayHandlerMixin -> com.mrcrayfish.guns.common.network.ServerPlayHandler
[main/INFO] [IIC Gun Mod Integration]: Successfully applied: ServerPlayHandlerMixin -> com.mrcrayfish.guns.common.network.ServerPlayHandler
[main/INFO] [IIC Gun Mod Integration]: Applying WorkbenchBlockEntityMixin to com.mrcrayfish.guns.blockentity.WorkbenchBlockEntity
[main/INFO] [IIC Gun Mod Integration]: Pre-apply: WorkbenchBlockEntityMixin -> com.mrcrayfish.guns.blockentity.WorkbenchBlockEntity
[main/INFO] [IIC Gun Mod Integration]: Successfully applied: WorkbenchBlockEntityMixin -> com.mrcrayfish.guns.blockentity.WorkbenchBlockEntity
[main/INFO] [IIC Gun Mod Integration]: Applying WorkbenchBlockMixin to com.mrcrayfish.guns.block.WorkbenchBlock
[main/INFO] [IIC Gun Mod Integration]: Pre-apply: WorkbenchBlockMixin -> com.mrcrayfish.guns.block.WorkbenchBlock
[main/INFO] [IIC Gun Mod Integration]: Successfully applied: WorkbenchBlockMixin -> com.mrcrayfish.guns.block.WorkbenchBlock
[Render thread/INFO] [IIC Gun Mod Integration]: Applying WorkbenchScreenMixin to com.mrcrayfish.guns.client.screen.WorkbenchScreen
[Render thread/INFO] [IIC Gun Mod Integration]: Pre-apply: WorkbenchScreenMixin -> com.mrcrayfish.guns.client.screen.WorkbenchScreen
[Render thread/INFO] [IIC Gun Mod Integration]: Successfully applied: WorkbenchScreenMixin -> com.mrcrayfish.guns.client.screen.WorkbenchScreen
```

### 跳过应用（无Gun Mod）

```
[main/INFO] [IIC Gun Mod Integration]: Gun Mod detected: false
[main/INFO] [IIC Gun Mod Integration]: Skipping ServerPlayHandlerMixin (Gun Mod not loaded)
[main/INFO] [IIC Gun Mod Integration]: Skipping WorkbenchBlockEntityMixin (Gun Mod not loaded)
[main/INFO] [IIC Gun Mod Integration]: Skipping WorkbenchBlockMixin (Gun Mod not loaded)
[Render thread/INFO] [IIC Gun Mod Integration]: Skipping WorkbenchScreenMixin (Gun Mod not loaded)
```

---

**开发者**: CARIERX  
**AI辅助**: Claude Opus 4.6 (1M context)  
**完成日期**: 2026-05-04
