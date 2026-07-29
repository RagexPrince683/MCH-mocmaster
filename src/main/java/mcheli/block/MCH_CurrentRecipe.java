package mcheli.block;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import mcheli.MCH_Lib;
import mcheli.MCH_IRecipeList;
import mcheli.MCH_MOD;
import mcheli.MCH_ModelManager;
import mcheli.MCH_ResourceHelper;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.aircraft.MCH_BaseVehicleInfoManager;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponInfoManager;
import mcheli.wrapper.modelloader.W_ModelCustom;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

import mcheli.block.MCH_DraftingTableGui.FilteredRecipeList;

public class MCH_CurrentRecipe {

   public final IRecipe recipe;
   public final int index;
   public final String displayName;
   public final List<ResourceLocation> descTexture;
   private static final Set<String> WARNED_DESC_TEXTURES = new HashSet<String>();
   private final MCH_BaseVehicleInfo acInfo;
   public List infoItem;
   public List infoData;
   private int descMaxPage;
   private int descPage;
   private W_ModelCustom model;
   public int modelRot;
   private ResourceLocation modelTexture;


   public MCH_CurrentRecipe(MCH_IRecipeList list, int idx) {
      if(list.getRecipeListSize() > 0) {
         this.recipe = list.getRecipe(idx);
      } else {
         this.recipe = null;
      }

      MCH_IRecipeList infoList = list;

      if(list instanceof FilteredRecipeList) {
         infoList = ((FilteredRecipeList)list).getBaseList();
      } //todo if not working move down under MCH_BaseVehicleInfo info = null;

      this.index = idx;
      this.displayName = this.recipe != null?this.recipe.getRecipeOutput().getDisplayName():"None";
      this.descTexture = this.getDescTexture(this.recipe);
      this.descPage = 0;
      this.descMaxPage = this.descTexture.size();
      MCH_BaseVehicleInfo info = null;
      if(infoList instanceof MCH_BaseVehicleInfoManager) {
         info = ((MCH_BaseVehicleInfoManager)infoList).getAcInfoFromItem(this.recipe);
         if(info != null) {
            ++this.descMaxPage;
            String dir = info.getDirectoryName();
            String name = info.name;
            this.model = MCH_ModelManager.get(dir, name);
            if(this.model != null) {
               try {
                  this.modelTexture = new ResourceLocation("mcheli",
                                                           "textures/" + dir + "/" + name + ".png");
               } catch (Exception var9) {
                  System.out.println("Texture not found : " + name);
                  this.modelTexture = new ResourceLocation("textures/blocks/planks_oak.png");
               }
               ++this.descMaxPage;
               if(infoList instanceof MCP_PlaneInfoManager) {
                  this.modelRot = 0;
               } else {
                  this.modelRot = 1;
               }
            }
         }
      }

      this.getAcInfoText(info);
      this.acInfo = info;
   }

   private void getAcInfoText(MCH_BaseVehicleInfo info) {
      this.infoItem = new ArrayList();
      this.infoData = new ArrayList();
      if(info != null) {
         this.getAcInfoTextSub("Name", info.getItemStack().getDisplayName());
         this.getAcInfoTextSub("HP", "" + info.maxHp);
         int seatNum = !info.isUAV?info.getNumSeat():info.getNumSeat() - 1;
         this.getAcInfoTextSub("Num of Seat", "" + seatNum);
         this.getAcInfoTextSub("GunnerMode", info.isEnableGunnerMode?"YES":"NO");
         this.getAcInfoTextSub("NightVision", info.isEnableNightVision?"YES":"NO");
         this.getAcInfoTextSub("Radar", info.isEnableEntityRadar?"YES":"NO");
         this.getAcInfoTextSub("Inventory", "" + info.inventorySize);
         if(info instanceof MCP_PlaneInfo) {
            MCP_PlaneInfo i = (MCP_PlaneInfo)info;
            this.getAcInfoTextSub("VTOL", i.isEnableVtol?"YES":"NO");
         }

         if(info.getWeaponNum() > 0) {
            this.getAcInfoTextSub("Armed----------------");

            for(int var6 = 0; var6 < info.getWeaponNum(); ++var6) {
               String type = info.getWeaponSetById(var6).type;
               MCH_WeaponInfo winfo = MCH_WeaponInfoManager.get(type);
               if(winfo != null) {
                  this.getAcInfoTextSub(winfo.getWeaponTypeName(), winfo.displayName);
               } else {
                  this.getAcInfoTextSub("ERROR", "Not found weapon " + (var6 + 1));
               }
            }
         }

      }
   }

