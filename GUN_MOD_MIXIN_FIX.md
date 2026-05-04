# Gun Mod Mixin 修复报告

**日期**: 2026-05-04  
**问题**: Gun Mod集成的Mixin编译失败  
**状态**: ✅ 已修复

---

## 问题描述

Gun Mod集成使用Dummy类作为Mixin目标，导致编译时出现以下错误：

1. **ServerPlayHandlerMixin**: Method signature does not match (参数顺序错误)
2. **WorkbenchBlockMixin**: Cannot resolve method 'getTicker' (Dummy类缺少方法)
3. **WorkbenchScreenMixin**: Cannot resolve method 'render' (Dummy类缺少方法)
4. **WorkbenchBlockEntityMixin**: 
   - Cannot resolve method 'load' (Dummy类缺少方法)
   - Cannot resolve method 'saveAdditional' (Dummy类缺少方法)
   - GTCapability类找不到 (编译时依赖问题)
   - Mekanism API参数类型不匹配

---

## 解决方案

### 核心思路

参考**EnhancedVisuals**和**XaerosWorldMap**的Mixin实现方式：
- **不使用Dummy类**
- 使用`@Pseudo`注解
- 使用`targets`直接指向真实的Gun Mod类
- 保留`remap = false`和`require = 0`

### 修改详情

#### 1. ServerPlayHandlerMixin

```java
@Pseudo
@Mixin(targets = "com.mrcrayfish.guns.common.network.ServerPlayHandler", remap = false)
public class ServerPlayHandlerMixin {
    @Inject(method = "handleCraft", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void iic$handleCraft(ServerPlayer player, BlockPos pos, ResourceLocation recipeId, CallbackInfo ci) {
        // ...
    }
}
```

**关键改动**:
- 添加`@Pseudo`注解
- 使用`targets = "com.mrcrayfish.guns.common.network.ServerPlayHandler"`
- 移除对DummyServerPlayHandler的依赖

#### 2. WorkbenchBlockEntityMixin

```java
@Pseudo
@Mixin(targets = "com.mrcrayfish.guns.blockentity.WorkbenchBlockEntity", remap = false)
public abstract class WorkbenchBlockEntityMixin extends BlockEntity implements IEnergyWorkbench {
    // ...
}
```

**关键改动**:
- 添加`@Pseudo`注解
- 使用`targets = "com.mrcrayfish.guns.blockentity.WorkbenchBlockEntity"`
- GTCapability使用反射避免编译时依赖
- 移除Mekanism直接支持，改用Forge Energy（Mekanism也支持）

#### 3. WorkbenchBlockMixin

```java
@Pseudo
@Mixin(targets = "com.mrcrayfish.guns.block.WorkbenchBlock", remap = false)
public class WorkbenchBlockMixin {
    @Inject(method = "getTicker", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void iic$getTicker(Level level, BlockState state, BlockEntityType<?> type,
                               CallbackInfoReturnable<BlockEntityTicker<?>> cir) {
        // ...
    }
}
```

**关键改动**:
- 添加`@Pseudo`注解
- 使用`targets = "com.mrcrayfish.guns.block.WorkbenchBlock"`

#### 4. WorkbenchScreenMixin

```java
@Pseudo
@Mixin(targets = "com.mrcrayfish.guns.client.screen.WorkbenchScreen", remap = false)
public abstract class WorkbenchScreenMixin extends AbstractContainerScreen {
    @Inject(method = "render", at = @At("TAIL"), remap = false, require = 0)
    private void iic$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        // ...
    }
}
```

**关键改动**:
- 添加`@Pseudo`注解
- 使用`targets = "com.mrcrayfish.guns.client.screen.WorkbenchScreen"`

---

## 技术细节

### @Pseudo注解的作用

`@Pseudo`告诉Mixin处理器：
- 目标类可能在编译时不存在
- 不要在编译时验证目标类的存在性
- 运行时如果类不存在，Mixin会被自动跳过

### targets vs value

- `value`: 需要在编译时能够解析的类
- `targets`: 可以是字符串形式的类名，编译时不需要存在

### require = 0

- 表示这个Mixin是可选的
- 如果目标类不存在，不会导致游戏崩溃
- 配合`@Pseudo`使用，实现完全的可选集成

---

## 能量系统改进

### GTCEu能量支持

使用反射避免编译时依赖：

