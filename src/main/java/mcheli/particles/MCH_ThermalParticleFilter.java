package mcheli.particles;

import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import mcheli.MCH_Lib;
import mcheli.MCH_ThermalVision;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntitySmokeFX;

@SideOnly(Side.CLIENT)
public final class MCH_ThermalParticleFilter {

   private static final Map<EntityFX, Float> hiddenSmokeAlpha = new IdentityHashMap<EntityFX, Float>();
   private static boolean reflectionWarningLogged;

   private MCH_ThermalParticleFilter() {}

   /** The single smoke classification used for both MCHeli and vanilla particles. */
   public static boolean isSmokeParticle(EntityFX particle) {
      return particle instanceof MCH_ISmokeParticle || particle instanceof EntitySmokeFX;
   }

   /** Countermeasure smoke remains a cold, opaque smoke visual in thermal vision. */
   public static boolean shouldHideSmokeParticle(EntityFX particle) {
      return isSmokeParticle(particle) && (!(particle instanceof MCH_ISmokeParticle)
         || !((MCH_ISmokeParticle)particle).isVisibleInThermal());
   }

   public static void beginRender() {
      restoreSmokeAlpha();
      if(!MCH_ThermalVision.isActiveCameraThermal()) {
         return;
      }

      Minecraft mc = Minecraft.getMinecraft();
      if(mc.effectRenderer == null) {
         return;
      }

      try {
         List[] layers = (List[])ObfuscationReflectionHelper.getPrivateValue(EffectRenderer.class,
            mc.effectRenderer, new String[]{"field_78876_b", "fxLayers"});
         for(int layerIndex = 0; layerIndex < layers.length; ++layerIndex) {
            List layer = layers[layerIndex];
            Iterator iterator = layer.iterator();
            while(iterator.hasNext()) {
               Object value = iterator.next();
               if(value instanceof EntityFX && shouldHideSmokeParticle((EntityFX)value)) {
                  EntityFX particle = (EntityFX)value;
                  Float alpha = (Float)ObfuscationReflectionHelper.getPrivateValue(EntityFX.class,
                     particle, new String[]{"field_82339_as", "particleAlpha"});
                  hiddenSmokeAlpha.put(particle, alpha);
                  particle.setAlphaF(0.0F);
               }
            }
         }
      } catch(RuntimeException e) {
         restoreSmokeAlpha();
         if(!reflectionWarningLogged) {
            reflectionWarningLogged = true;
            MCH_Lib.Log("Thermal smoke render filter could not inspect particle layers: %s", e.toString());
         }
      }
   }

   public static void endRender() {
      restoreSmokeAlpha();
   }

   private static void restoreSmokeAlpha() {
      Iterator<Map.Entry<EntityFX, Float>> iterator = hiddenSmokeAlpha.entrySet().iterator();
      while(iterator.hasNext()) {
         Map.Entry<EntityFX, Float> entry = iterator.next();
         EntityFX particle = entry.getKey();
         particle.setAlphaF(entry.getValue().floatValue());
      }
      hiddenSmokeAlpha.clear();
   }
}
