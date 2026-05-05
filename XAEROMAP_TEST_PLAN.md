# XaerosWorldMap 集成测试计划

## 测试环境

- Minecraft 1.20.1
- Forge 47.1.3
- XaerosWorldMap 1.39.12
- ImprovedMobs + IntegratedIndustrialCraft (最新版本)

## 前置条件

1. 确保 XaerosWorldMap 已安装
2. 确保 GregTech CEu 或其他工业 mod 已安装（用于产生污染）
3. 配置文件 `config/triaxis-difficulty.properties` 中：
   - `enablePollutionMapOverlay=true`
   - `pollutionOverlayAlpha=0.4`
   - `showPollutionTooltip=true`

## 测试步骤

### 1. Mixin 加载验证

**目标**: 确认 Mixin 正确加载

**步骤**:
1. 启动游戏
2. 检查日志文件 `logs/latest.log`

**预期结果**:
```
[IIC-MixinPlugin] Checking XaerosWorldMap mixin: cn.minerealms.iic.mixin.xaeromap.GuiMapMixin -> Target: xaero.map.gui.GuiMap -> Present: true
[IIC-MixinPlugin] Applying mixin: cn.minerealms.iic.mixin.xaeromap.GuiMapMixin to xaero.map.gui.GuiMap
[IIC-MixinPlugin] Successfully applied mixin: cn.minerealms.iic.mixin.xaeromap.GuiMapMixin to xaero.map.gui.GuiMap
[IIC-MixinPlugin] Marked GuiMapMixin as APPLIED in tracker
```

**失败标志**:
- 出现 `ClassNotFoundException` 或 `NoSuchFieldError`
- Mixin 未应用的警告

### 2. 基础渲染测试

**目标**: 确认污染叠加层能够显示

**步骤**:
1. 进入游戏世界
2. 建造一些 GregTech 机器（至少 10 台 MV 机器）
3. 运行机器一段时间（5-10 分钟）
4. 按 `M` 键打开世界地图

**预期结果**:
- 地图上显示彩色的污染叠加层
- 污染区块颜色根据污染值变化：
  - 绿色 → 黄色 (0-50)
  - 黄色 → 橙色 (50-100)
  - 橙色 → 红色 (100-200)
  - 深红色 (200+)

**失败标志**:
- 地图上没有任何叠加层
- 叠加层全是同一种颜色
- 游戏崩溃或卡顿

### 3. 鼠标悬停测试

**目标**: 确认鼠标悬停显示污染值

**步骤**:
1. 打开世界地图
2. 将鼠标移动到有污染的区块上

**预期结果**:
- 鼠标位置显示 tooltip: `§6Pollution: §c[数值]`
- 数值与实际污染值匹配（可通过 `/im industrial status` 验证）
- 移动鼠标时 tooltip 实时更新

**失败标志**:
- 没有 tooltip 显示
- Tooltip 显示错误的数值
- Tooltip 位置不正确

### 4. 地图交互测试

**目标**: 确认叠加层与地图交互正常

**步骤**:
1. 打开世界地图
2. 执行以下操作：
   - 缩放地图（鼠标滚轮）
   - 拖动地图（鼠标左键拖动）
   - 切换维度（如果有多个维度）

**预期结果**:
- 缩放时叠加层正确缩放
- 拖动时叠加层正确跟随
- 切换维度时叠加层清空并重新加载

**失败标志**:
- 叠加层不跟随地图移动
- 叠加层缩放比例错误
- 切换维度后显示错误的污染数据

### 5. 性能测试

**目标**: 确认叠加层不影响性能

**步骤**:
1. 创建大量污染区块（100+ 区块）
2. 打开世界地图
3. 快速缩放和拖动地图
4. 观察帧率和响应速度

**预期结果**:
- 帧率保持在 60 FPS 以上
- 地图响应流畅，无明显卡顿
- 内存占用正常（不超过 100MB 增长）

**失败标志**:
- 帧率下降到 30 FPS 以下
- 地图拖动有明显延迟
- 内存持续增长

### 6. 配置测试

**目标**: 确认配置选项生效

