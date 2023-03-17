package mcheli.aircraft;

import mcheli.MCH_ModelManager;
import mcheli.wrapper.W_McClient;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public class MCH_ItemAircraftRender implements IItemRenderer {

   float size = 0.1F;
   float x = 0.1F;
   float y = 0.1F;
   float z = 0.1F;


   public boolean handleRenderType(ItemStack item, ItemRenderType type) {
      if(item != null && item.getItem() instanceof MCH_ItemAircraft) {
         MCH_AircraftInfo info = ((MCH_ItemAircraft)item.getItem()).getAircraftInfo();
         if(info == null) {
            return false;
         }

         if(info != null && info.name.equalsIgnoreCase("mh-60l_dap")) {
            return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON || type == ItemRenderType.ENTITY || type == ItemRenderType.INVENTORY;
         }
      }

      return false;
   }

   public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
      return type == ItemRenderType.ENTITY || type == ItemRenderType.INVENTORY;
   }

   public void renderItem(ItemRenderType type, ItemStack item, Object ... data) {
      boolean isRender = true;
      GL11.glPushMatrix();
      GL11.glEnable(2884);
      W_McClient.MOD_bindTexture("textures/helicopters/mh-60l_dap.png");
      switch(MCH_ItemAircraftRender.NamelessClass956700996.$SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[type.ordinal()]) {
      case 1:
         GL11.glEnable('\u803a');
         GL11.glEnable(2903);
         GL11.glScalef(0.1F, 0.1F, 0.1F);
         MCH_ModelManager.render("helicopters", "mh-60l_dap");
         GL11.glDisable('\u803a');
         break;
      case 2:
         GL11.glEnable('\u803a');
         GL11.glEnable(2903);
         GL11.glTranslatef(0.0F, 0.005F, -0.165F);
         GL11.glScalef(0.1F, 0.1F, 0.1F);
         GL11.glRotatef(-10.0F, 0.0F, 0.0F, 1.0F);
         GL11.glRotatef(90.0F, 0.0F, -1.0F, 0.0F);
         GL11.glRotatef(-50.0F, 1.0F, 0.0F, 0.0F);
         MCH_ModelManager.render("helicopters", "mh-60l_dap");
         GL11.glDisable('\u803a');
         break;
      case 3:
         GL11.glEnable('\u803a');
         GL11.glEnable(2903);
         GL11.glTranslatef(0.3F, 0.5F, -0.5F);
         GL11.glScalef(0.1F, 0.1F, 0.1F);
         GL11.glRotatef(10.0F, 0.0F, 0.0F, 1.0F);
         GL11.glRotatef(140.0F, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(-10.0F, 1.0F, 0.0F, 0.0F);
         MCH_ModelManager.render("helicopters", "mh-60l_dap");
         GL11.glDisable('\u803a');
         break;
      case 4:
         GL11.glTranslatef(this.x, this.y, this.z);
         GL11.glScalef(this.size, this.size, this.size);
         MCH_ModelManager.render("helicopters", "mh-60l_dap");
         break;
      case 5:
      default:
         isRender = false;
      }

      GL11.glPopMatrix();
   }

   // $FF: synthetic class
   static class NamelessClass956700996 {

      // $FF: synthetic field
      static final int[] $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType = new int[ItemRenderType.values().length];


      static {
         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.ENTITY.ordinal()] = 1;
         } catch (NoSuchFieldError var5) {
            ;
         }

         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.EQUIPPED.ordinal()] = 2;
         } catch (NoSuchFieldError var4) {
            ;
         }

         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.EQUIPPED_FIRST_PERSON.ordinal()] = 3;
         } catch (NoSuchFieldError var3) {
            ;
         }

         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.INVENTORY.ordinal()] = 4;
         } catch (NoSuchFieldError var2) {
            ;
         }

         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.FIRST_PERSON_MAP.ordinal()] = 5;
         } catch (NoSuchFieldError var1) {
            ;
         }

      }
   }
}
