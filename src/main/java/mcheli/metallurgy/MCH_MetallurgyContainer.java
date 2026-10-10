package mcheli.metallurgy;

import java.util.Arrays;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class MCH_MetallurgyContainer extends Container {
    public final MCH_MetallurgyTile tile;
    private int[] previous;

    public MCH_MetallurgyContainer(InventoryPlayer player, MCH_MetallurgyTile tile) {
        this.tile = tile;
        if (tile instanceof MCH_BlastFurnaceTile) {
            for (int i = 0; i < 3; i++) machineSlot(i, 26 + i * 18, 30);
            for (int i = 0; i < 3; i++) machineSlot(i + 3, 98 + i * 18, 30);
            machineSlot(6, 44, 66);
        } else {
            machineSlot(0, 44, 30);
            machineSlot(1, 44, 66);
            machineSlot(2, 116, 66);
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(player, col + row * 9 + 9, 8 + col * 18, 112 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player, col, 8 + col * 18, 170));
        previous = new int[tile.fields().length];
        Arrays.fill(previous, -1);
    }

    private void machineSlot(final int index, int x, int y) {
        addSlotToContainer(new Slot(tile, index, x, y) {
            @Override public boolean isItemValid(ItemStack stack) { return tile.isItemValidForSlot(index, stack); }
        });
    }

    @Override public boolean canInteractWith(EntityPlayer player) { return tile.isUseableByPlayer(player); }
    @Override public void addCraftingToCrafters(ICrafting crafter) {
        super.addCraftingToCrafters(crafter);
        int[] fields = tile.fields();
        for (int i = 0; i < fields.length; i++) crafter.sendProgressBarUpdate(this, i, fields[i]);
    }
    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int[] fields = tile.fields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == previous[i]) continue;
            for (Object value : crafters) ((ICrafting) value).sendProgressBarUpdate(this, i, fields[i]);
        }
        previous = fields;
    }
    @Override public void updateProgressBar(int id, int value) { tile.receiveField(id, value); }

    /** Vanilla window-scoped button packet is processed by the server's container handler. */
    @Override public boolean enchantItem(EntityPlayer player, int button) {
        if (tile.getWorldObj().isRemote || !canInteractWith(player) || !(tile instanceof MCH_FoundryTile)
            || button < 0 || button > 1) return false;
        ((MCH_FoundryTile) tile).changeMold(button == 0 ? -1 : 1);
        detectAndSendChanges();
        return true;
    }

    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) return null;
        Slot source = (Slot) inventorySlots.get(index);
        if (!source.getHasStack()) return null;
        ItemStack stack = source.getStack(), original = stack.copy();
        int machineSize = tile.getSizeInventory();
        if (index < machineSize) {
            if (!mergeItemStack(stack, machineSize, inventorySlots.size(), true)) return null;
        } else {
            boolean moved = false;
            for (int slot = 0; slot < machineSize && stack.stackSize > 0; slot++) {
                if (tile.isItemValidForSlot(slot, stack)) moved |= mergeItemStack(stack, slot, slot + 1, false);
            }
            if (!moved) return null;
        }
        if (stack.stackSize == 0) source.putStack(null);
        else source.onSlotChanged();
        source.onPickupFromSlot(player, stack);
        return original;
    }
}
