package mcheli.aircraft;

import java.util.Arrays;
import java.util.List;

import mcheli.MCH_Achievement;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.ship.MCH_EntityShip;
import mcheli.wrapper.W_EntityPlayer;
import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockSponge;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.*;
import net.minecraft.world.World;
import mcheli.weapon.MCH_WeaponSet;


public abstract class MCH_ItemBaseVehicle extends W_Item {

   private static boolean isRegistedDispenseBehavior = false;

   //TODO add force dispense behavior/a new block in the mod to dispense vehicles from as say:
   // we want to prevent players from having dispensers but we also don't want vehicles to just be placeable anywhere
   // also: structures! Eg: airport runways, hangars, barracks, etc. Basically multiblock structures that can be built in
   // the world and allow vehicle spawning

   public static int timeHeld = 0;


   public MCH_ItemBaseVehicle(int i) {
      super(i);
   }



   public static void registerDispenseBehavior(Item item) {
      if(!isRegistedDispenseBehavior) {
         BlockDispenser.dispenseBehaviorRegistry.putObject(item, new MCH_ItemBaseVehicleDispenseBehavior());
      }
   }

   //@Override
   //public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean par4) {
   //   MCH_BaseVehicleInfo info = MCH_BaseVehicleInfoManager.getFromItem(stack.getItem());
   //   if (info != null && !"zzz".equals(info.category)) {
   //      lines.add(EnumChatFormatting.RED + "DANGER!");
   //      lines.add(EnumChatFormatting.RED + "This vehicle is not in the default category!");
   //      lines.add(EnumChatFormatting.RED + "May contain experimental features!");
   //   }
//
   //   super.addInformation(stack, player, lines, par4);
   //}

