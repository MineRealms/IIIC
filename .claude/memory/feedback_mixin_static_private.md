---
name: XaerosWorldMap Integration Mixin Fix
description: Fixed critical Mixin error - static methods must be private
type: feedback
---

**问题**: Mixin中的静态方法必须是private，否则会导致游戏崩溃

**错误信息**:
```
InvalidMixinException: Mixin contains non-private static method iic$isPollutionOverlayEnabled()Z
```

**原因**: 
- Mixin规范要求所有@Unique标记的静态方法必须是private
- 我最初将`iic$isPollutionOverlayEnabled()`声明为public static

**修复**:
```java
// 错误 ❌
@Unique
public static boolean iic$isPollutionOverlayEnabled() {
    return iic$showPollution;
}

// 正确 ✅
@Unique
private static boolean iic$isPollutionOverlayEnabled() {
    return iic$showPollution;
}
```

**Why**: Mixin框架需要控制方法的可见性以避免与目标类冲突。Public方法可能会暴露到目标类的公共API中，导致不可预测的行为。

**How to apply**: 
- 所有Mixin中的@Inject方法必须是private
- 所有@Unique方法必须是private
- 静态字段可以是private static（用于跨实例共享状态）
- 实例字段可以是private（用于单个实例状态）
- 永远不要在Mixin中使用public/protected方法（除非是@Overwrite，但应避免使用）
