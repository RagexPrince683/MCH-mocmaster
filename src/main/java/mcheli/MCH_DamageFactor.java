package mcheli;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_DamageFactor {

   private HashMap map = new HashMap();


   public void clear() {
      this.map.clear();
   }

   public void add(Class c, float value) {
      this.map.put(c, Float.valueOf(value));
   }

   public float getDamageFactor(Class c) {
      if(c == null) {
         return 1.0F;
      }

      Float exact = (Float)this.map.get(c);
      if(exact != null) {
         return exact.floatValue();
      }

      Class best = null;
      Float bestValue = null;
      Iterator i$ = this.map.entrySet().iterator();

      while(i$.hasNext()) {
         Map.Entry entry = (Map.Entry)i$.next();
         Class configured = (Class)entry.getKey();
         if(configured != EntityPlayer.class && configured != EntityLivingBase.class && configured.isAssignableFrom(c)
               && (best == null || best.isAssignableFrom(configured))) {
            best = configured;
            bestValue = (Float)entry.getValue();
         }
      }

      if(bestValue != null) {
         return bestValue.floatValue();
      }

      if(EntityPlayer.class.isAssignableFrom(c) || EntityVillager.class.isAssignableFrom(c)) {
         Float player = (Float)this.map.get(EntityPlayer.class);
         return player != null?player.floatValue():1.0F;
      }

      if(EntityLivingBase.class.isAssignableFrom(c)) {
         Float other = (Float)this.map.get(EntityLivingBase.class);
         if(other != null) {
            return other.floatValue();
         }

         Float player = (Float)this.map.get(EntityPlayer.class);
         return player != null?player.floatValue():1.0F;
      }

      return 1.0F;
   }

   public float getDamageFactor(Entity e) {
      return e != null?this.getDamageFactor(e.getClass()):1.0F;
   }
}
