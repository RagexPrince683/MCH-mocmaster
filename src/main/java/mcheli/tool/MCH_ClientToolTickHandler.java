package mcheli.tool;

import mcheli.MCH_ClientTickHandlerBase;
import mcheli.MCH_Config;
import mcheli.MCH_Key;
import mcheli.tool.rangefinder.MCH_ItemRangeFinder;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class MCH_ClientToolTickHandler extends MCH_ClientTickHandlerBase {

   public MCH_Key KeyUseItem;
   public MCH_Key KeyZoomIn;
   public MCH_Key KeyZoomOut;
   public MCH_Key KeySwitchMode;
   public MCH_Key[] Keys;


   public MCH_ClientToolTickHandler(Minecraft minecraft, MCH_Config config) {
      super(minecraft);
      this.updateKeybind(config);
   }

   public void updateKeybind(MCH_Config config) {
      this.KeyUseItem = new MCH_Key(MCH_Config.KeyAttack.prmInt);
      this.KeyZoomIn = new MCH_Key(MCH_Config.KeyZoom.prmInt);
      this.KeyZoomOut = new MCH_Key(MCH_Config.KeySwWeaponMode.prmInt);
      this.KeySwitchMode = new MCH_Key(MCH_Config.KeyFlare.prmInt);
      this.Keys = new MCH_Key[]{this.KeyUseItem, this.KeyZoomIn, this.KeyZoomOut, this.KeySwitchMode};
   }

   protected void onTick(boolean inGUI) {
      MCH_Key[] arr$ = this.Keys;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         MCH_Key k = arr$[i$];
         k.update();
      }

      this.onTick_ItemWrench(inGUI, super.mc.thePlayer);
      this.onTick_ItemRangeFinder(inGUI, super.mc.thePlayer);
   }

   private void onTick_ItemRangeFinder(boolean inGUI, EntityPlayer player) {
      if(MCH_ItemRangeFinder.rangeFinderUseCooldown > 0) {
         --MCH_ItemRangeFinder.rangeFinderUseCooldown;
      }

      ItemStack itemStack = null;
      if(player != null) {
         itemStack = super.mc.thePlayer.getCurrentEquippedItem();
         if(itemStack != null && itemStack.getItem() instanceof MCH_ItemRangeFinder) {
            boolean usingItem = player.getItemInUseDuration() > 8 && MCH_ItemRangeFinder.canUse(player);
            if(!MCH_ItemRangeFinder.continueUsingItem && usingItem) {
               MCH_ItemRangeFinder.onStartUseItem();
            }

            if(usingItem) {
               if(this.KeyUseItem.isKeyDown()) {
                  ((MCH_ItemRangeFinder)itemStack.getItem()).spotEntity(player, itemStack);
               }

               if(this.KeyZoomIn.isKeyPress() && MCH_ItemRangeFinder.zoom < 10.0F) {
                  MCH_ItemRangeFinder.zoom += MCH_ItemRangeFinder.zoom / 10.0F;
                  if(MCH_ItemRangeFinder.zoom > 10.0F) {
                     MCH_ItemRangeFinder.zoom = 10.0F;
                  }

                  W_McClient.MOD_playSoundFX("zoom", 0.05F, 1.0F);
                  W_Reflection.setCameraZoom(MCH_ItemRangeFinder.zoom);
               }

               if(this.KeyZoomOut.isKeyPress() && MCH_ItemRangeFinder.zoom > 1.2F) {
                  MCH_ItemRangeFinder.zoom -= MCH_ItemRangeFinder.zoom / 10.0F;
                  if(MCH_ItemRangeFinder.zoom < 1.2F) {
                     MCH_ItemRangeFinder.zoom = 1.2F;
                  }

                  W_McClient.MOD_playSoundFX("zoom", 0.05F, 0.9F);
                  W_Reflection.setCameraZoom(MCH_ItemRangeFinder.zoom);
               }

               if(this.KeySwitchMode.isKeyDown()) {
                  W_McClient.MOD_playSoundFX("lockon", 1.0F, 0.9F);
                  MCH_ItemRangeFinder.mode = (MCH_ItemRangeFinder.mode + 1) % 3;
                  if(super.mc.isSingleplayer() && MCH_ItemRangeFinder.mode == 0) {
                     MCH_ItemRangeFinder.mode = 1;
                  }
               }
            }
         }
      }

      if(MCH_ItemRangeFinder.continueUsingItem && (itemStack == null || !(itemStack.getItem() instanceof MCH_ItemRangeFinder))) {
         MCH_ItemRangeFinder.onStopUseItem();
      }

   }

   private void onTick_ItemWrench(boolean inGUI, EntityPlayer player) {
      if(player != null) {
         ItemStack itemStack = player.getCurrentEquippedItem();
         if(itemStack != null && itemStack.getItem() instanceof MCH_ItemWrench) {
            int maxdm = itemStack.getMaxDamage();
            int dm = itemStack.getItemDamage();
            if(dm <= maxdm) {
               ItemStack renderItemstack = W_Reflection.getItemRenderer_ItemToRender();
               if(renderItemstack == null || itemStack.getItem() == renderItemstack.getItem()) {
                  W_Reflection.setItemRenderer_ItemToRender(player.inventory.getCurrentItem());
               }
            }
         }

      }
   }
}
