package mcheli;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

//the item to render in our hand when in a vehicle,
// *when the player is invisible.(I don't think it even checks this, I think it just renders this regardless)
// Should probably be disabled when holding a Hand made gun item or Fl*nsmod gun item for driveby shooting compat
// but that could not be the only reason why items flicker when in a vehicle
public class MCH_InvisibleItemRender implements IItemRenderer {

   public boolean handleRenderType(ItemStack item, ItemRenderType type) {
      //TODO change
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
