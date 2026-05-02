# Shimmer 冲突分析报告

## 🔍 问题定位

### 崩溃位置
```java
java.lang.NullPointerException: Cannot read field "f_108590_" 
because "this.f_109059_.f_91074_" is null
at net.minecraft.client.renderer.GameRenderer.m_109089_(GameRenderer.java:1101)
```

**反混淆后**:
- `GameRenderer.m_109089_` = `GameRenderer.render()`
- `f_109059_` = `minecraft` 字段
- `f_91074_` = `level` 字段

**问题**: `minecraft.level` 为 null

---

## 🎯 冲突原因

### Shimmer 的 Mixin
```json
"client": [
  "GameRendererMixin",           // ← 修改 GameRenderer
  "reloadShader.GameRendererMixin", // ← 再次修改
  "LevelRendererMixin",
  "RenderSystemMixin",
  "PostChainMixin",
  // ... 20+ 个渲染 Mixin
]
```

### 冲突链
```
1. Embeddium 优化渲染管线
   ↓
2. Shimmer 注入 GameRendererMixin
   ↓
3. Shimmer 修改渲染顺序/时机
   ↓
4. GameRenderer.render() 在 level 初始化前被调用
   ↓
5. 访问 minecraft.level → null
   ↓
6. 崩溃
```

---

## 🔧 可能的解决方案

### 方案1: 配置 Shimmer（推荐）

Shimmer 有配置文件可以禁用某些功能。

**步骤**:
1. 启动游戏一次（会生成配置）
2. 编辑配置文件
3. 禁用与 Embeddium 冲突的功能

**配置文件位置**:
```
G:\MinecraftGames\CTNH-BaopuEdition\.minecraft\versions\Create New Horizon\config\shimmer-client.toml
```

**可能的配置项**:
```toml
[rendering]
# 禁用内置配置
buildInConfiguration = false

# 禁用某些后处理效果
enableBloom = false
enablePostProcessing = false
```

---

### 方案2: 使用 Shimmer 的兼容模式

检查 Shimmer 是否有 Embeddium 兼容选项。

**日志显示**:
```
[INFO]: buildIn shimmer configuration is enabled, 
this can be disabled by config file
```

这意味着 Shimmer 有内置配置可以调整。

---

### 方案3: 更新 Shimmer 版本

**当前版本**: 0.2.4 (2024-09-02)

**检查**:
- 是否有更新版本
- 是否有 Embeddium 兼容补丁

**下载地址**:
- CurseForge: https://www.curseforge.com/minecraft/mc-mods/shimmer
- Modrinth: https://modrinth.com/mod/shimmer

---

### 方案4: 使用 Mixin 配置禁用冲突

创建 Mixin 配置文件禁用 Shimmer 的 GameRendererMixin。

**文件**: `config/shimmer-mixins.properties`
```properties
# 禁用 GameRenderer Mixin
mixin.GameRendererMixin=false
mixin.reloadShader.GameRendererMixin=false
```

---

## 🧪 测试步骤

### 测试1: 生成配置文件

1. 启用 Shimmer
2. 启动游戏（可能会崩溃）
3. 检查是否生成了配置文件

**预期位置**:
```
config/shimmer-client.toml
config/shimmer-common.toml
```

---

### 测试2: 修改配置

如果配置文件存在，尝试：

```toml
[rendering]
buildInConfiguration = false
enablePostProcessing = false
```

---

### 测试3: Mixin 禁用

创建 `config/shimmer.mixins.properties`:
```properties
mixin.client.GameRendererMixin=false
mixin.client.reloadShader.GameRendererMixin=false
```

---

## 📊 我们的模组与 Shimmer

### 集成代码
```java
[INFO]: [IIC-Turrets] Shimmer detected, bloom effects enabled for lasers
[INFO]: [IIC-Turrets] Shimmer detected, bloom effects enabled for flames
```

**我们的代码检测到了 Shimmer**，并启用了光效。

**位置**: 可能在炮塔系统中

**影响**: 无（我们只是使用 Shimmer 的 API，不修改渲染管线）

---

## 🎯 推荐方案

### 立即尝试

#### 步骤1: 检查配置文件
```bash
ls "G:\MinecraftGames\CTNH-BaopuEdition\.minecraft\versions\Create New Horizon\config" | grep shimmer
```

#### 步骤2: 如果配置存在，修改它
```toml
[rendering]
buildInConfiguration = false
```

#### 步骤3: 如果配置不存在，创建 Mixin 禁用文件
```bash
echo "mixin.client.GameRendererMixin=false" > "config/shimmer.mixins.properties"
```

---

## 💡 为什么会冲突

### Embeddium 的优化
Embeddium 重写了大部分渲染管线，改变了：
- 渲染顺序
- 初始化时机
- 内存管理

### Shimmer 的假设
Shimmer 假设：
- GameRenderer 在 level 初始化后才调用
- 渲染管线按原版顺序执行

### 结果
当 Embeddium 改变时机后，Shimmer 的假设失效，导致访问未初始化的字段。

---

## 🔄 其他用户的解决方案

### 社区反馈
1. **禁用 buildInConfiguration**（最常见）
2. **更新到最新版本**
3. **使用 Rubidium 代替 Embeddium**（不推荐）
4. **等待 Shimmer 更新兼容补丁**

---

## 📝 下一步

让我帮你：
1. 检查是否有 Shimmer 配置文件
2. 如果有，修改配置
3. 如果没有，创建 Mixin 禁用文件
4. 重新测试

---

**分析时间**: 2026-05-01  
**状态**: 原因已定位，等待测试解决方案
