---
name: Pollution System Not Running
description: Critical bug - PollutionManager.tick() was never called, entire pollution system was inactive
type: feedback
---

**问题**: 污染系统完全不工作 - 机器运行但污染永远是0

**根本原因**: 
`EventHandler.onServerTick()` 中缺少 `PollutionManager.tick(level)` 调用

**Why**:
污染系统需要每tick更新才能：
1. 扫描玩家附近的GT机器
2. 计算机器产生的污染
3. 处理污染扩散和衰减
4. 转换临时污染为永久污染
5. 触发威胁机制（僵尸攻击机器等）

如果不调用tick方法，整个系统就是死的。

**错误代码**:
```java
// EventHandler.java - 错误 ❌
@SubscribeEvent
public static void onServerTick(TickEvent.ServerTickEvent event) {
    if (event.phase == TickEvent.Phase.END) {
        event.getServer().getPlayerList().getPlayers().forEach(player -> {
            DifficultyManager.tick(player);
        });
        
        event.getServer().getAllLevels().forEach(level -> {
            HordeIntegrationManager.tick(level);
            AlexsCavesIntegration.tick(level);
            // ❌ 缺少 PollutionManager.tick(level)
        });
    }
}
```

**修复代码**:
```java
// EventHandler.java - 正确 ✅
@SubscribeEvent
public static void onServerTick(TickEvent.ServerTickEvent event) {
    if (event.phase == TickEvent.Phase.END) {
        event.getServer().getPlayerList().getPlayers().forEach(player -> {
            DifficultyManager.tick(player);
        });
        
        event.getServer().getAllLevels().forEach(level -> {
            cn.minerealms.iic.pollution.PollutionManager.tick(level); // ✅ 添加
            HordeIntegrationManager.tick(level);
            AlexsCavesIntegration.tick(level);
        });
    }
}
```

**导致的连锁问题**:
1. ❌ 污染永远是0 - tick方法没被调用
2. ❌ 机器运行但不产生污染 - 扫描逻辑在tick中
3. ❌ 中位等级显示0 - 虽然有机器，但污染系统不扫描
4. ❌ 难度只有时间因子增长 - 污染和电压都是0
5. ❌ 威胁系统不工作 - 僵尸不会攻击机器

**How to apply**:
- 任何需要定期更新的系统都必须在事件处理器中注册tick调用
- 检查所有Manager类是否有tick方法，确保都被调用
- 使用 `grep -r "public static void tick" src/` 查找所有tick方法
- 使用 `grep -r "\.tick\(" src/` 查找所有tick调用，确保没有遗漏

**验证方法**:
- 启用debug模式：`/im industrial debug on`
- 查看日志是否有污染扫描信息
- 检查HUD是否显示污染值
- 确认机器运行时污染值增加
