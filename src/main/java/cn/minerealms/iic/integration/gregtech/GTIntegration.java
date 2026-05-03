package cn.minerealms.iic.integration.gregtech;

import cn.minerealms.iic.industrial.IndustrialLogger;
import cn.minerealms.iic.industrial.TriAxisConfig;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GregTech CEu Modern integration module.
 * <p>
 * This class provides integration with GregTech CEu Modern, allowing the mod to detect
 * and interact with GT machines, read their voltage tiers, check their active status,
 * and determine if they have energy.
 * <p>
 * Supports two integration modes:
 * <ol>
 *   <li>Direct type checking (if GTCEU is available at compile time)</li>
 *   <li>Reflection (if GTCEU is only available at runtime)</li>
 * </ol>
 * <p>
 * The integration automatically detects which mode to use and caches reflection
 * methods for performance. It supports both single-block machines and multiblock
 * structures, with special handling for multiblock voltage tier detection via energy hatches.
 * <p>
 * Debug logging is sampled (1 in {@value #SAMPLE_RATE} machines) to avoid log spam
 * when scanning large machine setups.
 *
 * @see MachineScanner
 * @see PollutionManager
 * @see ThreatManager
 * @author ImprovedMobs Team
 */
public class GTIntegration {

    private static boolean isGTLoaded = false;
    private static boolean checkDone = false;
    private static boolean useReflection = false;

    // Cached reflection classes and methods
    private static Class<?> metaMachineClass = null;
    private static Class<?> metaMachineBlockEntityClass = null;
    private static Class<?> iWorkableClass = null;
    private static Class<?> iEnergyContainerClass = null;
    private static Class<?> multiblockControllerClass = null;
    private static Class<?> gtCapabilityClass = null;
    private static Method getMetaMachineMethod = null;
    private static Method getDefinitionMethod = null;
    private static Method getTierMethod = null;
    private static Method isActiveMethod = null;
    private static Method getEnergyStoredMethod = null;
    private static Method getMachineStaticMethod = null;
    private static Object capabilityEnergyContainer = null;
    private static Object capabilityWorkable = null;

    /**
     * Sample counter for debug logging.
     * Only logs 1 in SAMPLE_RATE machines to avoid spam.
     */
    private static final AtomicInteger sampleCounter = new AtomicInteger(0);

    /**
     * Sample rate for debug logging (1 in 1000 machines).
     */
    private static final int SAMPLE_RATE = 1000;

    /**
     * Checks if GregTech CEu is loaded and initializes integration.
     * <p>
     * This method is called automatically on first use. It attempts to load GT classes
     * using reflection and caches the results. In GTCEU 1.8.0, all GT machines use
     * MetaMachineBlockEntity as their BlockEntity type.
     *
     * @return true if GregTech CEu is loaded and integration is successful
     */
    public static boolean isGTLoaded() {
        if (!checkDone) {
            IndustrialLogger.info("=== Initializing GregTech CEu Integration ===");

            try {
                metaMachineBlockEntityClass = Class.forName("com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity");
                metaMachineClass = Class.forName("com.gregtechceu.gtceu.api.machine.MetaMachine");
                iWorkableClass = Class.forName("com.gregtechceu.gtceu.api.capability.IWorkable");
                iEnergyContainerClass = Class.forName("com.gregtechceu.gtceu.api.capability.IEnergyContainer");
                multiblockControllerClass = Class.forName("com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine");
                gtCapabilityClass = Class.forName("com.gregtechceu.gtceu.api.capability.forge.GTCapability");
                Class<?> machineDefinitionClass = Class.forName("com.gregtechceu.gtceu.api.machine.MachineDefinition");

                // Cache methods
                getMetaMachineMethod = metaMachineBlockEntityClass.getMethod("getMetaMachine");
                getDefinitionMethod = metaMachineClass.getMethod("getDefinition");
                getTierMethod = machineDefinitionClass.getMethod("getTier");
                isActiveMethod = iWorkableClass.getMethod("isActive");
                getEnergyStoredMethod = iEnergyContainerClass.getMethod("getEnergyStored");

                // Cache GT capabilities
                capabilityEnergyContainer = gtCapabilityClass.getField("CAPABILITY_ENERGY_CONTAINER").get(null);
                capabilityWorkable = gtCapabilityClass.getField("CAPABILITY_WORKABLE").get(null);

                // Also cache the static getMachine method for BlockEntity lookup
                Class<?> blockEntityClass = Class.forName("net.minecraft.world.level.block.entity.BlockEntity");
                Class<?> levelArg = Class.forName("net.minecraft.world.level.BlockGetter");
                Class<?> posArg = Class.forName("net.minecraft.core.BlockPos");
                getMachineStaticMethod = metaMachineClass.getMethod("getMachine", levelArg, posArg);

                useReflection = true;
                isGTLoaded = true;
                IndustrialLogger.info("✓ GregTech CEu detected: Using MetaMachineBlockEntity mode");
                IndustrialLogger.info("  - MetaMachineBlockEntity: " + metaMachineBlockEntityClass.getName());
                IndustrialLogger.info("  - MetaMachine: " + metaMachineClass.getName());
                IndustrialLogger.info("  - MachineDefinition: " + machineDefinitionClass.getName());
                IndustrialLogger.info("  - GTCapability: " + gtCapabilityClass.getName());
            } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
                isGTLoaded = false;
                IndustrialLogger.warn("✗ GregTech CEu not found or incompatible version");
                IndustrialLogger.warn("  Error: " + e.getMessage());
            }

            checkDone = true;
            IndustrialLogger.info("=== GregTech CEu Integration: " + (isGTLoaded ? "ENABLED" : "DISABLED") + " ===");
        }
        return isGTLoaded;
    }

    /**
     * Checks if a BlockEntity is a GT machine (silent mode, no logging).
     * <p>
     * In GTCEU 1.8.0, all GT machines use MetaMachineBlockEntity as their BlockEntity type.
     * This method is optimized for frequent calls during scanning.
     *
     * @param be the BlockEntity to check
     * @return true if the BlockEntity is a GT machine
     */
    public static boolean isGTMachine(BlockEntity be) {
        if (!isGTLoaded() || be == null) return false;

        try {
            if (metaMachineBlockEntityClass != null && metaMachineBlockEntityClass.isInstance(be)) {
                return true;
            }
            return false;
        } catch (Throwable t) {
            // Silent failure, no logging
            return false;
        }
    }

    /**
     * Checks if a BlockEntity is a GT machine (verbose mode, with logging).
     * <p>
     * This method should only be used for the scan command or debugging purposes.
     * It outputs debug logs for each machine found.
     *
     * @param be the BlockEntity to check
     * @return true if the BlockEntity is a GT machine
     */
    public static boolean isGTMachineVerbose(BlockEntity be) {
        if (!isGTLoaded() || be == null) return false;

        try {
            if (metaMachineBlockEntityClass != null && metaMachineBlockEntityClass.isInstance(be)) {
                IndustrialLogger.debugGT("Found MetaMachineBlockEntity at " + be.getBlockPos());
                return true;
            }
            return false;
        } catch (Throwable t) {
            IndustrialLogger.error("Error checking if BlockEntity is GT machine: " + t.getMessage());
            return false;
        }
    }

    /**
     * Gets the voltage tier of a GT machine.
     * <p>
     * For machines with active recipes, gets the tier from the recipe's EU/t.
     * For multiblock structures, attempts to get tier from energy hatches.
     * Falls back to machine definition tier.
     *
     * @param be the BlockEntity to query
     * @return the voltage tier (0=ULV, 1=LV, 2=MV, etc.), or -1 if not a GT machine or error
     */
    public static int getVoltageTier(BlockEntity be) {
        if (!isGTLoaded() || be == null || !isGTMachine(be)) return -1;

        try {
            Object machine = getMetaMachineMethod.invoke(be);
            if (machine == null) {
                return -1;
            }

            // Priority 1: Get tier from active recipe's EU/t
            try {
                // Check if machine has IRecipeLogicMachine interface
                Class<?> recipeLogicMachineClass = Class.forName("com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine");
                if (recipeLogicMachineClass.isInstance(machine)) {
                    // Get RecipeLogic
                    java.lang.reflect.Method getRecipeLogicMethod = recipeLogicMachineClass.getMethod("getRecipeLogic");
                    Object recipeLogic = getRecipeLogicMethod.invoke(machine);

                    if (recipeLogic != null) {
                        // Use getLastRecipe() method instead of accessing field directly
                        java.lang.reflect.Method getLastRecipeMethod = recipeLogic.getClass().getMethod("getLastRecipe");
                        Object recipe = getLastRecipeMethod.invoke(recipeLogic);

                        if (recipe != null) {
                            // Get EU/t from recipe
                            java.lang.reflect.Method getInputEUtMethod = recipe.getClass().getMethod("getInputEUt");
                            long eut = (long) getInputEUtMethod.invoke(recipe);

                            // Calculate tier from EU/t
                            int tier = getVoltageTierFromEUt(eut);
                            if (tier >= 0) {
                                IndustrialLogger.info("[GTIntegration] Machine at " + be.getBlockPos() +
                                    " has active recipe with EU/t=" + eut + ", calculated tier=" + tier);
                                return tier;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Recipe-based tier detection failed, continue to other methods
            }

            // Priority 2: For multiblock controller, try to get voltage tier from energy hatch
            if (multiblockControllerClass != null && multiblockControllerClass.isInstance(machine)) {
                int multiblockTier = getMultiblockVoltageTier(machine);
                if (multiblockTier >= 0) {
                    return multiblockTier;
                }
            }

            // Priority 3: Get from definition
            Object definition = getDefinitionMethod.invoke(machine);
            if (definition != null) {
                int tier = (int) getTierMethod.invoke(definition);
                return tier;
            }
        } catch (Throwable t) {
            // Silent failure
        }
        return -1;
    }

    /**
     * Calculates voltage tier from EU/t consumption.
     * GT voltage tiers: ULV=8, LV=32, MV=128, HV=512, EV=2048, IV=8192, LuV=32768, ZPM=131072, UV=524288
     */
    private static int getVoltageTierFromEUt(long eut) {
        if (eut <= 0) return -1;
        if (eut <= 8) return 0;      // ULV
        if (eut <= 32) return 1;     // LV
        if (eut <= 128) return 2;    // MV
        if (eut <= 512) return 3;    // HV
        if (eut <= 2048) return 4;   // EV
        if (eut <= 8192) return 5;   // IV
        if (eut <= 32768) return 6;  // LuV
        if (eut <= 131072) return 7; // ZPM
        if (eut <= 524288) return 8; // UV
        return 9; // UHV+
    }

    /**
     * Gets the voltage tier of a multiblock structure from its energy hatches.
     * <p>
     * This method scans all parts of a multiblock structure looking for energy hatches
     * or tiered IO parts, and returns the highest tier found. This provides more accurate
     * voltage tier information for multiblocks than the controller's definition.
     *
     * @param multiblockController the multiblock controller machine
     * @return the highest voltage tier from energy hatches, or -1 if none found
     */
    private static int getMultiblockVoltageTier(Object multiblockController) {
        try {
            // Get parts list
            java.lang.reflect.Method getPartsMethod = multiblockController.getClass().getMethod("getParts");
            Object parts = getPartsMethod.invoke(multiblockController);

            if (parts instanceof java.util.List) {
                java.util.List<?> partsList = (java.util.List<?>) parts;
                int maxTier = -1;
                for (Object part : partsList) {
                    // Check if it's an EnergyHatchPartMachine or parent class TieredIOPartMachine
                    String partClassName = part.getClass().getName();
                    if (partClassName.contains("EnergyHatchPartMachine") ||
                        partClassName.contains("TieredIOPartMachine")) {
                        try {
                            // Try to get tier (inherited from TieredPartMachine)
                            java.lang.reflect.Method getTierMethod = part.getClass().getMethod("getTier");
                            int tier = (int) getTierMethod.invoke(part);
                            if (tier > maxTier) {
                                maxTier = tier;
                            }
                        } catch (Exception e) {
                            // Single part failure doesn't affect others
                        }
                    }
                }
                if (maxTier >= 0) {
                    return maxTier;
                }
            }
        } catch (Throwable t) {
            // Silent failure
        }
        return -1;
    }

    /**
     * Gets the voltage tier of a GT machine (verbose mode, with logging).
     * <p>
     * This method should only be used for the scan command. It outputs detailed
     * debug logs for troubleshooting.
     *
     * @param be the BlockEntity to query
     * @return the voltage tier, or -1 if not a GT machine or error
     */
    public static int getVoltageTierVerbose(BlockEntity be) {
        if (!isGTLoaded() || be == null || !isGTMachine(be)) return -1;

        try {
            Object machine = getMetaMachineMethod.invoke(be);
            if (machine == null) {
                IndustrialLogger.debugGT("MetaMachine is null at " + be.getBlockPos());
                return -1;
            }

            Object definition = getDefinitionMethod.invoke(machine);
            if (definition != null) {
                int tier = (int) getTierMethod.invoke(definition);
                IndustrialLogger.debugGT(String.format("Machine at %s has tier: %d", be.getBlockPos(), tier));
                return tier;
            } else {
                IndustrialLogger.debugGT("Definition is null at " + be.getBlockPos());
            }
        } catch (Throwable t) {
            IndustrialLogger.error("Error getting voltage tier at " + be.getBlockPos() + ": " + t.getMessage());
        }
        return -1;
    }

    /**
     * Checks if a GT machine is currently active (working).
     * <p>
     * A machine is considered active if its IWorkable capability's isActive() method returns true.
     * This method does not output logs to avoid spam during frequent checks.
     *
     * @param be the BlockEntity to check
     * @return true if the machine is active
     */
    public static boolean isMachineActive(BlockEntity be) {
        if (!isGTLoaded() || be == null || !isGTMachine(be)) return false;

        try {
            LazyOptional<?> workableCap = be.getCapability((net.minecraftforge.common.capabilities.Capability<?>) capabilityWorkable);
            if (workableCap != null && workableCap.isPresent()) {
                Object workable = workableCap.orElse(null);
                if (workable != null) {
                    boolean active = (boolean) isActiveMethod.invoke(workable);
                    return active;
                }
            }
        } catch (Throwable t) {
            // Silent failure
        }
        return false;
    }

    /**
     * Checks if a GT machine is a multiblock structure.
     * <p>
     * This method does not output logs to avoid spam during frequent checks.
     *
     * @param be the BlockEntity to check
     * @return true if the machine is a multiblock controller
     */
    public static boolean isMultiblock(BlockEntity be) {
        if (!isGTLoaded() || be == null || !isGTMachine(be)) return false;

        try {
            Object machine = getMetaMachineMethod.invoke(be);
            if (machine != null && multiblockControllerClass != null) {
                return multiblockControllerClass.isInstance(machine);
            }
        } catch (Throwable t) {
            // Silent failure
        }
        return false;
    }

    /**
     * Checks if a GT machine is a multiblock part (not a controller).
     * <p>
     * Multiblock parts include energy hatches, input/output buses, etc.
     * We want to filter these out when scanning to avoid counting the same
     * multiblock structure multiple times.
     *
     * @param be the BlockEntity to check
     * @return true if the machine is a multiblock part (not a controller)
     */
    public static boolean isMultiblockPart(BlockEntity be) {
        if (!isGTLoaded() || be == null || !isGTMachine(be)) return false;

        try {
            Object machine = getMetaMachineMethod.invoke(be);
            if (machine == null) return false;

            // Check if it's a multiblock controller - if yes, it's NOT a part
            if (multiblockControllerClass != null && multiblockControllerClass.isInstance(machine)) {
                return false;
            }

            // Check if the machine class name contains "PartMachine"
            String className = machine.getClass().getName();
            return className.contains("PartMachine");
        } catch (Throwable t) {
            // Silent failure
        }
        return false;
    }

    /**
     * Checks if a machine, cable, or energy hatch has energy flowing through it or stored.
     * <p>
     * For multiblock structures, this method checks if energy hatches are present.
     * For single-block machines, it checks the energy capability.
     * <p>
     * This is used to determine if a machine should generate pollution and be targeted by mobs.
     *
     * @param be the BlockEntity to check
     * @return true if the machine has energy or is active
     */
    public static boolean hasEnergyOrActive(BlockEntity be) {
        if (!isGTLoaded() || be == null || !isGTMachine(be)) {
            return false;
        }

        try {
            Object machine = null;

            // Get MetaMachine instance
            if (metaMachineBlockEntityClass != null && metaMachineBlockEntityClass.isInstance(be)) {
                machine = getMetaMachineMethod.invoke(be);
            } else if (metaMachineClass != null && metaMachineClass.isInstance(be)) {
                machine = be;
            }

            // 1. Check if currently working
            if (machine != null && iWorkableClass != null && iWorkableClass.isInstance(machine)) {
                boolean active = (boolean) isActiveMethod.invoke(machine);
                if (active) {
                    return true;
                }
            }

            // 2. Special handling: Check energy hatches for multiblock structures
            if (machine != null && multiblockControllerClass != null && multiblockControllerClass.isInstance(machine)) {
                try {
                    // Get parts list
                    java.lang.reflect.Method getPartsMethod = machine.getClass().getMethod("getParts");
                    Object parts = getPartsMethod.invoke(machine);

                    if (parts instanceof java.util.List) {
                        for (Object part : (java.util.List<?>) parts) {
                            // Check if it's an energy hatch
                            String partClassName = part.getClass().getName();
                            if (partClassName.contains("EnergyHatchPartMachine")) {
                                // Energy hatch exists means multiblock has energy
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                    // If getting energy hatch fails, continue to check energy storage
                }
            }

            // 3. Check energy storage (single-block machines or energy hatches)
            // Use Forge standard energy capability instead of GTCEU custom capability
            LazyOptional<?> cap = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY);
            if (cap.isPresent()) {
                Object energyStorage = cap.orElse(null);
                if (energyStorage instanceof net.minecraftforge.energy.IEnergyStorage storage) {
                    boolean hasEnergy = storage.getEnergyStored() > 0;
                    return hasEnergy;
                }
            }
        } catch (Throwable t) {
            // Silent failure
        }
        return false;
    }

    /**
     * Checks if a machine has energy (verbose mode, with detailed logging).
     * <p>
     * This method should only be used for the scan command. It outputs detailed
     * debug logs for troubleshooting energy detection issues.
     *
     * @param be the BlockEntity to check
     * @return true if the machine has energy or is active
     */
    public static boolean hasEnergyOrActiveVerbose(BlockEntity be) {
        if (!isGTLoaded() || be == null || !isGTMachine(be)) {
            IndustrialLogger.debugMachine("hasEnergyOrActive: Not GT machine or null");
            return false;
        }

        try {
            // 1. Check if currently working via CAPABILITY_WORKABLE
            if (capabilityWorkable != null) {
                LazyOptional<?> workableCap = be.getCapability((net.minecraftforge.common.capabilities.Capability<?>) capabilityWorkable);
                if (workableCap != null && workableCap.isPresent()) {
                    Object workable = workableCap.orElse(null);
                    if (workable != null) {
                        boolean active = (boolean) isActiveMethod.invoke(workable);
                        IndustrialLogger.debugMachine("Machine active check: " + active);
                        if (active) {
                            return true;
                        }
                    }
                } else {
                    IndustrialLogger.debugMachine("Machine is not IWorkable");
                }
            }

            // 2. Check for energy hatch in multiblock
            Object machine = getMetaMachineMethod.invoke(be);
            if (machine != null && multiblockControllerClass != null && multiblockControllerClass.isInstance(machine)) {
                IndustrialLogger.debugMachine("Machine is multiblock controller, checking energy hatches...");
                try {
                    java.lang.reflect.Method getPartsMethod = machine.getClass().getMethod("getParts");
                    Object parts = getPartsMethod.invoke(machine);

                    if (parts instanceof java.util.List) {
                        IndustrialLogger.debugMachine("Found " + ((java.util.List<?>) parts).size() + " parts");
                        for (Object part : (java.util.List<?>) parts) {
                            String partClassName = part.getClass().getName();
                            if (partClassName.contains("EnergyHatchPartMachine")) {
                                IndustrialLogger.debugMachine("Found energy hatch: " + partClassName);
                                if (part instanceof BlockEntity partBE) {
                                    LazyOptional<?> cap = partBE.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY);
                                    if (cap.isPresent()) {
                                        Object energyStorage = cap.orElse(null);
                                        if (energyStorage instanceof net.minecraftforge.energy.IEnergyStorage storage) {
                                            int stored = storage.getEnergyStored();
                                            IndustrialLogger.debugMachine("Energy hatch has " + stored + " FE");
                                            if (stored > 0) {
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    IndustrialLogger.debugMachine("Error checking energy hatches: " + e.getMessage());
                }
            }

            // 3. Check energy using ForgeCapabilities.ENERGY
            LazyOptional<?> cap = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY);
            IndustrialLogger.debugMachine("Energy capability present: " + cap.isPresent());

            if (cap.isPresent()) {
                Object energyStorage = cap.orElse(null);
                IndustrialLogger.debugMachine("Energy storage type: " + (energyStorage != null ? energyStorage.getClass().getName() : "null"));

                if (energyStorage instanceof net.minecraftforge.energy.IEnergyStorage storage) {
                    int stored = storage.getEnergyStored();
                    IndustrialLogger.debugMachine("Energy stored: " + stored + " FE");
                    boolean hasEnergy = stored > 0;
                    return hasEnergy;
                } else {
                    IndustrialLogger.debugMachine("Energy storage is not IEnergyStorage");
                }
            }
        } catch (Throwable t) {
            IndustrialLogger.error("Error checking energy: " + t.getMessage(), t);
        }

        IndustrialLogger.debugMachine("hasEnergyOrActive: Returning false");
        return false;
    }

    /**
     * Checks if a machine has an active recipe running.
     * This is more precise than hasEnergyOrActive as it specifically checks for recipe execution.
     *
     * @param be The BlockEntity to check
     * @return true if the machine is actively running a recipe
     */
    public static boolean hasActiveRecipe(BlockEntity be) {
        if (!isGTLoaded() || be == null) {
            return false;
        }

        try {
            if (metaMachineBlockEntityClass.isInstance(be)) {
                Object metaMachine = getMetaMachineMethod.invoke(be);
                if (metaMachine != null) {
                    // Check IWorkable capability
                    if (capabilityWorkable != null) {
                        LazyOptional<?> workableCap = (LazyOptional<?>) metaMachine.getClass()
                                .getMethod("getCapability", net.minecraftforge.common.capabilities.Capability.class)
                                .invoke(metaMachine, capabilityWorkable);

                        if (workableCap.isPresent()) {
                            Object workable = workableCap.orElse(null);
                            if (workable != null && iWorkableClass.isInstance(workable)) {
                                return (boolean) isActiveMethod.invoke(workable);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silent fail
        }

        return false;
    }

    /**
     * Gets the EU/t (energy per tick) of the currently running recipe.
     *
     * @param be The BlockEntity to check
     * @return The recipe EU/t, or 0 if no recipe is running
     */
    public static long getRecipeEUt(BlockEntity be) {
        if (!isGTLoaded() || be == null) {
            return 0;
        }

        try {
            if (metaMachineBlockEntityClass.isInstance(be)) {
                Object metaMachine = getMetaMachineMethod.invoke(be);
                if (metaMachine != null && iWorkableClass.isInstance(metaMachine)) {
                    // Try to get recipe logic
                    Method getRecipeLogicMethod = metaMachine.getClass().getMethod("getRecipeLogic");
                    Object recipeLogic = getRecipeLogicMethod.invoke(metaMachine);

                    if (recipeLogic != null) {
                        // Get last recipe
                        Method getLastRecipeMethod = recipeLogic.getClass().getMethod("getLastRecipe");
                        Object recipe = getLastRecipeMethod.invoke(recipeLogic);

                        if (recipe != null) {
                            // Get EU/t from recipe data
                            Method getDataMethod = recipe.getClass().getMethod("data");
                            Object recipeData = getDataMethod.invoke(recipe);

                            if (recipeData != null) {
                                Method getEUtMethod = recipeData.getClass().getMethod("getEUt");
                                return (long) getEUtMethod.invoke(recipeData);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silent fail
        }

        return 0;
    }

    /**
     * Checks if a machine has a muffler hatch (for multiblocks).
     * Machines with muffler hatches produce additional pollution.
     *
     * @param be The BlockEntity to check
     * @return true if the machine has a muffler hatch
     */
    public static boolean hasMufflerHatch(BlockEntity be) {
        if (!isGTLoaded() || be == null) {
            return false;
        }

        try {
            if (metaMachineBlockEntityClass.isInstance(be)) {
                Object metaMachine = getMetaMachineMethod.invoke(be);

                // Check if it's a multiblock controller
                if (metaMachine != null && multiblockControllerClass.isInstance(metaMachine)) {
                    // Get multiblock parts
                    Method getPartsMethod = multiblockControllerClass.getMethod("getParts");
                    Object parts = getPartsMethod.invoke(metaMachine);

                    if (parts instanceof java.util.List) {
                        for (Object part : (java.util.List<?>) parts) {
                            // Check if part is a muffler hatch
                            String partClassName = part.getClass().getSimpleName();
                            if (partClassName.contains("Muffler")) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silent fail
        }

        return false;
    }
}
