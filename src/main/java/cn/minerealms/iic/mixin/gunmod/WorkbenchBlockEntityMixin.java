package cn.minerealms.iic.mixin.gunmod;

import cn.minerealms.iic.integration.gunmod.DummyWorkbenchBlockEntity;
import cn.minerealms.iic.integration.gunmod.IEnergyWorkbench;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to add energy storage and progress tracking to MrCrayfish Gun Mod's WorkbenchBlockEntity.
 *
 * Adds support for:
 * - GTCEu energy (EU) and Forge Energy (FE) consumption
 * - Progress tracking for crafting operations
 * - NBT persistence for energy and progress data
 *
 * Uses DummyWorkbenchBlockEntity for compilation, targets real WorkbenchBlockEntity at runtime.
 */
@Mixin(value = DummyWorkbenchBlockEntity.class, remap = false)
public abstract class WorkbenchBlockEntityMixin extends BlockEntity implements IEnergyWorkbench {

    // ==================== Energy Storage ====================

    /**
     * Current stored energy in EU (GregTech CEu units)
     * 1 EU = 4 FE (Forge Energy)
     */
    @Unique
    private int iic$storedEnergy = 0;

    /**
     * Maximum energy capacity in EU
     * Default: 10000 EU (40000 FE)
     */
    @Unique
    private int iic$maxEnergy = 10000;

    // ==================== Progress Tracking ====================

    /**
     * Current crafting progress in ticks
     */
    @Unique
    private int iic$progress = 0;

    /**
     * Maximum progress required to complete crafting (in ticks)
     * - Ammo: 60 ticks (3 seconds)
     * - Weapons: 240 ticks (12 seconds)
     */
    @Unique
    private int iic$maxProgress = 0;

    /**
     * Total energy cost for current recipe in EU
     * - Ammo: 512 EU (2048 FE)
     * - Weapons: 2048 EU (8192 FE)
     */
    @Unique
    private int iic$recipeCost = 0;

    /**
     * Whether a crafting operation is currently in progress
     */
    @Unique
    private boolean iic$isCrafting = false;

    // ==================== Constructor ====================

    public WorkbenchBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ==================== NBT Persistence ====================

    /**
     * Save energy and progress data to NBT
     */
    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void iic$saveEnergyAndProgress(CompoundTag tag, CallbackInfo ci) {
        CompoundTag iicData = new CompoundTag();
        iicData.putInt("StoredEnergy", this.iic$storedEnergy);
        iicData.putInt("MaxEnergy", this.iic$maxEnergy);
        iicData.putInt("Progress", this.iic$progress);
        iicData.putInt("MaxProgress", this.iic$maxProgress);
        iicData.putInt("RecipeCost", this.iic$recipeCost);
        iicData.putBoolean("IsCrafting", this.iic$isCrafting);
        tag.put("IICWorkbenchData", iicData);
    }

    /**
     * Load energy and progress data from NBT
     */
    @Inject(method = "load", at = @At("TAIL"))
    private void iic$loadEnergyAndProgress(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("IICWorkbenchData")) {
            CompoundTag iicData = tag.getCompound("IICWorkbenchData");
            this.iic$storedEnergy = iicData.getInt("StoredEnergy");
            this.iic$maxEnergy = iicData.getInt("MaxEnergy");
            this.iic$progress = iicData.getInt("Progress");
            this.iic$maxProgress = iicData.getInt("MaxProgress");
            this.iic$recipeCost = iicData.getInt("RecipeCost");
            this.iic$isCrafting = iicData.getBoolean("IsCrafting");
        }
    }

    // ==================== Public API Methods ====================

    /**
     * Get current stored energy in EU
     */
    @Unique
    @Override
    public int iic$getStoredEnergy() {
        return this.iic$storedEnergy;
    }

    /**
     * Get maximum energy capacity in EU
     */
    @Unique
    @Override
    public int iic$getMaxEnergy() {
        return this.iic$maxEnergy;
    }

    /**
     * Add energy to the workbench (in EU)
     * @param amount Energy to add in EU
     * @return Actual amount added
     */
    @Unique
    @Override
    public int iic$addEnergy(int amount) {
        int toAdd = Math.min(amount, this.iic$maxEnergy - this.iic$storedEnergy);
        this.iic$storedEnergy += toAdd;
        this.setChanged();
        return toAdd;
    }

    /**
     * Extract energy from the workbench (in EU)
     * @param amount Energy to extract in EU
     * @param simulate If true, only simulate extraction
     * @return Actual amount extracted
     */
    @Unique
    @Override
    public int iic$extractEnergy(int amount, boolean simulate) {
        int toExtract = Math.min(amount, this.iic$storedEnergy);
        if (!simulate) {
            this.iic$storedEnergy -= toExtract;
            this.setChanged();
        }
        return toExtract;
    }

    /**
     * Get current crafting progress
     */
    @Unique
    @Override
    public int iic$getProgress() {
        return this.iic$progress;
    }

    /**
     * Get maximum progress for current recipe
     */
    @Unique
    @Override
    public int iic$getMaxProgress() {
        return this.iic$maxProgress;
    }

    /**
     * Check if workbench is currently crafting
     */
    @Unique
    @Override
    public boolean iic$isCrafting() {
        return this.iic$isCrafting;
    }

    /**
     * Start a crafting operation
     * @param maxProgress Total ticks required
     * @param recipeCost Total energy cost in EU
     * @return true if started successfully
     */
    @Unique
    @Override
    public boolean iic$startCrafting(int maxProgress, int recipeCost) {
        if (this.iic$isCrafting) {
            return false; // Already crafting
        }
        if (this.iic$storedEnergy < recipeCost) {
            return false; // Not enough energy
        }

        this.iic$isCrafting = true;
        this.iic$progress = 0;
        this.iic$maxProgress = maxProgress;
        this.iic$recipeCost = recipeCost;
        this.setChanged();
        return true;
    }

    /**
     * Cancel current crafting operation
     */
    @Unique
    @Override
    public void iic$cancelCrafting() {
        this.iic$isCrafting = false;
        this.iic$progress = 0;
        this.iic$maxProgress = 0;
        this.iic$recipeCost = 0;
        this.setChanged();
    }

    /**
     * Reset crafting state after completion
     */
    @Unique
    @Override
    public void iic$resetCrafting() {
        this.iic$isCrafting = false;
        this.iic$progress = 0;
        this.iic$maxProgress = 0;
        this.iic$recipeCost = 0;
        this.setChanged();
    }
}
