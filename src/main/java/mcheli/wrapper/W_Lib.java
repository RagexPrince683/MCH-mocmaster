/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.world.World
 */
package mcheli.wrapper;

import mcheli.MCH_MOD;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

public class W_Lib {
    public static boolean isEntityLivingBase(Entity entity) {
        return entity instanceof EntityLivingBase;
    }

    public static EntityLivingBase castEntityLivingBase(Object entity) {
        return (EntityLivingBase)entity;
    }

    public static Class getEntityLivingBaseClass() {
        return EntityLivingBase.class;
    }

    public static double getEntityMoveDist(Entity entity) {
        if (entity == null) {
            return 0.0;
        }
        return entity instanceof EntityLivingBase ? (double)((EntityLivingBase)entity).moveForward : 0.0;
    }

    public static boolean isClientPlayer(Entity entity) {
        if (entity instanceof EntityPlayer && entity.worldObj.isRemote) {
            return W_Entity.isEqual(MCH_MOD.proxy.getClientPlayer(), entity);
        }
        return false;
    }

    public static boolean isFirstPerson() {
        return MCH_MOD.proxy.isFirstPerson();
    }
}

