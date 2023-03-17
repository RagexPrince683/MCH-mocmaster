package mcheli.vehicle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Lib;
import mcheli.MCH_ModelManager;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_RenderAircraft;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;

@SideOnly(Side.CLIENT)
public class MCH_RenderVehicle extends MCH_RenderAircraft {

   public MCH_RenderVehicle() {
      super.shadowSize = 2.0F;
   }

   public void renderAircraft(MCH_EntityAircraft entity, double posX, double posY, double posZ, float yaw, float pitch, float roll, float tickTime) {
      MCH_VehicleInfo vehicleInfo = null;
      if(entity != null && entity instanceof MCH_EntityVehicle) {
         MCH_EntityVehicle vehicle = (MCH_EntityVehicle)entity;
         vehicleInfo = vehicle.getVehicleInfo();
         if(vehicleInfo != null) {
            if(vehicle.riddenByEntity != null && !vehicle.isDestroyed()) {
               vehicle.isUsedPlayer = true;
               vehicle.lastRiderYaw = vehicle.riddenByEntity.rotationYaw;
               vehicle.lastRiderPitch = vehicle.riddenByEntity.rotationPitch;
            } else if(!vehicle.isUsedPlayer) {
               vehicle.lastRiderYaw = vehicle.rotationYaw;
               vehicle.lastRiderPitch = vehicle.rotationPitch;
            }

            this.renderDebugHitBox(vehicle, posX, posY, posZ, yaw, pitch);
            this.renderDebugPilotSeat(vehicle, posX, posY, posZ, yaw, pitch, roll);
            GL11.glTranslated(posX, posY, posZ);
            GL11.glRotatef(yaw, 0.0F, -1.0F, 0.0F);
            GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
            this.bindTexture("textures/vehicles/" + vehicle.getTextureName() + ".png", vehicle);
            renderBody(vehicleInfo.model);
            MCH_WeaponSet ws = vehicle.getFirstSeatWeapon();
            this.drawPart(vehicle, vehicleInfo, yaw, pitch, ws, tickTime);
         }
      }
   }

   public void drawPart(MCH_EntityVehicle vehicle, MCH_VehicleInfo info, float yaw, float pitch, MCH_WeaponSet ws, float tickTime) {
      float rotBrl = ws.prevRotBarrel + (ws.rotBarrel - ws.prevRotBarrel) * tickTime;
      int index = 0;

      MCH_VehicleInfo.VPart vp;
      for(Iterator i$ = info.partList.iterator(); i$.hasNext(); 
    		  index = this.drawPart(vp, vehicle, info, yaw, pitch, rotBrl, tickTime, ws, index)) {
         vp = (MCH_VehicleInfo.VPart)i$.next();
      }

   }

   int drawPart(MCH_VehicleInfo.VPart vp, MCH_EntityVehicle vehicle, MCH_VehicleInfo info, float yaw, float pitch, float rotBrl, float tickTime, MCH_WeaponSet ws, int index) {
      GL11.glPushMatrix();
      float recoilBuf = 0.0F;
      if(index < ws.getWeaponNum()) {
         MCH_WeaponSet.Recoil bkIndex = ws.recoilBuf[index];
         recoilBuf = bkIndex.prevRecoilBuf + (bkIndex.recoilBuf - bkIndex.prevRecoilBuf) * tickTime;
      }

      if(vp.rotPitch || vp.rotYaw || vp.type == 1) {
         GL11.glTranslated(vp.pos.xCoord, vp.pos.yCoord, vp.pos.zCoord);
         if(vp.rotYaw) {
            GL11.glRotatef(-vehicle.lastRiderYaw + yaw, 0.0F, 1.0F, 0.0F);
         }

         if(vp.rotPitch) {
            float i$ = MCH_Lib.RNG(vehicle.lastRiderPitch, info.minRotationPitch, info.maxRotationPitch);
            GL11.glRotatef(i$ - pitch, 1.0F, 0.0F, 0.0F);
         }

         if(vp.type == 1) {
            GL11.glRotatef(rotBrl, 0.0F, 0.0F, -1.0F);
         }

         GL11.glTranslated(-vp.pos.xCoord, -vp.pos.yCoord, -vp.pos.zCoord);
      }

      if(vp.type == 2) {
         GL11.glTranslated(0.0D, 0.0D, (double)(-vp.recoilBuf * recoilBuf));
      }

      if(vp.type == 2 || vp.type == 3) {
         ++index;
      }

      MCH_VehicleInfo.VPart vcp;
      if(vp.child != null) {
         for(Iterator var14 = vp.child.iterator(); var14.hasNext(); index = this.drawPart(vcp, vehicle, info, yaw, pitch, rotBrl, recoilBuf, ws, index)) {
            vcp = (MCH_VehicleInfo.VPart)var14.next();
         }
      }

      if((vp.drawFP || !W_Lib.isClientPlayer(vehicle.riddenByEntity) || !W_Lib.isFirstPerson()) && (vp.type != 3 || !vehicle.isWeaponNotCooldown(ws, index))) {
         renderPart(vp.model, info.model, vp.modelName);
         MCH_ModelManager.render("vehicles", vp.modelName);
      }

      GL11.glPopMatrix();
      return index;
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }
}
