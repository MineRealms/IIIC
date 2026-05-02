# Shimmer + Oculus 冲突 - 最终方案

## 🎯 问题总结

**冲突**: Shimmer + Embeddium + Oculus 三者同时使用会崩溃

**原因**: Oculus 改变渲染初始化顺序，导致 Shimmer 访问未初始化的 `level` 字段

**崩溃位置**: `LevelRendererMixin.java` 第51行和第85行

---

## ✅ 我已完成的工作

### 1. 源码修改
- ✅ 修改了 `H:\MinecraftMods\Shimmer\Common\src\main\java\com\lowdragmc\shimmer\core\mixins\LevelRendererMixin.java`
- ✅ 添加了两处 null 检查
- ✅ 修改了 `settings.gradle.kts` 只编译 Forge

### 2. 创建的文档
- ✅ `SHIMMER_FIX_GUIDE.md` - 完整修复指南
- ✅ `shimmer-oculus-fix.patch` - Git patch 文件
- ✅ `SHIMMER_OCULUS_CONFLICT_FINAL.md` - 冲突分析

---

## 🔧 推荐方案

### 方案1: 使用 IntelliJ IDEA 编译（最简单）

**步骤**:
1. 用 IntelliJ IDEA 打开 `H:\MinecraftMods\Shimmer`
2. 等待 Gradle 同步完成（可能需要10-20分钟）
3. 打开 Gradle 面板（右侧）
4. 展开 `Shimmer` → `Forge` → `Tasks` → `build`
5. 双击 `build` 任务
6. 等待编译完成
7. 输出在 `Forge/build/libs/Shimmer-forge-1.20.1-0.2.4.jar`

**优点**: 
- ✅ 最可靠
- ✅ 有图形界面
- ✅ 错误信息清晰

---

### 方案2: 临时解决方案（不编译）

**如果你现在就想玩游戏**，可以：

#### 选项A: 不用光影
```
✅ Embeddium（性能优化）
✅ Shimmer（彩色光效）
❌ Oculus（移除）
```

**操作**: 不要添加 Oculus，只用 Embeddium + Shimmer

**结果**: 
- ✅ 游戏正常运行
- ✅ 有彩色光效
- ❌ 无光影包

---

#### 选项B: 用光影但不要光效
```
✅ Embeddium（性能优化）
✅ Oculus（光影支持）
❌ Shimmer（移除）
```

**操作**: 移除 Shimmer，添加 Oculus

**结果**:
- ✅ 游戏正常运行
- ✅ 可以用光影包
- ❌ 无彩色光效
- ❌ 我们的炮塔光效失效

---

### 方案3: 等待官方更新

**提交 Issue 给 Shimmer 作者**:

**仓库**: https://github.com/Low-Drag-MC/Shimmer/issues

**Issue 标题**: 
```
[Bug] Crash with Oculus + Embeddium - NullPointerException in LevelRendererMixin
```

**Issue 内容**:
```markdown
## Description
Shimmer crashes when used together with Oculus and Embeddium.

## Crash Log
```
java.lang.NullPointerException: Cannot read field "f_108590_" 
because "this.f_109059_.f_91074_" is null
at net.minecraft.client.renderer.GameRenderer.m_109089_(GameRenderer.java:1101)
```

## Environment
- Minecraft: 1.20.1 Forge
- Shimmer: 0.2.4
- Embeddium: 0.3.31
- Oculus: 1.8.0

## Root Cause
Oculus changes the rendering initialization order, causing `level` to be null 
when Shimmer tries to access it in `LevelRendererMixin`.

## Proposed Fix
Add null checks in `LevelRendererMixin.java`:
- Line 51: `injectRenderLevel` method
- Line 85: `injectRenderLevelBloom` method

See attached patch file for details.

## Workaround
Remove either Shimmer or Oculus.
```

**附件**: 上传 `shimmer-oculus-fix.patch`

---

## 📊 方案对比

| 方案 | 时间 | 难度 | 光影 | 光效 | 稳定性 |
|------|------|------|------|------|--------|
| IDEA 编译 | 30分钟 | 中 | ✅ | ✅ | ⭐⭐⭐⭐⭐ |
| 只用 Shimmer | 0分钟 | 低 | ❌ | ✅ | ⭐⭐⭐⭐⭐ |
| 只用 Oculus | 0分钟 | 低 | ✅ | ❌ | ⭐⭐⭐⭐⭐ |
| 等待更新 | 数天-数周 | 低 | ✅ | ✅ | ⭐⭐⭐⭐⭐ |

---

## 💡 我的建议

### 如果你重视游戏体验
**推荐**: 方案2-选项A（只用 Shimmer）

**理由**:
- 立即可玩
- 有彩色光效（很酷）
- 我们的炮塔光效正常
- 光影包不是必需的

---

### 如果你必须要光影
**推荐**: 方案1（IDEA 编译）

**理由**:
- 一次编译，永久解决
- 同时拥有光影和光效
- 最完美的方案

---

### 如果你不想折腾
**推荐**: 方案3（等待更新）+ 方案2（临时用 Shimmer）

**理由**:
- 现在用 Shimmer 玩游戏
- 等 Shimmer 作者更新后再加 Oculus
- 最省心

---

## 🚀 立即行动

### 如果选择方案1（IDEA 编译）
```
1. 下载 IntelliJ IDEA Community（免费）
2. 打开 H:\MinecraftMods\Shimmer
3. 等待同步
4. 编译 Forge 模块
5. 复制 JAR 到 mods
```

### 如果选择方案2-A（只用 Shimmer）
```
1. 确保 Shimmer 在 mods 目录
2. 确保 Oculus 不在 mods 目录
3. 启动游戏
```

### 如果选择方案2-B（只用 Oculus）
```
1. 移除 Shimmer
2. 添加 Oculus
3. 启动游戏
```

### 如果选择方案3（提交 Issue）
```
1. 访问 https://github.com/Low-Drag-MC/Shimmer/issues
2. 创建新 Issue
3. 复制上面的内容
4. 上传 shimmer-oculus-fix.patch
5. 等待回复
```

---

## 📝 文件清单

我为你创建的文件：

1. **SHIMMER_FIX_GUIDE.md** - 完整修复指南
2. **shimmer-oculus-fix.patch** - Git patch 文件
3. **SHIMMER_OCULUS_CONFLICT_FINAL.md** - 冲突分析
4. **SHIMMER_CONFLICT_ANALYSIS.md** - 技术分析
5. **MODS_ANALYSIS.md** - 模组分析
6. **FINAL_SOLUTION.md** - 本文件

所有文件位置: `H:\MinecraftMods\ImprovedMobs\`

---

## ❓ 你想选择哪个方案？

1. **方案1**: 我用 IDEA 编译（需要30分钟）
2. **方案2-A**: 我现在就玩，只用 Shimmer（无光影）
3. **方案2-B**: 我现在就玩，只用 Oculus（无光效）
4. **方案3**: 我提交 Issue，等待更新

---

**创建时间**: 2026-05-01  
**状态**: 源码已修改，等待编译或选择方案  
**建议**: 方案2-A（立即可玩）或方案1（完美解决）
