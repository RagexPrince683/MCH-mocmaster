package mcheli.gltd;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Camera;
import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.multiplay.MCH_Multiplay;
import mcheli.weapon.MCH_WeaponCAS;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponInfoManager;
import mcheli.wrapper.W_Block;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_EntityGLTD extends W_Entity {

   private boolean field_70279_a;
   private double speedMultiplier;
   private int gltdPosRotInc;
   private double gltdX;
   private double gltdY;
   private double gltdZ;
   private double gltdYaw;
   private double gltdPitch;
   @SideOnly(Side.CLIENT)
   private double velocityX;
   @SideOnly(Side.CLIENT)
   private double velocityY;
   @SideOnly(Side.CLIENT)
   private double velocityZ;
   public final MCH_Camera camera;
   public boolean zoomDir;
   public final MCH_WeaponCAS weaponCAS;
   public int countWait;
   public boolean isUsedPlayer;
   public float renderRotaionYaw;
   public float renderRotaionPitch;
   public int retryRiddenByEntityCheck;
   public Entity lastRiddenByEntity;


   public MCH_EntityGLTD(World world) {
      super(world);
      this.field_70279_a = true;
      this.speedMultiplier = 0.07D;
      super.preventEntitySpawning = true;
      this.setSize(0.5F, 0.5F);
      super.yOffset = super.height / 2.0F;
      this.camera = new MCH_Camera(world, this);
      MCH_WeaponInfo wi = MCH_WeaponInfoManager.get("a10gau8");
      
      this.weaponCAS = new MCH_WeaponCAS(world, Vec3.createVectorHelper(0.0D, 0.0D, 0.0D), 0.0F, 0.0F, "a10gau8", wi);
      this.weaponCAS.interval += this.weaponCAS.interval > 0?150:-150;
      this.weaponCAS.displayName = "A-10 GAU-8 Avenger";
      super.ignoreFrustumCheck = true;
      this.countWait = 0;
      this.retryRiddenByEntityCheck = 0;
      this.lastRiddenByEntity = null;
      this.isUsedPlayer = false;
      this.renderRotaionYaw = 0.0F;
      this.renderRotaionYaw = 0.0F;
      this.renderRotaionPitch = 0.0F;
      this.zoomDir = true;
      
      super.renderDistanceWeight = 2.0D;
   }

   public MCH_EntityGLTD(World par1World, double x, double y, double z) {
      this(par1World);
      this.setPosition(x, y + (double)super.yOffset, z);
      super.motionX = 0.0D;
      super.motionY = 0.0D;
      super.motionZ = 0.0D;
      super.prevPosX = x;
      super.prevPosY = y;
      super.prevPosZ = z;
      this.camera.setPosition(x, y, z);
   }

   protected boolean canTriggerWalking() {
      return false;
   }

   protected void entityInit() {
      super.dataWatcher.addObject(17, new Integer(0));
      super.dataWatcher.addObject(18, new Integer(1));
      super.dataWatcher.addObject(19, new Integer(0));
   }

   public AxisAlignedBB getCollisionBox(Entity par1Entity) {
      return par1Entity.boundingBox;
   }

   public AxisAlignedBB getBoundingBox() {
      return super.boundingBox;
   }

   public boolean canBePushed() {
      return false;
   }

   public double getMountedYOffset() {
      return (double)super.height * 0.0D - 0.3D;
   }

   public boolean attackEntityFrom(DamageSource ds, float damage) {
      if(this.isEntityInvulnerable()) {
         return false;
      } else if(!super.worldObj.isRemote && !super.isDead) {
         MCH_Config var10000 = MCH_MOD.config;
         damage = MCH_Config.applyDamageByExternal(this, ds, damage);
         if(!MCH_Multiplay.canAttackEntity(ds, this)) {
            return false;
         } else {
            this.setForwardDirection(-this.getForwardDirection());
            this.setTimeSinceHit(10);
            this.setDamageTaken((int)((float)this.getDamageTaken() + damage * 100.0F));
            this.setBeenAttacked();
            boolean flag = ds.getEntity() instanceof EntityPlayer && ((EntityPlayer)ds.getEntity()).capabilities.isCreativeMode;
            if(flag || (float)this.getDamageTaken() > 40.0F) {
               this.camera.initCamera(0, super.riddenByEntity);
               if(super.riddenByEntity != null) {
                  super.riddenByEntity.mountEntity(this);
               }

               if(!flag) {
                  this.dropItemWithOffset(MCH_MOD.itemGLTD, 1, 0.0F);
               }

               W_WorldFunc.MOD_playSoundEffect(super.worldObj, super.posX, super.posY, super.posZ, "hit", 1.0F, 1.0F);
               this.setDead();
            }

            return true;
         }
      } else {
         return true;
      }
   }

   @SideOnly(Side.CLIENT)
   public void performHurtAnimation() {}

   public boolean canBeCollidedWith() {
      return !super.isDead;
   }

   @SideOnly(Side.CLIENT)
   public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9) {
      if(this.field_70279_a) {
         this.gltdPosRotInc = par9 + 5;
      } else {
         double x = par1 - super.posX;
         double y = par3 - super.posY;
         double z = par5 - super.posZ;
         if(x * x + y * y + z * z <= 1.0D) {
            return;
         }

         this.gltdPosRotInc = 3;
      }

      this.gltdX = par1;
      this.gltdY = par3;
      this.gltdZ = par5;
      this.gltdYaw = (double)par7;
      this.gltdPitch = (double)par8;
      super.motionX = this.velocityX;
      super.motionY = this.velocityY;
      super.motionZ = this.velocityZ;
   }

   @SideOnly(Side.CLIENT)
   public void setVelocity(double x, double y, double z) {
      this.velocityX = super.motionX = x;
      this.velocityY = super.motionY = y;
      this.velocityZ = super.motionZ = z;
   }

   public void onUpdate() {
      super.onUpdate();
      if(this.getTimeSinceHit() > 0) {
         this.setTimeSinceHit(this.getTimeSinceHit() - 1);
      }

      if((float)this.getDamageTaken() > 0.0F) {
         this.setDamageTaken(this.getDamageTaken() - 1);
      }

      super.prevPosX = super.posX;
      super.prevPosY = super.posY;
      super.prevPosZ = super.posZ;
      double d3 = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      if(super.riddenByEntity != null) {
         this.camera.updateViewer(0, super.riddenByEntity);
      }

      double d4;
      double d5;
      double d10;
      double d11;
      if(super.worldObj.isRemote && this.field_70279_a) {
         if(this.gltdPosRotInc > 0) {
            d4 = super.posX + (this.gltdX - super.posX) / (double)this.gltdPosRotInc;
            d5 = super.posY + (this.gltdY - super.posY) / (double)this.gltdPosRotInc;
            d11 = super.posZ + (this.gltdZ - super.posZ) / (double)this.gltdPosRotInc;
            d10 = MathHelper.wrapAngleTo180_double(this.gltdYaw - (double)super.rotationYaw);
            super.rotationYaw = (float)((double)super.rotationYaw + d10 / (double)this.gltdPosRotInc);
            super.rotationPitch = (float)((double)super.rotationPitch + (this.gltdPitch - (double)super.rotationPitch) / (double)this.gltdPosRotInc);
            --this.gltdPosRotInc;
            this.setPosition(d4, d5, d11);
            this.setRotation(super.rotationYaw, super.rotationPitch);
         } else {
            d4 = super.posX + super.motionX;
            d5 = super.posY + super.motionY;
            d11 = super.posZ + super.motionZ;
            this.setPosition(d4, d5, d11);
            if(super.onGround) {
               super.motionX *= 0.5D;
               super.motionY *= 0.5D;
               super.motionZ *= 0.5D;
            }

            super.motionX *= 0.99D;
            super.motionY *= 0.95D;
            super.motionZ *= 0.99D;
         }
      } else {
         super.motionY -= 0.04D;
         d4 = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
         if(d4 > 0.35D) {
            d5 = 0.35D / d4;
            super.motionX *= d5;
            super.motionZ *= d5;
            d4 = 0.35D;
         }

         if(d4 > d3 && this.speedMultiplier < 0.35D) {
            this.speedMultiplier += (0.35D - this.speedMultiplier) / 35.0D;
            if(this.speedMultiplier > 0.35D) {
               this.speedMultiplier = 0.35D;
            }
         } else {
            this.speedMultiplier -= (this.speedMultiplier - 0.07D) / 35.0D;
            if(this.speedMultiplier < 0.07D) {
               this.speedMultiplier = 0.07D;
            }
         }

         if(super.onGround) {
            super.motionX *= 0.5D;
            super.motionY *= 0.5D;
            super.motionZ *= 0.5D;
         }

         this.moveEntity(super.motionX, super.motionY, super.motionZ);
         super.motionX *= 0.99D;
         super.motionY *= 0.95D;
         super.motionZ *= 0.99D;
         super.rotationPitch = 0.0F;
         d5 = (double)super.rotationYaw;
         d11 = super.prevPosX - super.posX;
         d10 = super.prevPosZ - super.posZ;
         if(d11 * d11 + d10 * d10 > 0.001D) {
            d5 = (double)((float)(Math.atan2(d10, d11) * 180.0D / 3.141592653589793D));
         }

         double d12 = MathHelper.wrapAngleTo180_double(d5 - (double)super.rotationYaw);
         if(d12 > 20.0D) {
            d12 = 20.0D;
         }

         if(d12 < -20.0D) {
            d12 = -20.0D;
         }

         super.rotationYaw = (float)((double)super.rotationYaw + d12);
         this.setRotation(super.rotationYaw, super.rotationPitch);
         if(!super.worldObj.isRemote) {
            MCH_Config var10000 = MCH_MOD.config;
            if(MCH_Config.Collision_DestroyBlock.prmBool) {
               for(int l = 0; l < 4; ++l) {
                  int i1 = MathHelper.floor_double(super.posX + ((double)(l % 2) - 0.5D) * 0.8D);
                  int j1 = MathHelper.floor_double(super.posZ + ((double)(l / 2) - 0.5D) * 0.8D);

                  for(int k1 = 0; k1 < 2; ++k1) {
                     int l1 = MathHelper.floor_double(super.posY) + k1;
                     if(W_WorldFunc.isEqualBlock(super.worldObj, i1, l1, j1, W_Block.getSnowLayer())) {
                        super.worldObj.setBlockToAir(i1, l1, j1);
                     }
                  }
               }
            }

            if(super.riddenByEntity != null && super.riddenByEntity.isDead) {
               super.riddenByEntity = null;
            }
         }
      }

      this.updateCameraPosition(false);
      if(this.countWait > 0) {
         --this.countWait;
      }

      if(this.countWait < 0) {
         ++this.countWait;
      }

      this.weaponCAS.update(this.countWait);
      if(this.lastRiddenByEntity != null && super.riddenByEntity == null) {
         if(this.retryRiddenByEntityCheck < 3) {
            ++this.retryRiddenByEntityCheck;
            this.setUnmoundPosition(this.lastRiddenByEntity);
         } else {
            this.unmountEntity();
         }
      } else {
         this.retryRiddenByEntityCheck = 0;
      }

      if(super.riddenByEntity != null) {
         this.lastRiddenByEntity = super.riddenByEntity;
      }

   }

   public void setUnmoundPosition(Entity e) {
      if(e != null) {
         float yaw = super.rotationYaw;
         double d0 = Math.sin((double)yaw * 3.141592653589793D / 180.0D) * 1.2D;
         double d1 = -Math.cos((double)yaw * 3.141592653589793D / 180.0D) * 1.2D;
         e.setPosition(super.posX + d0, super.posY + this.getMountedYOffset() + e.getYOffset() + 1.0D, super.posZ + d1);
         e.lastTickPosX = e.prevPosX = e.posX;
         e.lastTickPosY = e.prevPosY = e.posY;
         e.lastTickPosZ = e.prevPosZ = e.posZ;
      }
   }

   public void unmountEntity() {
      this.camera.setMode(0, 0);
      this.camera.setCameraZoom(1.0F);
      if(!super.worldObj.isRemote) {
         if(super.riddenByEntity != null) {
            if(!super.riddenByEntity.isDead) {
               super.riddenByEntity.mountEntity((Entity)null);
            }
         } else if(this.lastRiddenByEntity != null && !this.lastRiddenByEntity.isDead) {
            this.camera.updateViewer(0, this.lastRiddenByEntity);
            this.setUnmoundPosition(this.lastRiddenByEntity);
         }
      }

      super.riddenByEntity = null;
      this.lastRiddenByEntity = null;
   }

   public void updateCameraPosition(boolean foreceUpdate) {
      if(foreceUpdate || super.riddenByEntity != null && this.camera != null) {
         double x = -Math.sin((double)super.rotationYaw * 3.141592653589793D / 180.0D) * 0.6D;
         double z = Math.cos((double)super.rotationYaw * 3.141592653589793D / 180.0D) * 0.6D;
         this.camera.setPosition(super.posX + x, super.posY + 0.7D, super.posZ + z);
      }

   }

   @SideOnly(Side.CLIENT)
   public void zoomCamera(float f) {
      float z = this.camera.getCameraZoom();
      z += f;
      if(z < 1.0F) {
         z = 1.0F;
      }

      if(z > 10.0F) {
         z = 10.0F;
      }

      this.camera.setCameraZoom(z);
   }

   public void updateRiderPosition() {
      if(super.riddenByEntity != null) {
         double x = Math.sin((double)super.rotationYaw * 3.141592653589793D / 180.0D) * 0.5D;
         double z = -Math.cos((double)super.rotationYaw * 3.141592653589793D / 180.0D) * 0.5D;
         super.riddenByEntity.setPosition(super.posX + x, super.posY + this.getMountedYOffset() + super.riddenByEntity.getYOffset(), super.posZ + z);
      }

   }

   public void switchWeapon(int id) {}

   public boolean useCurrentWeapon(int option1, int option2) {
      if(this.countWait == 0 && super.riddenByEntity != null && this.weaponCAS.shot(super.riddenByEntity, this.camera.posX, this.camera.posY, this.camera.posZ, option1, option2)) {
         this.countWait = this.weaponCAS.interval;
         if(super.worldObj.isRemote) {
            this.countWait += this.countWait > 0?10:-10;
         } else {
            W_WorldFunc.MOD_playSoundEffect(super.worldObj, super.posX, super.posY, super.posZ, "gltd", 0.5F, 1.0F);
         }

         return true;
      } else {
         return false;
      }
   }

   protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {}

   protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {}

   @SideOnly(Side.CLIENT)
   public float getShadowSize() {
      return 0.0F;
   }

   public boolean interactFirst(EntityPlayer player) {
      if(super.riddenByEntity != null && super.riddenByEntity instanceof EntityPlayer && super.riddenByEntity != player) {
         return true;
      } else {
         player.rotationYaw = MathHelper.wrapAngleTo180_float(super.rotationYaw);
         player.rotationPitch = MathHelper.wrapAngleTo180_float(super.rotationPitch);
         if(!super.worldObj.isRemote) {
            player.mountEntity(this);
         } else {
            this.zoomDir = true;
            this.camera.setCameraZoom(1.0F);
            if(this.countWait > 0) {
               this.countWait = -this.countWait;
            }

            if(this.countWait > -60) {
               this.countWait = -60;
            }
         }

         this.updateCameraPosition(true);
         return true;
      }
   }

   public void setDamageTaken(int par1) {
      super.dataWatcher.updateObject(19, Integer.valueOf(par1));
   }

   public int getDamageTaken() {
      return super.dataWatcher.getWatchableObjectInt(19);
   }

   public void setTimeSinceHit(int par1) {
      super.dataWatcher.updateObject(17, Integer.valueOf(par1));
   }

   public int getTimeSinceHit() {
      return super.dataWatcher.getWatchableObjectInt(17);
   }

   public void setForwardDirection(int par1) {
      super.dataWatcher.updateObject(18, Integer.valueOf(par1));
   }

   public int getForwardDirection() {
      return 0;
   }

   @SideOnly(Side.CLIENT)
   public void func_70270_d(boolean par1) {
      this.field_70279_a = par1;
   }
}
