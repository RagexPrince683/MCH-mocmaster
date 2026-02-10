package mcheli;

import cpw.mods.fml.common.registry.GameRegistry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import mcheli.MCH_Config;
import mcheli.MCH_IRecipeList;
import mcheli.MCH_MOD;
import mcheli.MCH_RecipeFuel;
import mcheli.MCH_RecipeReloadRangeFinder;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_AircraftInfoManager;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.item.MCH_ItemInfo;
import mcheli.item.MCH_ItemInfoManager;
import mcheli.lweapon.MCH_ItemLightWeaponBullet;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.ship.MCH_ShipInfo;
import mcheli.ship.MCH_ShipInfoManager;
import mcheli.tank.MCH_TankInfo;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.throwable.MCH_ThrowableInfo;
import mcheli.throwable.MCH_ThrowableInfoManager;
import mcheli.uav.MCH_ItemUavStation;
import mcheli.vehicle.MCH_VehicleInfo;
import mcheli.vehicle.MCH_VehicleInfoManager;
import mcheli.wrapper.W_Block;
import mcheli.wrapper.W_Item;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;


import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.oredict.OreDictionary;


public class MCH_ItemRecipe implements MCH_IRecipeList {

   private static final MCH_ItemRecipe instance = new MCH_ItemRecipe();
   private static List commonItemRecipe = new ArrayList();


   public static MCH_ItemRecipe getInstance() {
      return instance;
   }

   public int getRecipeListSize() {
      return commonItemRecipe.size();
   }

   public IRecipe getRecipe(int index) {
      return (IRecipe)commonItemRecipe.get(index);
   }

   private static void addRecipeList(IRecipe recipe) {
      if(recipe != null) {
         commonItemRecipe.add(recipe);
      }

   }

   private static void registerCommonItemRecipe() {
      commonItemRecipe.clear();
      GameRegistry.addRecipe(new MCH_RecipeFuel());
      MCH_Config var10001 = MCH_MOD.config;
      addRecipeList(addRecipe(MCH_MOD.itemFuel, MCH_Config.ItemRecipe_Fuel.prmString));
      var10001 = MCH_MOD.config;
      addRecipeList(addRecipe(MCH_MOD.itemGLTD, MCH_Config.ItemRecipe_GLTD.prmString));
      var10001 = MCH_MOD.config;
      addRecipeList(addRecipe(MCH_MOD.itemChain, MCH_Config.ItemRecipe_Chain.prmString));
      var10001 = MCH_MOD.config;
      addRecipeList(addRecipe(MCH_MOD.itemParachute, MCH_Config.ItemRecipe_Parachute.prmString));
      var10001 = MCH_MOD.config;
      addRecipeList(addRecipe(MCH_MOD.itemContainer, MCH_Config.ItemRecipe_Container.prmString));

      for(int i = 0; i < MCH_MOD.itemUavStation.length; ++i) {
         MCH_ItemUavStation var10000 = MCH_MOD.itemUavStation[i];
         var10001 = MCH_MOD.config;
         addRecipeList(addRecipe(var10000, MCH_Config.ItemRecipe_UavStation[i].prmString));
      }

      var10001 = MCH_MOD.config;
      addRecipeList(addRecipe(MCH_MOD.itemWrench, MCH_Config.ItemRecipe_Wrench.prmString));
      var10001 = MCH_MOD.config;
      addRecipeList(addRecipe(MCH_MOD.itemRangeFinder, MCH_Config.ItemRecipe_RangeFinder.prmString));
      GameRegistry.addRecipe(new MCH_RecipeReloadRangeFinder());
      var10001 = MCH_MOD.config;
      addRecipeList(addRecipe(MCH_MOD.itemStinger, MCH_Config.ItemRecipe_Stinger.prmString));
      MCH_ItemLightWeaponBullet var1 = MCH_MOD.itemStingerBullet;
      StringBuilder var3 = (new StringBuilder()).append("2,");
      MCH_Config var10002 = MCH_MOD.config;
      addRecipeList(addRecipe(var1, var3.append(MCH_Config.ItemRecipe_StingerMissile.prmString).toString()));
      var10001 = MCH_MOD.config;
      addRecipeList(addRecipe(MCH_MOD.itemJavelin, MCH_Config.ItemRecipe_Javelin.prmString));
      var1 = MCH_MOD.itemJavelinBullet;
      var3 = (new StringBuilder()).append("2,");
      var10002 = MCH_MOD.config;
      addRecipeList(addRecipe(var1, var3.append(MCH_Config.ItemRecipe_JavelinMissile.prmString).toString()));
      Item var2 = W_Item.getItemFromBlock(MCH_MOD.blockDraftingTable);
      addRecipeList(addRecipe(MCH_MOD.itemRpg, MCH_Config.ItemRecipe_Rpg.prmString));
      var1 = MCH_MOD.itemRpgBullet;
      var3 = (new StringBuilder()).append("2,");
      addRecipeList(addRecipe(var1, var3.append(MCH_Config.ItemRecipe_RpgMissile.prmString).toString()));
      var10001 = MCH_MOD.config;
      addRecipeList(addRecipe(var2, MCH_Config.ItemRecipe_DraftingTable.prmString));
   }

