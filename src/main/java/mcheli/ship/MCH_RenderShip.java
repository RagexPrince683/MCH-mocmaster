package mcheli.ship;

import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_RenderAircraft;
import mcheli.aircraft.MCH_RenderAircraftLOD;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;

public class MCH_RenderShip extends MCH_RenderAircraftLOD {

    @Override
    protected void renderAircraftLODLow(MCH_EntityAircraft ac, double posX, double posY, double posZ, float yaw, float pitch, float roll, float tickTime) {
        // Heli specific low LOD rendering logic
        super.renderAircraftLODLow(ac, posX, posY, posZ, yaw, pitch, roll, tickTime);
    }

    @Override
    protected void renderAircraftLODMedium(MCH_EntityAircraft ac, double posX, double posY, double posZ, float yaw, float pitch, float roll, float tickTime) {
        // Ship specific medium LOD rendering logic
        super.renderAircraftLODMedium(ac, posX, posY, posZ, yaw, pitch, roll, tickTime);
    }

    public MCH_RenderShip() {
        super.shadowSize = 2.0F;
    }

    public void renderAircraft(MCH_EntityAircraft entity, double posX, double posY, double posZ, float yaw, float pitch, float roll, float tickTime) {
        MCH_ShipInfo shipInfo = null;
        if(entity != null && entity instanceof MCH_EntityShip) {
            MCH_EntityShip ship = (MCH_EntityShip)entity;
            shipInfo = ship.getShipInfo();
            if(shipInfo != null) {
                this.renderDebugHitBox(ship, posX, posY, posZ, yaw, pitch);
                this.renderDebugPilotSeat(ship, posX, posY, posZ, yaw, pitch, roll);
                GL11.glTranslated(posX, posY, posZ);
                GL11.glRotatef(yaw, 0.0F, -1.0F, 0.0F);
                GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(roll, 0.0F, 0.0F, 1.0F);
                this.bindTexture("textures/ships/" + ship.getTextureName() + ".png", ship);
                if(shipInfo.haveNozzle() && ship.partNozzle != null) {
                    this.renderNozzle(ship, shipInfo, tickTime);
                }

               // if(planeInfo.haveWing() && plane.partWing != null) {
               //     this.renderWing(plane, planeInfo, tickTime);
               // }

                if(shipInfo.haveRotor() && ship.partNozzle != null) {
                    this.renderRotor(ship, shipInfo, tickTime);
                }

                renderBody(shipInfo.model);
            }
        }
    }

    public void renderRotor(MCH_EntityShip ship, MCH_ShipInfo shipInfo, float tickTime) {
        float rot = ship.getNozzleRotation();
        float prevRot = ship.getPrevNozzleRotation();
        Iterator i$ = shipInfo.rotorList.iterator();

        while(i$.hasNext()) {
            MCH_ShipInfo.Rotor r = (MCH_ShipInfo.Rotor)i$.next();
            GL11.glPushMatrix();
            GL11.glTranslated(r.pos.xCoord, r.pos.yCoord, r.pos.zCoord);
            GL11.glRotatef((prevRot + (rot - prevRot) * tickTime) * r.maxRotFactor, (float)r.rot.xCoord, (float)r.rot.yCoord, (float)r.rot.zCoord);
            GL11.glTranslated(-r.pos.xCoord, -r.pos.yCoord, -r.pos.zCoord);
            renderPart(r.model, shipInfo.model, r.modelName);
            Iterator i$1 = r.blades.iterator();

            while(i$1.hasNext()) {
                MCH_ShipInfo.Blade b = (MCH_ShipInfo.Blade)i$1.next();
                float br = ship.prevRotationRotor;
                br += (ship.rotationRotor - ship.prevRotationRotor) * tickTime;
                GL11.glPushMatrix();
                GL11.glTranslated(b.pos.xCoord, b.pos.yCoord, b.pos.zCoord);
                GL11.glRotatef(br, (float)b.rot.xCoord, (float)b.rot.yCoord, (float)b.rot.zCoord);
                GL11.glTranslated(-b.pos.xCoord, -b.pos.yCoord, -b.pos.zCoord);

                for(int i = 0; i < b.numBlade; ++i) {
                    GL11.glTranslated(b.pos.xCoord, b.pos.yCoord, b.pos.zCoord);
                    GL11.glRotatef((float)b.rotBlade, (float)b.rot.xCoord, (float)b.rot.yCoord, (float)b.rot.zCoord);
                    GL11.glTranslated(-b.pos.xCoord, -b.pos.yCoord, -b.pos.zCoord);
                    renderPart(b.model, shipInfo.model, b.modelName);
                }

                GL11.glPopMatrix();
            }

            GL11.glPopMatrix();
        }

    }

    public void renderNozzle(MCH_EntityShip ship, MCH_ShipInfo shipInfo, float tickTime) {
        float rot = ship.getNozzleRotation();
        float prevRot = ship.getPrevNozzleRotation();
        Iterator i$ = shipInfo.nozzles.iterator();

        while(i$.hasNext()) {
            MCH_AircraftInfo.DrawnPart n = (MCH_AircraftInfo.DrawnPart)i$.next();
            GL11.glPushMatrix();
            GL11.glTranslated(n.pos.xCoord, n.pos.yCoord, n.pos.zCoord);
            GL11.glRotatef(prevRot + (rot - prevRot) * tickTime, (float)n.rot.xCoord, (float)n.rot.yCoord, (float)n.rot.zCoord);
            GL11.glTranslated(-n.pos.xCoord, -n.pos.yCoord, -n.pos.zCoord);
            renderPart(n.model, shipInfo.model, n.modelName);
            GL11.glPopMatrix();
        }

    }

    protected ResourceLocation getEntityTexture(Entity entity) {
        return W_Render.TEX_DEFAULT;
    }



}
