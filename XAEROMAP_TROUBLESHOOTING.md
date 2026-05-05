# XaerosWorldMap 集成故障排查

## 问题：游戏中没有污染叠加层

### 步骤 1: 确认使用最新的 JAR 文件

1. **复制新的 JAR 文件**:
   ```bash
   # 源文件
   H:\MinecraftMods\ImprovedMobs\build\libs\integratedindustrialcraft-1.0.0.jar
   
   # 目标位置
   G:\MinecraftGames\CTNH-BaopuEdition\.minecraft\versions\GregTech Odyssey\mods\
   ```

2. **删除旧文件**（如果存在）:
   - 删除任何旧版本的 `integratedindustrialcraft-*.jar`
   - 确保只有一个版本

3. **清除 Mixin 缓存**:
   ```bash
   # 删除 Mixin 缓存目录
   rm -rf "G:/MinecraftGames/CTNH-BaopuEdition/.minecraft/versions/GregTech Odyssey/.mixin.out"
   ```

### 步骤 2: 重启游戏并检查日志

1. **完全关闭游戏**（不是退出到主菜单）

2. **重新启动游戏**

3. **检查日志**:
   ```bash
   # 查找 MixinPlugin 日志
   grep "IIC-MixinPlugin" "G:/MinecraftGames/CTNH-BaopuEdition/.minecraft/versions/GregTech Odyssey/logs/latest.log"
   ```

   **预期输出**:
   ```
   [IIC-MixinPlugin] Loading mixin plugin for package: cn.minerealms.iic.mixin
   [IIC-MixinPlugin] Checking XaerosWorldMap mixin: cn.minerealms.iic.mixin.xaeromap.GuiMapMixin -> Target: xaero.map.gui.GuiMap -> Present: true
   [IIC-MixinPlugin] Applying mixin: cn.minerealms.iic.mixin.xaeromap.GuiMapMixin to xaero.map.gui.GuiMap
   [IIC-MixinPlugin] Successfully applied mixin: cn.minerealms.iic.mixin.xaeromap.GuiMapMixin to xaero.map.gui.GuiMap
   ```

   **如果看到错误**:
   ```
   [ERROR]: Critical problem: integratedindustrialcraft.mixins.json:xaeromap.GuiMapAccessor target xaero.map.gui.GuiMap was loaded too early.
   ```
   说明还在使用旧版本的 JAR 文件。

### 步骤 3: 检查配置

确认配置文件启用了污染叠加层：

```bash
# 配置文件位置
G:\MinecraftGames\CTNH-BaopuEdition\.minecraft\versions\GregTech Odyssey\config\triaxis-difficulty.properties
```

**必需的配置**:
```properties
enablePollutionMapOverlay=true
pollutionOverlayAlpha=0.4
showPollutionTooltip=true
```

### 步骤 4: 验证功能

1. **进入游戏世界**

2. **建造一些机器**（至少 10 台 MV 机器）

3. **运行机器 5-10 分钟**产生污染

4. **检查污染值**:
   ```
   /im industrial status
   ```
   应该显示非零的污染值

5. **打开世界地图**:
   - 按 `M` 键
   - 应该看到彩色的污染叠加层
   - 鼠标悬停应显示污染值

### 常见问题

#### 问题 1: "Critical problem: was loaded too early"

**原因**: 使用了旧版本的 JAR 文件（包含 GuiMapAccessor）

**解决**:
1. 删除旧的 JAR 文件
2. 复制新的 JAR 文件（构建时间应该是最新的）
3. 删除 `.mixin.out` 缓存目录
4. 重启游戏

#### 问题 2: 没有 MixinPlugin 日志输出

**原因**: Mixin 配置文件可能损坏或未正确打包

**解决**:
1. 验证 JAR 文件内容:
   ```bash
   unzip -l integratedindustrialcraft-1.0.0.jar | grep "mixins.json"
   ```
   应该看到 `integratedindustrialcraft.mixins.json`

