package mcheli.flare;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.particles.MCH_ParticleParam;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.weapon.MCH_IEntityLockChecker;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_EntityChaff extends W_Entity implements MCH_IEntityLockChecker {

   public double gravity;
   public double airResistance;
   public static final int MAX_TICK_EXISTED = 200;

   public MCH_EntityChaff(World par1World) {
      super(par1World);
      this.gravity = -0.001D;
      this.airResistance = 0.99D;
      this.setSize(1.0F, 1.0F);
      super.prevRotationYaw = super.rotationYaw;
      super.prevRotationPitch = super.rotationPitch;
   }

   public MCH_EntityChaff(World obj, double pX, double pY, double pZ, double mX, double mY, double mZ) {
      this(obj);
      this.setLocationAndAngles(pX, pY, pZ, 0.0F, 0.0F);
      super.yOffset = 0.0F;
      super.motionX = mX;
      super.motionY = mY;
      super.motionZ = mZ;
   }

   @Override
   public void onUpdate() {
      if (worldObj.isRemote) {
         renderDistanceWeight = 500;
      }
      if(ticksExisted > MAX_TICK_EXISTED) {
         setDead();
      }
      if(!super.worldObj.isRemote && !super.worldObj.blockExists((int)super.posX, (int)super.posY, (int)super.posZ)) {
         setDead();
      } else {
         super.onUpdate();

         if(!super.worldObj.isRemote) {
            this.onUpdateCollided();
         }

         super.posX += super.motionX;
         super.posY += super.motionY;
         super.posZ += super.motionZ;

         super.motionY += this.gravity;
         super.motionX *= this.airResistance;
         super.motionZ *= this.airResistance;

         if(this.isInWater() && !super.worldObj.isRemote) {
            setDead();
         }

         if(super.onGround && !super.worldObj.isRemote) {
            setDead();
         }

         this.setPosition(super.posX, super.posY, super.posZ);
      }
   }

   @SideOnly(Side.CLIENT)
   public boolean isInRangeToRenderDist(double par1) {
      double d1 = super.boundingBox.getAverageEdgeLength() * 4.0D;
      d1 *= 64.0D;
      return par1 < d1 * d1;
   }

   @Override
   public boolean canLockEntity(Entity var1) {
      return false;
   }

   public boolean isEntityInvulnerable() {
      return false;
   }

   @Override
   public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
   }

   @Override
   public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
   }

   public boolean canBeCollidedWith() {
      return true;
   }

   public float getCollisionBorderSize() {
      return 1.0F;
   }

   public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
      return false;
   }

   @SideOnly(Side.CLIENT)
   public float getShadowSize() {
      return 0.0F;
   }


   protected void onUpdateCollided() {
      Vec3 vec3 = W_WorldFunc.getWorldVec3(super.worldObj, super.posX, super.posY, super.posZ);
      Vec3 vec31 = W_WorldFunc.getWorldVec3(super.worldObj, super.posX + super.motionX, super.posY + super.motionY, super.posZ + super.motionZ);
      MovingObjectPosition mop = W_WorldFunc.clip(super.worldObj, vec3, vec31);
      vec3 = W_WorldFunc.getWorldVec3(super.worldObj, super.posX, super.posY, super.posZ);
      vec31 = W_WorldFunc.getWorldVec3(super.worldObj, super.posX + super.motionX, super.posY + super.motionY, super.posZ + super.motionZ);
      if(mop != null) {
         W_WorldFunc.getWorldVec3(super.worldObj, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
         this.onImpact(mop);
      }

   }

   private void onImpact(MovingObjectPosition mop) {
      if(!super.worldObj.isRemote) {
         this.setDead();
      }
   }

}
