package mcheli.metallurgy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

/** Native inventories; no XF base machine, fluid network, electricity or faction state. */
public abstract class MCH_MetallurgyTile extends TileEntity implements ISidedInventory {
    protected final ItemStack[] slots;
    private boolean active;
    public boolean formed;
    boolean dismantling;
    protected MCH_MetallurgyTile(int size) { slots = new ItemStack[size]; }
    public abstract int[] fields();
    public abstract void receiveField(int id, int value);
    public boolean isActive() { return active; }
    protected void setActive(boolean value) {
        if (active == value) return;
        active = value;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }
    @Override public net.minecraft.network.Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeVisualState(tag);
        return new net.minecraft.network.play.server.S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }
    @Override public void onDataPacket(net.minecraft.network.NetworkManager manager,
        net.minecraft.network.play.server.S35PacketUpdateTileEntity packet) {
        readVisualState(packet.func_148857_g());
    }

    protected void writeVisualState(NBTTagCompound tag) {
        tag.setBoolean("Active", active);
        tag.setBoolean("Formed", formed);
    }
    protected void readVisualState(NBTTagCompound tag) {
        active = tag.getBoolean("Active");
        formed = tag.getBoolean("Formed");
    }

    @Override public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
        // Foundry crucible and scaffold overhang the source's occupied block volume.
        return this instanceof MCH_FoundryTile
            ? net.minecraft.util.AxisAlignedBB.getBoundingBox(xCoord - 5, yCoord, zCoord - 5, xCoord + 6, yCoord + 7, zCoord + 6)
            : net.minecraft.util.AxisAlignedBB.getBoundingBox(xCoord - 1, yCoord, zCoord - 1, xCoord + 2, yCoord + 5, zCoord + 2);
    }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public double getMaxRenderDistanceSquared() { return 65536.0D; }

    @Override public int getSizeInventory() { return slots.length; }
    @Override public ItemStack getStackInSlot(int slot) { return slots[slot]; }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public boolean hasCustomInventoryName() { return false; }
    @Override public void openInventory() {}
    @Override public void closeInventory() {}

    @Override public ItemStack decrStackSize(int slot, int count) {
        ItemStack stack = slots[slot];
        if (stack == null || count <= 0) return null;
        ItemStack removed = stack.stackSize <= count ? stack : stack.splitStack(count);
        if (removed == stack || stack.stackSize == 0) slots[slot] = null;
        markDirty();
        return removed;
    }

    @Override public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack stack = slots[slot];
        slots[slot] = null;
        markDirty();
        return stack;
    }

    @Override public void setInventorySlotContents(int slot, ItemStack stack) {
        slots[slot] = stack;
        if (stack != null) stack.stackSize = Math.min(stack.stackSize,
            Math.min(getInventoryStackLimit(), stack.getMaxStackSize()));
        markDirty();
    }

    @Override public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj != null && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
            && player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64;
    }

    @Override public boolean canInsertItem(int slot, ItemStack stack, int side) {
        for (int allowed : getAccessibleSlotsFromSide(side)) {
            if (allowed == slot) return isItemValidForSlot(slot, stack);
        }
        return false;
    }

    protected boolean hasOutputSpace(int slot, net.minecraft.item.Item output) {
        ItemStack old = slots[slot];
        return old == null || (old.getItem() == output && old.getItemDamage() == 0 && !old.hasTagCompound()
            && old.stackSize < Math.min(getInventoryStackLimit(), old.getMaxStackSize()));
    }

    protected void output(int slot, ItemStack output) {
        if (slots[slot] == null) slots[slot] = output.copy();
        else slots[slot].stackSize += output.stackSize;
    }

    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        formed = tag.getBoolean("Formed");
        for (int i = 0; i < slots.length; i++) slots[i] = null;
        NBTTagList list = tag.getTagList("Items", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            int slot = entry.getByte("Slot") & 255;
            if (slot < slots.length) slots[slot] = ItemStack.loadItemStackFromNBT(entry);
        }
    }

    @Override public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("MetallurgyVersion", 2);
        tag.setBoolean("Formed", formed);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) continue;
            NBTTagCompound entry = new NBTTagCompound();
            entry.setByte("Slot", (byte) i);
            slots[i].writeToNBT(entry);
            list.appendTag(entry);
        }
        tag.setTag("Items", list);
    }
}
