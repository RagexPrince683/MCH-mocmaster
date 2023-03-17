package mcheli;

import net.minecraft.entity.Entity;

import java.util.HashMap;

public class MCH_DamageFactor {

   private HashMap map = new HashMap();


   public void clear() {
      this.map.clear();
   }

   public void add(Class c, float value) {
      this.map.put(c, Float.valueOf(value));
   }

   public float getDamageFactor(Class c) {
      return this.map.containsKey(c)?((Float)this.map.get(c)).floatValue():1.0F;
   }

   public float getDamageFactor(Entity e) {
      return e != null?this.getDamageFactor(e.getClass()):1.0F;
   }
}
