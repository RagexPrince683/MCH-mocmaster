/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.Item
 *  net.minecraftforge.client.IItemRenderer
 *  net.minecraftforge.client.MinecraftForgeClient
 */
package mcheli.wrapper;

import net.minecraft.item.Item;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

public class W_MinecraftForgeClient {
    public static void registerItemRenderer(Item item, IItemRenderer renderer) {
        if (item != null) {
            MinecraftForgeClient.registerItemRenderer((Item)item, (IItemRenderer)renderer);
        }
    }
}

