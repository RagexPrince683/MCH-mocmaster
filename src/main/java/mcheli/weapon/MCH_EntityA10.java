package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class MCH_EntityA10 extends W_Entity {

   public static final int DATAWT_NAME = 29;
   public final int DESPAWN_COUNT;
   public int despawnCount;
   public Entity shootingAircraft;
   public Entity shootingEntity;
   public int shotCount;
   public int direction;
   public int power;
   public float acceleration;
   public int explosionPower;
   public boolean isFlaming;
   public String name;
   public MCH_WeaponInfo weaponInfo;
   static int snd_num = 0;


   public MCH_EntityA10(World world) {
      super(world);
      this.DESPAWN_COUNT = 70;
      this.despawnCount = 0;
      this.shotCount = 0;
      this.direction = 0;
      super.ignoreFrustumCheck = true;
      super.preventEntitySpawning = false;
      this.setSize(5.0F, 3.0F);
      super.yOffset = super.height / 2.0F;
      super.motionX = 0.0D;
      super.motionY = 0.0D;
      super.motionZ = 0.0D;
      this.power = 32;
      this.acceleration = 4.0F;
      this.explosionPower = 1;
      this.isFlaming = false;
      this.shootingEntity = null;
      this.shootingAircraft = null;
      super.isImmuneToFire = true;
      super.renderDistanceWeight *= 10.0D;
   }

   public MCH_EntityA10(World world, double x, double y, double z) {
      this(world);
      super.lastTickPosX = super.prevPosX = super.posX = x;
      super.lastTickPosY = super.prevPosY = super.posY = y;
      super.lastTickPosZ = super.prevPosZ = super.posZ = z;
   }

   protected boolean canTriggerWalking() {
      return false;
   }

   protected void entityInit() {
      this.getDataWatcher().addObject(29, String.valueOf(""));
   }

   public void setWeaponName(String s) {
      if(s != null && !s.isEmpty()) {
         this.weaponInfo = MCH_WeaponInfoManager.get(s);
         if(this.weaponInfo != null && !super.worldObj.isRemote) {
            this.getDataWatcher().updateObject(29, String.valueOf(s));
         }
      }

   }

   public String getWeaponName() {
      return this.getDataWatcher().getWatchableObjectString(29);
   }

   public MCH_WeaponInfo getInfo() {
      return this.weaponInfo;
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

   public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
      return false;
   }

   public boolean canBeCollidedWith() {
      return false;
   }

   public void setDead() {
      super.setDead();
   }

   public void onUpdate() {
      super.onUpdate();
      if(!super.isDead) {
         ++this.despawnCount;
      }

      if(this.weaponInfo == null) {
         this.setWeaponName(this.getWeaponName());
         if(this.weaponInfo == null) {
            this.setDead();
            return;
         }
      }

      if(super.worldObj.isRemote) {
         this.onUpdate_Client();
      } else {
         this.onUpdate_Server();
      }

      if(!super.isDead) {
         if(this.despawnCount <= 20) {
            super.motionY = -0.3D;
         } else {
            this.setPosition(super.posX + super.motionX, super.posY + super.motionY, super.posZ + super.motionZ);
            super.motionY += 0.02D;
         }
      }

   }

   public boolean isRender() {
      return this.despawnCount > 20;
   }

   private void onUpdate_Client() {
      this.shotCount += 4;
   }

   private void onUpdate_Server() {
      if(!super.isDead) {
         if(this.despawnCount > 70) {
            this.setDead();
         } else if(this.despawnCount > 0 && this.shotCount < 40) {
            for(int i = 0; i < 2; ++i) {
               this.shotGAU8(true, this.shotCount);
               ++this.shotCount;
            }

            if(this.shotCount == 38) {
               W_WorldFunc.MOD_playSoundEffect(super.worldObj, super.posX, super.posY, super.posZ, "gau-8_snd", 150.0F, 1.0F);
            }
         }
      }

   }

   protected void shotGAU8(boolean playSound, int cnt) {
      float yaw = (float)(90 * this.direction);
      float pitch = 30.0F;
      double x = super.posX;
      double y = super.posY;
      double z = super.posZ;
      double tX = super.rand.nextDouble() - 0.5D;
      double tY = -2.6D;
      double tZ = super.rand.nextDouble() - 0.5D;
      if(this.direction == 0) {
         tZ += 10.0D;
         z += (double)cnt * 0.6D;
      }

      if(this.direction == 1) {
         tX -= 10.0D;
         x -= (double)cnt * 0.6D;
      }

      if(this.direction == 2) {
         tZ -= 10.0D;
         z -= (double)cnt * 0.6D;
      }

      if(this.direction == 3) {
         tX += 10.0D;
         x += (double)cnt * 0.6D;
      }

      double dist = (double)MathHelper.sqrt_double(tX * tX + tY * tY + tZ * tZ);
      tX = tX * 4.0D / dist;
      tY = tY * 4.0D / dist;
      tZ = tZ * 4.0D / dist;
      MCH_EntityBullet e = new MCH_EntityBullet(super.worldObj, x, y, z, tX, tY, tZ, yaw, pitch, (double)this.acceleration);
      e.setName(this.getWeaponName());
      e.explosionPower = this.shotCount % 4 == 0?this.explosionPower:0;
      e.setPower(this.power);
      e.shootingEntity = this.shootingEntity;
      e.shootingAircraft = this.shootingAircraft;
      super.worldObj.spawnEntityInWorld(e);
   }

   protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
      par1NBTTagCompound.setString("WeaponName", this.getWeaponName());
   }

   protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
      this.despawnCount = 200;
      if(par1NBTTagCompound.hasKey("WeaponName")) {
         this.setWeaponName(par1NBTTagCompound.getString("WeaponName"));
      }

   }

   @SideOnly(Side.CLIENT)
   public float getShadowSize() {
      return 10.0F;
   }

}
