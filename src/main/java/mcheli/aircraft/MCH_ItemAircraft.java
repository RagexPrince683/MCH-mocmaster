package mcheli.aircraft;

import java.util.List;
import mcheli.MCH_Achievement;
import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_ItemAircraftDispenseBehavior;
import mcheli.wrapper.W_EntityPlayer;
import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockSponge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
      double d0 = player.prevPosX + (player.posX - player.prevPosX) * (double)f;
      double d1 = player.prevPosY + (player.posY - player.prevPosY) * (double)f + 1.62D - (double)player.yOffset;
      double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * (double)f;
      Vec3 vec3 = W_WorldFunc.getWorldVec3(world, d0, d1, d2);
      float f3 = MathHelper.cos(-f2 * 0.017453292F - 3.1415927F);
      float f4 = MathHelper.sin(-f2 * 0.017453292F - 3.1415927F);
      float f5 = -MathHelper.cos(-f1 * 0.017453292F);
      float f6 = MathHelper.sin(-f1 * 0.017453292F);
      float f7 = f4 * f5;
      float f8 = f3 * f5;
      double d3 = 5.0D;
      Vec3 vec31 = vec3.addVector((double)f7 * d3, (double)f6 * d3, (double)f8 * d3);
      MovingObjectPosition mop = W_WorldFunc.clip(world, vec3, vec31, true);
      if(mop == null) {
         return par1ItemStack;
      } else {
         Vec3 vec32 = player.getLook(f);
         boolean flag = false;
         float f9 = 1.0F;
         List list = world.getEntitiesWithinAABBExcludingEntity(player, player.boundingBox.addCoord(vec32.xCoord * d3, vec32.yCoord * d3, vec32.zCoord * d3).expand((double)f9, (double)f9, (double)f9));

         for(int i = 0; i < list.size(); ++i) {
            Entity block = (Entity)list.get(i);
            if(block.canBeCollidedWith()) {
               float f10 = block.getCollisionBorderSize();
               AxisAlignedBB axisalignedbb = block.boundingBox.expand((double)f10, (double)f10, (double)f10);
               if(axisalignedbb.isVecInside(vec3)) {
                  flag = true;
               }
            }
         }

         if(flag) {
            return par1ItemStack;
         } else {
            if(W_MovingObjectPosition.isHitTypeTile(mop)) {
               MCH_Config var10000 = MCH_MOD.config;
               if(MCH_Config.PlaceableOnSpongeOnly.prmBool) {
                  Block var32 = world.getBlock(mop.blockX, mop.blockY, mop.blockZ);
                  if(!(var32 instanceof BlockSponge)) {
                     return par1ItemStack;
                  }
               }

               if (par1ItemStack.stackTagCompound == null)
                  par1ItemStack.stackTagCompound = new NBTTagCompound();

               NBTTagCompound tag = par1ItemStack.stackTagCompound;

               if (!tag.hasKey("DeployStart")) {
                  tag.setLong("DeployStart", world.getTotalWorldTime());
                  tag.setInteger("TargetX", mop.blockX);
                  tag.setInteger("TargetY", mop.blockY);
                  tag.setInteger("TargetZ", mop.blockZ);
                  player.setItemInUse(par1ItemStack, this.getMaxItemUseDuration(par1ItemStack));
                  if (world.isRemote)
                     player.addChatMessage(new ChatComponentText("Hold click to deploy vehicle..."));
               }

            }

            return par1ItemStack;
         }
      }
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
      if (stack.stackTagCompound == null || player.worldObj.isRemote) {
         System.out.println("[DEBUG] Stack has no tag or is remote world.");
         return;
      }

      NBTTagCompound tag = stack.stackTagCompound;

      // Validate both deploy state and click-hold continuously
      boolean holdingClick = player.getItemInUse() == stack;
      boolean hasDeployStart = tag.hasKey("DeployStart");

      if (!holdingClick) {
         if (hasDeployStart) {
            System.out.println("[DEBUG] Player released right-click, cancelling.");
            cancelDeployment(tag, player, "Vehicle deployment cancelled (input released).");
         }
         return;
      }

      if (!hasDeployStart) {
         System.out.println("[DEBUG] No DeployStart tag.");
         return;
      }

      // Valid raytrace check
      MovingObjectPosition mop = getSolidBlockLookedAt(player, 5.0D);
      if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
         System.out.println("[DEBUG] Raytrace failed or not a block, cancelling.");
         cancelDeployment(tag, player, "Vehicle deployment cancelled (target lost).");
         return;
      }

      int currentX = mop.blockX;
      int currentY = mop.blockY;
      int currentZ = mop.blockZ;
      System.out.println("[DEBUG] Raytrace hit block at: " + currentX + "," + currentY + "," + currentZ);

      if (!tag.hasKey("TargetX") || !tag.hasKey("TargetY") || !tag.hasKey("TargetZ")) {
         System.out.println("[DEBUG] No saved target, setting initial target.");
         tag.setInteger("TargetX", currentX);
         tag.setInteger("TargetY", currentY);
         tag.setInteger("TargetZ", currentZ);
      } else {
         int targetX = tag.getInteger("TargetX");
         int targetY = tag.getInteger("TargetY");
         int targetZ = tag.getInteger("TargetZ");

         System.out.println("[DEBUG] Saved target: " + targetX + "," + targetY + "," + targetZ);

         if (currentX != targetX || currentY != targetY || currentZ != targetZ) {
            System.out.println("[DEBUG] Target changed, cancelling.");
            cancelDeployment(tag, player, "Vehicle deployment cancelled (target changed).");
            return;
         }
      }

      long deployStart = tag.getLong("DeployStart");
      long timeHeld = player.worldObj.getTotalWorldTime() - deployStart;
      System.out.println("[DEBUG] Time held: " + timeHeld + " ticks. Required: " + MCH_Config.placetimer.prmInt);

      if (timeHeld >= MCH_Config.placetimer.prmInt) {
         int targetX = tag.getInteger("TargetX");
         int targetY = tag.getInteger("TargetY");
         int targetZ = tag.getInteger("TargetZ");

         System.out.println("[DEBUG] Deployment complete. Spawning vehicle at: " + targetX + "," + targetY + "," + targetZ);

         this.spawnAircraft(stack, player.worldObj, player, targetX, targetY, targetZ);
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
      if (!world.isRemote && stack.stackTagCompound != null && stack.stackTagCompound.hasKey("DeployStart")) {
         System.out.println("[DEBUG] Player stopped using item manually.");
         cancelDeployment(stack.stackTagCompound, player, "Vehicle deployment cancelled (input released).");
      }
   }


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
