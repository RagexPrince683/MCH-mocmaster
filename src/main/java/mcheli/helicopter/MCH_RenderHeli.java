package mcheli.helicopter;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.*;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderHeli extends MCH_RenderAircraftLOD {

   @Override
   protected void renderAircraftLODLow(MCH_EntityAircraft ac, double posX, double posY, double posZ, float yaw, float pitch, float roll, float tickTime) {
      // Heli specific low LOD rendering logic
      MCH_HeliInfo heliInfo = null;
      MCH_EntityHeli heli = (MCH_EntityHeli)ac;
      heliInfo = heli.getHeliInfo();
      if(heliInfo != null) {
         this.renderDebugHitBox(heli, posX, posY, posZ, yaw, pitch);
         this.renderDebugPilotSeat(heli, posX, posY, posZ, yaw, pitch, roll);
         GL11.glTranslated(posX, posY, posZ);
         GL11.glRotatef(yaw, 0.0F, -1.0F, 0.0F);
         GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(roll, 0.0F, 0.0F, 1.0F);
         this.bindTexture("textures/helicopters/" + heli.getTextureName() + ".png", heli);
         renderBody(heliInfo.model);
         this.drawModelBlade(heli, heliInfo, tickTime);
      } else {
         this.bindTexture("textures/helicopters/" + heli.getTextureName() + ".png", heli);
         renderBody(heliInfo.model);
         this.drawModelBlade(heli, heliInfo, tickTime);
      }
      super.renderAircraftLODLow(ac, posX, posY, posZ, yaw, pitch, roll, tickTime);
   }

   @Override
   protected void renderAircraftLODMedium(MCH_EntityAircraft ac, double posX, double posY, double posZ, float yaw, float pitch, float roll, float tickTime) {
      // Heli specific medium LOD rendering logic
      MCH_HeliInfo heliInfo = null;
      MCH_EntityHeli heli = (MCH_EntityHeli)ac;
      heliInfo = heli.getHeliInfo();
      if(heliInfo != null) {
         this.renderDebugHitBox(heli, posX, posY, posZ, yaw, pitch);
         this.renderDebugPilotSeat(heli, posX, posY, posZ, yaw, pitch, roll);
         GL11.glTranslated(posX, posY, posZ);
         GL11.glRotatef(yaw, 0.0F, -1.0F, 0.0F);
         GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(roll, 0.0F, 0.0F, 1.0F);
         this.bindTexture("textures/helicopters/" + heli.getTextureName() + ".png", heli);
         renderBody(heliInfo.model);
         this.drawModelBlade(heli, heliInfo, tickTime);
      }
      super.renderAircraftLODMedium(ac, posX, posY, posZ, yaw, pitch, roll, tickTime);
   }

   //@Override
   //public void renderAircraft(MCH_EntityAircraft ac, double posX, double posY, double posZ, float yaw, float pitch, float roll, float tickTime) {
   //   // Heli specific high LOD rendering logic
   //   super.renderAircraft(ac, posX, posY, posZ, yaw, pitch, roll, tickTime);
   //}

   public MCH_RenderHeli() {
      super.shadowSize = 2.0F;
   }

   public void renderAircraft(MCH_EntityAircraft entity, double posX, double posY, double posZ, float yaw, float pitch, float roll, float tickTime) {
      MCH_HeliInfo heliInfo = null;
      if(entity != null && entity instanceof MCH_EntityHeli) {
         MCH_EntityHeli heli = (MCH_EntityHeli)entity;
         heliInfo = heli.getHeliInfo();
         if(heliInfo != null) {
            this.renderDebugHitBox(heli, posX, posY, posZ, yaw, pitch);
            this.renderDebugPilotSeat(heli, posX, posY, posZ, yaw, pitch, roll);
            GL11.glTranslated(posX, posY, posZ);
            GL11.glRotatef(yaw, 0.0F, -1.0F, 0.0F);
            GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(roll, 0.0F, 0.0F, 1.0F);
            this.bindTexture("textures/helicopters/" + heli.getTextureName() + ".png", heli);
            renderBody(heliInfo.model);
            this.drawModelBlade(heli, heliInfo, tickTime);
         } else {
            System.out.println("MCH_RenderHeli.renderAircraft: heliInfo is null but we render the thing anyway");
            this.renderAircraftLODMedium(entity, posX, posY, posZ, yaw, pitch, roll, tickTime);
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

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }
}