   public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean par4) {
      MCH_BaseVehicleInfo aircraftInfo = this.getAircraftInfo();
      MCH_BaseVehicleInfo info = aircraftInfo != null && aircraftInfo.category.equals("zzz") ? null : aircraftInfo;
      MCH_EntityBaseVehicle ac = aircraftInfo != null ? createAircraft(player.worldObj, -1.0D, -1.0D, -1.0D, stack) : null;
      if (info != null) {
         lines.add(EnumChatFormatting.YELLOW + "Category: " + info.category);
         if(info.weight != 0.0D) {
            lines.add(EnumChatFormatting.YELLOW + "Weight: " + formatPounds(info.weight) + " lb");
         }
         if(info.maximumExternalPayloadCapacity != 0.0D) {
            lines.add(EnumChatFormatting.YELLOW + "Max Payload: " + formatPounds(info.maximumExternalPayloadCapacity) + " lb");
         }
         //todo && not ship
         if(info.isFloat) {
            if (!(ac instanceof MCH_EntityShip)) {
               lines.add(EnumChatFormatting.YELLOW + "Floats on water");
            } else {
                lines.add(EnumChatFormatting.YELLOW + "Requires water (ship)");
            }
         }
         //lines.add(EnumChatFormatting.DARK_PURPLE + "Weapon: " + info.weaponSetList);
         //         tooltip.add(TextFormatting.DARK_PURPLE + "Weapons: " + Arrays.stream(ac.weapons).map(MCH_WeaponSet::getName).collect(Collectors.joining(", ")));
         //lines.add(EnumChatFormatting.DARK_PURPLE + "Weapons: " + Arrays.stream(ac.weapons).map(MCH_WeaponSet::getName).collect(Collectors.joining(", ")));
         //im sure this will work
         if(ac != null) {
            lines.add(EnumChatFormatting.DARK_PURPLE + "Weapons:");

            Arrays.stream(ac.weapons)
                    .map(MCH_WeaponSet::getName)
                    .forEach(name -> lines.add(EnumChatFormatting.GRAY + " - " + name));
         }
         //should look cleaner
      }

      if (isUavInfo(aircraftInfo)) {
         lines.add(EnumChatFormatting.AQUA + getUavTypeLabel(aircraftInfo) + ": use a UAV Station to place and control.");
         if(isSmallUavInfo(aircraftInfo)) {
            lines.add(EnumChatFormatting.GRAY + "Small UAVs may also use a Portable UAV Controller.");
         } else {
            lines.add(EnumChatFormatting.GRAY + "Large UAVs require the standard UAV Station.");
         }
      }
//
      super.addInformation(stack, player, lines, par4);
   }

   private static String formatPounds(double pounds) {
      if(Math.abs(pounds - (double)((long)pounds)) < 0.001D) {
         return String.valueOf((long)pounds);
      }

      return String.format(java.util.Locale.ROOT, "%.1f", pounds);
   }

   public abstract MCH_BaseVehicleInfo getAircraftInfo();

   public abstract MCH_EntityBaseVehicle createAircraft(World var1, double var2, double var4, double var6, ItemStack var8);

   protected boolean shouldPlaceInstantly() {
      return false;
   }

   private static boolean isUavInfo(MCH_BaseVehicleInfo info) {
      return info != null && (info.isUAV || info.isNewUAV);
   }

   private static boolean isSmallUavInfo(MCH_BaseVehicleInfo info) {
      return info != null && info.isSmallUAV;
   }

   private static String getUavTypeLabel(MCH_BaseVehicleInfo info) {
      return isSmallUavInfo(info) ? "Small UAV" : "Large UAV";
   }

   private static String getUavPlacementMessage(MCH_BaseVehicleInfo info) {
      return isSmallUavInfo(info)
              ? "Small UAVs must be placed from a UAV Station or Portable UAV Controller."
              : "Large UAVs must be placed from a UAV Station.";
   }

   private void notifyUavStationRequired(World world, EntityPlayer player) {
      if(!world.isRemote) {
         player.addChatMessage(new ChatComponentText(getUavPlacementMessage(this.getAircraftInfo())));
      }
   }

   MCH_EntityBaseVehicle ac;
   //todo add a wait time for the aircraft to be placed, we dont want people abusing vehicle hopping
   public MCH_EntityBaseVehicle onTileClick(ItemStack itemStack, World world, float rotationYaw, int x, int y, int z) {

      MCH_EntityBaseVehicle ac = this.createAircraft(world, (double)((float)x + 0.5F), (double)((float)y + 1.0F), (double)((float)z + 0.5F), itemStack);
      if(ac == null) {
         logPlacementDebug(world, "onTileClick createAircraft returned null: item=%s target=(%d,%d,%d) yaw=%.2f info=%s", getItemDebugName(itemStack), Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z), Float.valueOf(rotationYaw), getInfoDebugName());
         return null;
      } else {
         //hopefully reloads the 'aircraft' (vehicle)'s textures when placed.
         //if(ac.getAcInfo() != null) {
         //   ac.getAcInfo().reload();
         //   ac.changeType(ac.getAcInfo().name);
         //   ac.onAcInfoReloaded();
         //}
         //causes a crash when the fucking model is not loaded
         ac.onAcInfoReloaded();
         ac.initRotationYaw((float)(((MathHelper.floor_double((double)(rotationYaw * 4.0F / 360.0F) + 0.5D) & 3) - 1) * 90));
         AxisAlignedBB placementBox = ac.boundingBox.expand(-0.1D, -0.1D, -0.1D);
         List collisionBoxes = world.getCollidingBoundingBoxes(ac, placementBox);
         removePlacementSupportCollisions(collisionBoxes, (double)y + 1.0D);
         if(!collisionBoxes.isEmpty()) {
            logPlacementDebug(world, "onTileClick blocked by collision: item=%s info=%s entity=%s pos=(%.3f,%.3f,%.3f) target=(%d,%d,%d) size=(%.3f,%.3f) bb=%s collisions=%s", getItemDebugName(itemStack), getInfoDebugName(), ac.getEntityName(), Double.valueOf(ac.posX), Double.valueOf(ac.posY), Double.valueOf(ac.posZ), Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z), Float.valueOf(ac.width), Float.valueOf(ac.height), formatAabb(placementBox), describeCollisionBoxes(collisionBoxes));
            return null;
         }

         logPlacementDebug(world, "onTileClick placement clear: item=%s info=%s entity=%s target=(%d,%d,%d) size=(%.3f,%.3f) bb=%s", getItemDebugName(itemStack), getInfoDebugName(), ac.getEntityName(), Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z), Float.valueOf(ac.width), Float.valueOf(ac.height), formatAabb(placementBox));
         return ac;
      }
   }

   public String toString() {
      MCH_BaseVehicleInfo info = this.getAircraftInfo();
      return info != null?super.toString() + "(" + info.getDirectoryName() + ":" + info.name + ")":super.toString() + "(null)";
   }

   public ItemStack onItemRightClick(ItemStack par1ItemStack, World world, EntityPlayer player) {
      if(isUavInfo(this.getAircraftInfo())) {
         notifyUavStationRequired(world, player);
         return par1ItemStack;
      }

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

      if (mop == null) {
         logPlacementDebug(world, "rightClick ignored: no raytrace hit item=%s player=%s pos=(%.3f,%.3f,%.3f) pitch=%.2f yaw=%.2f", getItemDebugName(par1ItemStack), player.getCommandSenderName(), Double.valueOf(player.posX), Double.valueOf(player.posY), Double.valueOf(player.posZ), Float.valueOf(player.rotationPitch), Float.valueOf(player.rotationYaw));
         return par1ItemStack;
      }

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

      if (blockingEntity) {
         logPlacementDebug(world, "rightClick ignored: blocking entity at eye ray item=%s player=%s", getItemDebugName(par1ItemStack), player.getCommandSenderName());
         return par1ItemStack;
      }

      if (W_MovingObjectPosition.isHitTypeTile(mop)) {
         if (MCH_MOD.config.PlaceableOnSpongeOnly.prmBool) {
            Block block = world.getBlock(mop.blockX, mop.blockY, mop.blockZ);
            if (!(block instanceof BlockSponge)) {
               logPlacementDebug(world, "rightClick ignored: PlaceableOnSpongeOnly target block=%s at (%d,%d,%d) item=%s", Block.blockRegistry.getNameForObject(block), Integer.valueOf(mop.blockX), Integer.valueOf(mop.blockY), Integer.valueOf(mop.blockZ), getItemDebugName(par1ItemStack));
               return par1ItemStack;
            }
         }

         if (world.getWorldTime() < 100) {
            logPlacementDebug(world, "rightClick ignored: world too new time=%d item=%s", Long.valueOf(world.getWorldTime()), getItemDebugName(par1ItemStack));
            return par1ItemStack;
         }

         if(player.capabilities.isCreativeMode || this.shouldPlaceInstantly()) {
            if(par1ItemStack.stackTagCompound != null) {
               clearDeployTags(par1ItemStack.stackTagCompound);
            }
            deployVehicle(par1ItemStack, world, player, mop.blockX, mop.blockY, mop.blockZ, 0);
            return par1ItemStack;
         }

         if (par1ItemStack.stackTagCompound == null)
            par1ItemStack.stackTagCompound = new NBTTagCompound();

         NBTTagCompound tag = par1ItemStack.stackTagCompound;



         if (!tag.hasKey("DeployStart") ) { //&& !this.ac.isUAV() crashed the game
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
         //else {
         //   if(ac.isUAV() || ac.isNewUAV()) {
         //      if(world.isRemote) {
         //         if(ac.isSmallUAV()) {
         //            W_EntityPlayer.addChatMessage(player, "Please use the UAV station OR Portable Controller");
         //         } else {
         //            W_EntityPlayer.addChatMessage(player, "Please use the UAV station");
         //         }
         //      }
//
         //      ac = null;
         //   }
         //}
         //part of the crashed the game awards

         if (!player.isUsingItem()) {
            player.setItemInUse(par1ItemStack, this.getMaxItemUseDuration(par1ItemStack));
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
      if(isUavInfo(this.getAircraftInfo())) {
         if(stack.stackTagCompound != null) {
            clearDeployTags(stack.stackTagCompound);
         }
         player.stopUsingItem();
         notifyUavStationRequired(player.worldObj, player);
         return;
      }

      int used = this.getMaxItemUseDuration(stack) - count;

      // ===== READY MESSAGE (SERVER ONLY, ONCE) =====
      if (!player.worldObj.isRemote && stack.stackTagCompound != null) {
         NBTTagCompound tag = stack.stackTagCompound;
         tag.setBoolean("ReadyMsgSent", false);

         if (used == MCH_Config.placetimer.prmInt && !tag.getBoolean("ReadyMsgSent")) {
            player.addChatMessage(new ChatComponentText("Vehicle ready for deployment!"));
            tag.setBoolean("ReadyMsgSent", true);
         }
      }

      // ===== SERVER LOGIC ONLY BEYOND THIS POINT =====
      if (stack.stackTagCompound == null || player.worldObj.isRemote) {
         return;
      }

      NBTTagCompound tag = stack.stackTagCompound;

      // Valid raytrace check
      MovingObjectPosition mop = getBlockIncludingWater(player, 5.0D);
      if (mop == null) {
         cancelDeployment(tag, player, "Vehicle deployment cancelled (target lost).");
         return;
      }

      Block block = player.worldObj.getBlock(mop.blockX, mop.blockY, mop.blockZ);
      if (!block.getMaterial().isSolid() && block.getMaterial() != Material.water) {
         cancelDeployment(tag, player, "Vehicle deployment cancelled (invalid surface).");
         return;
      }

      int currentX = mop.blockX;
      int currentY = mop.blockY;
      int currentZ = mop.blockZ;

      if (!tag.hasKey("TargetX") || !tag.hasKey("TargetY") || !tag.hasKey("TargetZ")) {
         tag.setInteger("TargetX", currentX);
         tag.setInteger("TargetY", currentY);
         tag.setInteger("TargetZ", currentZ);
      } else {
         int targetX = tag.getInteger("TargetX");
         int targetY = tag.getInteger("TargetY");
         int targetZ = tag.getInteger("TargetZ");

         boolean coordsChanged = currentX != targetX || currentY != targetY || currentZ != targetZ;
         boolean isLiquid = block.getMaterial().isLiquid();

         if (coordsChanged && !isLiquid) {
            cancelDeployment(tag, player, "Vehicle deployment cancelled (target changed).");
            return;
         }
      }
   }


   private void cancelDeployment(NBTTagCompound tag, EntityPlayer player, String message) {
      //System.out.println("[DEBUG] CancelDeployment called: " + message);
      clearDeployTags(tag);
      player.addChatMessage(new ChatComponentText(message));
      player.stopUsingItem();
   }

   private void clearDeployTags(NBTTagCompound tag) {
      tag.removeTag("DeployStart");
      tag.removeTag("TargetX");
      tag.removeTag("TargetY");
      tag.removeTag("TargetZ");
      //System.out.println("[DEBUG] Cleared deployment tags.");
   }

   public MovingObjectPosition getBlockIncludingWater(EntityPlayer player, double range) {

      //if !player.worldObj.isRemote)
      //why does this STUPID FUCKING SHIT FATALLY ERROR THE SERVER

      Vec3 eyePos = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
      Vec3 lookVec = player.getLookVec();
      Vec3 targetPos = eyePos.addVector(lookVec.xCoord * range, lookVec.yCoord * range, lookVec.zCoord * range);

      MovingObjectPosition mop = player.worldObj.rayTraceBlocks(eyePos, targetPos);

      if (mop == null) {
         // fallback: get block at position player is looking at ignoring collision
         int checkX = (int)Math.floor(targetPos.xCoord);
         int checkY = (int)Math.floor(targetPos.yCoord);
         int checkZ = (int)Math.floor(targetPos.zCoord);

         Block block = player.worldObj.getBlock(checkX, checkY, checkZ);
         if (block.getMaterial() == Material.water || block.getMaterial().isSolid()) {
            return new MovingObjectPosition(checkX, checkY, checkZ, 0, targetPos);
         }
      }

      return mop;
   }

   //@Override
   //public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
   //   if (stack.stackTagCompound != null && stack.stackTagCompound.hasKey("DeployStart")) {
   //      cancelDeployment(stack.stackTagCompound, player, "Vehicle deployment cancelled (input released).");
   //   }
   //}

   @Override
   public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
      if(isUavInfo(this.getAircraftInfo())) {
         if(stack.stackTagCompound != null) {
            clearDeployTags(stack.stackTagCompound);
         }
         notifyUavStationRequired(world, player);
         return;
      }

      int used = this.getMaxItemUseDuration(stack) - timeLeft;

      if (used >= MCH_Config.placetimer.prmInt) {
         NBTTagCompound tag = stack.getTagCompound();
         if(tag == null || !tag.hasKey("TargetX") || !tag.hasKey("TargetY") || !tag.hasKey("TargetZ")) {
            logPlacementDebug(world, "stopUsing ignored: missing deploy target item=%s player=%s used=%d tagPresent=%s", getItemDebugName(stack), player.getCommandSenderName(), Integer.valueOf(used), Boolean.valueOf(tag != null));
            return;
         }

         int x = tag.getInteger("TargetX");
         int y = tag.getInteger("TargetY");
         int z = tag.getInteger("TargetZ");

         deployVehicle(stack, world, player, x, y, z, used);
         clearDeployTags(tag);
      }
      // Minecraft handles usage reset automatically
   }

   private boolean deployVehicle(ItemStack stack, World world, EntityPlayer player, int x, int y, int z, int used) {
      logPlacementDebug(world, "placement attempt: item=%s player=%s used=%d target=(%d,%d,%d) instant=%s", getItemDebugName(stack), player.getCommandSenderName(), Integer.valueOf(used), Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z), Boolean.valueOf(this.shouldPlaceInstantly()));
      MCH_EntityBaseVehicle placed = spawnAircraft(stack, world, player, x, y, z);
      if(placed == null) {
         if(!world.isRemote) {
            player.addChatMessage(new ChatComponentText("Vehicle deployment failed. Check server log for [VehiclePlacement] details."));
         }
         return false;
      }

      W_WorldFunc.MOD_playSoundAtEntity(player, "deploy", 1.0F, 1.0F);

      if(!world.isRemote) {
         player.addChatMessage(new ChatComponentText("Vehicle deployed."));

         MinecraftServer server = MinecraftServer.getServer();
         if(server != null) {
            for(Object o : server.getConfigurationManager().playerEntityList) {
               EntityPlayerMP other = (EntityPlayerMP)o;
               if(other != player) {
                  other.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_BLUE + player.getCommandSenderName() + " has deployed a vehicle!"));
               }
            }
         }
      }

      return true;
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
   //CANT FUCKING DO THAT BECAUSE THIS MOD WAS CODED BY LITERAL MONKIES AND USES A WRAPPER!!!





   public MCH_EntityBaseVehicle spawnAircraft(ItemStack itemStack, World world, EntityPlayer player, int x, int y, int z) {
      if(isUavInfo(this.getAircraftInfo())) {
         notifyUavStationRequired(world, player);
         logPlacementDebug(world, "spawnAircraft rejected UAV before placement: item=%s info=%s target=(%d,%d,%d) remote=%s", getItemDebugName(itemStack), getInfoDebugName(), Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z), Boolean.valueOf(world.isRemote));
         return null;
      }

      MCH_EntityBaseVehicle ac = this.onTileClick(itemStack, world, player.rotationYaw, x, y, z);
      if(ac != null) {
         logPlacementDebug(world, "spawnAircraft candidate: item=%s info=%s entity=%s type=%s remote=%s pos=(%.3f,%.3f,%.3f)", getItemDebugName(itemStack), getInfoDebugName(), ac.getEntityName(), ac.getTypeName(), Boolean.valueOf(world.isRemote), Double.valueOf(ac.posX), Double.valueOf(ac.posY), Double.valueOf(ac.posZ));
         if(ac.isUAV() || ac.isNewUAV()) {
            if(world.isRemote) {
               if(ac.isSmallUAV()) {
                  W_EntityPlayer.addChatMessage(player, "Please use the UAV station OR Portable Controller");
               } else {
                  W_EntityPlayer.addChatMessage(player, "Please use the UAV station");
               }
            }

            logPlacementDebug(world, "spawnAircraft rejected UAV item=%s type=%s small=%s new=%s", getItemDebugName(itemStack), ac.getTypeName(), Boolean.valueOf(ac.isSmallUAV()), Boolean.valueOf(ac.isNewUAV()));
            ac = null;
         } else {
            if(!world.isRemote) {
               ac.getAcDataFromItem(itemStack);
               ac.setVehicleOwnerUUID(player.getUniqueID());
               ac.setVehicleAccessLocked(false);
               ac.markFreshlyPlaced();
               boolean spawned = world.spawnEntityInWorld(ac);
               logPlacementDebug(world, "spawnAircraft world.spawnEntityInWorld result=%s item=%s type=%s entityId=%d dim=%d", Boolean.valueOf(spawned), getItemDebugName(itemStack), ac.getTypeName(), Integer.valueOf(ac.getEntityId()), Integer.valueOf(world.provider.dimensionId));
               MCH_Achievement.addStat(player, MCH_Achievement.welcome, 1);
            }

            if(!player.capabilities.isCreativeMode) {
               --itemStack.stackSize;
            }
         }
      }

      if(ac == null) {
         logPlacementDebug(world, "spawnAircraft failed: item=%s info=%s target=(%d,%d,%d) remote=%s", getItemDebugName(itemStack), getInfoDebugName(), Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z), Boolean.valueOf(world.isRemote));
      }

      return ac;
   }

   private static void logPlacementDebug(World world, String format, Object... args) {
      MCH_Lib.Log(world, "[VehiclePlacement] " + format, args);
   }

   private String getInfoDebugName() {
      MCH_BaseVehicleInfo info = this.getAircraftInfo();
      return info != null ? info.getKindName() + "/" + info.name + " category=" + info.category + " dir=" + info.getDirectoryName() : "null";
   }

   private static String getItemDebugName(ItemStack stack) {
      if(stack == null || stack.getItem() == null) {
         return "null";
      }

      Object name = Item.itemRegistry.getNameForObject(stack.getItem());
      return String.valueOf(name) + "@" + stack.getItemDamage() + "x" + stack.stackSize;
   }

   private static String formatAabb(AxisAlignedBB bb) {
      return bb == null ? "null" : String.format("[%.3f,%.3f,%.3f -> %.3f,%.3f,%.3f]", new Object[]{Double.valueOf(bb.minX), Double.valueOf(bb.minY), Double.valueOf(bb.minZ), Double.valueOf(bb.maxX), Double.valueOf(bb.maxY), Double.valueOf(bb.maxZ)});
   }

   private static void removePlacementSupportCollisions(List boxes, double supportTopY) {
      for(java.util.Iterator it = boxes.iterator(); it.hasNext();) {
         AxisAlignedBB bb = (AxisAlignedBB)it.next();
         if(bb.maxY <= supportTopY + 1.0E-4D) {
            it.remove();
         }
      }
   }

   private static String describeCollisionBoxes(List boxes) {
      StringBuilder sb = new StringBuilder();
      sb.append("count=").append(boxes.size());
      int max = Math.min(boxes.size(), 5);
      for(int i = 0; i < max; ++i) {
         sb.append(" #").append(i).append('=').append(formatAabb((AxisAlignedBB)boxes.get(i)));
      }
      if(boxes.size() > max) {
         sb.append(" ...");
      }
      return sb.toString();
   }

   public void rideEntity(ItemStack item, Entity target, EntityPlayer player) {
      MCH_Config var10000 = MCH_MOD.config;
      if(!MCH_Config.PlaceableOnSpongeOnly.prmBool && target instanceof EntityMinecartEmpty && target.riddenByEntity == null) {
         MCH_EntityBaseVehicle ac = this.spawnAircraft(item, player.worldObj, player, (int)target.posX, (int)target.posY + 2, (int)target.posZ);
         if(!player.worldObj.isRemote && ac != null) {
            ac.mountEntity(target);
         }
      }

   }

}
