package mcheli.aircraft;

import mcheli.MCH_Lib;
import mcheli.parachute.MCH_ItemParachute;
import mcheli.uav.MCH_EntityUavStation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.IFluidContainerItem;

public class MCH_AircraftGuiContainer
extends Container {
    public final EntityPlayer player;
    public final MCH_EntityAircraft aircraft;

    public MCH_AircraftGuiContainer(EntityPlayer player, MCH_EntityAircraft ac) {
        this.player = player;
        this.aircraft = ac;
        MCH_AircraftInventory iv = this.aircraft.getGuiInventory();
        iv.getClass();
        this.addSlotToContainer(new Slot((IInventory)iv, 0, 10, 30));
        iv.getClass();
        this.addSlotToContainer(new Slot((IInventory)iv, 1, 10, 48));
        iv.getClass();
        this.addSlotToContainer(new Slot((IInventory)iv, 2, 10, 66));
        int num = this.aircraft.getNumEjectionSeat();
        for (int i = 0; i < num; ++i) {
            iv.getClass();
            this.addSlotToContainer(new Slot((IInventory)iv, 3 + i, 10 + 18 * i, 105));
        }
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlotToContainer(new Slot((IInventory)player.inventory, 9 + x + y * 9, 25 + x * 18, 135 + y * 18));
            }
        }
        for (int x = 0; x < 9; ++x) {
            this.addSlotToContainer(new Slot((IInventory)player.inventory, x, 25 + x * 18, 195));
        }
    }

    public int getInventoryStartIndex() {
        if (this.aircraft == null) {
            return 3;
        }
        return 3 + this.aircraft.getNumEjectionSeat();
    }

    public void detectAndSendChanges() {
        super.detectAndSendChanges();
    }

    public boolean canInteractWith(EntityPlayer player) {
        MCH_EntityUavStation us;
        if (this.aircraft.getGuiInventory().isUseableByPlayer(player)) {
            return true;
        }
        if (this.aircraft.isUAV() && (us = this.aircraft.getUavStation()) != null) {
            double x = us.posX + (double)us.posUavX;
            double z = us.posZ + (double)us.posUavZ;
            if (this.aircraft.posX < x + 10.0 && this.aircraft.posX > x - 10.0 && this.aircraft.posZ < z + 10.0 && this.aircraft.posZ > z - 10.0) {
                return true;
            }
        }
        return false;
    }

    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        block6 : {
            Slot slot;
            ItemStack itemStack;
            MCH_AircraftInventory iv;
            block7 : {
                block5 : {
                    iv = this.aircraft.getGuiInventory();
                    slot = (Slot)this.inventorySlots.get(slotIndex);
                    if (slot == null) {
                        return null;
                    }
                    itemStack = slot.getStack();
                    MCH_Lib.DbgLog(player.worldObj, "transferStackInSlot : %d :" + (Object)itemStack, slotIndex);
                    if (itemStack == null) {
                        return null;
                    }
                    if (slotIndex >= this.getInventoryStartIndex()) break block5;
                    for (int i = this.getInventoryStartIndex(); i < this.inventorySlots.size(); ++i) {
                        Slot playerSlot = (Slot)this.inventorySlots.get(i);
                        if (playerSlot.getStack() != null) continue;
                        playerSlot.putStack(itemStack);
                        slot.putStack(null);
                        return itemStack;
                    }
                    break block6;
                }
                if (!(itemStack.getItem() instanceof MCH_ItemFuel)) break block7;
                for (int i = 0; i < 3; ++i) {
                    if (iv.getFuelSlotItemStack(i) != null) continue;
                    iv.getClass();
                    iv.setInventorySlotContents(0 + i, itemStack);
                    slot.putStack(null);
                    return itemStack;
                }
                break block6;
            }
            if (!(itemStack.getItem() instanceof MCH_ItemParachute)) break block6;
            int num = this.aircraft.getNumEjectionSeat();
            for (int i = 0; i < num; ++i) {
                if (iv.getParachuteSlotItemStack(i) != null) continue;
                iv.getClass();
                iv.setInventorySlotContents(3 + i, itemStack);
                slot.putStack(null);
                return itemStack;
            }
        }
        return null;
    }

    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!player.worldObj.isRemote) {
            ItemStack is;
            int i;
            MCH_AircraftInventory iv = this.aircraft.getGuiInventory();
            for (i = 0; i < 3; ++i) {
                is = iv.getFuelSlotItemStack(i);
                if (is == null || is.getItem() instanceof IFluidContainerItem) continue;
                iv.getClass();
                this.dropPlayerItem(player, 0 + i);
            }
            for (i = 0; i < 2; ++i) {
                is = iv.getParachuteSlotItemStack(i);
                if (is == null || is.getItem() instanceof MCH_ItemParachute) continue;
                iv.getClass();
                this.dropPlayerItem(player, 3 + i);
            }
        }
    }

    public void dropPlayerItem(EntityPlayer player, int slotID) {
        ItemStack itemstack;
        if (!player.worldObj.isRemote && (itemstack = this.aircraft.getGuiInventory().getStackInSlotOnClosing(slotID)) != null) {
            player.dropPlayerItemWithRandomChoice(itemstack, false);
        }
    }
}

