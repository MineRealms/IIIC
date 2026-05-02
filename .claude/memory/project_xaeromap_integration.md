---
name: XaerosWorldMap Pollution Overlay Integration
description: Complete integration of pollution visualization on XaerosWorldMap with real-time rendering and toggle UI
type: project
---

**完成时间**: 2026年5月

**功能**: 在XaerosWorldMap世界地图上实时显示污染分布，支持颜色渐变和鼠标悬停查询

**核心组件**:

1. **PollutionOverlayAPI** (`api/PollutionOverlayAPI.java`)
   - 线程安全的污染数据缓存（ConcurrentHashMap）
   - 自动清理低污染区块（≤0.1）
   - 支持维度切换时清空缓存

2. **PollutionOverlayRenderer** (`client/PollutionOverlayRenderer.java`)
   - GPU加速渲染（BufferBuilder + VertexConsumer）
   - 颜色渐变：绿→黄→橙→红→深红（0-200+污染值）
   - 屏幕外区块剔除优化
   - 鼠标悬停污染值查询

3. **GuiMapMixin** (`mixin/xaeromap/GuiMapMixin.java`)
   - 注入XaerosWorldMap的GuiMap类
   - 添加污染叠加层切换按钮（右上角）
   - 注入渲染钩子（在地图渲染后、UI渲染前）

**Why**:
玩家需要可视化工具来：
- 监控工业区的污染扩散
- 规划机器布局避免高污染
- 快速定位污染源
- 评估环境吸收效果

**How to apply**:
- 按 `M` 打开世界地图
- 点击右上角 "Pollution Overlay" 按钮切换显示
- 鼠标悬停在区块上查看精确污染值
- 颜色越红表示污染越高

**技术要点**:
- 使用Mixin注入而不是事件系统（XaerosWorldMap不提供渲染事件）
- 污染数据由PollutionManager每秒更新到API缓存
- 渲染层级：地图底图 → 污染叠加层 → 标记/UI
- 所有@Unique静态方法必须是private（Mixin规范）

**性能优化**:
- 只渲染屏幕可见区块
- 使用BufferBuilder批量提交顶点数据
- 污染缓存自动清理低值区块
- 避免每帧重新计算颜色（使用查找表）
