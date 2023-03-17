package mcheli;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

@SideOnly(Side.CLIENT)
public class MCH_RenderNull extends W_Render {

   public MCH_RenderNull() {
      super.shadowSize = 0.0F;
   }

   public void doRender(Entity entity, double posX, double posY, double posZ, float par8, float tickTime) {}

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }
}
