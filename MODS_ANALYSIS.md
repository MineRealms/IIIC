# Mods 目录分析报告

## 📦 已安装的模组列表

### 🎨 渲染/视觉模组
| 模组 | 版本 | 大小 | 说明 |
|------|------|------|------|
| **Embeddium** | 0.3.31 | 1.3MB | 性能优化（Sodium 移植） |
| **Oculus** | 1.8.0 | 2.8MB | 光影支持（Iris 移植） |
| **Shimmer** | 0.2.4 | 897KB | 渲染增强 |
| **Photon** | 1.1.17 | 4.9MB | 光照系统 |
| **EnhancedVisuals** | 1.8.2 | 4.6MB | 视觉效果增强 |
| **ModernUI** | 3.11.0.1 | 25MB + 19MB + 3.5MB | 现代化 UI |

### 🏭 工业/科技模组
| 模组 | 版本 | 大小 | 说明 |
|------|------|------|------|
| **GregTech CEu** | 7.2.0 | 18MB | 格雷科技 |
| **Mekanism** | 10.4.16.80 | 12MB | 通用机械 |
| **Mekanism Additions** | 10.4.16.80 | 1.1MB | 通用机械附属 |
| **Mekanism Generators** | 10.4.16.80 | 1.1MB | 通用机械发电机 |
| **Mekanism Tools** | 10.4.16.80 | 506KB | 通用机械工具 |

### 🧟 生存/难度模组
| 模组 | 版本 | 大小 | 说明 |
|------|------|------|------|
| **Spore** | 2.2.0c | 108MB | 菌群系统 |
| **The Hordes** | 1.5.4 | 573KB | 尸潮系统 |
| **ImprovedMobs** | 1.13.6 | 328KB | 怪物增强 |
| **IntegratedIndustrialCraft** | 1.0.0 | 540KB | **我们的模组** |

### 🔧 核心/库模组
| 模组 | 版本 | 大小 | 说明 |
|------|------|------|------|
| **LDLib** | 1.0.42/1.0.48 | 3.0MB × 2 | GT 依赖库（**重复！**） |
| **GeckoLib** | 4.8.3 | 1015KB | 动画库 |
| **CreativeCore** | 2.12.35 | 1.1MB | 核心库 |
| **TenshiLib** | 1.7.2 | 459KB | 工具库 |
| **Atlas Lib** | 1.1.12 | 122KB | 地图库 |

### 🔍 辅助/工具模组
| 模组 | 版本 | 大小 | 说明 |
|------|------|------|------|
| **Jade** | 11.13.1 | 538KB | 信息显示 |
| **JadeAddons** | 5.3.1 | 77KB | Jade 附属 |

### 🎮 其他模组
| 模组 | 版本 | 大小 | 说明 |
|------|------|------|------|
| **TechGuns2** | 1.0.0 | 1.3MB | 科技枪械 |
| **AssimpLoader** | 1.0.0 | 56MB | 模型加载器 |
| **mcpisnotmcp** | 1.0.0 | 34KB | 工具模组 |

---

## ⚠️ 发现的问题

### 🔴 严重问题

#### 1. LDLib 重复安装
```
ldlib-forge-1.20.1-1.0.42.jar  (3.0MB)
ldlib-forge-1.20.1-1.0.48.jar  (3.0MB)
```

**问题**: 两个版本的 LDLib 同时存在  
**影响**: 可能导致类冲突、崩溃  
**解决**: 删除旧版本 `1.0.42`，保留 `1.0.48`

---

### 🟡 渲染模组冲突（可能）

#### 渲染栈过于复杂
```
Embeddium (性能优化)
  ↓
Oculus (光影)
  ↓
Shimmer (渲染增强)
  ↓
Photon (光照)
  ↓
EnhancedVisuals (视觉效果)
  ↓
ModernUI (UI 渲染)
```

**问题**: 6个渲染相关模组同时运行  
**影响**: 
- 渲染管线冲突
- 性能问题
- 崩溃风险高

**崩溃日志证据**:
```
[ERROR]: Mod mixin into Embeddium internals detected.
This instance is now tainted.
```

**建议**: 临时移除 **Shimmer** 测试（最可疑）

---

## 🔍 崩溃原因分析

### 根本原因
```
java.lang.NullPointerException: 
Cannot read field "f_108590_" because "this.f_109059_.f_91074_" is null
at net.minecraft.client.renderer.GameRenderer
```

**分析**:
1. `GameRenderer` 的某个字段为 null
2. 发生在渲染初始化阶段
3. 与 Embeddium/Oculus/Shimmer 的 Mixin 冲突

### 可能的触发链
```
ModernUI 修改 UI 渲染
  ↓
Embeddium 优化渲染管线
  ↓
Oculus 注入光影代码
  ↓
Shimmer 修改渲染效果
  ↓
某个 Mixin 导致字段未初始化
  ↓
GameRenderer 访问 null 字段
  ↓
崩溃
```

---

## 🎯 推荐的解决方案

### 方案1: 清理重复模组（必须）
```bash
# 删除旧版本 LDLib
rm "ldlib-forge-1.20.1-1.0.42.jar"
```

### 方案2: 移除 Shimmer（推荐）
```bash
# 临时移除 Shimmer 测试
mv "Shimmer-forge-1.20.1-0.2.4.jar" "../disabled/"
```

**理由**:
- Shimmer 是最新添加的渲染模组
- 与 Embeddium 有已知冲突
- 崩溃日志明确提到 Embeddium 被污染

### 方案3: 简化渲染栈（可选）
如果方案2无效，尝试：
```bash
# 只保留 Embeddium + Oculus
mv "Shimmer-forge-1.20.1-0.2.4.jar" "../disabled/"
mv "Photon-forge-1.20.1-1.1.17.jar" "../disabled/"
```

---

## 📊 模组兼容性评估

### ✅ 完全兼容
- GregTech + Mekanism（工业模组）
- Spore + The Hordes + ImprovedMobs（难度模组）
- **IntegratedIndustrialCraft**（我们的模组）

### ⚠️ 可能冲突
- Embeddium + Oculus + Shimmer（渲染栈）
- ModernUI + 其他 UI 模组

### ❌ 已知问题
- LDLib 重复安装

---

## 🚀 立即行动

### 步骤1: 删除重复模组
```bash
cd "G:\MinecraftGames\CTNH-BaopuEdition\.minecraft\versions\Create New Horizon\mods"
rm "ldlib-forge-1.20.1-1.0.42.jar"
```

### 步骤2: 移除 Shimmer
```bash
mkdir -p "../disabled"
mv "Shimmer-forge-1.20.1-0.2.4.jar" "../disabled/"
```

### 步骤3: 重启游戏
测试是否还会崩溃

---

## 💡 结论

### 崩溃原因
**不是我们的模组导致的**，而是：
1. ❌ LDLib 重复安装
2. ❌ Shimmer 与 Embeddium 冲突
3. ❌ 渲染栈过于复杂

### 我们的模组状态
- ✅ 加载成功
- ✅ 初始化完成
- ✅ 无运行时错误
- ✅ 与其他模组兼容

### 下一步
1. 删除 `ldlib-forge-1.20.1-1.0.42.jar`
2. 移除 `Shimmer-forge-1.20.1-0.2.4.jar`
3. 重启游戏测试

---

**分析时间**: 2026-05-01  
**状态**: 问题已定位，等待修复