   public static void registerItemRecipe() {
      registerCommonItemRecipe();
      Iterator i$ = MCH_HeliInfoManager.map.keySet().iterator();

      String name;
      while(i$.hasNext()) {
         name = (String)i$.next();
         MCH_HeliInfo info = (MCH_HeliInfo)MCH_HeliInfoManager.map.get(name);
         addRecipeAndRegisterList(info, info.item, MCH_HeliInfoManager.getInstance());
      }

      i$ = MCP_PlaneInfoManager.map.keySet().iterator();

      while(i$.hasNext()) {
         name = (String)i$.next();
         MCP_PlaneInfo info1 = (MCP_PlaneInfo)MCP_PlaneInfoManager.map.get(name);
         addRecipeAndRegisterList(info1, info1.item, MCP_PlaneInfoManager.getInstance());
      }

      i$ = MCH_ShipInfoManager.map.keySet().iterator();

      while(i$.hasNext()) {
         name = (String)i$.next();
         MCH_ShipInfo info1 = (MCH_ShipInfo)MCH_ShipInfoManager.map.get(name);
         addRecipeAndRegisterList(info1, info1.item, MCH_ShipInfoManager.getInstance());
      }

      i$ = MCH_TankInfoManager.map.keySet().iterator();

      while(i$.hasNext()) {
         name = (String)i$.next();
         MCH_TankInfo info2 = (MCH_TankInfo)MCH_TankInfoManager.map.get(name);
         addRecipeAndRegisterList(info2, info2.item, MCH_TankInfoManager.getInstance());
      }

      i$ = MCH_VehicleInfoManager.map.keySet().iterator();

      while(i$.hasNext()) {
         name = (String)i$.next();
         MCH_VehicleInfo info3 = (MCH_VehicleInfo)MCH_VehicleInfoManager.map.get(name);
         addRecipeAndRegisterList(info3, info3.item, MCH_VehicleInfoManager.getInstance());
      }

      MCH_ThrowableInfo info4;
      for(i$ = MCH_ThrowableInfoManager.getKeySet().iterator(); i$.hasNext(); info4.recipeString = null) {
         name = (String)i$.next();
         info4 = MCH_ThrowableInfoManager.get(name);
         Iterator i$1 = info4.recipeString.iterator();

         while(i$1.hasNext()) {
            String s = (String)i$1.next();
            if(s.length() >= 3) {
               IRecipe recipe = addRecipe(info4.item, s, info4.isShapedRecipe);
               info4.recipe.add(recipe);
               addRecipeList(recipe);
            }
         }
      }

      MCH_ItemInfo info5;
      //YOU WILL REGISTER THE FUCKING RECIPE AND YOU WILL LIKE IT MOTHERFUCKER
      for(i$ = MCH_ItemInfoManager.getKeySet().iterator(); i$.hasNext(); info5.recipeString = null) {
         name = (String)i$.next();
         info5 = MCH_ItemInfoManager.get(name);
         Iterator i$1 = info5.recipeString.iterator();

         while(i$1.hasNext()) {
            String s = (String)i$1.next();
            if(s.length() >= 3) {
               IRecipe recipe = addRecipe(info5.item, s, info5.isShapedRecipe);
               info5.recipe.add(recipe);
               addRecipeList(recipe);
            }
         }
      }

   }

