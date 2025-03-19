package mcheli.ship;

import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_RenderAircraft;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;

public class MCH_RenderShip extends MCH_RenderAircraft {

    public MCH_RenderShip() {
        super.shadowSize = 2.0F;
    }

    public void renderAircraft(MCH_EntityAircraft entity, double posX, double posY, double posZ, float yaw, float pitch, float roll, float tickTime) {
        MCH_ShipInfo planeInfo = null;
        if(entity != null && entity instanceof MCH_EntityShip) {
            MCH_EntityShip plane = (MCH_EntityShip)entity;
            planeInfo = plane.getShipInfo();
            if(planeInfo != null) {
                this.renderDebugHitBox(plane, posX, posY, posZ, yaw, pitch);
                this.renderDebugPilotSeat(plane, posX, posY, posZ, yaw, pitch, roll);
                GL11.glTranslated(posX, posY, posZ);
                GL11.glRotatef(yaw, 0.0F, -1.0F, 0.0F);
                GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(roll, 0.0F, 0.0F, 1.0F);
                this.bindTexture("textures/ships/" + plane.getTextureName() + ".png", plane);
                if(planeInfo.haveNozzle() && plane.partNozzle != null) {
                    this.renderNozzle(plane, planeInfo, tickTime);
                }

               // if(planeInfo.haveWing() && plane.partWing != null) {
               //     this.renderWing(plane, planeInfo, tickTime);
               // }

                if(planeInfo.haveRotor() && plane.partNozzle != null) {
                    this.renderRotor(plane, planeInfo, tickTime);
                }

                renderBody(planeInfo.model);
            }
        }
    }

    public void renderRotor(MCH_EntityShip plane, MCH_ShipInfo planeInfo, float tickTime) {
        float rot = plane.getNozzleRotation();
        float prevRot = plane.getPrevNozzleRotation();
        Iterator i$ = planeInfo.rotorList.iterator();

        while(i$.hasNext()) {
            MCH_ShipInfo.Rotor r = (MCH_ShipInfo.Rotor)i$.next();
            GL11.glPushMatrix();
            GL11.glTranslated(r.pos.xCoord, r.pos.yCoord, r.pos.zCoord);
            GL11.glRotatef((prevRot + (rot - prevRot) * tickTime) * r.maxRotFactor, (float)r.rot.xCoord, (float)r.rot.yCoord, (float)r.rot.zCoord);
            GL11.glTranslated(-r.pos.xCoord, -r.pos.yCoord, -r.pos.zCoord);
            renderPart(r.model, planeInfo.model, r.modelName);
            Iterator i$1 = r.blades.iterator();

            while(i$1.hasNext()) {
                MCH_ShipInfo.Blade b = (MCH_ShipInfo.Blade)i$1.next();
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

    public void renderNozzle(MCH_EntityShip plane, MCH_ShipInfo planeInfo, float tickTime) {
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