   private void getAcInfoTextSub(String item, String data) {
      this.infoItem.add(item + " :");
      this.infoData.add(data);
   }

   private void getAcInfoTextSub(String item) {
      this.infoItem.add(item);
      this.infoData.add("");
   }

   public void switchNextPage() {
      if(this.descMaxPage >= 2) {
         this.descPage = (this.descPage + 1) % this.descMaxPage;
      } else {
         this.descPage = 0;
      }

   }

   public void switchPrevPage() {
      --this.descPage;
      if(this.descPage < 0 && this.descMaxPage >= 2) {
         this.descPage = this.descMaxPage - 1;
      } else {
         this.descPage = 0;
      }

   }

   public int getDescCurrentPage() {
      return this.descPage;
   }

   public void setDescCurrentPage(int page) {
      if(this.descMaxPage > 0) {
         this.descPage = page < this.descMaxPage?page:this.descMaxPage - 1;
      } else {
         this.descPage = 0;
      }

   }

   public int getDescMaxPage() {
      return this.descMaxPage;
   }

   public ResourceLocation getCurrentPageTexture() {
      return this.descPage < this.descTexture.size()?(ResourceLocation)this.descTexture.get(this.descPage):null;
   }

   public W_ModelCustom getModel() {
      return this.model;
   }

   public ResourceLocation getModelTexture() {
      return this.modelTexture;
   }

   public MCH_BaseVehicleInfo getAcInfo() {
      return this.acInfo;
   }

   public boolean isCurrentPageTexture() {
      return this.descPage >= 0 && this.descPage < this.descTexture.size();
   }

   public boolean isCurrentPageModel() {
      return this.getAcInfo() != null && this.getModel() != null && this.descPage == this.descTexture.size();
   }

   public boolean isCurrentPageAcInfo() {
      return this.getAcInfo() != null && this.descPage == this.descMaxPage - 1;
   }

   private List<ResourceLocation> getDescTexture(IRecipe r) {
      ArrayList<ResourceLocation> list = new ArrayList<ResourceLocation>();
      if(r != null) {
         for(int i = 0; i < 20; ++i) {
            String itemName = r.getRecipeOutput().getUnlocalizedName();
            if(itemName.startsWith("tile.")) {
               itemName = itemName.substring(5);
            }

            if(itemName.indexOf(":") >= 0) {
               itemName = itemName.substring(itemName.indexOf(":") + 1);
            }

            // ResourceLocation paths omit assets/mcheli and never begin with a slash.
            String texturePath = normalizeTexturePath("textures/drafting_table_desc/" + itemName + "#" + i + ".png");
            String assetPath = MCH_ResourceHelper.normalizeAssetPath("assets/mcheli/" + texturePath);
            if(MCH_ResourceHelper.resourceExists(assetPath)) {
               list.add(new ResourceLocation("mcheli", texturePath));
            } else {
               warnMissingDescTexture(r.getRecipeOutput().getDisplayName(), assetPath);
            }
         }
      }

      return list;
   }

   private static String normalizeTexturePath(String path) {
      path = path.replace('\\', '/');
      while(path.startsWith("/")) path = path.substring(1);
      while(path.contains("//")) path = path.replace("//", "/");
      return path;
   }

   private static void warnMissingDescTexture(String outputName, String resourcePath) {
      String warningKey = outputName + "|" + resourcePath;
      if(WARNED_DESC_TEXTURES.add(warningKey)) {
         MCH_Lib.Log("WARNING: Drafting table description texture unresolved for '%s': %s", outputName, resourcePath);
      }
   }
}
