package mcheli.debug;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.wrapper.W_ModelBase;
import net.minecraft.client.model.ModelRenderer;

@SideOnly(Side.CLIENT)
public class MCH_ModelTest extends W_ModelBase {

   public ModelRenderer test;


   public MCH_ModelTest() {
      boolean SIZE = true;
      this.test = new ModelRenderer(this, 0, 0);
      this.test.addBox(-5.0F, -5.0F, -5.0F, 10, 10, 10, 0.0F);
   }

   public void renderModel(double yaw, double pitch, float par7) {
      this.test.render(par7);
   }
}