   private static void addRecipeAndRegisterList(MCH_AircraftInfo info, Item item, MCH_AircraftInfoManager im) {
      int count = 0;
      Iterator i$ = info.recipeString.iterator();

      while(i$.hasNext()) {
         String s = (String)i$.next();
         ++count;
         if(s.length() >= 3) {
            IRecipe recipe = addRecipe(item, s, info.isShapedRecipe);
            info.recipe.add(recipe);
            im.addRecipe(recipe, count, info.name, s);
         }
      }

      info.recipeString = null;
   }

   public static IRecipe addRecipe(Item item, String data) {
      return addShapedRecipe(item, data);
   }

   public static IRecipe addRecipe(Item item, String data, boolean isShaped) {
      return isShaped?addShapedRecipe(item, data):addShapelessRecipe(item, data);
   }

   public static IRecipe addShapedRecipe(Item item, String data)
   {
      ArrayList<Object> rcp = new ArrayList<Object>();
      String[] s = data.split("\\s*,\\s*");
      if (s.length < 3) return null;

      int start = 0;
      int createNum = 1;

      if (isNumber(s[0])) {
         start = 1;
         createNum = Integer.parseInt(s[0]);
         if (createNum <= 0) createNum = 1;
      }

      int idx = start;

      // shape
      for (int i = 0; i < 3; i++) {
         String row = s[idx++];
         rcp.add(row.substring(1, row.length() - 1));
      }

      boolean key = true;
      while (idx < s.length) {
         if (key) {
            rcp.add(s[idx].charAt(0));
         } else {
            String name = s[idx].toLowerCase();
            int meta = 0;

            if (idx + 1 < s.length && isNumber(s[idx + 1])) {
               meta = Integer.parseInt(s[++idx]);
            }

            if (OreDictionary.doesOreNameExist(name)) {
               rcp.add(name);
            } else {
               rcp.add(new ItemStack(W_Item.getItemByName(name), 1, meta));
            }
         }
         key = !key;
         idx++;
      }

      IRecipe recipe = new ShapedOreRecipe(
              new ItemStack(item, createNum),
              rcp.toArray()
      );

      GameRegistry.addRecipe(recipe);
      return recipe;
   }


   public static IRecipe addShapelessRecipe(Item item, String data)
   {
      ArrayList<Object> rcp = new ArrayList<Object>();
      String[] s = data.split("\\s*,\\s*");

      int createNum = 1;
      int start = 0;

      if (isNumber(s[0])) {
         createNum = Integer.parseInt(s[0]);
         if (createNum <= 0) createNum = 1;
         start = 1;
      }

      for (int i = start; i < s.length; i++) {
         String name = s[i].toLowerCase();
         int meta = 0;

         if (i + 1 < s.length && isNumber(s[i + 1])) {
            meta = Integer.parseInt(s[++i]);
         }

         if (OreDictionary.doesOreNameExist(name)) {
            rcp.add(name);
         } else {
            rcp.add(new ItemStack(W_Item.getItemByName(name), 1, meta));
         }
      }

      IRecipe recipe = new ShapelessOreRecipe(
              new ItemStack(item, createNum),
              rcp.toArray()
      );

      GameRegistry.addRecipe(recipe);
      return recipe;
   }


   //public static ShapelessRecipes getShapelessRecipe(ItemStack par1ItemStack, Object ... par2ArrayOfObj) {
   //   ArrayList arraylist = new ArrayList();
   //   Object[] aobject = par2ArrayOfObj;
   //   int i = par2ArrayOfObj.length;
//
   //   for(int j = 0; j < i; ++j) {
   //      Object object1 = aobject[j];
   //      if(object1 instanceof ItemStack) {
   //         arraylist.add(((ItemStack)object1).copy());
   //      } else if(object1 instanceof Item) {
   //         arraylist.add(new ItemStack((Item)object1));
   //      } else {
   //         if(!(object1 instanceof Block)) {
   //            throw new RuntimeException("Invalid shapeless recipe!");
   //         }
//
   //         arraylist.add(new ItemStack((Block)object1));
   //      }
   //   }
//
   //   return new ShapelessRecipes(par1ItemStack, arraylist);
   //}

   public static boolean isNumber(String s) {
      if(s != null && !s.isEmpty()) {
         byte[] buf = s.getBytes();
         byte[] arr$ = buf;
         int len$ = buf.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            byte b = arr$[i$];
            if(b < 48 || b > 57) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

}
