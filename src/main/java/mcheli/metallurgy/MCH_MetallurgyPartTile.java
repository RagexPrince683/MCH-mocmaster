package mcheli.metallurgy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/** Saved direct controller link and inventory forwarding only. No fluid or RF APIs. */
public class MCH_MetallurgyPartTile extends TileEntity implements ISidedInventory {
    private static final int[] NO_SLOTS = new int[0];
    private int controllerX, controllerY, controllerZ, facing;
    private boolean linked, foundry;

    public void link(int x, int y, int z, int facing, boolean foundry) {
        controllerX = x; controllerY = y; controllerZ = z;
        this.facing = facing; this.foundry = foundry; linked = true;
        markDirty();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }
    public boolean belongsTo(MCH_MetallurgyTile tile) {
        return linked && tile.xCoord == controllerX && tile.yCoord == controllerY && tile.zCoord == controllerZ
            && foundry == (tile instanceof MCH_FoundryTile);
    }
    public boolean ownerLoaded() {
        return linked && worldObj != null && worldObj.blockExists(controllerX, controllerY, controllerZ);
    }
    public MCH_MetallurgyTile controller() {
        if (!ownerLoaded()) return null;
        if (worldObj.getBlock(controllerX, controllerY, controllerZ) != (foundry ? MCH_Metallurgy.foundry : MCH_Metallurgy.blastFurnace)
            || worldObj.getBlockMetadata(controllerX, controllerY, controllerZ) != facing) return null;
        TileEntity tile = worldObj.getTileEntity(controllerX, controllerY, controllerZ);
        return tile instanceof MCH_MetallurgyTile && belongsTo((MCH_MetallurgyTile) tile)
            && ((MCH_MetallurgyTile) tile).formed ? (MCH_MetallurgyTile) tile : null;
    }
    public void validateOwner() {
        if (worldObj == null || worldObj.isRemote) return;
        if (linked && !ownerLoaded()) {
            worldObj.scheduleBlockUpdate(xCoord, yCoord, zCoord, MCH_Metallurgy.part, 200);
        } else if (controller() == null) worldObj.setBlockToAir(xCoord, yCoord, zCoord);
    }
    @Override public boolean canUpdate() { return false; }
    @Override public void validate() {
        super.validate();
        if (worldObj != null && !worldObj.isRemote) {
            // Native pending update queue also works while the chunk is being loaded.
            worldObj.func_147446_b(xCoord, yCoord, zCoord, MCH_Metallurgy.part, 1, 0);
        }
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        linked = tag.getBoolean("Linked"); foundry = tag.getBoolean("Foundry");
        controllerX = tag.getInteger("ControllerX"); controllerY = tag.getInteger("ControllerY");
        controllerZ = tag.getInteger("ControllerZ"); facing = tag.getInteger("Facing");
    }
    @Override public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("Linked", linked); tag.setBoolean("Foundry", foundry);
        tag.setInteger("ControllerX", controllerX); tag.setInteger("ControllerY", controllerY);
        tag.setInteger("ControllerZ", controllerZ); tag.setInteger("Facing", facing);
    }
    @Override public net.minecraft.network.Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new net.minecraft.network.play.server.S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }
    @Override public void onDataPacket(net.minecraft.network.NetworkManager manager,
        net.minecraft.network.play.server.S35PacketUpdateTileEntity packet) { readFromNBT(packet.func_148857_g()); }

    @Override public int getSizeInventory() { MCH_MetallurgyTile c = controller(); return c == null ? 0 : c.getSizeInventory(); }
    private MCH_MetallurgyTile inventory(int slot) {
        MCH_MetallurgyTile c = controller();
        return c != null && slot >= 0 && slot < c.getSizeInventory() ? c : null;
    }
    @Override public ItemStack getStackInSlot(int slot) { MCH_MetallurgyTile c = inventory(slot); return c == null ? null : c.getStackInSlot(slot); }
    @Override public ItemStack decrStackSize(int slot, int count) { MCH_MetallurgyTile c = inventory(slot); return c == null ? null : c.decrStackSize(slot, count); }
    @Override public ItemStack getStackInSlotOnClosing(int slot) { MCH_MetallurgyTile c = inventory(slot); return c == null ? null : c.getStackInSlotOnClosing(slot); }
    @Override public void setInventorySlotContents(int slot, ItemStack stack) { MCH_MetallurgyTile c = inventory(slot); if (c != null) c.setInventorySlotContents(slot, stack); }
    @Override public String getInventoryName() { MCH_MetallurgyTile c = controller(); return c == null ? "" : c.getInventoryName(); }
    @Override public boolean hasCustomInventoryName() { return false; }
    @Override public int getInventoryStackLimit() { MCH_MetallurgyTile c = controller(); return c == null ? 0 : c.getInventoryStackLimit(); }
    @Override public boolean isUseableByPlayer(EntityPlayer player) { MCH_MetallurgyTile c = controller(); return c != null && c.isUseableByPlayer(player); }
    @Override public void openInventory() {}
    @Override public void closeInventory() {}
    @Override public void markDirty() {
        super.markDirty();
        MCH_MetallurgyTile c = controller();
        if (c != null) c.markDirty();
    }
    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { MCH_MetallurgyTile c = inventory(slot); return c != null && c.isItemValidForSlot(slot, stack); }
    @Override public int[] getAccessibleSlotsFromSide(int side) { MCH_MetallurgyTile c = controller(); return c == null ? NO_SLOTS : c.getAccessibleSlotsFromSide(side); }
    @Override public boolean canInsertItem(int slot, ItemStack stack, int side) { MCH_MetallurgyTile c = inventory(slot); return c != null && c.canInsertItem(slot, stack, side); }
    @Override public boolean canExtractItem(int slot, ItemStack stack, int side) { MCH_MetallurgyTile c = inventory(slot); return c != null && c.canExtractItem(slot, stack, side); }
}
