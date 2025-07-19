package mcheli.light;

import mcheli.MCH_Lib;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

public class EntityMCH_Light extends Entity {

    private Entity parent;
    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;

    public EntityMCH_Light(World world) {
        super(world);
        this.setSize(0.1F, 0.1F);
        this.renderDistanceWeight = 10.0D;
    }

    public EntityMCH_Light(World world, Entity parent) {
        this(world);
        this.parent = parent;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (this.parent == null || this.parent.isDead) {
            this.setDead();
            return;
        }

        Vec3 start = Vec3.createVectorHelper(parent.posX, parent.posY + parent.getEyeHeight(), parent.posZ);
        Vec3 lookVec = parent.getLookVec();
        Vec3 end = start.addVector(lookVec.xCoord * 10, lookVec.yCoord * 10, lookVec.zCoord * 10);

        MovingObjectPosition hit = this.worldObj.func_147447_a(start, end, false, true, false);
        int lightX, lightY, lightZ;

        if (hit != null) {
            lightX = hit.blockX;
            lightY = hit.blockY;
            lightZ = hit.blockZ;

            EnumFacing side = EnumFacing.getFront(hit.sideHit);
            lightX += side.getFrontOffsetX();
            lightY += side.getFrontOffsetY();
            lightZ += side.getFrontOffsetZ();
        } else {
            lightX = MathHelper.floor_double(end.xCoord);
            lightY = MathHelper.floor_double(end.yCoord);
            lightZ = MathHelper.floor_double(end.zCoord);
        }

        updateLight(lightX, lightY, lightZ);
    }

    private void updateLight(int x, int y, int z) {
        if (x != lastX || y != lastY || z != lastZ) {
            if (lastX != Integer.MIN_VALUE) {
                worldObj.setLightValue(EnumSkyBlock.Block, lastX, lastY, lastZ, 0);
                worldObj.markBlockForUpdate(lastX, lastY, lastZ);
            }

            worldObj.setLightValue(EnumSkyBlock.Block, x, y, z, 15);
            worldObj.markBlockForUpdate(x, y, z);

            lastX = x;
            lastY = y;
            lastZ = z;
        }
    }

    @Override
    public void setDead() {
        super.setDead();
        if (!worldObj.isRemote && lastX != Integer.MIN_VALUE) {
            worldObj.setLightValue(EnumSkyBlock.Block, lastX, lastY, lastZ, 0);
            worldObj.markBlockForUpdate(lastX, lastY, lastZ);
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }
}
