package mcheli.aircraft;

import java.util.*;

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
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;

public abstract class MCH_ItemAircraft extends W_Item {

   private static boolean isRegistedDispenseBehavior = false;
   private static final int PLACEMENT_DELAY = 5; // Delay in seconds
   private static final Map<String, Long> playerCooldowns = new HashMap<>();

   private static final int SPAWN_DELAY_TICKS = 100; // 5 seconds (100 ticks)
   private final Map<UUID, Long> spawnDelays = new HashMap<>();


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
         lines.add(EnumChatFormatting.RED + "WARNING!");
         lines.add(EnumChatFormatting.RED + "This drone has the new UAV mechanic!");
         lines.add(EnumChatFormatting.RED + "It may contain bugs!");
         lines.add(EnumChatFormatting.RED + "Clear your inventory before use!");
      }
//
      super.addInformation(stack, player, lines, par4);
   }
   //todo add a variable that allows you to add desc for other items too not just new uavs

   public abstract MCH_AircraftInfo getAircraftInfo();

   public abstract MCH_EntityAircraft createAircraft(World var1, double var2, double var4, double var6, ItemStack var8);

   MCH_EntityAircraft ac;
   public MCH_EntityAircraft onTileClick(ItemStack itemStack, World world, float rotationYaw, int x, int y, int z) {

      //todo add a wait time for the aircraft to be placed, we dont want people abusing vehicle hopping
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
         //fixed models not rendering as of MCHO hotfix
         ac.initRotationYaw((float)(((MathHelper.floor_double((double)(rotationYaw * 4.0F / 360.0F) + 0.5D) & 3) - 1) * 90));
         return !world.getCollidingBoundingBoxes(ac, ac.boundingBox.expand(-0.1D, -0.1D, -0.1D)).isEmpty()?null:ac;
      }
   }

   public String toString() {
      MCH_AircraftInfo info = this.getAircraftInfo();
      return info != null?super.toString() + "(" + info.getDirectoryName() + ":" + info.name + ")":super.toString() + "(null)";
   }

   //@Override we aren't overriding anything
   public ItemStack onItemRightClick(ItemStack par1ItemStack, World world, EntityPlayer player) {
      UUID playerId = player.getUniqueID();
      long currentTime = System.currentTimeMillis(); // Get current real-world time in milliseconds

      // Check if the player has a cooldown
      if (spawnDelays.containsKey(playerId)) {
         long timeLeft = spawnDelays.get(playerId) - currentTime;
         if (timeLeft > 0) {
            player.addChatMessage(new ChatComponentText("You must wait " + (timeLeft / 1000) + " seconds before spawning another aircraft."));
            return par1ItemStack;
         }
      }

      // Aircraft placement logic
      float f = 1.0F;
      float f1 = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * f;
      float f2 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * f;
      double d0 = player.prevPosX + (player.posX - player.prevPosX) * f;
      double d1 = player.prevPosY + (player.posY - player.prevPosY) * f + 1.62D - player.yOffset;
      double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * f;
      Vec3 vec3 = W_WorldFunc.getWorldVec3(world, d0, d1, d2);
      float f3 = MathHelper.cos(-f2 * 0.017453292F - 3.1415927F);
      float f4 = MathHelper.sin(-f2 * 0.017453292F - 3.1415927F);
      float f5 = -MathHelper.cos(-f1 * 0.017453292F);
      float f6 = MathHelper.sin(-f1 * 0.017453292F);
      float f7 = f4 * f5;
      float f8 = f3 * f5;
      double d3 = 5.0D;
      Vec3 vec31 = vec3.addVector(f7 * d3, f6 * d3, f8 * d3);
      MovingObjectPosition mop = W_WorldFunc.clip(world, vec3, vec31, true);

      if (mop == null) {
         return par1ItemStack;
      }

      Vec3 vec32 = player.getLook(f);
      boolean flag = false;
      float f9 = 1.0F;
      List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(player,
              player.boundingBox.addCoord(vec32.xCoord * d3, vec32.yCoord * d3, vec32.zCoord * d3)
                      .expand(f9, f9, f9));

      for (Entity block : list) {
         if (block.canBeCollidedWith()) {
            float f10 = block.getCollisionBorderSize();
            AxisAlignedBB axisalignedbb = block.boundingBox.expand(f10, f10, f10);
            if (axisalignedbb.isVecInside(vec3)) {
               flag = true;
            }
         }
      }

      if (flag) {
         return par1ItemStack;
      }

      if (W_MovingObjectPosition.isHitTypeTile(mop)) {
         MCH_Config var10000 = MCH_MOD.config;
         if (MCH_Config.PlaceableOnSpongeOnly.prmBool) {
            Block var32 = world.getBlock(mop.blockX, mop.blockY, mop.blockZ);
            if (!(var32 instanceof BlockSponge)) {
               return par1ItemStack;
            }
         }

         // Set the delay before allowing another spawn
         spawnDelays.put(playerId, currentTime + (SPAWN_DELAY_TICKS * 50)); // Convert ticks to milliseconds (5 seconds)

         player.addChatMessage(new ChatComponentText("Vehicle spawned! You must wait " + (SPAWN_DELAY_TICKS / 20) + " seconds before spawning another one."));
      }

      return par1ItemStack;
   }


   public MCH_EntityAircraft spawnAircraft(ItemStack itemStack, World world, EntityPlayer player, int x, int y, int z) {
      String playerName = player.getCommandSenderName();
      long currentTime = System.currentTimeMillis();

      if (playerCooldowns.containsKey(playerName)) {
         long lastPlacementTime = playerCooldowns.get(playerName);
         if ((currentTime - lastPlacementTime) < (PLACEMENT_DELAY * 1000)) {
            if (world.isRemote) {
               W_EntityPlayer.addChatMessage(player, "You must wait before placing another aircraft.");
            }
            return null;
         }
      }

      playerCooldowns.put(playerName, currentTime);

      try {
         Thread.sleep(PLACEMENT_DELAY * 1000); // Delay before spawning aircraft
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      MCH_EntityAircraft ac = this.onTileClick(itemStack, world, player.rotationYaw, x, y, z);
      if(ac != null) {
         if(ac.isUAV() || ac.isNewUAV()) {
            if(world.isRemote) {
               if(ac.isSmallUAV()) {
                  W_EntityPlayer.addChatMessage(player, "Please use the UAV station OR Portable Controller");
               } else {
                  W_EntityPlayer.addChatMessage(player, "Please use the UAV station to control this vehicle.");
               }
            }
         }

         if (!world.isRemote) {
            world.spawnEntityInWorld(ac);
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
