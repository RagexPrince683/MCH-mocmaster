/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.particle.EntityFX
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.AxisAlignedBB
 *  net.minecraft.util.IIcon
 *  net.minecraft.world.World
 */
package mcheli.wrapper;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public abstract class W_EntityFX
extends EntityFX {
    public W_EntityFX(World par1World, double par2, double par4, double par6) {
        super(par1World, par2, par4, par6);
    }

    public W_EntityFX(World par1World, double par2, double par4, double par6, double par8, double par10, double par12) {
        super(par1World, par2, par4, par6, par8, par10, par12);
    }

    public AxisAlignedBB getBoundingBox() {
        return this.boundingBox;
    }

    public void setIcon(IIcon icon) {
        this.setParticleIcon(icon);
    }

    protected void doBlockCollisions() {
        super.func_145775_I();
    }
}

