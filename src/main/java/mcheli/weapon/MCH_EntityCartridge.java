package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.model.IModelCustom;

public class MCH_EntityCartridge extends W_Entity {

   public final String texture_name;
   public final IModelCustom model;
   private final float bound;
   private final float gravity;
   private final float scale;
   private int countOnUpdate;
   public float targetYaw;
   public float targetPitch;


   @SideOnly(Side.CLIENT)
   public static void spawnCartridge(World world, MCH_Cartridge cartridge, double x, double y, double z, double mx, double my, double mz, float yaw, float pitch) {
      if(cartridge != null) {
         MCH_EntityCartridge entityFX = new MCH_EntityCartridge(world, cartridge, x, y, z, mx + ((double)world.rand.nextFloat() - 0.5D) * 0.07D, my, mz + ((double)world.rand.nextFloat() - 0.5D) * 0.07D);
         entityFX.prevRotationYaw = yaw;
         entityFX.rotationYaw = yaw;
         entityFX.targetYaw = yaw;
         entityFX.prevRotationPitch = pitch;
         entityFX.rotationPitch = pitch;
         entityFX.targetPitch = pitch;
         float cy = yaw + cartridge.yaw;
         float cp = pitch + cartridge.pitch;
         double tX = (double)(-MathHelper.sin(cy / 180.0F * 3.1415927F) * MathHelper.cos(cp / 180.0F * 3.1415927F));
         double tZ = (double)(MathHelper.cos(cy / 180.0F * 3.1415927F) * MathHelper.cos(cp / 180.0F * 3.1415927F));
         double tY = (double)(-MathHelper.sin(cp / 180.0F * 3.1415927F));
         double d = (double)MathHelper.sqrt_double(tX * tX + tY * tY + tZ * tZ);
         if(Math.abs(d) > 0.001D) {
            entityFX.motionX += tX * (double)cartridge.acceleration / d;
            entityFX.motionY += tY * (double)cartridge.acceleration / d;
            entityFX.motionZ += tZ * (double)cartridge.acceleration / d;
         }

         world.spawnEntityInWorld(entityFX);
      }

   }

   public MCH_EntityCartridge(World par1World, MCH_Cartridge c, double x, double y, double z, double mx, double my, double mz) {
      super(par1World);
      this.setPositionAndRotation(x, y, z, 0.0F, 0.0F);
      super.motionX = mx;
      super.motionY = my;
      super.motionZ = mz;
      this.texture_name = c.name;
      this.model = c.model;
      this.bound = c.bound;
      this.gravity = c.gravity;
      this.scale = c.scale;
      this.countOnUpdate = 0;
   }

   public float getScale() {
      return this.scale;
   }

   public void onUpdate() {
      super.prevPosX = super.posX;
      super.prevPosY = super.posY;
      super.prevPosZ = super.posZ;
      super.prevRotationYaw = super.rotationYaw;
      super.prevRotationPitch = super.rotationPitch;
      MCH_Config var10001 = MCH_MOD.config;
      if(this.countOnUpdate < MCH_Config.AliveTimeOfCartridge.prmInt) {
         ++this.countOnUpdate;
      } else {
         this.setDead();
      }

      super.motionX *= 0.98D;
      super.motionZ *= 0.98D;
      super.motionY += (double)this.gravity;
      this.move();
   }

   public void rotation() {
      if(super.rotationYaw < this.targetYaw - 3.0F) {
         super.rotationYaw += 10.0F;
         if(super.rotationYaw > this.targetYaw) {
            super.rotationYaw = this.targetYaw;
         }
      } else if(super.rotationYaw > this.targetYaw + 3.0F) {
         super.rotationYaw -= 10.0F;
         if(super.rotationYaw < this.targetYaw) {
            super.rotationYaw = this.targetYaw;
         }
      }

      if(super.rotationPitch < this.targetPitch) {
         super.rotationPitch += 10.0F;
         if(super.rotationPitch > this.targetPitch) {
            super.rotationPitch = this.targetPitch;
         }
      } else if(super.rotationPitch > this.targetPitch) {
         super.rotationPitch -= 10.0F;
         if(super.rotationPitch < this.targetPitch) {
            super.rotationPitch = this.targetPitch;
         }
      }

   }

   public void move() {
      Vec3 vec1 = W_WorldFunc.getWorldVec3(super.worldObj, super.posX, super.posY, super.posZ);
      Vec3 vec2 = W_WorldFunc.getWorldVec3(super.worldObj, super.posX + super.motionX, super.posY + super.motionY, super.posZ + super.motionZ);
      MovingObjectPosition m = W_WorldFunc.clip(super.worldObj, vec1, vec2);
      double d = Math.max(Math.abs(super.motionX), Math.abs(super.motionY));
      d = Math.max(d, Math.abs(super.motionZ));
      if(W_MovingObjectPosition.isHitTypeTile(m)) {
         this.setPosition(m.hitVec.xCoord, m.hitVec.yCoord, m.hitVec.zCoord);
         super.motionX += d * (double)(super.rand.nextFloat() - 0.5F) * 0.10000000149011612D;
         super.motionY += d * (double)(super.rand.nextFloat() - 0.5F) * 0.10000000149011612D;
         super.motionZ += d * (double)(super.rand.nextFloat() - 0.5F) * 0.10000000149011612D;
         if(d > 0.10000000149011612D) {
            this.targetYaw += (float)(d * (double)(super.rand.nextFloat() - 0.5F) * 720.0D);
            this.targetPitch = (float)(d * (double)(super.rand.nextFloat() - 0.5F) * 720.0D);
         } else {
            this.targetPitch = 0.0F;
         }

         switch(m.sideHit) {
         case 0:
            if(super.motionY > 0.0D) {
               super.motionY = -super.motionY * (double)this.bound;
            }
            break;
         case 1:
            if(super.motionY < 0.0D) {
               super.motionY = -super.motionY * (double)this.bound;
            }

            this.targetPitch *= 0.3F;
            break;
         case 2:
            if(super.motionZ > 0.0D) {
               super.motionZ = -super.motionZ * (double)this.bound;
            } else {
               super.posZ += super.motionZ;
            }
            break;
         case 3:
            if(super.motionZ < 0.0D) {
               super.motionZ = -super.motionZ * (double)this.bound;
            } else {
               super.posZ += super.motionZ;
            }
            break;
         case 4:
            if(super.motionX > 0.0D) {
               super.motionX = -super.motionX * (double)this.bound;
            } else {
               super.posX += super.motionX;
            }
            break;
         case 5:
            if(super.motionX < 0.0D) {
               super.motionX = -super.motionX * (double)this.bound;
            } else {
               super.posX += super.motionX;
            }
         }
      } else {
         super.posX += super.motionX;
         super.posY += super.motionY;
         super.posZ += super.motionZ;
         if(d > 0.05000000074505806D) {
            this.rotation();
         }
      }

   }

   protected void readEntityFromNBT(NBTTagCompound var1) {}

   protected void writeEntityToNBT(NBTTagCompound var1) {}
}
