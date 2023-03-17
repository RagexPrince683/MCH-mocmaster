/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.item.Item
 *  net.minecraft.util.RegistryNamespaced
 */
package mcheli.wrapper;

import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

public class W_Item
extends Item {
    public W_Item(int par1) {
    }

    public W_Item() {
    }

    public static int getIdFromItem(Item i) {
        return i == null ? 0 : Item.itemRegistry.getIDForObject((Object)i);
    }

    public Item setTexture(String par1Str) {
        this.setTextureName(W_MOD.DOMAIN + ":" + par1Str);
        return this;
    }

    public static Item getItemById(int i) {
        return Item.getItemById((int)i);
    }

    public static Item getItemByName(String nm) {
        if (nm.indexOf(58) < 0) {
            nm = "minecraft:" + nm;
        }
        //return (Item)Item.itemRegistry.getObject(nm);
        //return GameData.getItemRegistry().getObject(nm);
        return GameData.getItemRegistry().get(nm);
    }

    public static String getNameForItem(Item item) {
        return Item.itemRegistry.getNameForObject((Object)item);
    }

    public static Item getItemFromBlock(Block block) {
        return Item.getItemFromBlock((Block)block);
    }
}

