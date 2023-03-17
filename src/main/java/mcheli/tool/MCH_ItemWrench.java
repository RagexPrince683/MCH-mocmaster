package mcheli.tool;

import com.google.common.collect.Multimap;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLogic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class MCH_ItemWrench extends W_Item {

   private float damageVsEntity;
   private final ToolMaterial toolMaterial;
   private static Random rand = new Random();


   public MCH_ItemWrench(int itemId, ToolMaterial material) {
      super(itemId);
      this.toolMaterial = material;
      super.maxStackSize = 1;
     // this.setMaxDamage(material.getMaxUses());
      this.setMaxDamage(0);
      this.damageVsEntity = 4.0F + material.getDamageVsEntity();
   }

   public boolean func_150897_b(Block b) {
      Material material = b.getMaterial();
      return material == Material.iron?true:material instanceof MaterialLogic;
   }

   public float func_150893_a(ItemStack itemStack, Block block) {
      Material material = block.getMaterial();
      return material == Material.iron?20.5F:(material instanceof MaterialLogic?5.5F:2.0F);
   }

   public static int getUseAnimCount(ItemStack stack) {
      return getAnimCount(stack, "MCH_WrenchAnim");
   }

   public static void setUseAnimCount(ItemStack stack, int n) {
      setAnimCount(stack, "MCH_WrenchAnim", n);
   }

   public static int getAnimCount(ItemStack stack, String name) {
      if(!stack.hasTagCompound()) {
         stack.stackTagCompound = new NBTTagCompound();
      }

      if(stack.stackTagCompound.hasKey(name)) {
         return stack.stackTagCompound.getInteger(name);
      } else {
         stack.stackTagCompound.setInteger(name, 0);
         return 0;
      }
   }

   public static void setAnimCount(ItemStack stack, String name, int n) {
      if(!stack.hasTagCompound()) {
         stack.stackTagCompound = new NBTTagCompound();
      }

      stack.stackTagCompound.setInteger(name, n);
   }

   public boolean hitEntity(ItemStack itemStack, EntityLivingBase entity, EntityLivingBase player) {

      return true;
   }

   public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int count) {
      setUseAnimCount(stack, 0);
   }

   public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
      MCH_EntityAircraft ac;
      if(player.worldObj.isRemote) {
         ac = this.getMouseOverAircraft(player);
         if(ac != null) {
            int cnt = getUseAnimCount(stack);
            if(cnt <= 0) {
               cnt = 16;
            } else {
               --cnt;
            }

            setUseAnimCount(stack, cnt);
         }
      }

      if(!player.worldObj.isRemote && count < this.getMaxItemUseDuration(stack) && count % 20 == 0) {
         ac = this.getMouseOverAircraft(player);
         if(ac != null && ac.getHP() > 0 && ac.repair(1)) {
        	
            //stack.damageItem(100, player);
        	 for(int i = 0; i< player.inventory.getSizeInventory(); i++) {
        		 if(player.inventory.getStackInSlot(i) == stack) {
        			 player.inventory.setInventorySlotContents(i, null); 
        		 }
        	 }
        	
            W_WorldFunc.MOD_playSoundEffect(player.worldObj, (double)((int)ac.posX), (double)((int)ac.posY), (double)((int)ac.posZ), "wrench", 1.0F, 0.9F + rand.nextFloat() * 0.2F);
         }
      }

   }

   public void onUpdate(ItemStack item, World world, Entity entity, int n, boolean b) {
      if(entity instanceof EntityPlayer) {
         EntityPlayer player = (EntityPlayer)entity;
         ItemStack itemStack = player.getCurrentEquippedItem();
         if(itemStack == item) {
            MCH_MOD.proxy.setCreativeDigDelay(0);
         }
      }

   }

   public MCH_EntityAircraft getMouseOverAircraft(EntityPlayer player) {
      MovingObjectPosition m = this.getMouseOver(player, 1.0F);
      MCH_EntityAircraft ac = null;
      if(m != null) {
         if(m.entityHit instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft)m.entityHit;
         } else if(m.entityHit instanceof MCH_EntitySeat) {
            MCH_EntitySeat seat = (MCH_EntitySeat)m.entityHit;
            if(seat.getParent() != null) {
               ac = seat.getParent();
            }
         }
      }

      return ac;
   }

   private static MovingObjectPosition rayTrace(EntityLivingBase entity, double dist, float tick) {
      Vec3 vec3 = Vec3.createVectorHelper(entity.posX, entity.posY + (double)entity.getEyeHeight(), entity.posZ);
      Vec3 vec31 = entity.getLook(tick);
      Vec3 vec32 = vec3.addVector(vec31.xCoord * dist, vec31.yCoord * dist, vec31.zCoord * dist);
      return entity.worldObj.func_147447_a(vec3, vec32, false, false, true);
   }

   private MovingObjectPosition getMouseOver(EntityLivingBase user, float tick) {
      Entity pointedEntity = null;
      double d0 = 4.0D;
      MovingObjectPosition objectMouseOver = rayTrace(user, d0, tick);
      double d1 = d0;
      Vec3 vec3 = Vec3.createVectorHelper(user.posX, user.posY + (double)user.getEyeHeight(), user.posZ);
      if(objectMouseOver != null) {
         d1 = objectMouseOver.hitVec.distanceTo(vec3);
      }

      Vec3 vec31 = user.getLook(tick);
      Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
      pointedEntity = null;
      Vec3 vec33 = null;
      float f1 = 1.0F;
      List list = user.worldObj.getEntitiesWithinAABBExcludingEntity(user, user.boundingBox.addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand((double)f1, (double)f1, (double)f1));
      double d2 = d1;

      for(int i = 0; i < list.size(); ++i) {
         Entity entity = (Entity)list.get(i);
         if(entity.canBeCollidedWith()) {
            float f2 = entity.getCollisionBorderSize();
            AxisAlignedBB axisalignedbb = entity.boundingBox.expand((double)f2, (double)f2, (double)f2);
            MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);
            if(axisalignedbb.isVecInside(vec3)) {
               if(0.0D < d2 || d2 == 0.0D) {
                  pointedEntity = entity;
                  vec33 = movingobjectposition == null?vec3:movingobjectposition.hitVec;
                  d2 = 0.0D;
               }
            } else if(movingobjectposition != null) {
               double d3 = vec3.distanceTo(movingobjectposition.hitVec);
               if(d3 < d2 || d2 == 0.0D) {
                  if(entity == user.ridingEntity && !entity.canRiderInteract()) {
                     if(d2 == 0.0D) {
                        pointedEntity = entity;
                        vec33 = movingobjectposition.hitVec;
                     }
                  } else {
                     pointedEntity = entity;
                     vec33 = movingobjectposition.hitVec;
                     d2 = d3;
                  }
               }
            }
         }
      }

      if(pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
         objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);
      }

      return objectMouseOver;
   }

   public boolean onBlockDestroyed(ItemStack itemStack, World world, Block block, int x, int y, int z, EntityLivingBase entity) {
      if((double)block.getBlockHardness(world, x, y, z) != 0.0D) {
         itemStack.damageItem(2, entity);
      }

      return true;
   }

   @SideOnly(Side.CLIENT)
   public boolean isFull3D() {
      return true;
   }

   public EnumAction getItemUseAction(ItemStack itemStack) {
      return EnumAction.block;
   }

   public int getMaxItemUseDuration(ItemStack itemStack) {
      return 72000;
   }

   public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
      player.setItemInUse(itemStack, this.getMaxItemUseDuration(itemStack));
      return itemStack;
   }

   public int getItemEnchantability() {
      return this.toolMaterial.getEnchantability();
   }

   public String getToolMaterialName() {
      return this.toolMaterial.toString();
   }

   public boolean getIsRepairable(ItemStack item1, ItemStack item2) {
      return this.toolMaterial.func_150995_f() == item2.getItem()?true:super.getIsRepairable(item1, item2);
   }

   public Multimap getItemAttributeModifiers() {
      Multimap multimap = super.getItemAttributeModifiers();
      multimap.put(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(), new AttributeModifier(Item.field_111210_e, "Weapon modifier", (double)this.damageVsEntity, 0));
      return multimap;
   }

}
