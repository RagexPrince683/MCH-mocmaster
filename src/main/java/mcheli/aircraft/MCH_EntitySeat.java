/*     */ package mcheli.aircraft;
/*     */
/*     */ import cpw.mods.fml.relauncher.Side;
/*     */ import cpw.mods.fml.relauncher.SideOnly;
/*     */ import mcheli.MCH_Lib;
/*     */ import mcheli.wrapper.W_Entity;
/*     */ import net.minecraft.entity.Entity;
/*     */ import net.minecraft.entity.player.EntityPlayer;
/*     */ import net.minecraft.item.ItemStack;
/*     */ import net.minecraft.nbt.NBTTagCompound;
/*     */ import net.minecraft.util.AxisAlignedBB;
/*     */ import net.minecraft.util.DamageSource;
/*     */ import net.minecraft.world.World;
/*     */
/*     */
/*     */
/*     */ public class MCH_EntitySeat
        /*     */   extends W_Entity
        /*     */ {
   /*     */   public String parentUniqueID;
   /*     */   private MCH_EntityAircraft parent;
   /*     */   public int seatID;
   /*     */   public int parentSearchCount;
   /*     */   protected Entity lastRiddenByEntity;
   /*     */   public static final float BB_SIZE = 1.0F;
   /*     */
   /*     */   public MCH_EntitySeat(World world) {
      /*  28 */     super(world);
      /*  29 */     setSize(1.0F, 1.0F);
      /*  30 */     this.yOffset = 0.0F;
      /*  31 */     this.motionX = 0.0D;
      /*  32 */     this.motionY = 0.0D;
      /*  33 */     this.motionZ = 0.0D;
      /*  34 */     this.seatID = -1;
      /*  35 */     setParent((MCH_EntityAircraft)null);
      /*  36 */     this.parentSearchCount = 0;
      /*  37 */     this.lastRiddenByEntity = null;
      /*  38 */     this.ignoreFrustumCheck = true;
      /*  39 */     this.isImmuneToFire = true;
      /*     */   }
   /*     */
   /*     */   public MCH_EntitySeat(World world, double x, double y, double z) {
      /*  43 */     this(world);
      /*  44 */     setPosition(x, y + 1.0D, z);
      /*  45 */     this.prevPosX = x;
      /*  46 */     this.prevPosY = y + 1.0D;
      /*  47 */     this.prevPosZ = z;
      /*     */   }
   /*     */
   /*     */   protected boolean canTriggerWalking() {
      /*  51 */     return false;
      /*     */   }
   /*     */
   /*     */   public AxisAlignedBB getCollisionBox(Entity par1Entity) {
      /*  55 */     return par1Entity.boundingBox;
      /*     */   }
   /*     */
   /*     */   public AxisAlignedBB getBoundingBox() {
      /*  59 */     return this.boundingBox;
      /*     */   }
   /*     */
   /*     */   public boolean canBePushed() {
      /*  63 */     return false;
      /*     */   }
   /*     */
   /*     */   public double getMountedYOffset() {
      /*  67 */     return -0.3D;
      /*     */   }
   /*     */
   /*     */   public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
      /*  71 */     return (getParent() != null) ? getParent().attackEntityFrom(par1DamageSource, par2) : false;
      /*     */   }
   /*     */
   /*     */   public boolean canBeCollidedWith() {
      /*  75 */     return !this.isDead;
      /*     */   }
   /*     */
   /*     */   @SideOnly(Side.CLIENT)
   /*     */   public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9) {}
   /*     */
   /*     */   public void setDead() {
      /*  82 */     super.setDead();
      /*     */   }
   /*     */
   /*     */   public void onUpdate() {
      /*  86 */     super.onUpdate();
      /*  87 */     this.fallDistance = 0.0F;
      /*  88 */     if (this.riddenByEntity != null) {
         /*  89 */       this.riddenByEntity.fallDistance = 0.0F;
         /*     */     }
      /*     */
      /*  92 */     if (this.lastRiddenByEntity == null && this.riddenByEntity != null) {
         /*  93 */       if (getParent() != null) {
            /*  94 */         MCH_Lib.DbgLog(this.worldObj, "MCH_EntitySeat.onUpdate:SeatID=%d", new Object[] { Integer.valueOf(this.seatID), this.riddenByEntity.toString() });
            /*  95 */         getParent().onMountPlayerSeat(this, this.riddenByEntity);
            /*     */       }
         /*  97 */     } else if (this.lastRiddenByEntity != null && this.riddenByEntity == null && getParent() != null) {
         /*  98 */       MCH_Lib.DbgLog(this.worldObj, "MCH_EntitySeat.onUpdate:SeatID=%d", new Object[] { Integer.valueOf(this.seatID), this.lastRiddenByEntity.toString() });
         /*  99 */       getParent().onUnmountPlayerSeat(this, this.lastRiddenByEntity);
         /*     */     }
      /*     */
      /* 102 */     if (this.worldObj.isRemote) {
         /* 103 */       onUpdate_Client();
         /*     */     } else {
         /* 105 */       onUpdate_Server();
         /*     */     }
      /*     */
      /* 108 */     this.lastRiddenByEntity = this.riddenByEntity;
      /*     */   }
   /*     */
   /*     */   private void onUpdate_Client() {
      /* 112 */     checkDetachmentAndDelete();
      /*     */   }
   /*     */
   /*     */   private void onUpdate_Server() {
      /* 116 */     checkDetachmentAndDelete();
      /* 117 */     if (this.riddenByEntity != null && this.riddenByEntity.isDead) {
         /* 118 */       this.riddenByEntity = null;
         /*     */     }
      /*     */   }
   /*     */
   /*     */
   /*     */   public void updateRiderPosition() {
      /* 124 */     updatePosition();
      /*     */   }
   /*     */
   /*     */   public void updatePosition() {
      /* 128 */     Entity ridEnt = this.riddenByEntity;
      /* 129 */     if (ridEnt != null) {
         /* 130 */       ridEnt.setPosition(this.posX, this.posY, this.posZ);
         /* 131 */       ridEnt.motionX = ridEnt.motionY = ridEnt.motionZ = 0.0D;
         /*     */     }
      /*     */   }
   /*     */
   /*     */
   /*     */   public void updateRotation(float yaw, float pitch) {
      /* 137 */     Entity ridEnt = this.riddenByEntity;
      /* 138 */     if (ridEnt != null) {
         /* 139 */       ridEnt.rotationYaw = yaw;
         /* 140 */       ridEnt.rotationPitch = pitch;
         /*     */     }
      /*     */   }
   /*     */
   /*     */
   /*     */   protected void checkDetachmentAndDelete() {
      /* 146 */     if (!this.isDead && (this.seatID < 0 || getParent() == null || (getParent()).isDead)) {
         /* 147 */       if (getParent() != null && (getParent()).isDead) {
            /* 148 */         this.parentSearchCount = 100000000;
            /*     */       }
         /*     */
         /* 151 */       if (this.parentSearchCount >= 1200) {
            /* 152 */         setDead();
            /* 153 */         if (!this.worldObj.isRemote && this.riddenByEntity != null) {
               /* 154 */           this.riddenByEntity.mountEntity((Entity)null);
               /*     */         }
            /*     */
            /* 157 */         setParent((MCH_EntityAircraft)null);
            /* 158 */         MCH_Lib.DbgLog(this.worldObj, "[Error]座席エンティティは本体が見つからないため削除 seat=%d, parentUniqueID=%s", new Object[] { Integer.valueOf(this.seatID), this.parentUniqueID });
            /*     */       } else {
            /* 160 */         this.parentSearchCount++;
            /*     */       }
         /*     */     } else {
         /* 163 */       this.parentSearchCount = 0;
         /*     */     }
      /*     */   }
   /*     */
   /*     */
   /*     */   protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
      /* 169 */     par1NBTTagCompound.setInteger("SeatID", this.seatID);
      /* 170 */     par1NBTTagCompound.setString("ParentUniqueID", this.parentUniqueID);
      /*     */   }
   /*     */
   /*     */   protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
      /* 174 */     this.seatID = par1NBTTagCompound.getInteger("SeatID");
      /* 175 */     this.parentUniqueID = par1NBTTagCompound.getString("ParentUniqueID");
      /*     */   }
   /*     */
   /*     */   @SideOnly(Side.CLIENT)
   /*     */   public float getShadowSize() {
      /* 180 */     return 0.0F;
      /*     */   }
   /*     */
   /*     */   public boolean canRideMob(Entity entity) {
      /* 184 */     return (getParent() != null && this.seatID >= 0) ? (!(getParent().getSeatInfo(this.seatID + 1) instanceof MCH_SeatRackInfo)) : false;
      /*     */   }
   /*     */
   /*     */   public boolean isGunnerMode() {
      /* 188 */     return (this.riddenByEntity != null && getParent() != null) ? getParent().getIsGunnerMode(this.riddenByEntity) : false;
      /*     */   }
   /*     */
   /*     */   public boolean interactFirst(EntityPlayer player) {
      /* 192 */     if (getParent() != null && !getParent().isDestroyed()) {
         /* 193 */       if (!getParent().checkTeam(player)) {
            /* 194 */         return false;
            /*     */       }
         /* 196 */       ItemStack itemStack = player.getCurrentEquippedItem();
         /* 197 */       if (itemStack != null && itemStack.getItem() instanceof mcheli.tool.MCH_ItemWrench)
            /* 198 */         return getParent().interactFirst(player);
         /* 199 */      // if (itemStack != null && itemStack.getItem() instanceof mcheli.mob.MCH_ItemSpawnGunner)
            /* 200 */   //      return getParent().interactFirst(player);
         /* 201 */       if (this.riddenByEntity != null)
            /* 202 */         return false;
         /* 203 */       if (player.ridingEntity != null)
            /* 204 */         return false;
         /* 205 */       if (!canRideMob((Entity)player)) {
            /* 206 */         return false;
            /*     */       }
         /* 208 */       player.mountEntity((Entity)this);
         /* 209 */       return true;
         /*     */     }
      /*     */
      /*     */
      /* 213 */     return false;
      /*     */   }
   /*     */
   /*     */
   /*     */   public MCH_EntityAircraft getParent() {
      /* 218 */     return this.parent;
      /*     */   }
   /*     */
   /*     */   public void setParent(MCH_EntityAircraft parent) {
      /* 222 */     this.parent = parent;
      /*     */   }
   /*     */ }
