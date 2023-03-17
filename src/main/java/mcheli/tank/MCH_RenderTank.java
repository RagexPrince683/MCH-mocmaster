package mcheli.tank;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_RenderAircraft;
import mcheli.wrapper.W_Render;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderTank extends MCH_RenderAircraft {

   public MCH_RenderTank() {
      super.shadowSize = 2.0F;
   }

   public void renderAircraft(MCH_EntityAircraft entity, double posX, double posY, double posZ, float yaw, float pitch, float roll, float tickTime) {
      MCH_TankInfo tankInfo = null;
      if(entity != null && entity instanceof MCH_EntityTank) {
         MCH_EntityTank tank = (MCH_EntityTank)entity;
         tankInfo = tank.getTankInfo();
         if(tankInfo != null) {
            this.renderWheel(tank, posX, posY, posZ);
            this.renderDebugHitBox(tank, posX, posY, posZ, yaw, pitch);
            this.renderDebugPilotSeat(tank, posX, posY, posZ, yaw, pitch, roll);
            GL11.glTranslated(posX, posY, posZ);
            GL11.glRotatef(yaw, 0.0F, -1.0F, 0.0F);
            GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(roll, 0.0F, 0.0F, 1.0F);
            this.bindTexture("textures/tanks/" + tank.getTextureName() + ".png", tank);
            renderBody(tankInfo.model);
         }
      }
   }

   public void renderWheel(MCH_EntityTank tank, double posX, double posY, double posZ) {
      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.TestMode.prmBool) {
         if(MCH_RenderAircraft.debugModel != null) {
            GL11.glColor4f(0.75F, 0.75F, 0.75F, 0.5F);
            MCH_EntityWheel[] tessellator = tank.WheelMng.wheels;
            int wp = tessellator.length;

            int i;
            MCH_EntityWheel w1;
            for(i = 0; i < wp; ++i) {
               w1 = tessellator[i];
               GL11.glPushMatrix();
               GL11.glTranslated(w1.posX - tank.posX + posX, w1.posY - tank.posY + posY + 0.25D, w1.posZ - tank.posZ + posZ);
               GL11.glScalef(w1.width, w1.height / 2.0F, w1.width);
               this.bindTexture("textures/seat_pilot.png");
               MCH_RenderAircraft.debugModel.renderAll();
               GL11.glPopMatrix();
            }

            GL11.glColor4f(0.75F, 0.75F, 0.75F, 1.0F);
            Tessellator var13 = Tessellator.instance;
            var13.startDrawing(1);
            Vec3 var14 = tank.getTransformedPosition(tank.WheelMng.weightedCenter);
            var14.xCoord -= tank.posX;
            var14.yCoord -= tank.posY;
            var14.zCoord -= tank.posZ;

            for(i = 0; i < tank.WheelMng.wheels.length / 2; ++i) {
               var13.setColorRGBA_I(((i & 4) > 0?16711680:0) | ((i & 2) > 0?'\uff00':0) | ((i & 1) > 0?255:0), 192);
               w1 = tank.WheelMng.wheels[i * 2 + 0];
               MCH_EntityWheel w2 = tank.WheelMng.wheels[i * 2 + 1];
               if(w1.isPlus) {
                  var13.addVertex(w2.posX - tank.posX + posX, w2.posY - tank.posY + posY, w2.posZ - tank.posZ + posZ);
                  var13.addVertex(w1.posX - tank.posX + posX, w1.posY - tank.posY + posY, w1.posZ - tank.posZ + posZ);
                  var13.addVertex(w1.posX - tank.posX + posX, w1.posY - tank.posY + posY, w1.posZ - tank.posZ + posZ);
                  var13.addVertex(posX + var14.xCoord, posY + var14.yCoord, posZ + var14.zCoord);
                  var13.addVertex(posX + var14.xCoord, posY + var14.yCoord, posZ + var14.zCoord);
                  var13.addVertex(w2.posX - tank.posX + posX, w2.posY - tank.posY + posY, w2.posZ - tank.posZ + posZ);
               } else {
                  var13.addVertex(w1.posX - tank.posX + posX, w1.posY - tank.posY + posY, w1.posZ - tank.posZ + posZ);
                  var13.addVertex(w2.posX - tank.posX + posX, w2.posY - tank.posY + posY, w2.posZ - tank.posZ + posZ);
                  var13.addVertex(w2.posX - tank.posX + posX, w2.posY - tank.posY + posY, w2.posZ - tank.posZ + posZ);
                  var13.addVertex(posX + var14.xCoord, posY + var14.yCoord, posZ + var14.zCoord);
                  var13.addVertex(posX + var14.xCoord, posY + var14.yCoord, posZ + var14.zCoord);
                  var13.addVertex(w1.posX - tank.posX + posX, w1.posY - tank.posY + posY, w1.posZ - tank.posZ + posZ);
               }
            }

            var13.draw();
         }
      }
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }
}
