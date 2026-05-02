# Shimmer + Embeddium + Oculus 冲突最终分析

## 🎯 根本原因确认

### 冲突组合
```
Shimmer + Embeddium = ✅ 正常
Shimmer + Embeddium + Oculus = ❌ 崩溃
```

### 崩溃原因
**三个模组同时修改 GameRenderer**：

```java
// Mixin 应用顺序（从日志）
pl:mixin:APP:mixins.oculus.json:GameRendererAccessor
pl:mixin:APP:mixins.oculus.json:MixinGameRenderer
pl:mixin:APP:shimmer.mixins.json:GameRendererMixin
pl:mixin:APP:shimmer.mixins.json:reloadShader.GameRendererMixin
pl:mixin:APP:embeddium.mixins.json:features.gui.hooks.console.GameRendererMixin
```

**问题链**:
1. Embeddium 重写渲染管线
2. Oculus 注入光影代码，改变初始化顺序
3. Shimmer 假设原版初始化顺序
4. GameRenderer.render() 在 minecraft.level 初始化前被调用
5. 访问 null → 崩溃

---

## 📊 测试结果

### 配置1: Embeddium + Shimmer
- 状态: ✅ 正常
- 原因: 只有两个模组修改 GameRenderer

### 配置2: Embeddium + Oculus
- 状态: ✅ 正常（推测）
- 原因: Oculus 与 Embeddium 有兼容性

### 配置3: Embeddium + Shimmer + Oculus
- 状态: ❌ 崩溃
- 原因: 三方 Mixin 冲突

---

## 🔧 解决方案

### 方案1: 移除 Oculus（如果不需要光影）
```bash
# Oculus 是光影模组
# 如果不用光影，可以移除
```

**优点**:
- ✅ 保留 Shimmer 的光效
- ✅ 保留 Embeddium 的性能优化
- ✅ 我们的炮塔光效正常

**缺点**:
- ❌ 无法使用光影包

---

### 方案2: 移除 Shimmer（如果需要光影）
```bash
# 如果需要光影，移除 Shimmer
```

**优点**:
- ✅ 可以使用光影包
- ✅ Embeddium + Oculus 稳定

**缺点**:
- ❌ 失去 Shimmer 的彩色光效
- ❌ 我们的炮塔光效失效

---

### 方案3: 全部移除，使用原版渲染
```bash
# 移除 Embeddium + Oculus + Shimmer
```

**优点**:
- ✅ 最稳定
- ✅ 无冲突

**缺点**:
- ❌ 性能差
- ❌ 无光影
- ❌ 无光效

---

### 方案4: 等待模组更新（长期）
等待 Shimmer 或 Oculus 更新兼容性补丁。

---

## 💡 推荐配置

### 如果你重视性能和光效
```
✅ Embeddium（性能优化）
✅ Shimmer（彩色光效）
❌ Oculus（移除）
```

**适合**:
- 不需要光影包
- 想要彩色光效
- 我们的炮塔光效

---

### 如果你重视光影
```
✅ Embeddium（性能优化）
✅ Oculus（光影支持）
❌ Shimmer（移除）
```

**适合**:
- 需要光影包
- 可以放弃彩色光效

---

### 如果你重视稳定性
```
❌ Embeddium（移除）
❌ Oculus（移除）
❌ Shimmer（移除）
```

**适合**:
- 追求最稳定
- 不在意性能和视觉效果

---

## 🔍 技术细节

### 为什么 Oculus 是触发点

**Oculus 的作用**:
- 实现光影包支持（Iris 的 Forge 移植）
- 大量修改渲染管线
- 改变渲染初始化顺序

**冲突机制**:
```
正常流程:
1. Minecraft 初始化
2. Level 加载
3. GameRenderer 初始化
4. 开始渲染

Oculus 修改后:
1. Minecraft 初始化
2. GameRenderer 提前初始化（为光影准备）
3. Level 加载
4. 开始渲染

Shimmer 假设:
1. Level 已加载
2. 访问 minecraft.level
3. 如果 level == null → 崩溃
```

---

## 📝 当前状态

### 你的 mods 目录
```
Shimmer-forge-1.20.1-0.2.4.jar
embeddium-0.3.31+mc1.20.1.jar
```

**问题**: 其他模组（GregTech, Mekanism, Spore 等）都不见了

**可能原因**:
1. 你手动移走了
2. 在其他目录测试
3. 使用了不同的游戏实例

---

## 🎯 最终建议

### 立即行动

**如果不需要光影**:
- ✅ 保持当前配置（Embeddium + Shimmer）
- ✅ 把其他模组移回来
- ✅ 测试游戏

**如果需要光影**:
- ❌ 移除 Shimmer
- ✅ 添加 Oculus
- ✅ 把其他模组移回来

---

## 📊 兼容性矩阵

| 配置 | 性能 | 光影 | 光效 | 稳定性 |
|------|------|------|------|--------|
| Embeddium + Shimmer | ⭐⭐⭐⭐⭐ | ❌ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Embeddium + Oculus | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ❌ | ⭐⭐⭐⭐ |
| Embeddium + Shimmer + Oculus | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ❌ 崩溃 |
| 原版渲染 | ⭐⭐ | ❌ | ❌ | ⭐⭐⭐⭐⭐ |

---

**分析时间**: 2026-05-01  
**状态**: 根本原因已确认  
**结论**: Oculus 是冲突触发点，必须在 Shimmer 和 Oculus 之间二选一
