package mcheli;

import mcheli.container.MCH_ItemContainer;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.vehicle.MCH_VehicleInfo;
import mcheli.vehicle.MCH_VehicleInfoManager;
import mcheli.wrapper.W_Achievement;
import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_LanguageRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.stats.Achievement;
import net.minecraftforge.common.AchievementPage;

import java.util.Iterator;

public class MCH_Achievement {

   public static Achievement welcome = null;
   public static Achievement supplyFuel = null;
   public static Achievement supplyAmmo = null;
   public static Achievement aintWarHell = null;
   public static Achievement reliefSupplies = null;
   public static Achievement rideValkyries = null;


   public static void PreInit() {
      Item item = getAnyAircraftIcon("ah-64");
      boolean BC = true;
      boolean BR = true;
      String name = "McHeliWelcome";
      welcome = W_Achievement.registerAchievement("mcheli" + name, name, 1, 1, item, (Achievement)null);
      W_LanguageRegistry.addNameForObject(welcome, "en_US", "Welcome to MC Helicopter MOD", name, "Put the helicopter");
      name = "McHeliSupplyFuel";
      supplyFuel = W_Achievement.registerAchievement("mcheli" + name, name, -1, 1, MCH_MOD.itemFuel, (Achievement)null);
      W_LanguageRegistry.addNameForObject(supplyFuel, "en_US", "Refueling", name, "Refuel aircraft");
      item = getAircraftIcon("ammo_box");
      name = "McHeliSupplyAmmo";
      supplyAmmo = W_Achievement.registerAchievement("mcheli" + name, name, 3, 1, item, (Achievement)null);
      W_LanguageRegistry.addNameForObject(supplyAmmo, "en_US", "Supply ammo", name, "Supply ammo to the aircraft");
      item = getAircraftIcon("uh-1c");
      name = "McHeliRideValkyries";
      rideValkyries = W_Achievement.registerAchievement("mcheli" + name, name, -1, 3, item, (Achievement)null);
      W_LanguageRegistry.addNameForObject(rideValkyries, "en_US", "Ride Of The Valkyries", name, "?");
      item = getAircraftIcon("mh-60l_dap");
      name = "McHeliAintWarHell";
      aintWarHell = W_Achievement.registerAchievement("mcheli" + name, name, 3, 3, item, (Achievement)null);
      W_LanguageRegistry.addNameForObject(aintWarHell, "en_US", "Ain\'t war hell?", name, "?");
      MCH_ItemContainer item1 = MCH_MOD.itemContainer;
      name = "McHeliReliefSupplies";
      reliefSupplies = W_Achievement.registerAchievement("mcheli" + name, name, -1, -1, item1, (Achievement)null);
      W_LanguageRegistry.addNameForObject(reliefSupplies, "en_US", "Relief supplies", name, "Drop a container");
      Achievement[] achievements = new Achievement[]{welcome, supplyFuel, supplyAmmo, aintWarHell, rideValkyries, reliefSupplies};
      AchievementPage.registerAchievementPage(new AchievementPage("MC Helicopter", achievements));
   }

   public static Item getAircraftIcon(String defaultIconAircraft) {
      Item item = W_Item.getItemByName("stone");
      MCH_HeliInfo info = MCH_HeliInfoManager.get(defaultIconAircraft);
      if(info != null && info.getItem() != null) {
         return info.getItem();
      } else {
         MCP_PlaneInfo info1 = MCP_PlaneInfoManager.get(defaultIconAircraft);
         if(info1 != null && info1.getItem() != null) {
            return info1.getItem();
         } else {
            MCH_VehicleInfo info2 = MCH_VehicleInfoManager.get(defaultIconAircraft);
            return info2 != null && info2.getItem() != null?info2.getItem():item;
         }
      }
   }

   public static Item getAnyAircraftIcon(String defaultIconAircraft) {
      Object item = W_Item.getItemByName("stone");
      if(MCH_HeliInfoManager.map.size() > 0) {
         MCH_HeliInfo info = MCH_HeliInfoManager.get(defaultIconAircraft);
         if(info != null && info.item != null) {
            item = info.item;
         } else {
            Iterator i$ = MCH_HeliInfoManager.map.values().iterator();

            while(i$.hasNext()) {
               MCH_HeliInfo i = (MCH_HeliInfo)i$.next();
               if(i.item != null) {
                  item = i.item;
                  break;
               }
            }
         }
      }

      return (Item)item;
   }

   public static void addStat(Entity player, Achievement a, int i) {
      if(a != null && player instanceof EntityPlayer && !player.worldObj.isRemote) {
         ((EntityPlayer)player).addStat(a, i);
      }

   }

}
