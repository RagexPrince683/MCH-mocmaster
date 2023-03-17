package mcheli.wrapper.modelloader;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class W_TextureCoordinate {

   public float u;
   public float v;
   public float w;


   public W_TextureCoordinate(float u, float v) {
      this(u, v, 0.0F);
   }

   public W_TextureCoordinate(float u, float v, float w) {
      this.u = u;
      this.v = v;
      this.w = w;
   }
}
