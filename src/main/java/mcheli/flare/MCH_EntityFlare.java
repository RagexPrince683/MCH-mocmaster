package mcheli.flare;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import mcheli.particles.MCH_ParticleParam;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_EntityFlare extends W_Entity implements IEntityAdditionalSpawnData {

   public double gravity;
   public double airResistance;
   public float size;
   public int fuseCount;


   public MCH_EntityFlare(World par1World) {
      super(par1World);
      this.gravity = -0.013D;
      this.airResistance = 0.992D;
      this.setSize(1.0F, 1.0F);
      super.prevRotationYaw = super.rotationYaw;
      super.prevRotationPitch = super.rotationPitch;
      this.size = 6.0F;
      this.fuseCount = 0;
   }

   public MCH_EntityFlare(World par1World, double pX, double pY, double pZ, double mX, double mY, double mZ, float size, int fuseCount) {
      this(par1World);
      this.setLocationAndAngles(pX, pY, pZ, 0.0F, 0.0F);
      super.yOffset = 0.0F;
      super.motionX = mX;
      super.motionY = mY;
      super.motionZ = mZ;
      this.size = size;
      this.fuseCount = fuseCount;
   }

   public boolean isEntityInvulnerable() {
      return true;
   }

   @SideOnly(Side.CLIENT)
   public boolean isInRangeToRenderDist(double par1) {
      double d1 = super.boundingBox.getAverageEdgeLength() * 4.0D;
      d1 *= 64.0D;
      return par1 < d1 * d1;
   }

   public void setDead() {
      super.setDead();
      if(this.fuseCount > 0 && super.worldObj.isRemote) {
         this.fuseCount = 0;
         boolean num = true;

         for(int i = 0; i < 20; ++i) {
            double x = (super.rand.nextDouble() - 0.5D) * 10.0D;
            double y = (super.rand.nextDouble() - 0.5D) * 10.0D;
            double z = (super.rand.nextDouble() - 0.5D) * 10.0D;
            MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "smoke", super.posX + x, super.posY + y, super.posZ + z);
            prm.age = 200 + super.rand.nextInt(100);
            prm.size = (float)(20 + super.rand.nextInt(25));
            prm.motionX = (super.rand.nextDouble() - 0.5D) * 0.45D;
            prm.motionY = (super.rand.nextDouble() - 0.5D) * 0.01D;
            prm.motionZ = (super.rand.nextDouble() - 0.5D) * 0.45D;
            prm.a = super.rand.nextFloat() * 0.1F + 0.85F;
            prm.b = super.rand.nextFloat() * 0.2F + 0.5F;
            prm.g = prm.b + 0.05F;
            prm.r = prm.b + 0.1F;
            MCH_ParticlesUtil.spawnParticle(prm);
         }
      }

   }

   public void writeSpawnData(ByteBuf buffer) {
      try {
         buffer.writeByte(this.fuseCount);
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public void readSpawnData(ByteBuf additionalData) {
      try {
         this.fuseCount = additionalData.readByte();
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public void onUpdate() {
      if(this.fuseCount > 0 && super.ticksExisted >= this.fuseCount) {
         this.setDead();
      } else if(!super.worldObj.isRemote && !super.worldObj.blockExists((int)super.posX, (int)super.posY, (int)super.posZ)) {
         this.setDead();
      } else if(super.ticksExisted > 300 && !super.worldObj.isRemote) {
         this.setDead();
      } else {
         super.onUpdate();
         if(!super.worldObj.isRemote) {
            this.onUpdateCollided();
         }

         super.posX += super.motionX;
         super.posY += super.motionY;
         super.posZ += super.motionZ;
         if(super.worldObj.isRemote) {
            boolean num = true;
            double x = (super.posX - super.prevPosX) / 2.0D;
            double y = (super.posY - super.prevPosY) / 2.0D;
            double z = (super.posZ - super.prevPosZ) / 2.0D;

            for(int i = 0; i < 2; ++i) {
               MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "smoke", super.prevPosX + x * (double)i, super.prevPosY + y * (double)i, super.prevPosZ + z * (double)i);
               prm.size = 6.0F + super.rand.nextFloat();
               if(this.size < 5.0F) {
                  prm.a = (float)((double)prm.a * 0.75D);
                  if(super.rand.nextInt(2) == 0) {
                     continue;
                  }
               }

               if(this.fuseCount > 0) {
                  prm.a = super.rand.nextFloat() * 0.1F + 0.85F;
                  prm.b = super.rand.nextFloat() * 0.1F + 0.5F;
                  prm.g = prm.b + 0.05F;
                  prm.r = prm.b + 0.1F;
               }

               MCH_ParticlesUtil.spawnParticle(prm);
            }
         }

         super.motionY += this.gravity;
         super.motionX *= this.airResistance;
         super.motionZ *= this.airResistance;
         if(this.isInWater() && !super.worldObj.isRemote) {
            this.setDead();
         }

         if(super.onGround && !super.worldObj.isRemote) {
            this.setDead();
         }

         this.setPosition(super.posX, super.posY, super.posZ);
      }

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

   protected void onImpact(MovingObjectPosition par1MovingObjectPosition) {
      if(!super.worldObj.isRemote) {
         this.setDead();
      }

   }

   public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
      par1NBTTagCompound.setTag("direction", this.newDoubleNBTList(new double[]{super.motionX, super.motionY, super.motionZ}));
   }

   public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
      this.setDead();
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
}
