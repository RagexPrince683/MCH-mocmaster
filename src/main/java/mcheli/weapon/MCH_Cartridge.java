package mcheli.weapon;

import net.minecraftforge.client.model.IModelCustom;

public class MCH_Cartridge {

   public IModelCustom model;
   public final String name;
   public final float acceleration;
   public final float yaw;
   public final float pitch;
   public final float bound;
   public final float gravity;
   public final float scale;


   public MCH_Cartridge(String nm, float a, float y, float p, float b, float g, float s) {
      this.name = nm;
      this.acceleration = a;
      this.yaw = y;
      this.pitch = p;
      this.bound = b;
      this.gravity = g;
      this.scale = s;
   }
}
