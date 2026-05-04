package cn.minerealms.iic.integration.gunmod;

/**
 * Interface for accessing energy and progress data from Gun Mod's WorkbenchBlockEntity.
 * This interface is implemented via Mixin at runtime.
 */
public interface IEnergyWorkbench {

    /**
     * Get current stored energy in EU
     */
    int iic$getStoredEnergy();

    /**
     * Get maximum energy capacity in EU
     */
    int iic$getMaxEnergy();

    /**
     * Add energy to the workbench (in EU)
     * @param amount Energy to add in EU
     * @return Actual amount added
     */
    int iic$addEnergy(int amount);

    /**
     * Extract energy from the workbench (in EU)
     * @param amount Energy to extract in EU
     * @param simulate If true, only simulate extraction
     * @return Actual amount extracted
     */
    int iic$extractEnergy(int amount, boolean simulate);

    /**
     * Get current crafting progress
     */
    int iic$getProgress();

    /**
     * Get maximum progress for current recipe
     */
    int iic$getMaxProgress();

    /**
     * Check if workbench is currently crafting
     */
    boolean iic$isCrafting();

    /**
     * Start a crafting operation
     * @param maxProgress Total ticks required
     * @param recipeCost Total energy cost in EU
     * @return true if started successfully
     */
    boolean iic$startCrafting(int maxProgress, int recipeCost);

    /**
     * Cancel current crafting operation
     */
    void iic$cancelCrafting();

    /**
     * Reset crafting state after completion
     */
    void iic$resetCrafting();
}
