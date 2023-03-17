package mcheli;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

public class MCH_InvisibleItemRender implements IItemRenderer {

   public boolean handleRenderType(ItemStack item, ItemRenderType type) {
      return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON;
   }

   public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
      return false;
   }

   public boolean useCurrentWeapon() {
      return false;
   }

   public void renderItem(ItemRenderType type, ItemStack item, Object ... data) {}
}
