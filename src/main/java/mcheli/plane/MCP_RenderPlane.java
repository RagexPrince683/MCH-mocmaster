package mcheli.plane;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_RenderAircraft;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;

@SideOnly(Side.CLIENT)
public class MCP_RenderPlane extends MCH_RenderAircraft {

   public MCP_RenderPlane() {
      super.shadowSize = 2.0F;
   }

   public void renderAircraft(MCH_EntityAircraft entity, double posX, double posY, double posZ, float yaw, float pitch, float roll, float tickTime) {
      MCP_PlaneInfo planeInfo = null;
      
      if(entity != null && entity instanceof MCP_EntityPlane) {
         MCP_EntityPlane plane = (MCP_EntityPlane)entity;
         planeInfo = plane.getPlaneInfo();
         if(planeInfo != null) {
        	 
        	 
            this.renderDebugHitBox(plane, posX, posY, posZ, yaw, pitch);
            this.renderDebugPilotSeat(plane, posX, posY, posZ, yaw, pitch, roll);
            GL11.glTranslated(posX, posY, posZ);
            GL11.glRotatef(yaw, 0.0F, -1.0F, 0.0F);
            GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(roll, 0.0F, 0.0F, 1.0F);
            this.bindTexture("textures/planes/" + plane.getTextureName() + ".png", plane);
            if(planeInfo.haveNozzle() && plane.partNozzle != null) {
               this.renderNozzle(plane, planeInfo, tickTime);
            }

            if(planeInfo.haveWing() && plane.partWing != null) {
               this.renderWing(plane, planeInfo, tickTime);
            }

            if(planeInfo.haveRotor() && plane.partNozzle != null) {
               this.renderRotor(plane, planeInfo, tickTime);
            }
            //for(MCH_RadarContact contact : plane.contacts) {
            	//System.out.println("yeet yeet fucko");
            	//MCH_RenderAircraft.renderRadarMarker(contact.x, contact.y, contact.z, contact.width, contact.height);
            //}
            renderBody(planeInfo.model);
         }
      }
   }

   public void renderRotor(MCP_EntityPlane plane, MCP_PlaneInfo planeInfo, float tickTime) {
      float rot = plane.getNozzleRotation();
      float prevRot = plane.getPrevNozzleRotation();
      Iterator i$ = planeInfo.rotorList.iterator();

      while(i$.hasNext()) {
         MCP_PlaneInfo.Rotor r = (MCP_PlaneInfo.Rotor)i$.next();
         GL11.glPushMatrix();
         GL11.glTranslated(r.pos.xCoord, r.pos.yCoord, r.pos.zCoord);
         GL11.glRotatef((prevRot + (rot - prevRot) * tickTime) * r.maxRotFactor, (float)r.rot.xCoord, (float)r.rot.yCoord, (float)r.rot.zCoord);
         GL11.glTranslated(-r.pos.xCoord, -r.pos.yCoord, -r.pos.zCoord);
         renderPart(r.model, planeInfo.model, r.modelName);
         Iterator i$1 = r.blades.iterator();

         while(i$1.hasNext()) {
            MCP_PlaneInfo.Blade b = (MCP_PlaneInfo.Blade)i$1.next();
            float br = plane.prevRotationRotor;
            br += (plane.rotationRotor - plane.prevRotationRotor) * tickTime;
            GL11.glPushMatrix();
            GL11.glTranslated(b.pos.xCoord, b.pos.yCoord, b.pos.zCoord);
            GL11.glRotatef(br, (float)b.rot.xCoord, (float)b.rot.yCoord, (float)b.rot.zCoord);
            GL11.glTranslated(-b.pos.xCoord, -b.pos.yCoord, -b.pos.zCoord);

            for(int i = 0; i < b.numBlade; ++i) {
               GL11.glTranslated(b.pos.xCoord, b.pos.yCoord, b.pos.zCoord);
               GL11.glRotatef((float)b.rotBlade, (float)b.rot.xCoord, (float)b.rot.yCoord, (float)b.rot.zCoord);
               GL11.glTranslated(-b.pos.xCoord, -b.pos.yCoord, -b.pos.zCoord);
               renderPart(b.model, planeInfo.model, b.modelName);
            }

            GL11.glPopMatrix();
         }

         GL11.glPopMatrix();
      }

   }

   public void renderWing(MCP_EntityPlane plane, MCP_PlaneInfo planeInfo, float tickTime) {
      float rot = plane.getWingRotation();
      float prevRot = plane.getPrevWingRotation();

      for(Iterator i$ = planeInfo.wingList.iterator(); i$.hasNext(); GL11.glPopMatrix()) {
         MCP_PlaneInfo.Wing w = (MCP_PlaneInfo.Wing)i$.next();
         GL11.glPushMatrix();
         GL11.glTranslated(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord);
         GL11.glRotatef((prevRot + (rot - prevRot) * tickTime) * w.maxRotFactor, (float)w.rot.xCoord, (float)w.rot.yCoord, (float)w.rot.zCoord);
         GL11.glTranslated(-w.pos.xCoord, -w.pos.yCoord, -w.pos.zCoord);
         renderPart(w.model, planeInfo.model, w.modelName);
         if(w.pylonList != null) {
            Iterator i$1 = w.pylonList.iterator();

            while(i$1.hasNext()) {
               MCP_PlaneInfo.Pylon p = (MCP_PlaneInfo.Pylon)i$1.next();
               GL11.glPushMatrix();
               GL11.glTranslated(p.pos.xCoord, p.pos.yCoord, p.pos.zCoord);
               GL11.glRotatef((prevRot + (rot - prevRot) * tickTime) * p.maxRotFactor, (float)p.rot.xCoord, (float)p.rot.yCoord, (float)p.rot.zCoord);
               GL11.glTranslated(-p.pos.xCoord, -p.pos.yCoord, -p.pos.zCoord);
               renderPart(p.model, planeInfo.model, p.modelName);
               GL11.glPopMatrix();
            }
         }
      }

   }

   public void renderNozzle(MCP_EntityPlane plane, MCP_PlaneInfo planeInfo, float tickTime) {
      float rot = plane.getNozzleRotation();
      float prevRot = plane.getPrevNozzleRotation();
      Iterator i$ = planeInfo.nozzles.iterator();

      while(i$.hasNext()) {
         MCH_AircraftInfo.DrawnPart n = (MCH_AircraftInfo.DrawnPart)i$.next();
         GL11.glPushMatrix();
         GL11.glTranslated(n.pos.xCoord, n.pos.yCoord, n.pos.zCoord);
         GL11.glRotatef(prevRot + (rot - prevRot) * tickTime, (float)n.rot.xCoord, (float)n.rot.yCoord, (float)n.rot.zCoord);
         GL11.glTranslated(-n.pos.xCoord, -n.pos.yCoord, -n.pos.zCoord);
         renderPart(n.model, planeInfo.model, n.modelName);
         GL11.glPopMatrix();
      }

   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }
}
