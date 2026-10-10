package mcheli.metallurgy;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class MCH_BlastFurnaceTile extends MCH_MetallurgyTile {
    private static final int[] INPUTS = {0, 1, 2}, OUTPUTS = {3, 4, 5}, FUEL = {6};
    public int fuel;
    public int progress;
    public float level;
    public MCH_BlastFurnaceTile() { super(7); }
    @Override public String getInventoryName() { return "container.mcheli.blast_furnace"; }
    @Override public int[] getAccessibleSlotsFromSide(int side) { return side == 0 ? OUTPUTS : side == 1 ? INPUTS : FUEL; }
    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot < 3 && slot >= 0 ? MCH_Metallurgy.isIron(stack)
            : slot == 6 && MCH_Metallurgy.fuelValue(stack) > 0;
    }
    @Override public boolean canExtractItem(int slot, ItemStack stack, int side) { return side == 0 && slot >= 3 && slot <= 5; }

    @Override public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;
        if (!formed || !MCH_MetallurgyStructure.foundation(worldObj, xCoord, yCoord, zCoord)) {
            setActive(false); setLevel(0);
            if (formed && progress != 0) { progress = 0; markDirty(); }
            return;
        }
        // HBM/RTM owns steel production. Do not burn input or manufacture a private bypass.
        if (!MCH_Metallurgy.canProduceSteel()) { setActive(false); setLevel(0); return; }
        int value;
        while ((value = MCH_Metallurgy.fuelValue(slots[6])) > 0 && fuel + value <= MCH_Metallurgy.MAX_FUEL) {
            decrStackSize(6, 1);
            fuel += value;
            markDirty();
        }
        int input = -1, output = -1;
        net.minecraft.item.Item steel = MCH_Metallurgy.steelItem();
        for (int i : INPUTS) if (MCH_Metallurgy.isIron(slots[i])) { input = i; break; }
        int iron = 0;
        for (int i : INPUTS) if (MCH_Metallurgy.isIron(slots[i])) iron += slots[i].stackSize;
        setLevel(fuel > 0 ? iron / 192.0F : 0);
        for (int i : OUTPUTS) if (hasOutputSpace(i, steel)) { output = i; break; }
        if (input < 0 || output < 0 || fuel == 0) {
            setActive(false);
            if (progress != 0) { progress = 0; markDirty(); }
            return;
        }
        setActive(true);
        if (++progress >= MCH_Metallurgy.BLAST_TICKS) {
            decrStackSize(input, 1);
            output(output, new ItemStack(steel));
            fuel--;
            progress = 0;
        }
        // Timers must dirty the chunk too, so autosaves/unloads preserve mid-process state.
        markDirty();
    }

    private void setLevel(float value) {
        if (level == value) return;
        level = value;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }
    @Override protected void writeVisualState(NBTTagCompound tag) {
        super.writeVisualState(tag);
        tag.setFloat("Level", level);
    }
    @Override protected void readVisualState(NBTTagCompound tag) {
        super.readVisualState(tag);
        level = Math.max(0, Math.min(1, tag.getFloat("Level")));
    }

    @Override public int[] fields() { return new int[] {fuel, progress}; }
    @Override public void receiveField(int id, int value) { if (id == 0) fuel = value; else if (id == 1) progress = value; }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        fuel = Math.max(0, Math.min(MCH_Metallurgy.MAX_FUEL, tag.getInteger("Fuel")));
        progress = Math.max(0, Math.min(MCH_Metallurgy.BLAST_TICKS - 1, tag.getInteger("Progress")));
    }
    @Override public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Fuel", fuel);
        tag.setInteger("Progress", progress);
    }
}