2. 检查 mixin 配置:
   ```bash
   unzip -p integratedindustrialcraft-1.0.0.jar integratedindustrialcraft.mixins.json
   ```
   应该包含:
   ```json
   {
     "plugin": "cn.minerealms.iic.mixin.MixinPlugin",
     "client": [
       "enhancedvisuals.VisualManagerMixin",
       "xaeromap.GuiMapMixin"
     ]
   }
   ```

#### 问题 3: Mixin 应用成功但没有叠加层

**原因**: 可能没有污染数据或配置禁用了叠加层

**解决**:
1. 检查配置 `enablePollutionMapOverlay=true`
2. 使用 `/im industrial status` 确认有污染数据
3. 检查日志是否有渲染错误

#### 问题 4: XaerosWorldMap 未安装

**原因**: MixinPlugin 会检测 XaerosWorldMap 是否存在

**解决**:
1. 确认 `XaerosWorldMap_*.jar` 在 mods 目录
2. 检查日志:
   ```
   [IIC-MixinPlugin] Class not found: xaero.map.gui.GuiMap
   ```
   如果看到这个，说明 XaerosWorldMap 未安装

### 调试命令

```bash
# 检查 JAR 文件时间戳
ls -lh "G:/MinecraftGames/CTNH-BaopuEdition/.minecraft/versions/GregTech Odyssey/mods/integratedindustrialcraft-1.0.0.jar"

# 检查 JAR 文件内容
unzip -l "G:/MinecraftGames/CTNH-BaopuEdition/.minecraft/versions/GregTech Odyssey/mods/integratedindustrialcraft-1.0.0.jar" | grep -i "GuiMap"

# 检查 Mixin 配置
unzip -p "G:/MinecraftGames/CTNH-BaopuEdition/.minecraft/versions/GregTech Odyssey/mods/integratedindustrialcraft-1.0.0.jar" integratedindustrialcraft.mixins.json

# 检查最新日志
tail -100 "G:/MinecraftGames/CTNH-BaopuEdition/.minecraft/versions/GregTech Odyssey/logs/latest.log" | grep -i "iic\|xaero\|mixin"

# 检查污染状态（游戏内）
/im industrial status
/im industrial debug on
```

### 验证清单

- [ ] 新的 JAR 文件已复制到 mods 目录
- [ ] 旧的 JAR 文件已删除
- [ ] `.mixin.out` 缓存已清除
- [ ] 游戏已完全重启
- [ ] 日志显示 "Successfully applied mixin: GuiMapMixin"
- [ ] 没有 "was loaded too early" 错误
- [ ] 配置文件 `enablePollutionMapOverlay=true`
- [ ] 游戏中有污染数据（`/im industrial status`）
- [ ] 打开地图能看到叠加层
- [ ] 鼠标悬停显示污染值

### 如果仍然无效

请提供以下信息：

1. **JAR 文件信息**:
   ```bash
   ls -lh "G:/MinecraftGames/CTNH-BaopuEdition/.minecraft/versions/GregTech Odyssey/mods/integratedindustrialcraft-1.0.0.jar"
   md5sum "G:/MinecraftGames/CTNH-BaopuEdition/.minecraft/versions/GregTech Odyssey/mods/integratedindustrialcraft-1.0.0.jar"
   ```

2. **Mixin 配置**:
   ```bash
   unzip -p "G:/MinecraftGames/CTNH-BaopuEdition/.minecraft/versions/GregTech Odyssey/mods/integratedindustrialcraft-1.0.0.jar" integratedindustrialcraft.mixins.json
   ```

3. **日志片段**:
   ```bash
   grep -A 5 -B 5 "integratedindustrialcraft\|IIC-MixinPlugin\|xaeromap" "G:/MinecraftGames/CTNH-BaopuEdition/.minecraft/versions/GregTech Odyssey/logs/latest.log"
   ```

4. **配置文件**:
   ```bash
   cat "G:/MinecraftGames/CTNH-BaopuEdition/.minecraft/versions/GregTech Odyssey/config/triaxis-difficulty.properties" | grep -i "overlay"
   ```
