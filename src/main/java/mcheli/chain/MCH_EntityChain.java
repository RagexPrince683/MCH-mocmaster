package mcheli.chain;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.List;

public class MCH_EntityChain extends W_Entity {

   private int isServerTowEntitySearchCount;
   public Entity towEntity;
   public Entity towedEntity;
   public String towEntityUUID;
   public String towedEntityUUID;
   private int chainLength;
   private boolean isTowing;


   public MCH_EntityChain(World world) {
      super(world);
      super.preventEntitySpawning = true;
      this.setSize(1.0F, 1.0F);
      super.yOffset = super.height / 2.0F;
      this.towEntity = null;
      this.towedEntity = null;
      this.towEntityUUID = "";
      this.towedEntityUUID = "";
      this.isTowing = false;
      super.ignoreFrustumCheck = true;
      this.setChainLength(4);
      this.isServerTowEntitySearchCount = 50;
   }

   public MCH_EntityChain(World par1World, double par2, double par4, double par6) {
      this(par1World);
      this.setPosition(par2, par4 + (double)super.yOffset, par6);
      super.motionX = 0.0D;
      super.motionY = 0.0D;
      super.motionZ = 0.0D;
      super.prevPosX = par2;
      super.prevPosY = par4;
      super.prevPosZ = par6;
   }

   protected boolean canTriggerWalking() {
      return false;
   }

   protected void entityInit() {
      this.getDataWatcher().addObject(30, Integer.valueOf(0));
      this.getDataWatcher().addObject(31, Integer.valueOf(0));
   }

   public AxisAlignedBB getCollisionBox(Entity par1Entity) {
      return par1Entity.boundingBox;
   }

   public AxisAlignedBB getBoundingBox() {
      return null;
   }

   public boolean canBePushed() {
      return true;
   }

   public boolean attackEntityFrom(DamageSource d, float par2) {
      return false;
   }

   public void setChainLength(int n) {
      if(n > 15) {
         n = 15;
      }

      if(n < 3) {
         n = 3;
      }

      this.chainLength = n;
   }

   public int getChainLength() {
      return this.chainLength;
   }

   public void setDead() {
      super.setDead();
      this.playDisconnectTowingEntity();
      this.isTowing = false;
      this.towEntity = null;
      this.towedEntity = null;
   }

   public boolean isTowingEntity() {
      return this.isTowing && !super.isDead && this.towedEntity != null && this.towEntity != null;
   }

   public boolean canBeCollidedWith() {
      return false;
   }

   public void setTowEntity(Entity towedEntity, Entity towEntity) {
      if(towedEntity != null && towEntity != null && !towedEntity.isDead && !towEntity.isDead && !W_Entity.isEqual(towedEntity, towEntity)) {
         this.isTowing = true;
         this.towedEntity = towedEntity;
         this.towEntity = towEntity;
         if(!super.worldObj.isRemote) {
            this.getDataWatcher().updateObject(30, Integer.valueOf(W_Entity.getEntityId(towedEntity)));
            this.getDataWatcher().updateObject(31, Integer.valueOf(W_Entity.getEntityId(towEntity)));
            this.isServerTowEntitySearchCount = 0;
         }

         if(towEntity instanceof MCH_EntityAircraft) {
            ((MCH_EntityAircraft)towEntity).setTowChainEntity(this);
         }

         if(towedEntity instanceof MCH_EntityAircraft) {
            ((MCH_EntityAircraft)towedEntity).setTowedChainEntity(this);
         }
      } else {
         this.isTowing = false;
         this.towedEntity = null;
         this.towEntity = null;
      }

   }

   public void searchTowingEntity() {
      if((this.towedEntity == null || this.towEntity == null) && !this.towedEntityUUID.isEmpty() && !this.towEntityUUID.isEmpty() && super.boundingBox != null) {
         List list = super.worldObj.getEntitiesWithinAABBExcludingEntity(this, super.boundingBox.expand(32.0D, 32.0D, 32.0D));
         if(list != null) {
            for(int i = 0; i < list.size(); ++i) {
               Entity entity = (Entity)list.get(i);
               String uuid = entity.getPersistentID().toString();
               if(this.towedEntity == null && uuid.compareTo(this.towedEntityUUID) == 0) {
                  this.towedEntity = entity;
               } else if(this.towEntity == null && uuid.compareTo(this.towEntityUUID) == 0) {
                  this.towEntity = entity;
               } else if(this.towEntity != null && this.towedEntity != null) {
                  this.setTowEntity(this.towedEntity, this.towEntity);
                  break;
               }
            }
         }
      }

   }

