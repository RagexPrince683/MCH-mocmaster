package mcheli.uav;

import mcheli.MCH_Lib;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** Persistent, idempotent inventory hand-off used while a player pilots a new UAV. */
public final class MCH_UavInventory {
    private static final String TAG = "MCH_UavPilotInventory";
    private static final String STORED = "Stored";
    private static final String INVENTORY = "Inventory";
    private static final String UAV_UUID = "UavUUID";
    private static final String REASON = "Reason";
    private static final String INVENTORY_CLEARED = "InventoryCleared";

    private MCH_UavInventory() {}

    public static boolean hasStoredPilotInventory(EntityPlayer player) {
        return player != null && getTag(player).getBoolean(STORED);
    }

    public static void storePilotInventory(EntityPlayerMP player, String uavUuid) {
        if (player == null || player.worldObj == null || player.worldObj.isRemote) {
            return;
        }
        NBTTagCompound tag = getTag(player);
        if (tag.getBoolean(STORED)) {
            return;
        }
        NBTTagList list = new NBTTagList();
        player.inventory.writeToNBT(list);
        tag.setTag(INVENTORY, list);
        tag.setBoolean(STORED, true);
        tag.setString(UAV_UUID, uavUuid == null ? "" : uavUuid);
        tag.setString(REASON, "stored");
        tag.setBoolean(INVENTORY_CLEARED, false);
        player.inventoryContainer.detectAndSendChanges();
        MCH_Lib.DbgLog(player.worldObj, "Stored UAV pilot inventory snapshot for %s", new Object[] { player.getCommandSenderName() });
    }


    public static void storeAndClearPilotInventory(EntityPlayerMP player, String uavUuid) {
        if (player == null || player.worldObj == null || player.worldObj.isRemote) {
            return;
        }
        NBTTagCompound tag = getTag(player);
        if (tag.getBoolean(STORED) && tag.getBoolean(INVENTORY_CLEARED)) {
            return;
        }

        NBTTagList list = new NBTTagList();
        player.inventory.writeToNBT(list);
        tag.setTag(INVENTORY, list);
        tag.setBoolean(STORED, true);
        tag.setString(UAV_UUID, uavUuid == null ? "" : uavUuid);
        tag.setString(REASON, "delayed_clear");
        tag.setBoolean(INVENTORY_CLEARED, true);
        player.inventory.clearInventory(null, -1);
        player.inventoryContainer.detectAndSendChanges();
        MCH_Lib.DbgLog(player.worldObj, "Stored and cleared delayed UAV pilot inventory for %s", new Object[] { player.getCommandSenderName() });
    }

    public static boolean restorePilotInventory(EntityPlayerMP player, String reason) {
        if (player == null || player.worldObj == null || player.worldObj.isRemote) {
            return false;
        }
        NBTTagCompound tag = getTag(player);
        if (!tag.getBoolean(STORED)) {
            return false;
        }

        boolean inventoryWasCleared = !tag.hasKey(INVENTORY_CLEARED) || tag.getBoolean(INVENTORY_CLEARED);
        if (!inventoryWasCleared) {
            clearStoredPilotInventory(player);
            player.inventoryContainer.detectAndSendChanges();
            MCH_Lib.DbgLog(player.worldObj, "Cleared UAV pilot inventory snapshot for %s (%s)", new Object[] { player.getCommandSenderName(), reason });
            return true;
        }

        ItemStack[] current = copyInventory(player);
        player.inventory.clearInventory(null, -1);
        try {
            if (tag.hasKey(INVENTORY)) {
                player.inventory.readFromNBT(tag.getTagList(INVENTORY, 10));
            }
        } catch (Throwable t) {
            MCH_Lib.Log(player, "Failed to restore UAV pilot inventory for %s: %s", new Object[] { player.getCommandSenderName(), t.toString() });
        }

        clearStoredPilotInventory(player);
        restoreOrDropCurrentItems(player, current);
        player.inventoryContainer.detectAndSendChanges();
        MCH_Lib.DbgLog(player.worldObj, "Restored UAV pilot inventory for %s (%s)", new Object[] { player.getCommandSenderName(), reason });
        return true;
    }

    public static void clearStoredPilotInventory(EntityPlayer player) {
        if (player != null) {
            player.getEntityData().removeTag(TAG);
        }
    }

    private static NBTTagCompound getTag(EntityPlayer player) {
        NBTTagCompound root = player.getEntityData();
        if (!root.hasKey(TAG)) {
            root.setTag(TAG, new NBTTagCompound());
        }
        return root.getCompoundTag(TAG);
    }

    private static ItemStack[] copyInventory(EntityPlayer player) {
        int main = player.inventory.mainInventory.length;
        int armor = player.inventory.armorInventory.length;
        ItemStack[] stacks = new ItemStack[main + armor];
        for (int i = 0; i < main; ++i) {
            stacks[i] = player.inventory.mainInventory[i] == null ? null : player.inventory.mainInventory[i].copy();
        }
        for (int i = 0; i < armor; ++i) {
            stacks[main + i] = player.inventory.armorInventory[i] == null ? null : player.inventory.armorInventory[i].copy();
        }
        return stacks;
    }

    private static void restoreOrDropCurrentItems(EntityPlayerMP player, ItemStack[] stacks) {
        for (int i = 0; i < stacks.length; ++i) {
            ItemStack stack = stacks[i];
            if (stack == null || stack.stackSize <= 0) {
                continue;
            }
            ItemStack copy = stack.copy();
            if (!player.inventory.addItemStackToInventory(copy) || copy.stackSize > 0) {
                dropStack(player, copy.stackSize > 0 ? copy : stack);
            }
        }
    }

    private static void dropStack(EntityPlayerMP player, ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) {
            return;
        }
        EntityItem item = player.dropPlayerItemWithRandomChoice(stack, false);
        if (item != null) {
            item.delayBeforeCanPickup = 40;
        }
    }
}
