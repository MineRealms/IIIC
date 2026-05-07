package cn.minerealms.iic.turrets.common.block_entity;

import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.upgrade.IUpgradeData;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

/**
 * CIWS炮塔升级数据
 */
public final class CIWSTurretUpgradeData implements IUpgradeData {
    private final MachineEnergyContainer<CIWSTurretBlockEntity> energyContainer;
    private final EnergyInventorySlot energySlot;
    private final boolean targetsHostile;
    private final boolean targetsPassive;
    private final boolean targetsPlayers;
    private final boolean targetsTrusted;
    private final boolean areaMode;
    private final CompoundTag components;

    public CIWSTurretUpgradeData(
            MachineEnergyContainer<CIWSTurretBlockEntity> energyContainer,
            EnergyInventorySlot energySlot,
            boolean targetsHostile,
            boolean targetsPassive,
            boolean targetsPlayers,
            boolean targetsTrusted,
            boolean areaMode,
            List<ITileComponent> components
    ) {
        this.energyContainer = energyContainer;
        this.energySlot = energySlot;
        this.targetsHostile = targetsHostile;
        this.targetsPassive = targetsPassive;
        this.targetsPlayers = targetsPlayers;
        this.targetsTrusted = targetsTrusted;
        this.areaMode = areaMode;
        this.components = new CompoundTag();
        for (ITileComponent component : components) {
            component.write(this.components);
        }
    }

    public MachineEnergyContainer<CIWSTurretBlockEntity> energyContainer() {
        return energyContainer;
    }

    public EnergyInventorySlot energySlot() {
        return energySlot;
    }

    public boolean targetsHostile() {
        return targetsHostile;
    }

    public boolean targetsPassive() {
        return targetsPassive;
    }

    public boolean targetsPlayers() {
        return targetsPlayers;
    }

    public boolean targetsTrusted() {
        return targetsTrusted;
    }

    public boolean areaMode() {
        return areaMode;
    }

    public CompoundTag components() {
        return components;
    }

    public void writeToStack(CompoundTag tag) {
        tag.put("energyContainer", energyContainer.serializeNBT());
        tag.put("energySlot", energySlot.serializeNBT());
        tag.putBoolean("targetsHostile", targetsHostile);
        tag.putBoolean("targetsPassive", targetsPassive);
        tag.putBoolean("targetsPlayers", targetsPlayers);
        tag.putBoolean("targetsTrusted", targetsTrusted);
        tag.putBoolean("areaMode", areaMode);
    }
}
