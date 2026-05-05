# Mixin 指南 - Forge 1.20.1

## 目录
1. [基础配置](#1-基础配置)
2. [Mixin 配置详解](#2-mixin-配置详解)
3. [Shadow 使用指南](#3-shadow-使用指南)
4. [第三方 mod Mixin](#4-第三方-mod-mixin)
5. [常见问题与解决方案](#5-常见问题与解决方案)
6. [调试技巧](#6-调试技巧)

---

## 1. 基础配置

### mods.toml 配置
```toml
[[mixins]]
config="yourmod.mixins.json"
```

### mixin 配置文件结构
```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "your.package.mixin",
  "compatibilityLevel": "JAVA_17",
  "mixins": [],
  "client": [],
  "injectors": {
    "defaultRequire": 1
  }
}
```

### build.gradle 配置
```groovy
mixin {
    config "yourmod.mixins.json"
}
```

### MANIFEST.MF (自动生成)
```properties
MixinConfigs: yourmod.mixins.json
```
> 注意：Mixingradle 插件会自动在 MANIFEST.MF 中添加此属性

---

## 2. Mixin 配置详解

| 字段 | 必填 | 说明 |
|------|------|------|
| `required` | 是 | true 时配置加载失败会导致游戏崩溃 |
| `minVersion` | 是 | 最小 Mixin 版本，建议 0.8 |
| `package` | 是 | Mixin 类所在包名 |
| `compatibilityLevel` | 是 | Java 版本，如 JAVA_17 |
| `refmap` | 否 | 混淆映射表，仅用于原版 Minecraft 类 |
| `plugin` | 否 | 自定义插件，用于条件加载 |
| `mixins` | 否 | 服务端 Mixin 列表 |
| `client` | 否 | 客户端 Mixin 列表 |
| `injectors.defaultRequire` | 是 | 0=可选，1=必需 |

### 何时使用 refmap
- **需要**：Mixin 目标是 Minecraft 原版类（Screen, Entity, Player 等）
- **不需要**：Mixin 目标是第三方 mod 类，或使用反射获取字段

### 何时使用 plugin
- **需要**：根据游戏环境条件性加载 Mixin
- **不需要**：默认加载所有 Mixin

---

## 3. Shadow 使用指南

### 3.1 原版 Minecraft 类（推荐）

```java
@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Shadow
    protected int width;

    @Shadow
    protected int height;

    @Shadow
    public abstract Minecraft getMinecraft();
}
```

### 3.2 混淆字段重映射

对于继承自混淆类的字段，需要显式声明 `remap = true`：

```java
@Mixin(value = SomeClass.class, remap = false)
public class SomeMixin {

    // 来自父类 Screen（混淆类）的字段
    @Shadow(remap = true)
    public int width;

    @Shadow(remap = true)
    public int height;
}
```

### 3.3 抽象方法

```java
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    public abstract Level getLevel();
}
```

---

## 4. 第三方 mod Mixin

### 4.1 核心问题
第三方 mod 没有官方 refmap，Mixin 无法通过 @Shadow 访问字段。

### 4.2 解决方案：使用反射

```java
@Mixin(value = xaero.map.gui.GuiMap.class, remap = false, priority = 2000)
public abstract class ThirdPartyMixin {

    @Unique
    private static Minecraft iic$getMinecraft() {
        return Minecraft.getInstance();
    }

    @Inject(method = "renderMethod", at = @At("HEAD"), remap = false)
    private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        // 使用反射获取第三方 mod 的字段
        try {
            Class<?> targetClass = Class.forName("xaero.map.gui.GuiMap");

            Field cameraXField = targetClass.getDeclaredField("cameraX");
            cameraXField.setAccessible(true);
            double cameraX = cameraXField.getDouble(this);

            Field scaleField = targetClass.getDeclaredField("scale");
            scaleField.setAccessible(true);
            double scale = scaleField.getDouble(this);

            // 使用获取到的值
        } catch (Exception e) {
            // 处理异常
        }
    }
}
```

### 4.3 使用静态方法代替抽象方法

```java
// 错误 - getMinecraft() 在目标类中不存在
@Shadow
public abstract Minecraft getMinecraft();

// 正确 - 使用静态方法
@Unique
private static Minecraft getMinecraftInstance() {
    return Minecraft.getInstance();
}
```

### 4.4 使用 Minecraft 窗口尺寸

```java
// 替代 @Shadow(remap = true) public int width;
@Unique
private static int getWidth() {
    return Minecraft.getInstance().getWindow().getGuiScaledWidth();
}

@Unique
private static int getHeight() {
    return Minecraft.getInstance().getWindow().getGuiScaledHeight();
}
```

---

## 5. 常见问题与解决方案

### 问题 1：Mixin 配置完全未加载
**症状**：日志中完全没有 mixin 配置的提及，MixinPlugin 未被调用

**原因**：
1. refmap 文件不存在
2. MixinPlugin 加载失败导致整个配置被跳过

**解决方案**：
```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "your.package.mixin",
  "compatibilityLevel": "JAVA_17",
  "mixins": [],
  "injectors": {
    "defaultRequire": 1
  }
}
```
- 移除 `refmap` 字段（除非你有有效的 refmap 文件）
- 移除 `plugin` 字段（除非必要）

### 问题 2：No refMap loaded
**症状**：`@Shadow method/field was not located in the target class. No refMap loaded.`

**原因**：目标类是第三方 mod，没有 refmap

**解决方案**：使用反射代替 @Shadow（见上文第 4 节）

### 问题 3：Invalid descriptor
**症状**：`Invalid descriptor on mixin -> @Inject::methodName`

**原因**：注入方法签名与目标方法不匹配

**解决方案**：
- 检查方法参数类型是否正确
- 使用 `remap = false` 避免混淆名称问题

### 问题 4：Super class not found
**症状**：`Super class 'X' of Mixin was not found in the hierarchy`

**原因**：
1. Mixin 目标类不匹配
2. 依赖的 mod 未安装

**解决方案**：
- 使用 MixinPlugin 进行条件加载
- 设置 `required: false` 使其可选

---

## 6. 调试技巧

### 6.1 启用 Mixin 调试日志
在运行配置中添加 JVM 参数：
```
-Dmixin.debug=true
-Dmixin.checks=true
```

### 6.2 强制报错
将 `required` 设为 `true`，使配置加载失败时崩溃而非静默跳过：
```json
{
  "required": true
}
```

### 6.3 添加日志输出
在 Mixin 中添加调试日志：
```java
@Inject(method = "render", at = @At("HEAD"), remap = false)
private void onRender(CallbackInfo ci) {
    System.out.println("[DEBUG] Mixin triggered!");
}
```

### 6.4 检查配置是否加载
在日志中搜索：
```
Successfully loaded Mixin Connector  // Mixin 连接器加载
Mixin config xxx.mixins.json         // 特定配置加载
```

---

## 示例：完整的第三方 mod Mixin

```java
package your.package.mixin.thirdparty;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(value = com.example.mod.TargetClass.class, remap = false, priority = 1000)
public abstract class ThirdPartyModMixin {

    @Unique
    private static Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    @Unique
    private ResourceKey<Level> lastDimension = null;

    @Inject(method = "renderMethod", at = @At("HEAD"), remap = false)
    private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        Minecraft mc = getMinecraft();
        if (mc.level != null) {
            ResourceKey<Level> currentDim = mc.level.dimension();
            if (lastDimension != null && !lastDimension.equals(currentDim)) {
                // 维度变化，清除缓存
            }
            lastDimension = currentDim;
        }

        // 你的逻辑
    }

    @Unique
    private void doSomethingWithTargetClass() {
        try {
            Class<?> targetClass = Class.forName("com.example.mod.TargetClass");

            Field someField = targetClass.getDeclaredField("someField");
            someField.setAccessible(true);
            Object value = someField.get(this);

            // 使用 value
        } catch (Exception e) {
            // 优雅处理异常
        }
    }
}
```

---

## 参考资源

- [Mixin 官方 Wiki](https://github.com/SpongePowered/Mixin/wiki)
- [Forge Mixin 教程](https://gist.github.com/TelepathicGrunt/3784f8a8b317bac11039474012de5fb4)
- [Mixin 配置规范](https://github.com/SpongePowered/Mixin/wiki/Configuration)

---

## 检查清单

在提交 Mixin 代码前检查：

- [ ] mixin 配置的 refmap 字段只在必要时使用
- [ ] 第三方 mod 使用反射而非 @Shadow
- [ ] 使用 `remap = false` 针对第三方类
- [ ] 使用 `Minecraft.getInstance()` 代替不存在的抽象方法
- [ ] 配置中 `required: true` 用于测试，生产环境可设为 false
- [ ] 客户端 Mixin 放在 `client` 数组中
- [ ] 方法注入使用正确的签名