package mcheli.aircraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class MCH_EntityHitBox extends W_Entity {

   public MCH_EntityBaseVehicle parent;
   public int debugId;
   private MCH_BoundingBox extraBoundingBox;
   private boolean physical;


   public MCH_EntityHitBox(World world) {
      super(world);
      this.setSize(1.0F, 1.0F);
      super.yOffset = 0.0F;
      super.motionX = 0.0D;
      super.motionY = 0.0D;
      super.motionZ = 0.0D;
      this.parent = null;
      this.extraBoundingBox = null;
      this.physical = false;
      super.ignoreFrustumCheck = true;
      super.isImmuneToFire = true;
   }

   public MCH_EntityHitBox(World world, MCH_EntityBaseVehicle ac, float w, float h) {
      this(world);
      this.setPosition(ac.posX, ac.posY + 1.0D, ac.posZ);
      super.prevPosX = ac.posX;
      super.prevPosY = ac.posY + 1.0D;
      super.prevPosZ = ac.posZ;
      this.parent = ac;
      this.setSize(w, h);
   }

   public MCH_EntityHitBox(World world, MCH_EntityBaseVehicle ac, MCH_BoundingBox boundingBox, int index) {
      this(world);
      this.parent = ac;
      this.extraBoundingBox = boundingBox;
      this.physical = true;
      this.debugId = index;
      this.getDataWatcher().updateObject(16, Byte.valueOf((byte)1));
      this.getDataWatcher().updateObject(17, Integer.valueOf(ac.getEntityId()));
      this.getDataWatcher().updateObject(18, Float.valueOf(boundingBox.width));
      this.getDataWatcher().updateObject(19, Float.valueOf(boundingBox.height));
      this.setSize(boundingBox.width, boundingBox.height);
      this.setPosition(boundingBox.nowPos.xCoord, boundingBox.nowPos.yCoord, boundingBox.nowPos.zCoord);
   }

   protected void entityInit() {
      this.getDataWatcher().addObject(16, Byte.valueOf((byte)0));
      this.getDataWatcher().addObject(17, Integer.valueOf(0));
      this.getDataWatcher().addObject(18, Float.valueOf(1.0F));
      this.getDataWatcher().addObject(19, Float.valueOf(1.0F));
   }

   public void updatePhysicalPosition() {
      if(this.extraBoundingBox != null) {
         this.setSize(this.extraBoundingBox.width, this.extraBoundingBox.height);
         this.setPosition(this.extraBoundingBox.nowPos.xCoord, this.extraBoundingBox.nowPos.yCoord,
                 this.extraBoundingBox.nowPos.zCoord);
      }
   }

   public boolean isPhysical() {
      return this.physical || this.getDataWatcher().getWatchableObjectByte(16) != 0;
   }

   protected boolean canTriggerWalking() {
      return false;
   }

   public AxisAlignedBB getCollisionBox(Entity par1Entity) {
      return this.isPhysical()?super.boundingBox:null;
   }

   public AxisAlignedBB getBoundingBox() {
      return this.isPhysical()?super.boundingBox:null;
   }

   public boolean canBePushed() {
      return false;
   }

   public double getMountedYOffset() {
      return -0.3D;
   }

   public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
      if(this.parent == null && super.worldObj.isRemote) {
         Entity entity = super.worldObj.getEntityByID(this.getDataWatcher().getWatchableObjectInt(17));
         if(entity instanceof MCH_EntityBaseVehicle) {
            this.parent = (MCH_EntityBaseVehicle)entity;
         }
      }
      if(this.parent != null && this.extraBoundingBox != null) {
         this.parent.lastBBDamageFactor = this.extraBoundingBox.damegeFactor;
         this.parent.lastHitBoundingBoxType = this.extraBoundingBox.boundingBoxType;
      }
      return this.parent != null?this.parent.attackEntityFrom(par1DamageSource, par2):false;
   }

   public boolean canBeCollidedWith() {
      return !super.isDead;
   }

   public void setDead() {
      super.setDead();
   }

   public void onUpdate() {
      super.onUpdate();
      if(this.isPhysical() && super.worldObj.isRemote) {
         float width = this.getDataWatcher().getWatchableObjectFloat(18);
         float height = this.getDataWatcher().getWatchableObjectFloat(19);
         if(super.width != width || super.height != height) {
            this.setSize(width, height);
         }
      }
      if(this.isPhysical() && this.parent == null && super.worldObj.isRemote) {
         Entity entity = super.worldObj.getEntityByID(this.getDataWatcher().getWatchableObjectInt(17));
         if(entity instanceof MCH_EntityBaseVehicle) {
            this.parent = (MCH_EntityBaseVehicle)entity;
         }
      }
   }

   protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {}

   protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {}

   @SideOnly(Side.CLIENT)
   public float getShadowSize() {
      return 0.0F;
   }

   public boolean interactFirst(EntityPlayer player) {
      return this.parent != null?this.parent.interactFirst(player):false;
   }
}
