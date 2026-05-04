package cn.minerealms.iic.mixin.gunmod;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin to conditionally apply Gun Mod mixins only when the mod is present.
 */
public class GunModMixinPlugin implements IMixinConfigPlugin {

    private static final String GUN_MOD_ID = "cgm";
    private boolean isGunModLoaded = false;

    @Override
    public void onLoad(String mixinPackage) {
        // Check if Gun Mod is loaded
        try {
            isGunModLoaded = LoadingModList.get().getModFileById(GUN_MOD_ID) != null;
            System.out.println("[IIC] Gun Mod detected: " + isGunModLoaded);
        } catch (Exception e) {
            isGunModLoaded = false;
            System.out.println("[IIC] Gun Mod not detected");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Only apply Gun Mod mixins if the mod is loaded
        if (mixinClassName.contains("gunmod")) {
            return isGunModLoaded;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