   public void onUpdate() {
      super.onUpdate();
      if(this.towedEntity == null || this.towedEntity.isDead || this.towEntity == null || this.towEntity.isDead) {
         this.towedEntity = null;
         this.towEntity = null;
         this.isTowing = false;
      }

      if(this.towedEntity != null) {
         this.towedEntity.fallDistance = 0.0F;
      }

      super.prevPosX = super.posX;
      super.prevPosY = super.posY;
      super.prevPosZ = super.posZ;
      if(super.worldObj.isRemote) {
         this.onUpdate_Client();
      } else {
         this.onUpdate_Server();
      }

   }

   public void onUpdate_Client() {
      if(!this.isTowingEntity()) {
         this.setTowEntity(super.worldObj.getEntityByID(this.getDataWatcher().getWatchableObjectInt(30)), super.worldObj.getEntityByID(this.getDataWatcher().getWatchableObjectInt(31)));
      }

      double d4 = super.posX + super.motionX;
      double d5 = super.posY + super.motionY;
      double d11 = super.posZ + super.motionZ;
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

   public void onUpdate_Server() {
      if(this.isServerTowEntitySearchCount > 0) {
         this.searchTowingEntity();
         if(this.towEntity != null && this.towedEntity != null) {
            this.isServerTowEntitySearchCount = 0;
         } else {
            --this.isServerTowEntitySearchCount;
         }
      } else if(this.towEntity == null || this.towedEntity == null) {
         this.setDead();
      }

      super.motionY -= 0.01D;
      if(!this.isTowing) {
         super.motionX *= 0.8D;
         super.motionY *= 0.8D;
         super.motionZ *= 0.8D;
      }

      this.moveEntity(super.motionX, super.motionY, super.motionZ);
      if(this.isTowingEntity()) {
         this.setPosition(this.towEntity.posX, this.towEntity.posY + 2.0D, this.towEntity.posZ);
         this.updateTowingEntityPosRot();
      }

      super.motionX *= 0.99D;
      super.motionY *= 0.95D;
      super.motionZ *= 0.99D;
   }

   public void updateTowingEntityPosRot() {
      double dx = this.towedEntity.posX - this.towEntity.posX;
      double dy = this.towedEntity.posY - this.towEntity.posY;
      double dz = this.towedEntity.posZ - this.towEntity.posZ;
      double dist = (double)MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz);
      float DIST = (float)this.getChainLength();
      float MAX_DIST = (float)(this.getChainLength() + 2);
      if(dist > (double)DIST) {
         this.towedEntity.rotationYaw = (float)(Math.atan2(dz, dx) * 180.0D / 3.141592653589793D) + 90.0F;
         this.towedEntity.prevRotationYaw = this.towedEntity.rotationYaw;
         double tmp = dist - (double)DIST;
         float accl = 0.001F;
         this.towedEntity.motionX -= dx * (double)accl / tmp;
         this.towedEntity.motionY -= dy * (double)accl / tmp;
         this.towedEntity.motionZ -= dz * (double)accl / tmp;
         if(dist > (double)MAX_DIST) {
            this.towedEntity.setPosition(this.towEntity.posX + dx * (double)MAX_DIST / dist, this.towEntity.posY + dy * (double)MAX_DIST / dist, this.towEntity.posZ + dz * (double)MAX_DIST / dist);
         }
      }

   }

   public void playDisconnectTowingEntity() {
      W_WorldFunc.MOD_playSoundEffect(super.worldObj, super.posX, super.posY, super.posZ, "chain_ct", 1.0F, 1.0F);
   }

   protected void writeEntityToNBT(NBTTagCompound nbt) {
      if(this.isTowing && this.towEntity != null && !this.towEntity.isDead && this.towedEntity != null && !this.towedEntity.isDead) {
         nbt.setString("TowEntityUUID", this.towEntity.getPersistentID().toString());
         nbt.setString("TowedEntityUUID", this.towedEntity.getPersistentID().toString());
         nbt.setInteger("ChainLength", this.getChainLength());
      }

   }

   protected void readEntityFromNBT(NBTTagCompound nbt) {
      this.towEntityUUID = nbt.getString("TowEntityUUID");
      this.towedEntityUUID = nbt.getString("TowedEntityUUID");
      this.setChainLength(nbt.getInteger("ChainLength"));
   }

   @SideOnly(Side.CLIENT)
   public float getShadowSize() {
      return 0.0F;
   }

   public boolean interactFirst(EntityPlayer player) {
      return false;
   }
}
