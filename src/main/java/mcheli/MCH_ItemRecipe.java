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

   //called in postInit
   public static void registerItemRecipe() {
      registerCommonItemRecipe();

      //HELICOPTERS
      Iterator i$ = MCH_HeliInfoManager.map.keySet().iterator();
      String name;
      while(i$.hasNext()) {
         name = (String)i$.next();
         MCH_HeliInfo info = (MCH_HeliInfo)MCH_HeliInfoManager.map.get(name);
         addRecipeAndRegisterList(info, info.item, MCH_HeliInfoManager.getInstance());
      }

      //PLANES
      i$ = MCP_PlaneInfoManager.map.keySet().iterator();
      while(i$.hasNext()) {
         name = (String)i$.next();
         MCP_PlaneInfo info1 = (MCP_PlaneInfo)MCP_PlaneInfoManager.map.get(name);
         addRecipeAndRegisterList(info1, info1.item, MCP_PlaneInfoManager.getInstance());
      }


      //SHIPS
      i$ = MCH_ShipInfoManager.map.keySet().iterator();
      while(i$.hasNext()) {
         name = (String)i$.next();
         MCH_ShipInfo info1 = (MCH_ShipInfo)MCH_ShipInfoManager.map.get(name);
         addRecipeAndRegisterList(info1, info1.item, MCH_ShipInfoManager.getInstance());
      }


      //TANKS
      i$ = MCH_TankInfoManager.map.keySet().iterator();
      while(i$.hasNext()) {
         name = (String)i$.next();
         MCH_TankInfo info2 = (MCH_TankInfo)MCH_TankInfoManager.map.get(name);
         addRecipeAndRegisterList(info2, info2.item, MCH_TankInfoManager.getInstance());
      }


      //TURRETS
      i$ = MCH_VehicleInfoManager.map.keySet().iterator();
      while(i$.hasNext()) {
         name = (String)i$.next();
         MCH_VehicleInfo info3 = (MCH_VehicleInfo)MCH_VehicleInfoManager.map.get(name);
         addRecipeAndRegisterList(info3, info3.item, MCH_VehicleInfoManager.getInstance());
      }

      //THROWABLE ITEMS
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


      //CRAFT ITEMS - THESE HAVE ALREADY BEEN OREDICTED!
      MCH_ItemInfo info5;
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

   public static IRecipe addShapedRecipe(Item item, String data) {
      ArrayList<Object> rcp = new ArrayList<>();
      String[] s = data.split("\\s*,\\s*");
      if (s.length < 3) {
         return null;
      } else {
         int start = 0;
         int createNum = 1;
         if (isNumber(s[0])) {
            start = 1;
            createNum = Integer.valueOf(s[0]).intValue();
            if (createNum <= 0) createNum = 1;
         }

         int idx = start;

         // read 3 shape strings (or until we run out)
         for (int i = 0; i < 3 && idx < s.length; ++i) {
            if (s[idx].length() > 0 && s[idx].charAt(0) == 34 && s[idx].charAt(s[idx].length() - 1) == 34) {
               rcp.add(s[idx].subSequence(1, s[idx].length() - 1));
               ++idx;
            }
         }

         if (idx == 0) return null;

         boolean expectChar = true;
         boolean usesOre = false;

         for (; idx < s.length; ++idx) {
            if (s[idx].length() <= 0) return null;

            if (expectChar) {
               if (s[idx].length() != 1) return null;
               char recipeChar = s[idx].toUpperCase().charAt(0);
               if (recipeChar < 'A' || recipeChar > 'Z') return null;
               rcp.add(Character.valueOf(recipeChar));
            } else {
               String var12 = s[idx].trim().toLowerCase();
               int meta = 0;
               if (idx + 1 < s.length && isNumber(s[idx + 1])) {
                  ++idx;
                  meta = Integer.parseInt(s[idx]);
               }

               // Try to see if this ingredient matches an MCH_ItemInfo with ore dict names
               mcheli.item.MCH_ItemInfo info = mcheli.item.MCH_ItemInfoManager.get(var12);
               if (info == null) {
                  // try uppercase key fallback (some keys might be stored in different case)
                  info = mcheli.item.MCH_ItemInfoManager.get(s[idx - (meta > 0 ? 1 : 0)]);
               }

               if (info != null && info.oreDictNames != null && !info.oreDictNames.isEmpty()) {
                  // Use the first ore name (you can change selection logic if you want a different preference)
                  String oreName = info.oreDictNames.get(0);
                  rcp.add(oreName);
                  usesOre = true;
               } else {
                  // fallback to normal item lookup
                  Item found = W_Item.getItemByName(var12);
                  if (found == null) {
                     System.out.println("Error: addShapedRecipe could not find item by name: " + var12 + " (data: " + data + ")");
                     rcp.add(new ItemStack((Item) null)); // keep structure but will likely log error later
                  } else {
                     rcp.add(new ItemStack(found, 1, meta));
                  }
               }
            }

            expectChar = !expectChar;
         }

         Object[] objArr = rcp.toArray(new Object[0]);
         IRecipe created;

         if (usesOre) {
            // Use ShapedOreRecipe which understands ore-dict ingredient strings
            created = new ShapedOreRecipe(new ItemStack(item, createNum), objArr);
            GameRegistry.addRecipe(created);
         } else {
            // Legacy: add as shaped recipe and try to keep old behavior
            created = GameRegistry.addShapedRecipe(new ItemStack(item, createNum), objArr);
            // GameRegistry.addShapedRecipe returns a ShapedRecipes object in some Forge versions
         }

         return created;
      }
   }


   public static IRecipe addShapelessRecipe(Item item, String data) {
      ArrayList<Object> rcp = new ArrayList<>();
      String[] s = data.split("\\s*,\\s*");
      if (s.length < 1) return null;

      int idx = 0;
      int createNum = 1;
      if (isNumber(s[0])) {
         createNum = Integer.parseInt(s[0]);
         if (createNum <= 0) createNum = 1;
         idx = 1;
      }

      boolean usesOre = false;

      for (int i = idx; i < s.length; ++i) {
         if (s[i].length() <= 0) return null;
         String r = s[i].trim().toLowerCase();
         int meta = 0;
         if (i + 1 < s.length && isNumber(s[i + 1])) {
            ++i;
            meta = Integer.parseInt(s[i]);
         }

         if (isNumber(r)) {
            int numeric = Integer.parseInt(r);
            // keep previous numeric handling (blocks/items by id)
            if (numeric <= 255) {
               rcp.add(new ItemStack(W_Block.getBlockById(numeric), 1, meta));
            } else if (numeric <= 511) {
               rcp.add(new ItemStack(W_Item.getItemById(numeric), 1, meta));
            } else if (numeric <= 2255) {
               rcp.add(new ItemStack(W_Block.getBlockById(numeric), 1, meta));
            } else if (numeric <= 2267) {
               rcp.add(new ItemStack(W_Item.getItemById(numeric), 1, meta));
            } else if (numeric <= 4095) {
               rcp.add(new ItemStack(W_Block.getBlockById(numeric), 1, meta));
            } else if (numeric <= 31999) {
               rcp.add(new ItemStack(W_Item.getItemById(numeric + 256), 1, meta));
            }
         } else {
            // check if this name refers to one of our craft items that has oreDictNames
            mcheli.item.MCH_ItemInfo info = mcheli.item.MCH_ItemInfoManager.get(r);
            if (info == null) info = mcheli.item.MCH_ItemInfoManager.get(r.toUpperCase());
            if (info != null && info.oreDictNames != null && !info.oreDictNames.isEmpty()) {
               // use the first ore name
               rcp.add(info.oreDictNames.get(0));
               usesOre = true;
            } else {
               Item found = W_Item.getItemByName(r);
               if (found == null) {
                  System.out.println("Error: addShapelessRecipe could not find item: " + r + " (data: " + data + ")");
                  rcp.add(new ItemStack((Item) null));
               } else {
                  rcp.add(new ItemStack(found, 1, meta));
               }
            }
         }
      }

      Object[] inputs = rcp.toArray(new Object[0]);
      IRecipe created;

      if (usesOre) {
         // ShapelessOreRecipe accepts mixed ItemStack and ore-name inputs
         created = new ShapelessOreRecipe(new ItemStack(item, createNum), inputs);
         GameRegistry.addRecipe(created);
      } else {
         ShapelessRecipes shapeless = getShapelessRecipe(new ItemStack(item, createNum), inputs);
         // validate items
         for (int i = 0; i < shapeless.recipeItems.size(); ++i) {
            ItemStack is = (ItemStack) shapeless.recipeItems.get(i);
            if (is.getItem() == null) {
               System.out.println("Error: Invalid ShapelessRecipes! " + item + " : " + data);
            }
         }
         GameRegistry.addRecipe(shapeless);
         created = shapeless;
      }

      return created;
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
               throw new RuntimeException("Invalid shapeless recipe!");
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