```java
try {
    Class<?> gtCapClass = Class.forName("com.gregtechceu.gtceu.api.capability.GTCapability");
    Object capabilityField = gtCapClass.getField("CAPABILITY_ENERGY_CONTAINER").get(null);
    var gtCapability = self.getCapability((Capability<IEnergyContainer>) capabilityField, null);
    // ...
} catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | NoClassDefFoundError ignored) {
    // GTCEu not available
}
```

### Forge Energy支持

简化为只使用Forge Energy API：

```java
IEnergyStorage feEnergy = self.getCapability(ForgeCapabilities.ENERGY, null).resolve().orElse(null);
if (feEnergy != null) {
    int feAmount = amount * 4; // 1 EU = 4 FE
    int extracted = feEnergy.extractEnergy(feAmount, false);
    return extracted >= feAmount;
}
```

**优势**:
- Mekanism也实现了Forge Energy接口
- 避免了Mekanism API的复杂性
- 更好的兼容性

---

## 编译结果

```bash
./gradlew compileJava
# ✅ BUILD SUCCESSFUL

./gradlew build
# ✅ BUILD SUCCESSFUL
# JAR: build/libs/integratedindustrialcraft-1.0.0.jar (579KB)
```

**警告说明**:
- `deprecation`: 使用了已过时的API（正常）
- `unchecked`: 反射调用的泛型警告（正常）
- 无错误，编译完全成功

---

## 参考实现

### EnhancedVisuals

```java
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleRespawn(Lnet/minecraft/network/protocol/game/ClientboundRespawnPacket;)V", at = @At("TAIL"))
    public void handleRespawn(ClientboundRespawnPacket packet, CallbackInfo info) {
        EnhancedVisuals.EVENTS.respawn();
    }
}
```

### XaerosWorldMap

```java
@Mixin(value={ClientPacketListener.class})
public class MixinClientPlayNetworkHandler implements IWorldMapClientPlayNetHandler {
    @Inject(at={@At(value="HEAD")}, method={"updateLevelChunk"})
    public void onOnChunkData(int x, int z, ClientboundLevelChunkPacketData packet, CallbackInfo info) {
        XaeroWorldMapCore.onChunkData(x, z, packet);
    }
}
```

**共同点**:
- 直接Mixin到Minecraft或其他mod的真实类
- 不使用Dummy类
- 简洁明了

---

## 经验总结

### ✅ 正确做法

1. **直接Mixin到真实类**: 使用`@Pseudo`和`targets`
2. **参考成熟项目**: EnhancedVisuals、XaerosWorldMap等
3. **使用反射处理可选依赖**: GTCEu能量系统
4. **优先使用通用API**: Forge Energy而不是Mekanism专有API

### ❌ 错误做法

1. **使用Dummy类**: 导致方法签名不匹配
2. **硬编码依赖**: 编译时必须存在所有依赖
3. **过度复杂**: 不必要的抽象层

---

## 后续建议

### 可选改进

1. **移除Dummy类**: 现在已经不需要了，可以删除
2. **添加更多能量系统支持**: 
   - IC2 Energy (EU)
   - Applied Energistics 2 (AE)
   - Refined Storage (RS)
3. **改进GUI**: 添加能量输入接口的可视化

### 测试建议

1. **有Gun Mod**: 验证能量系统和进度条正常工作
2. **无Gun Mod**: 验证游戏正常启动，Mixin被跳过
3. **多种能量系统**: 测试GTCEu、Mekanism、Forge Energy

---

## 文件清单

### 修改的文件

```
src/main/java/cn/minerealms/iic/mixin/gunmod/
├── ServerPlayHandlerMixin.java      (✅ 使用@Pseudo + targets)
├── WorkbenchBlockEntityMixin.java   (✅ 使用@Pseudo + targets)
├── WorkbenchBlockMixin.java         (✅ 使用@Pseudo + targets)
└── WorkbenchScreenMixin.java        (✅ 使用@Pseudo + targets)

src/main/java/cn/minerealms/iic/integration/gunmod/
├── DummyServerPlayHandler.java      (保留但不再使用)
├── DummyWorkbenchBlock.java         (保留但不再使用)
├── DummyWorkbenchScreen.java        (保留但不再使用)
└── IEnergyWorkbench.java            (接口，继续使用)
```

---

**开发者**: CARIERX  
**AI辅助**: Claude Opus 4.6 (1M context)  
**完成日期**: 2026-05-04
