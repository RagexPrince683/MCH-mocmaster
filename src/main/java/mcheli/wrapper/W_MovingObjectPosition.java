/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.MovingObjectPosition
 *  net.minecraft.util.MovingObjectPosition$MovingObjectType
 *  net.minecraft.util.Vec3
 */
package mcheli.wrapper;

import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class W_MovingObjectPosition {
    public static boolean isHitTypeEntity(MovingObjectPosition m) {
        if (m == null) {
            return false;
        }
        return m.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY;
    }

    public static boolean isHitTypeTile(MovingObjectPosition m) {
        if (m == null) {
            return false;
        }
        return m.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    public static MovingObjectPosition newMOP(int p1, int p2, int p3, int p4, Vec3 p5, boolean p6) {
        return new MovingObjectPosition(p1, p2, p3, p4, p5, p6);
    }
}

