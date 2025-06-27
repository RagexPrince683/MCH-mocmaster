package mcheli.aircraft;

import java.util.List;
import mcheli.MCH_Achievement;
import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.wrapper.W_EntityPlayer;
import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockSponge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.World;

public abstract class MCH_ItemAircraft extends W_Item {

   private static boolean isRegistedDispenseBehavior = false;


   public MCH_ItemAircraft(int i) {
      super(i);
   }



   public static void registerDispenseBehavior(Item item) {
      if(!isRegistedDispenseBehavior) {
         BlockDispenser.dispenseBehaviorRegistry.putObject(item, new MCH_ItemAircraftDispenseBehavior());
      }
   }

   public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean par4) {
      MCH_EntityAircraft ac = createAircraft(player.worldObj, -1.0D, -1.0D, -1.0D, stack);
      if (ac != null &&
              ac.isNewUAV()) {
         lines.add(EnumChatFormatting.RED + "DANGER!");
         lines.add(EnumChatFormatting.RED + "This drone has a new UAV mechanic!");
         lines.add(EnumChatFormatting.RED + "It may contain a lot of bugs!");
         lines.add(EnumChatFormatting.RED + "Clear your inventory before use!");
      }
//
      super.addInformation(stack, player, lines, par4);
   }

   public abstract MCH_AircraftInfo getAircraftInfo();

   public abstract MCH_EntityAircraft createAircraft(World var1, double var2, double var4, double var6, ItemStack var8);

   MCH_EntityAircraft ac;
   //todo add a wait time for the aircraft to be placed, we dont want people abusing vehicle hopping
   public MCH_EntityAircraft onTileClick(ItemStack itemStack, World world, float rotationYaw, int x, int y, int z) {

      MCH_EntityAircraft ac = this.createAircraft(world, (double)((float)x + 0.5F), (double)((float)y + 1.0F), (double)((float)z + 0.5F), itemStack);
      if(ac == null) {
         return null;
      } else {
         //hopefully reloads the 'aircraft' (vehicle)'s textures when placed.
         //if(ac.getAcInfo() != null) {
         //   ac.getAcInfo().reload();
         //   ac.changeType(ac.getAcInfo().name);
         //   ac.onAcInfoReloaded();
         //}
         //causes a crash when the fucking model is not loaded
         ac.initRotationYaw((float)(((MathHelper.floor_double((double)(rotationYaw * 4.0F / 360.0F) + 0.5D) & 3) - 1) * 90));
         return !world.getCollidingBoundingBoxes(ac, ac.boundingBox.expand(-0.1D, -0.1D, -0.1D)).isEmpty()?null:ac;
      }
   }

   public String toString() {
      MCH_AircraftInfo info = this.getAircraftInfo();
      return info != null?super.toString() + "(" + info.getDirectoryName() + ":" + info.name + ")":super.toString() + "(null)";
   }

   public ItemStack onItemRightClick(ItemStack par1ItemStack, World world, EntityPlayer player) {
      float f = 1.0F;
      float f1 = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * f;
      float f2 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * f;
      double d0 = player.prevPosX + (player.posX - player.prevPosX) * f;
      double d1 = player.prevPosY + (player.posY - player.prevPosY) * f + 1.62D - player.yOffset;
      double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * f;
      Vec3 vec3 = W_WorldFunc.getWorldVec3(world, d0, d1, d2);
      float f3 = MathHelper.cos(-f2 * 0.017453292F - (float)Math.PI);
      float f4 = MathHelper.sin(-f2 * 0.017453292F - (float)Math.PI);
      float f5 = -MathHelper.cos(-f1 * 0.017453292F);
      float f6 = MathHelper.sin(-f1 * 0.017453292F);
      float f7 = f4 * f5;
      float f8 = f3 * f5;
      double d3 = 5.0D;
      Vec3 vec31 = vec3.addVector(f7 * d3, f6 * d3, f8 * d3);
      MovingObjectPosition mop = W_WorldFunc.clip(world, vec3, vec31, true);

      if (mop == null) return par1ItemStack;

      Vec3 look = player.getLook(f);
      boolean blockingEntity = false;
      float expand = 1.0F;
      List entities = world.getEntitiesWithinAABBExcludingEntity(player, player.boundingBox.addCoord(look.xCoord * d3, look.yCoord * d3, look.zCoord * d3).expand(expand, expand, expand));

      for (Object o : entities) {
         Entity ent = (Entity)o;
         if (ent.canBeCollidedWith()) {
            float border = ent.getCollisionBorderSize();
            if (ent.boundingBox.expand(border, border, border).isVecInside(vec3)) {
               blockingEntity = true;
               break;
            }
         }
      }

      if (blockingEntity) return par1ItemStack;

      if (W_MovingObjectPosition.isHitTypeTile(mop)) {
         if (MCH_MOD.config.PlaceableOnSpongeOnly.prmBool) {
            Block block = world.getBlock(mop.blockX, mop.blockY, mop.blockZ);
            if (!(block instanceof BlockSponge)) return par1ItemStack;
         }

         if (world.getWorldTime() < 100) return par1ItemStack;

         if (par1ItemStack.stackTagCompound == null)
            par1ItemStack.stackTagCompound = new NBTTagCompound();

         NBTTagCompound tag = par1ItemStack.stackTagCompound;

         if (!tag.hasKey("DeployStart")) {
            tag.setLong("DeployStart", par1ItemStack.getMaxItemUseDuration());
            //this.getMaxItemUseDuration(stack) - count
            //idk idk this is beyond my mental capacity to even fucking look at rn IDK IDK IDK
            tag.setInteger("TargetX", mop.blockX);
            tag.setInteger("TargetY", mop.blockY);
            tag.setInteger("TargetZ", mop.blockZ);
            player.setItemInUse(par1ItemStack, this.getMaxItemUseDuration(par1ItemStack));

            if (world.isRemote)
               player.addChatMessage(new ChatComponentText("Hold click to deploy vehicle..."));
         }
         //todo reset deploystart on single click but not on hold?
      }

      return par1ItemStack;
   }

   @Override
   public int getMaxItemUseDuration(ItemStack stack) {
      return 72000;
   }

   @Override
   public EnumAction getItemUseAction(ItemStack stack) {
      return EnumAction.bow;
   }

   @Override
   public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
      if (player.worldObj.isRemote) return;

      NBTTagCompound tag = stack.getTagCompound();
      if (tag == null || !tag.hasKey("StartCount")) return;

      int startCount = tag.getInteger("StartCount");
      int timeHeld = startCount - count;
      int limit = MCH_Config.placetimer.prmInt;

      // If count never decreased, client likely released → cancel
      if (count == startCount || timeHeld < 0) {
         clearDeployTags(tag);
         player.stopUsingItem();
         player.addChatMessage(new ChatComponentText("Vehicle deployment cancelled (release detected)."));
         return;
      }

      // Safety timeout in case the previous check misses
      if (timeHeld > limit * 2) {
         clearDeployTags(tag);
         player.stopUsingItem();
         player.addChatMessage(new ChatComponentText("Vehicle deployment cancelled (timeout)."));
         return;
      }

      // Only spawn when truly held enough time
      if (timeHeld >= limit) {
         int x = tag.getInteger("TargetX");
         int y = tag.getInteger("TargetY");
         int z = tag.getInteger("TargetZ");
         this.spawnAircraft(stack, player.worldObj, player, x, y, z);
         player.addChatMessage(new ChatComponentText("Vehicle deployed."));
         W_WorldFunc.MOD_playSoundAtEntity(player, "deploy", 1.0F, 1.0F);
         clearDeployTags(tag);
         player.stopUsingItem();
      }
   }

   private void cancelDeployment(NBTTagCompound tag, EntityPlayer player, String message) {
      System.out.println("[DEBUG] CancelDeployment called: " + message);
      clearDeployTags(tag);
      player.addChatMessage(new ChatComponentText(message));
      player.stopUsingItem();
   }

   private void clearDeployTags(NBTTagCompound tag) {
      tag.removeTag("DeployStart");
      tag.removeTag("TargetX");
      tag.removeTag("TargetY");
      tag.removeTag("TargetZ");
      System.out.println("[DEBUG] Cleared deployment tags.");
   }

   private MovingObjectPosition getSolidBlockLookedAt(EntityPlayer player, double distance) {
      Vec3 eyePos = player.getPosition(1.0F).addVector(0, player.getEyeHeight(), 0);
      Vec3 lookVec = player.getLook(1.0F);
      Vec3 reachVec = eyePos.addVector(lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance);
      return player.worldObj.rayTraceBlocks(eyePos, reachVec);
   }

   @Override
   public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
      if (stack.stackTagCompound != null && stack.stackTagCompound.hasKey("DeployStart")) {
         cancelDeployment(stack.stackTagCompound, player, "Vehicle deployment cancelled (input released).");
      }
   }

   //@Override
   //public boolean canContinueUsing(ItemStack stack, World world, EntityLivingBase entity, int count) {
   //   return true;
   //}
   //idk if we will still need this but it is here

   //@Override
   //public boolean canContinueUsing(ItemStack oldStack, ItemStack newStack) {
   //   // Return true only if StartCount tag is still present (meaning still holding)
   //   NBTTagCompound tag = oldStack.getTagCompound();
   //   return tag != null && tag.hasKey("StartCount");
   //}
   //CANT FUCKING DO THAT BECAUSE THIS MOD WAS CODED BY LITERAL MONKIES!!!





   public MCH_EntityAircraft spawnAircraft(ItemStack itemStack, World world, EntityPlayer player, int x, int y, int z) {


      MCH_EntityAircraft ac = this.onTileClick(itemStack, world, player.rotationYaw, x, y, z);
      if(ac != null) {
         if(ac.isUAV() || ac.isNewUAV()) {
            if(world.isRemote) {
               if(ac.isSmallUAV()) {
                  W_EntityPlayer.addChatMessage(player, "Please use the UAV station OR Portable Controller");
               } else {
                  W_EntityPlayer.addChatMessage(player, "Please use the UAV station");
               }
            }

            ac = null;
         } else {
            if(!world.isRemote) {
               ac.getAcDataFromItem(itemStack);
               world.spawnEntityInWorld(ac);
               MCH_Achievement.addStat(player, MCH_Achievement.welcome, 1);
            }

            if(!player.capabilities.isCreativeMode) {
               --itemStack.stackSize;
            }
         }
      }

      return ac;
   }

   public void rideEntity(ItemStack item, Entity target, EntityPlayer player) {
      MCH_Config var10000 = MCH_MOD.config;
      if(!MCH_Config.PlaceableOnSpongeOnly.prmBool && target instanceof EntityMinecartEmpty && target.riddenByEntity == null) {
         MCH_EntityAircraft ac = this.spawnAircraft(item, player.worldObj, player, (int)target.posX, (int)target.posY + 2, (int)target.posZ);
         if(!player.worldObj.isRemote && ac != null) {
            ac.mountEntity(target);
         }
      }

   }

}
