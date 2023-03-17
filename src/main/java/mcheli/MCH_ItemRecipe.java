package mcheli;

import cpw.mods.fml.common.registry.GameRegistry;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_AircraftInfoManager;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.lweapon.MCH_ItemLightWeaponBullet;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_PlaneInfoManager;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

   public static IRecipe addShapedRecipe(Item item, String data) {
      ArrayList rcp = new ArrayList();
      String[] s = data.split("\\s*,\\s*");
      if(s.length < 3) {
         return null;
      } else {
         byte start = 0;
         int createNum = 1;
         if(isNumber(s[0])) {
            start = 1;
            createNum = Integer.valueOf(s[0]).intValue();
            if(createNum <= 0) {
               createNum = 1;
            }
         }

         int idx = start;

         for(int isChar = start; isChar < 3 + start; ++isChar) {
            if(s[idx].length() > 0 && s[idx].charAt(0) == 34 && s[idx].charAt(s[idx].length() - 1) == 34) {
               rcp.add(s[idx].subSequence(1, s[idx].length() - 1));
               ++idx;
            }
         }

         if(idx == 0) {
            return null;
         } else {
            int r;
            for(boolean var11 = true; idx < s.length; ++idx) {
               if(s[idx].length() <= 0) {
                  return null;
               }

               if(var11) {
                  if(s[idx].length() != 1) {
                     return null;
                  }

                  char recipe = s[idx].toUpperCase().charAt(0);
                  if(recipe < 65 || recipe > 90) {
                     return null;
                  }

                  rcp.add(Character.valueOf(recipe));
               } else {
                  String var12 = s[idx].trim().toLowerCase();
                  r = 0;
                  if(idx + 1 < s.length && isNumber(s[idx + 1])) {
                     ++idx;
                     r = Integer.parseInt(s[idx]);
                  }

                  if(isNumber(var12)) {
                     return null;
                  }

                  rcp.add(new ItemStack(W_Item.getItemByName(var12), 1, r));
               }

               var11 = !var11;
            }

            Object[] var13 = new Object[rcp.size()];

            for(r = 0; r < var13.length; ++r) {
               var13[r] = rcp.get(r);
            }

            ShapedRecipes var14 = (ShapedRecipes)GameRegistry.addShapedRecipe(new ItemStack(item, createNum), var13);

            for(int i = 0; i < var14.recipeItems.length; ++i) {
               if(var14.recipeItems[i] != null && var14.recipeItems[i].getItem() == null) {
                  throw new RuntimeException("Error: Invalid ShapedRecipes! " + item + " : " + data);
               }
            }

            return var14;
         }
      }
   }

   public static IRecipe addShapelessRecipe(Item item, String data) {
      ArrayList rcp = new ArrayList();
      String[] s = data.split("\\s*,\\s*");
      if(s.length < 1) {
         return null;
      } else {
         byte start = 0;
         byte createNum = 1;
         if(isNumber(s[0]) && createNum <= 0) {
            createNum = 1;
         }

         int i;
         for(int recipe = start; recipe < s.length; ++recipe) {
            if(s[recipe].length() <= 0) {
               return null;
            }

            String r = s[recipe].trim().toLowerCase();
            i = 0;
            if(recipe + 1 < s.length && isNumber(s[recipe + 1])) {
               ++recipe;
               i = Integer.parseInt(s[recipe]);
            }

            if(isNumber(r)) {
               int is = Integer.parseInt(r);
               if(is <= 255) {
                  rcp.add(new ItemStack(W_Block.getBlockById(is), 1, i));
               } else if(is <= 511) {
                  rcp.add(new ItemStack(W_Item.getItemById(is), 1, i));
               } else if(is <= 2255) {
                  rcp.add(new ItemStack(W_Block.getBlockById(is), 1, i));
               } else if(is <= 2267) {
                  rcp.add(new ItemStack(W_Item.getItemById(is), 1, i));
               } else if(is <= 4095) {
                  rcp.add(new ItemStack(W_Block.getBlockById(is), 1, i));
               } else if(is <= 31999) {
                  rcp.add(new ItemStack(W_Item.getItemById(is + 256), 1, i));
               }
            } else {
               rcp.add(new ItemStack(W_Item.getItemByName(r), 1, i));
            }
         }

         Object[] var10 = new Object[rcp.size()];

         for(int var11 = 0; var11 < var10.length; ++var11) {
            var10[var11] = rcp.get(var11);
         }

         ShapelessRecipes var12 = getShapelessRecipe(new ItemStack(item, createNum), var10);

         for(i = 0; i < var12.recipeItems.size(); ++i) {
            ItemStack var13 = (ItemStack)var12.recipeItems.get(i);
            if(var13.getItem() == null) {
               throw new RuntimeException("Error: Invalid ShapelessRecipes! " + item + " : " + data);
            }
         }

         GameRegistry.addRecipe(var12);
         return var12;
      }
   }

   public static ShapelessRecipes getShapelessRecipe(ItemStack par1ItemStack, Object ... par2ArrayOfObj) {
      ArrayList arraylist = new ArrayList();
      Object[] aobject = par2ArrayOfObj;
      int i = par2ArrayOfObj.length;

      for(int j = 0; j < i; ++j) {
         Object object1 = aobject[j];
         if(object1 instanceof ItemStack) {
            arraylist.add(((ItemStack)object1).copy());
         } else if(object1 instanceof Item) {
            arraylist.add(new ItemStack((Item)object1));
         } else {
            if(!(object1 instanceof Block)) {
               throw new RuntimeException("Invalid shapeless recipy!");
            }

            arraylist.add(new ItemStack((Block)object1));
         }
      }

      return new ShapelessRecipes(par1ItemStack, arraylist);
   }

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
