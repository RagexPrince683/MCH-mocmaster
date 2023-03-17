package mcheli.aircraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class MCH_EntityHitBox extends W_Entity {

   public MCH_EntityAircraft parent;
   public int debugId;


   public MCH_EntityHitBox(World world) {
      super(world);
      this.setSize(1.0F, 1.0F);
      super.yOffset = 0.0F;
      super.motionX = 0.0D;
      super.motionY = 0.0D;
      super.motionZ = 0.0D;
      this.parent = null;
      super.ignoreFrustumCheck = true;
      super.isImmuneToFire = true;
   }

   public MCH_EntityHitBox(World world, MCH_EntityAircraft ac, float w, float h) {
      this(world);
      this.setPosition(ac.posX, ac.posY + 1.0D, ac.posZ);
      super.prevPosX = ac.posX;
      super.prevPosY = ac.posY + 1.0D;
      super.prevPosZ = ac.posZ;
      this.parent = ac;
      this.setSize(w, h);
   }

   protected boolean canTriggerWalking() {
      return false;
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
      return -0.3D;
   }

   public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
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
