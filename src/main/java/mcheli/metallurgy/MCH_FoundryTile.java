package mcheli.metallurgy;

import java.util.List;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class MCH_FoundryTile extends MCH_MetallurgyTile {
    private static final int[] INPUT = {0}, FUEL = {1}, OUTPUT = {2};
    public int steel;
    public int heat;
    public int meltTimer;
    public int progress;
    private String mold = "grate";
    public float tilt;
    public float lastTilt;
    public MCH_FoundryTile() { super(3); }
    @Override public String getInventoryName() { return "container.mcheli.steel_foundry"; }
    @Override public int[] getAccessibleSlotsFromSide(int side) { return side == 0 ? OUTPUT : side == 1 ? INPUT : FUEL; }
    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 ? MCH_Metallurgy.isSteel(stack) : slot == 1 && stack != null && stack.getItem() == Items.coal;
    }
    @Override public boolean canExtractItem(int slot, ItemStack stack, int side) { return slot == 2 && side == 0; }

    public MCH_Metallurgy.Cast selected() { return MCH_Metallurgy.cast(mold); }

    public void changeMold(int direction) {
        List<MCH_Metallurgy.Cast> casts = MCH_Metallurgy.casts();
        if (casts.isEmpty()) return;
        int index = selectionIndex(casts);
        mold = casts.get((index + direction + casts.size()) % casts.size()).name;
        progress = 0;
        markDirty();
    }

    private int selectionIndex(List<MCH_Metallurgy.Cast> casts) {
        for (int i = 0; i < casts.size(); i++) if (casts.get(i).name.equals(mold)) return i;
        return 0;
    }

    @Override public void updateEntity() {
        if (worldObj == null) return;
        if (worldObj.isRemote) {
            lastTilt = tilt;
            tilt = Math.max(0, Math.min(30, tilt + (isActive() ? 1 : -1)));
            return;
        }
        if (!formed) { setActive(false); return; }
        MCH_Metallurgy.Cast cast = selected();
        if (cast == null) {
            List<MCH_Metallurgy.Cast> casts = MCH_Metallurgy.casts();
            if (!casts.isEmpty()) { mold = casts.get(0).name; progress = 0; markDirty(); cast = selected(); }
        }
        boolean changed = false;
        // Melt ingots only. Recycling converted armor/parts would multiply provider materials.
        boolean canMelt = MCH_Metallurgy.isSteel(slots[0]) && steel < MCH_Metallurgy.MAX_STEEL;
        if (canMelt && heat == 0 && isItemValidForSlot(1, slots[1])) {
            decrStackSize(1, 1);
            heat = MCH_Metallurgy.MAX_HEAT;
            changed = true;
        }
        if (canMelt && heat > 0) {
            if (++meltTimer >= MCH_Metallurgy.MELT_TICKS) {
                decrStackSize(0, 1);
                steel++;
                heat--;
                meltTimer = 0;
            }
            changed = true;
        } else if (meltTimer != 0) { meltTimer = 0; changed = true; }

        if (cast != null && steel >= cast.steel && hasOutputSpace(2, cast.output())) {
            if (++progress >= MCH_Metallurgy.CAST_TICKS) {
                steel -= cast.steel;
                output(2, cast.stack());
                progress = 0;
            }
            changed = true;
        } else if (progress != 0) { progress = 0; changed = true; }
        setActive(progress > 0);
        if (changed) markDirty();
        // Retry blocked delivery at hopper cadence, including when casting is idle.
        if (slots[2] != null && worldObj.getTotalWorldTime() % 8 == 0) pushOutputDown();
    }

    private void pushOutputDown() {
        TileEntity below = worldObj.getTileEntity(xCoord, yCoord - 1, zCoord);
        if (!(below instanceof IInventory)) return;
        IInventory inventory = (IInventory) below;
        ISidedInventory sided = below instanceof ISidedInventory ? (ISidedInventory) below : null;
        int[] accessible;
        if (sided != null) accessible = sided.getAccessibleSlotsFromSide(1);
        else {
            accessible = new int[inventory.getSizeInventory()];
            for (int i = 0; i < accessible.length; i++) accessible[i] = i;
        }
        for (int slot : accessible) {
            if (slots[2] == null) break;
            ItemStack source = slots[2], dest = inventory.getStackInSlot(slot);
            if (!inventory.isItemValidForSlot(slot, source)
                || (sided != null && !sided.canInsertItem(slot, source, 1))) continue;
            if (dest != null && (!dest.isItemEqual(source) || !ItemStack.areItemStackTagsEqual(dest, source))) continue;
            int limit = Math.min(inventory.getInventoryStackLimit(), source.getMaxStackSize());
            int move = Math.min(source.stackSize, limit - (dest == null ? 0 : dest.stackSize));
            if (move <= 0) continue;
            if (dest == null) { dest = source.copy(); dest.stackSize = move; }
            else dest.stackSize += move;
            inventory.setInventorySlotContents(slot, dest);
            source.stackSize -= move;
            if (source.stackSize == 0) slots[2] = null;
            inventory.markDirty();
            markDirty();
        }
    }

    @Override public int[] fields() {
        return new int[] {steel, heat, meltTimer, progress, selectionIndex(MCH_Metallurgy.casts())};
    }
    @Override public void receiveField(int id, int value) {
        switch (id) {
            case 0: steel = value; break;
            case 1: heat = value; break;
            case 2: meltTimer = value; break;
            case 3: progress = value; break;
            case 4:
                List<MCH_Metallurgy.Cast> casts = MCH_Metallurgy.casts();
                if (value >= 0 && value < casts.size()) mold = casts.get(value).name;
                break;
            default: break;
        }
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        steel = Math.max(0, Math.min(MCH_Metallurgy.MAX_STEEL, tag.getInteger("Steel")));
        heat = Math.max(0, Math.min(MCH_Metallurgy.MAX_HEAT, tag.getInteger("Heat")));
        meltTimer = Math.max(0, Math.min(MCH_Metallurgy.MELT_TICKS - 1, tag.getInteger("MeltTimer")));
        progress = Math.max(0, Math.min(MCH_Metallurgy.CAST_TICKS - 1, tag.getInteger("Progress")));
        if (tag.hasKey("Mold", 8)) mold = tag.getString("Mold");
    }
    @Override public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Steel", steel);
        tag.setInteger("Heat", heat);
        tag.setInteger("MeltTimer", meltTimer);
        tag.setInteger("Progress", progress);
        tag.setString("Mold", mold);
    }
}
