package mcheli.lweapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.MCH_ModelManager;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_McClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public class MCH_ItemLightWeaponRender implements IItemRenderer {

   public boolean handleRenderType(ItemStack item, ItemRenderType type) {
      return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON;
   }

   public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
      return false;
   }

   public boolean useCurrentWeapon() {
      return false;
   }

   public void renderItem(ItemRenderType type, ItemStack item, Object ... data) {
      boolean isRender = false;
      if(type == ItemRenderType.EQUIPPED_FIRST_PERSON || type == ItemRenderType.EQUIPPED) {
         isRender = true;
         if(data[1] instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)data[1];
            if(MCH_ItemLightWeaponBase.isHeld(player) && W_Lib.isFirstPerson() && W_Lib.isClientPlayer(player)) {
               isRender = false;
            }
         }
      }

      if(isRender) {
         renderItem(item, W_Lib.castEntityLivingBase((Entity)data[1]), type == ItemRenderType.EQUIPPED_FIRST_PERSON);
      }

   }

   @SideOnly(Side.CLIENT)
   public static void renderItem(ItemStack pitem, Entity entity, boolean isFirstPerson) {
      if(pitem != null && pitem.getItem() != null) {
         String name = MCH_ItemLightWeaponBase.getName(pitem);
         GL11.glEnable('\u803a');
         GL11.glEnable(2903);
         GL11.glPushMatrix();
         MCH_Config var10000 = MCH_MOD.config;
         if(MCH_Config.SmoothShading.prmBool) {
            GL11.glShadeModel(7425);
         }

         GL11.glEnable(2884);
         W_McClient.MOD_bindTexture("textures/lweapon/" + name + ".png");
         if(isFirstPerson) {
            GL11.glTranslatef(0.0F, 0.005F, -0.165F);
            GL11.glScalef(2.0F, 2.0F, 2.0F);
            GL11.glRotatef(-10.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-50.0F, 1.0F, 0.0F, 0.0F);
         } else {
            GL11.glTranslatef(0.3F, 0.3F, 0.0F);
            GL11.glScalef(2.0F, 2.0F, 2.0F);
            GL11.glRotatef(20.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(10.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(15.0F, 1.0F, 0.0F, 0.0F);
         }

         MCH_ModelManager.render("lweapons", name);
         GL11.glShadeModel(7424);
         GL11.glPopMatrix();
         GL11.glDisable('\u803a');
      }
   }
}
