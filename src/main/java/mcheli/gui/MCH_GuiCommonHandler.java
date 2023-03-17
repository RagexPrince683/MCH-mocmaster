package mcheli.gui;

import cpw.mods.fml.common.network.IGuiHandler;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.*;
import mcheli.block.MCH_DraftingTableGui;
import mcheli.block.MCH_DraftingTableGuiContainer;
import mcheli.multiplay.MCH_ContainerScoreboard;
import mcheli.multiplay.MCH_GuiScoreboard;
import mcheli.uav.MCH_ContainerUavStation;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.uav.MCH_GuiUavStation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

public class MCH_GuiCommonHandler implements IGuiHandler {

   public static final int GUIID_UAV_STATION = 0;
   public static final int GUIID_AIRCRAFT = 1;
   public static final int GUIID_CONFG = 2;
   public static final int GUIID_INVENTORY = 3;
   public static final int GUIID_DRAFTING = 4;
   public static final int GUIID_MULTI_MNG = 5;


   public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
      MCH_Lib.DbgLog(world, "MCH_GuiCommonHandler.getServerGuiElement ID=%d (%d, %d, %d)", new Object[]{Integer.valueOf(id), Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z)});
      switch(id) {
      case 0:
         if(player.ridingEntity instanceof MCH_EntityUavStation) {
            return new MCH_ContainerUavStation(player.inventory, (MCH_EntityUavStation)player.ridingEntity);
         }
         break;
      case 1:
         MCH_EntityAircraft ac = null;
         if(player.ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft)player.ridingEntity;
         } else if(player.ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation)player.ridingEntity).getControlAircract();
         }

         if(ac != null) {
            return new MCH_AircraftGuiContainer(player, ac);
         }
         break;
      case 2:
         return new MCH_ConfigGuiContainer(player);
      case 3:
      default:
         break;
      case 4:
         return new MCH_DraftingTableGuiContainer(player, x, y, z);
      case 5:
         if(MinecraftServer.getServer().isSinglePlayer()) {
            MCH_Config var10000 = MCH_MOD.config;
            if(!MCH_Config.DebugLog) {
               return null;
            }
         }

         return new MCH_ContainerScoreboard(player);

         case 6:
            System.out.println("opening GUI");
            ac = null;
            if(player.ridingEntity instanceof MCH_EntityAircraft) {
               ac = (MCH_EntityAircraft)player.ridingEntity;
            } else if(player.ridingEntity instanceof MCH_EntityUavStation) {
               ac = ((MCH_EntityUavStation)player.ridingEntity).getControlAircract();
            }

            if(ac != null) {
               ac.print("aaaaaaaaaaaaaaaaa");
               MCH_ContainerHardpoint container = new MCH_ContainerHardpoint(player, (int)player.posX, (int)player.posY, (int)player.posZ, ac);
               return container;
            }
            System.out.println("ffailed to g GUI");

      }

      return null;
   }

   public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
      MCH_Lib.DbgLog(world, "MCH_GuiCommonHandler.getClientGuiElement ID=%d (%d, %d, %d)", new Object[]{Integer.valueOf(id), Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z)});
      switch(id) {
      case 0:
         if(player.ridingEntity instanceof MCH_EntityUavStation) {
            return new MCH_GuiUavStation(player.inventory, (MCH_EntityUavStation)player.ridingEntity);
         }
         break;
      case 1:
         MCH_EntityAircraft ac = null;
         if(player.ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft)player.ridingEntity;
         } else if(player.ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation)player.ridingEntity).getControlAircract();
         }

         if(ac != null) {
            return new MCH_AircraftGui(player, ac);
         }
         break;
      case 2:
         return new MCH_ConfigGui(player);
      case 3:
      default:
         break;
      case 4:
         return new MCH_DraftingTableGui(player, x, y, z);
      case 5:
         return new MCH_GuiScoreboard(player);
      case 6:
         System.out.println("opening GUI");
         ac = null;
         if(player.ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft)player.ridingEntity;
         } else if(player.ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation)player.ridingEntity).getControlAircract();
         }

         if(ac != null) {
            ac.print("aaaaaaaaaaaaaaaaa");
            MCH_ContainerHardpoint container = new MCH_ContainerHardpoint(player, (int)player.posX, (int)player.posY, (int)player.posZ, ac);
            return new MCH_HardpointGui(container);
         }
         System.out.println("ffailed to g GUI");

      }

      return null;
   }
}
