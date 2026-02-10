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

      while (i$.hasNext()) {
         String s = (String) i$.next();
         ++count;
         if (s.length() < 3) continue;

         try {
            // Split tokens same as addShapedRecipe does
            String[] parts = s.split("\\s*,\\s*");
            if (parts.length < 3) {
               System.out.println("[MCH Recipe] Bad recipe format for " + info.name + " : " + s);
               IRecipe recipe = addRecipe(item, s, info.isShapedRecipe);
               info.recipe.add(recipe);
               im.addRecipe(recipe, count, info.name, s);
               continue;
            }

            // Detect if the first token is a number (create count)
            int start = 0;
            if (isNumber(parts[0])) start = 1;

            // Copy shape rows (3 rows) as-is
            int idx = start;
            List<String> outParts = new ArrayList<String>();
            for (int r = 0; r < 3 && idx < parts.length; r++, idx++) {
               outParts.add(parts[idx]);
            }

            // Now process key -> ingredient pairs
            boolean expectKey = true;
            while (idx < parts.length) {
               String keyToken = parts[idx++]; // should be single letter
               outParts.add(keyToken); // keep the key

               if (idx >= parts.length) break;

               String nameToken = parts[idx++].trim();
               // see if next token is a numeric metadata count
               String metaToken = null;
               if (idx < parts.length && isNumber(parts[idx])) {
                  metaToken = parts[idx++];
               }

               // Normalize name (don't strip case - keep raw token for fallback)
               String nameLookup = nameToken;

               // Try to resolve to an Item or Block
               Item lookupItem = W_Item.getItemByName(nameLookup);
               Block lookupBlock = null;
               if (lookupItem == null) {
                  // try block lookup if wrapper exposes it
                  try {
                     // Many maps use block names rarely; fallback: try W_Block if available
                     lookupBlock = (Block) W_Block.class.getMethod("getBlockByName", String.class).invoke(null, nameLookup);
                  } catch (Exception ignored) {
                     // If W_Block.getBlockByName doesn't exist, ignore — most recipe tokens are items
                  }
               }

               String replacement = nameToken; // default: keep original token
               int meta = 0;
               if (metaToken != null) {
                  try { meta = Integer.parseInt(metaToken); } catch (NumberFormatException ignored) {}
               }

               // If we found an Item or Block, check OreDictionary for it
               ItemStack probe = null;
               if (lookupItem != null) {
                  probe = new ItemStack(lookupItem, 1, meta);
               } else if (lookupBlock != null) {
                  probe = new ItemStack(lookupBlock, 1, meta);
               }

               if (probe != null) {
                  int[] ids = net.minecraftforge.oredict.OreDictionary.getOreIDs(probe);
                  if (ids != null && ids.length > 0) {
                     String oreName = net.minecraftforge.oredict.OreDictionary.getOreName(ids[0]);
                     replacement = "oredict:" + oreName;
                     System.out.println("[MCH Vehicle Recipe] Auto-replaced " + nameToken + " -> " + replacement + " for " + info.name);
                  } else {
                     // no oredict: keep as original name token
                     replacement = nameToken;
                  }
               } else {
                  // probe failed to find a registered item/block — keep original token (may be e.g. numeric or pre-existing oredict)
                  replacement = nameToken;
               }

               // add replacement and optional meta back to outParts
               outParts.add(replacement);
               if (metaToken != null) outParts.add(metaToken);
               expectKey = true;
            }

            // Rebuild the data string
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < outParts.size(); i++) {
               if (i > 0) sb.append(", ");
               sb.append(outParts.get(i));
            }
            String newData = sb.toString();

            // Create recipe using the transformed string (which now contains any oredict:... where applicable)
            IRecipe recipe = addRecipe(item, newData, info.isShapedRecipe);

            // Store and register with manager
            info.recipe.add(recipe);
            im.addRecipe(recipe, count, info.name, newData);
            // Keep a log so we can verify vehicles are oredict-ready
            if (recipe instanceof net.minecraftforge.oredict.ShapedOreRecipe) {
               System.out.println("[MCH Vehicle Recipe] Registered Ore-aware recipe for " + info.name + " : " + newData);
            } else {
               System.out.println("[MCH Vehicle Recipe] Registered vanilla recipe for " + info.name + " : " + newData);
            }
         } catch (Throwable t) {
            System.out.println("[MCH Vehicle Recipe ERROR] Failed to register recipe for " + info.name + " : " + s);
            t.printStackTrace();
            // attempt to register original as fallback
            try {
               IRecipe recipe = addRecipe(item, s, info.isShapedRecipe);
               info.recipe.add(recipe);
               im.addRecipe(recipe, count, info.name, s);
            } catch (Throwable tt) {
               System.out.println("[MCH Vehicle Recipe ERROR] Fallback also failed for " + info.name + " : " + s);
               tt.printStackTrace();
            }
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
         if (row.startsWith("\"") && row.endsWith("\"")) {
            rcp.add(row.substring(1, row.length() - 1));
         } else {
            return null;
         }
      }

      boolean hasOreDict = false;

      // key -> ingredient pairs
      while (idx < s.length) {
         char key = s[idx++].toUpperCase().charAt(0);
         String name = s[idx++].trim().toLowerCase();

         int meta = 0;
         if (idx < s.length && isNumber(s[idx])) {
            meta = Integer.parseInt(s[idx++]);
         }

         rcp.add(key);

         Item resolved = W_Item.getItemByName(name);
         if (resolved == null) {
            //System.out.println("[MCH Recipe] Invalid item: " + name);
            //rcp.add(ItemStack.);
            //.EMPTY does not exist in forge 1.7.10 schizo ass ai
            continue;
         }

         ItemStack stack = new ItemStack(resolved, 1, meta);
         int[] oreIDs = net.minecraftforge.oredict.OreDictionary.getOreIDs(stack);

         if (oreIDs.length > 0) {
            String oreName = net.minecraftforge.oredict.OreDictionary.getOreName(oreIDs[0]);
            rcp.add(oreName);
            hasOreDict = true;
         } else {
            rcp.add(stack);
         }
      }

      Object[] recipeArgs = rcp.toArray();

      ItemStack output = new ItemStack(item, createNum);

      if (hasOreDict) {
         IRecipe recipe = new net.minecraftforge.oredict.ShapedOreRecipe(output, recipeArgs);
         GameRegistry.addRecipe(recipe);
         System.out.println("[MCH Recipe] Registered OREDICT recipe: " + data);
         return recipe;
      } else {
         ShapedRecipes recipe = (ShapedRecipes)GameRegistry.addShapedRecipe(output, recipeArgs);
         return recipe;
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
               System.out.println("Error: Invalid ShapelessRecipes! " + item + " : " + data);
               //throw new RuntimeException("Error: Invalid ShapelessRecipes! " + item + " : " + data);
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
