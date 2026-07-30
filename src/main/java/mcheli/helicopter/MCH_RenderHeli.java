package mcheli.helicopter;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.MCH_Blade;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_RenderBaseVehicle;
import mcheli.aircraft.MCH_Rotor;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderHeli extends MCH_RenderBaseVehicle {

   public MCH_RenderHeli() {
      super.shadowSize = 2.0F;
   }

   public void renderBaseVehicle(MCH_EntityBaseVehicle entity, double posX, double posY, double posZ, float yaw, float pitch, float roll, float tickTime) {
      MCH_HeliInfo heliInfo = null;
      if(entity != null && entity instanceof MCH_EntityHeli) {
         MCH_EntityHeli heli = (MCH_EntityHeli)entity;
         heliInfo = heli.getHeliInfo();
         if(heliInfo != null) {
            this.renderDebugHitBox(heli, posX, posY, posZ, yaw, pitch, roll);
            this.renderDebugPilotSeat(heli, posX, posY, posZ, yaw, pitch, roll);
            GL11.glTranslated(posX, posY, posZ);
            GL11.glRotatef(yaw, 0.0F, -1.0F, 0.0F);
            GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(roll, 0.0F, 0.0F, 1.0F);
            this.bindTexture(MCH_EntityBaseVehicle.getTexturePath("helicopters", heli.getTextureName()), heli);
            renderBodyWithSkinOverlay(heliInfo.model, "helicopters", heli);
            this.drawModelBlade(heli, heliInfo, tickTime);
         }
      }
   }

   public void drawModelBlade(MCH_EntityHeli heli, MCH_HeliInfo info, float tickTime) {
      for(int i = 0; i < heli.rotors.length && i < info.rotorList.size(); ++i) {
         MCH_HeliInfo.Rotor rotorInfo = (MCH_HeliInfo.Rotor)info.rotorList.get(i);
         MCH_Rotor rotor = heli.rotors[i];
         GL11.glPushMatrix();
         if(rotorInfo.oldRenderMethod) {
            GL11.glTranslated(rotorInfo.pos.xCoord, rotorInfo.pos.yCoord, rotorInfo.pos.zCoord);
         }

         MCH_Blade[] arr$ = rotor.blades;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_Blade b = arr$[i$];
            GL11.glPushMatrix();
            float rot = b.getRotation();
            float prevRot = b.getPrevRotation();
            if(rot - prevRot < -180.0F) {
               prevRot -= 360.0F;
            } else if(prevRot - rot < -180.0F) {
               prevRot += 360.0F;
            }

            if(!rotorInfo.oldRenderMethod) {
               GL11.glTranslated(rotorInfo.pos.xCoord, rotorInfo.pos.yCoord, rotorInfo.pos.zCoord);
            }

            GL11.glRotatef(prevRot + (rot - prevRot) * tickTime, (float)rotorInfo.rot.xCoord, (float)rotorInfo.rot.yCoord, (float)rotorInfo.rot.zCoord);
            if(!rotorInfo.oldRenderMethod) {
               GL11.glTranslated(-rotorInfo.pos.xCoord, -rotorInfo.pos.yCoord, -rotorInfo.pos.zCoord);
            }

            renderPart(rotorInfo.model, info.model, rotorInfo.modelName);
            GL11.glPopMatrix();
         }

         GL11.glPopMatrix();
      }

   }

   /** Snapshot-only equivalent which needs no entity or simulated rotor objects. */
   public static void drawSnapshotBlades(MCH_HeliInfo info, float phase, boolean folded) {
      for(int rotorIndex = 0; rotorIndex < info.rotorList.size(); ++rotorIndex) {
         MCH_HeliInfo.Rotor rotorInfo = (MCH_HeliInfo.Rotor)info.rotorList.get(rotorIndex);
         GL11.glPushMatrix();
         try {
            if(rotorInfo.oldRenderMethod) {
               GL11.glTranslated(rotorInfo.pos.xCoord, rotorInfo.pos.yCoord, rotorInfo.pos.zCoord);
            }
            for(int bladeIndex = 0; bladeIndex < rotorInfo.bladeNum; ++bladeIndex) {
               GL11.glPushMatrix();
               try {
                  float angle = phase + (float)(bladeIndex * rotorInfo.bladeRot);
                  if(folded && rotorInfo.haveFoldFunc) {
                     float foldAngle = (float)(5 + bladeIndex * 3);
                     angle = angle < 180.0F ? foldAngle : 360.0F - foldAngle;
                  }
                  if(!rotorInfo.oldRenderMethod) {
                     GL11.glTranslated(rotorInfo.pos.xCoord, rotorInfo.pos.yCoord, rotorInfo.pos.zCoord);
                  }
                  GL11.glRotatef(angle, (float)rotorInfo.rot.xCoord, (float)rotorInfo.rot.yCoord, (float)rotorInfo.rot.zCoord);
                  if(!rotorInfo.oldRenderMethod) {
                     GL11.glTranslated(-rotorInfo.pos.xCoord, -rotorInfo.pos.yCoord, -rotorInfo.pos.zCoord);
                  }
                  renderPart(rotorInfo.model, info.model, rotorInfo.modelName);
               } finally {
                  GL11.glPopMatrix();
               }
            }
         } finally {
            GL11.glPopMatrix();
         }
      }
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }
}
