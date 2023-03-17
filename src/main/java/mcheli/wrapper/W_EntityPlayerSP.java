/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.entity.EntityPlayerSP
 *  net.minecraft.entity.Entity
 */
package mcheli.wrapper;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;

public class W_EntityPlayerSP {
    public static void closeScreen(Entity p) {
        if (p instanceof EntityPlayerSP) {
            ((EntityPlayerSP)p).closeScreen();
        }
    }
}

