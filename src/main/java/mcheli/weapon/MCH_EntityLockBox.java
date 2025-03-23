package mcheli.weapon;

import mcheli.wrapper.W_Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class MCH_EntityLockBox extends W_Entity {

    public MCH_EntityLockBox(World par1World) {
        super(par1World);
        renderDistanceWeight = 1000;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound p_70037_1_) {

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {

    }

    @Override
    public void onUpdate() {
    }
}
