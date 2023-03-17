/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.MathHelper
 *  net.minecraft.util.Vec3
 */
package mcheli.wrapper;

import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class W_Vec3 {
    public static void rotateAroundZ(float par1, Vec3 vOut) {
        float f1 = MathHelper.cos((float)par1);
        float f2 = MathHelper.sin((float)par1);
        double d0 = vOut.xCoord * (double)f1 + vOut.yCoord * (double)f2;
        double d1 = vOut.yCoord * (double)f1 - vOut.xCoord * (double)f2;
        double d2 = vOut.zCoord;
        vOut.xCoord = d0;
        vOut.yCoord = d1;
        vOut.zCoord = d2;
    }
}