**步骤**:
1. 修改 `config/triaxis-difficulty.properties`:
   - 设置 `enablePollutionMapOverlay=false`
   - 重启游戏
   - 打开地图，确认叠加层不显示
2. 修改 `pollutionOverlayAlpha=0.8`
   - 重启游戏
   - 打开地图，确认叠加层更不透明
3. 修改 `showPollutionTooltip=false`
   - 重启游戏
   - 打开地图，确认鼠标悬停不显示 tooltip

**预期结果**:
- 所有配置选项正确生效

**失败标志**:
- 配置修改后无效果
- 需要删除配置文件才能生效

### 7. 兼容性测试

**目标**: 确认与其他 mod 兼容

**步骤**:
1. 与 GregTech CEu 的地图集成同时启用
2. 与其他地图 mod（如 JourneyMap）同时安装
3. 与其他 Mixin mod 同时安装

**预期结果**:
- 所有 mod 正常工作
- 没有 Mixin 冲突
- 没有渲染冲突

**失败标志**:
- 游戏崩溃
- 地图显示异常
- Mixin 冲突警告

## 调试工具

### 1. 日志检查
```bash
# 检查 Mixin 加载
grep -i "xaeromap\|GuiMapMixin" logs/latest.log

# 检查错误
grep -i "error\|exception" logs/latest.log | grep -i "xaero\|pollution"
```

### 2. 游戏内命令
```
/im industrial debug on     # 启用调试日志
/im industrial status       # 查看当前污染状态
/im industrial scan         # 扫描附近机器
```

### 3. 配置验证
```bash
# 检查配置文件
cat config/triaxis-difficulty.properties | grep -i "pollution.*overlay"
```

## 常见问题排查

### 问题 1: 叠加层不显示

**可能原因**:
1. Mixin 未加载
2. 配置禁用了叠加层
3. 没有污染数据

**排查步骤**:
1. 检查日志确认 Mixin 已应用
2. 检查配置文件 `enablePollutionMapOverlay=true`
3. 使用 `/im industrial status` 确认有污染数据

### 问题 2: 颜色显示错误

**可能原因**:
1. 污染值计算错误
2. 颜色插值算法问题

**排查步骤**:
1. 使用 `/im industrial status` 查看实际污染值
2. 检查 `iic$getPollutionColor()` 方法的颜色映射逻辑

### 问题 3: 性能问题

**可能原因**:
1. 污染区块过多
2. 渲染优化不足

**排查步骤**:
1. 检查污染区块数量（`PollutionOverlayAPI.getAllPollutedChunks().size()`）
2. 启用 F3 调试界面查看渲染统计
3. 检查是否有屏幕外剔除失效

### 问题 4: Tooltip 不显示

**可能原因**:
1. 配置禁用了 tooltip
2. 鼠标坐标转换错误

**排查步骤**:
1. 检查配置 `showPollutionTooltip=true`
2. 添加调试日志输出鼠标坐标和转换后的世界坐标

## 测试报告模板

```markdown
## 测试结果

**测试日期**: YYYY-MM-DD
**测试人员**: [姓名]
**环境**: Minecraft 1.20.1 + Forge 47.1.3 + XaerosWorldMap 1.39.12

### 测试项目

| 测试项 | 状态 | 备注 |
|--------|------|------|
| Mixin 加载 | ✅/❌ | |
| 基础渲染 | ✅/❌ | |
| 鼠标悬停 | ✅/❌ | |
| 地图交互 | ✅/❌ | |
| 性能测试 | ✅/❌ | FPS: XX |
| 配置测试 | ✅/❌ | |
| 兼容性测试 | ✅/❌ | |

### 发现的问题

1. [问题描述]
   - 重现步骤: ...
   - 预期结果: ...
   - 实际结果: ...
   - 日志: ...

### 总体评价

[通过/不通过]

### 建议

[改进建议]
```

## 自动化测试（未来）

考虑添加自动化测试：

1. **单元测试**:
   - 颜色插值算法测试
   - 坐标转换测试
   - 污染数据缓存测试

2. **集成测试**:
   - Mixin 应用测试
   - 渲染管线测试

3. **性能基准测试**:
   - 不同污染区块数量下的帧率
   - 内存占用测试
