/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ChatComponentText
 *  net.minecraft.util.IChatComponent
 *  net.minecraft.world.World
 */
package mcheli.wrapper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

public abstract class W_EntityPlayer
extends EntityPlayer {
    public W_EntityPlayer(World par1World, EntityPlayer player) {
        super(par1World, player.getGameProfile());
    }

    public static void closeScreen(Entity p) {
        if (p != null) {
            if (p.worldObj.isRemote) {
                W_EntityPlayerSP.closeScreen(p);
            } else if (p instanceof EntityPlayerMP) {
                ((EntityPlayerMP)p).closeScreen();
            }
        }
    }

    public static boolean hasItem(EntityPlayer player, Item item) {
        return item != null && player.inventory.hasItem(item);
    }

    public static boolean consumeInventoryItem(EntityPlayer player, Item item) {
        return item != null && player.inventory.consumeInventoryItem(item);
    }

    public static void addChatMessage(EntityPlayer player, String s) {
        player.addChatMessage((IChatComponent)new ChatComponentText(s));
    }

    public static EntityItem dropPlayerItemWithRandomChoice(EntityPlayer player, ItemStack item, boolean b1, boolean b2) {
        return player.func_146097_a(item, b1, b2);
    }

    public static boolean isPlayer(Entity entity) {
        if (entity instanceof EntityPlayer) {
            return true;
        }
        return false;
    }
}

