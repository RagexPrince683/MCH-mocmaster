package mcheli;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.gltd.MCH_EntityGLTD;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_EntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;

@SideOnly(Side.CLIENT)
public class MCH_ItemRendererDummy extends ItemRenderer {

   protected static Minecraft mc;
   protected static ItemRenderer backupItemRenderer;
   protected static MCH_ItemRendererDummy instance;


   public MCH_ItemRendererDummy(Minecraft par1Minecraft) {
      super(par1Minecraft);
      mc = par1Minecraft;
   }

   public void renderItemInFirstPerson(float par1) {
      if(mc.thePlayer == null) {
         super.renderItemInFirstPerson(par1);
      } else if(!(mc.thePlayer.ridingEntity instanceof MCH_EntityAircraft) && !(mc.thePlayer.ridingEntity instanceof MCH_EntityUavStation) && !(mc.thePlayer.ridingEntity instanceof MCH_EntityGLTD)) {
         super.renderItemInFirstPerson(par1);
      }

   }

   public static void enableDummyItemRenderer() {
      if(instance == null) {
         instance = new MCH_ItemRendererDummy(Minecraft.getMinecraft());
      }

      if(!(mc.entityRenderer.itemRenderer instanceof MCH_ItemRendererDummy)) {
         backupItemRenderer = mc.entityRenderer.itemRenderer;
      }

      W_EntityRenderer.setItemRenderer(mc, instance);
   }

   public static void disableDummyItemRenderer() {
      if(backupItemRenderer != null) {
         W_EntityRenderer.setItemRenderer(mc, backupItemRenderer);
      }

   }
}
