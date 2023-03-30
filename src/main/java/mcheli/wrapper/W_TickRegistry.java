/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.common.FMLCommonHandler
 *  cpw.mods.fml.common.eventhandler.EventBus
 *  cpw.mods.fml.relauncher.Side
 */
package mcheli.wrapper;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.relauncher.Side;
import mcheli.wrapper.W_TickHandler;

public class W_TickRegistry {
    public static void registerTickHandler(W_TickHandler handler, Side side) {
        FMLCommonHandler.instance().bus().register((Object)handler);
    }
}

