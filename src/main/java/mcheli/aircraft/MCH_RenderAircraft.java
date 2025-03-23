package mcheli.aircraft;

import java.util.Iterator;
import java.util.Random;

import mcheli.MCH_ClientCommonTickHandler;
import mcheli.MCH_ClientEventHook;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.flare.MCH_EntityChaff;
import mcheli.flare.MCH_EntityFlare;
import mcheli.gui.MCH_Gui;
import mcheli.lweapon.MCH_ClientLightWeaponTickHandler;
import mcheli.multiplay.MCH_GuiTargetMarker;
import mcheli.plane.MCP_EntityPlane;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vector.Vector3f;
import mcheli.weapon.*;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_EntityRenderer;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_MOD;
import mcheli.wrapper.W_Render;
import mcheli.wrapper.modelloader.W_ModelCustom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public abstract class MCH_RenderAircraft extends W_Render {

   public static boolean renderingEntity = false;
   public static IModelCustom debugModel = null;

   public static Random rand = new Random();


   public void doRender(Entity entity, double posX, double posY, double posZ, float par8, float tickTime) {



      MCH_EntityAircraft ac = (MCH_EntityAircraft)entity;
      //this will fire like constantly so yay emoji
      //if(ac.getAcInfo() != null) {
      //   ac.getAcInfo().reload();
      //   ac.changeType(ac.getAcInfo().name);
      //   ac.onAcInfoReloaded();
      //}
      MCH_AircraftInfo info = ac.getAcInfo();
      if(info != null) {
         GL11.glPushMatrix();
         float yaw = this.calcRot(ac.getRotYaw(), ac.prevRotationYaw, tickTime);
         float pitch = ac.calcRotPitch(tickTime);
         float roll = this.calcRot(ac.getRotRoll(), ac.prevRotationRoll, tickTime);
         MCH_Config var10000 = MCH_MOD.config;
         if(MCH_Config.EnableModEntityRender.prmBool) {
            this.renderRiddenEntity(ac, tickTime, yaw, pitch + info.entityPitch, roll + info.entityRoll, info.entityWidth, info.entityHeight);
         }

         if(!shouldSkipRender(entity)) {
            this.setCommonRenderParam(info.smoothShading, ac.getBrightnessForRender(tickTime));
            if(ac.isDestroyed()) {
               GL11.glColor4f(0.15F, 0.15F, 0.15F, 1.0F);
            } else {
               GL11.glColor4f(0.75F, 0.75F, 0.75F, 1.0F);
            }

            if (ac.ironCurtainRunningTick > 0) {
               float actualFactor = ac.ironCurtainLastFactor +
                       (ac.ironCurtainCurrentFactor - ac.ironCurtainLastFactor) *
                               (float) Math.sin(MCH_ClientEventHook.smoothing * Math.PI / 2);
               GL11.glColor4f(0.8F * actualFactor, 0.4F * actualFactor, 0.4F * actualFactor, 1.0F);
            }

            this.renderAircraft(ac, posX, posY, posZ, yaw, pitch, roll, tickTime);
            this.renderCommonPart(ac, info, posX, posY, posZ, tickTime);
            renderLight(posX, posY, posZ, tickTime, ac, info);
            this.restoreCommonRenderParam();
         }

         GL11.glPopMatrix();
         MCH_GuiTargetMarker.addMarkEntityPos(1, entity, posX, posY + (double)info.markerHeight, posZ);
         MCH_ClientLightWeaponTickHandler.markEntity(entity, posX, posY, posZ);
         renderEntityMarker(ac);
      }

   }

   public static boolean shouldSkipRender(Entity entity) {
      if(entity instanceof MCH_IEntityCanRideAircraft) {
         MCH_IEntityCanRideAircraft e = (MCH_IEntityCanRideAircraft)entity;
         if(e.isSkipNormalRender()) {
            return !renderingEntity;
         }
      } else if((entity.getClass().toString().indexOf("flansmod.common.driveables.EntityPlane") > 0 || entity.getClass().toString().indexOf("flansmod.common.driveables.EntityVehicle") > 0) && entity.ridingEntity instanceof MCH_EntitySeat) {
         return !renderingEntity;
      }

      return false;
   }

   public void doRenderShadowAndFire(Entity entity, double p_76979_2_, double p_76979_4_, double p_76979_6_, float p_76979_8_, float p_76979_9_) {
      if(entity.canRenderOnFire()) {
         this.renderEntityOnFire(entity, p_76979_2_, p_76979_4_, p_76979_6_, p_76979_9_);
      }

   }

   private void renderEntityOnFire(Entity entity, double x, double y, double z, float tick) {
      GL11.glDisable(2896);
      IIcon iicon = Blocks.fire.getFireIcon(0);
      IIcon iicon1 = Blocks.fire.getFireIcon(1);
      GL11.glPushMatrix();
      GL11.glTranslatef((float)x, (float)y, (float)z);
      float f1 = entity.width * 1.4F;
      GL11.glScalef(f1 * 2.0F, f1 * 2.0F, f1 * 2.0F);
      Tessellator tessellator = Tessellator.instance;
      float f2 = 1.5F;
      float f3 = 0.0F;
      float f4 = entity.height / f1;
      float f5 = (float)(entity.posY + entity.boundingBox.minY);
      GL11.glRotatef(-super.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
      GL11.glTranslatef(0.0F, 0.0F, -0.3F + (float)((int)f4) * 0.02F);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      float f6 = 0.0F;
      int i = 0;
      tessellator.startDrawingQuads();

      while(f4 > 0.0F) {
         IIcon iicon2 = i % 2 == 0?iicon:iicon1;
         this.bindTexture(TextureMap.locationBlocksTexture);
         float f7 = iicon2.getMinU();
         float f8 = iicon2.getMinV();
         float f9 = iicon2.getMaxU();
         float f10 = iicon2.getMaxV();
         if(i / 2 % 2 == 0) {
            float f11 = f9;
            f9 = f7;
            f7 = f11;
         }

         tessellator.addVertexWithUV((double)(f2 - f3), (double)(0.0F - f5), (double)f6, (double)f9, (double)f10);
         tessellator.addVertexWithUV((double)(-f2 - f3), (double)(0.0F - f5), (double)f6, (double)f7, (double)f10);
         tessellator.addVertexWithUV((double)(-f2 - f3), (double)(1.4F - f5), (double)f6, (double)f7, (double)f8);
         tessellator.addVertexWithUV((double)(f2 - f3), (double)(1.4F - f5), (double)f6, (double)f9, (double)f8);
         f4 -= 0.45F;
         f5 -= 0.45F;
         f2 *= 0.9F;
         f6 += 0.03F;
         ++i;
      }

      tessellator.draw();
      GL11.glPopMatrix();
      GL11.glEnable(2896);
   }

   public static void renderLight(double x, double y, double z, float tickTime, MCH_EntityAircraft ac, MCH_AircraftInfo info) {
      if(ac.haveSearchLight()) {
         if(ac.isSearchLightON()) {
            Entity entity = ac.getEntityBySeatId(1);
            if(entity != null) {
               ac.lastSearchLightYaw = entity.rotationYaw;
               ac.lastSearchLightPitch = entity.rotationPitch;
            } else {
               entity = ac.getEntityBySeatId(0);
               if(entity != null) {
                  ac.lastSearchLightYaw = entity.rotationYaw;
                  ac.lastSearchLightPitch = entity.rotationPitch;
               }
            }

            float yaw = ac.lastSearchLightYaw;
            float pitch = ac.lastSearchLightPitch;
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(3553);
            GL11.glShadeModel(7425);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 1);
            GL11.glDisable(3008);
            GL11.glDisable(2884);
            GL11.glDepthMask(false);
            float rot = ac.prevRotYawWheel + (ac.rotYawWheel - ac.prevRotYawWheel) * tickTime;
            Iterator i$ = info.searchLights.iterator();

            while(i$.hasNext()) {
               MCH_AircraftInfo.SearchLight sl = (MCH_AircraftInfo.SearchLight)i$.next();
               GL11.glPushMatrix();
               GL11.glTranslated(sl.pos.xCoord, sl.pos.yCoord, sl.pos.zCoord);
               float height;
               if(!sl.fixDir) {
                  GL11.glRotatef(yaw - ac.getRotYaw() + sl.yaw, 0.0F, -1.0F, 0.0F);
                  GL11.glRotatef(pitch + 90.0F - ac.getRotPitch() + sl.pitch, 1.0F, 0.0F, 0.0F);
               } else {
                  height = 0.0F;
                  if(sl.steering) {
                     height = -rot * sl.stRot;
                  }

                  GL11.glRotatef(0.0F + sl.yaw + height, 0.0F, -1.0F, 0.0F);
                  GL11.glRotatef(90.0F + sl.pitch, 1.0F, 0.0F, 0.0F);
               }

               height = sl.height;
               float width = sl.width / 2.0F;
               Tessellator tessellator = Tessellator.instance;
               tessellator.startDrawing(6);
               tessellator.setColorRGBA_I(16777215 & sl.colorStart, sl.colorStart >> 24 & 255);
               tessellator.addVertex(0.0D, 0.0D, 0.0D);
               tessellator.setColorRGBA_I(16777215 & sl.colorEnd, sl.colorEnd >> 24 & 255);
               boolean VNUM = true;

               for(int i = 0; i < 25; ++i) {
                  float angle = (float)(15.0D * (double)i / 180.0D * 3.141592653589793D);
                  tessellator.addVertex((double)(MathHelper.sin(angle) * width), (double)height, (double)(MathHelper.cos(angle) * width));
               }

               tessellator.draw();
               GL11.glPopMatrix();
            }

            GL11.glDepthMask(true);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(3553);
            GL11.glEnable(3008);
            GL11.glBlendFunc(770, 771);
            RenderHelper.enableStandardItemLighting();
         }
      }
   }

   protected void bindTexture(String path, MCH_EntityAircraft ac) {
      // if(ac == MCH_ClientCommonTickHandler.ridingAircraft) {
      //    int bk = MCH_ClientCommonTickHandler.cameraMode;
      //    MCH_ClientCommonTickHandler.cameraMode = 0;
      //    super.bindTexture(new ResourceLocation(W_MOD.DOMAIN, path));
      //    MCH_ClientCommonTickHandler.cameraMode = bk;
      // } else {
      //   super.bindTexture(new ResourceLocation(W_MOD.DOMAIN, path));
      //}
      if(MCH_ClientCommonTickHandler.cameraMode == 2) {
         super.bindTexture(new ResourceLocation(W_MOD.DOMAIN, "textures/test.png"));
      }else {
         super.bindTexture(new ResourceLocation(W_MOD.DOMAIN, path));
      }
   }

   public void renderRiddenEntity(MCH_EntityAircraft ac, float tickTime, float yaw, float pitch, float roll, float width, float height) {
      MCH_ClientEventHook.setCancelRender(false);
      GL11.glPushMatrix();
      this.renderEntitySimple(ac, ac.riddenByEntity, tickTime, yaw, pitch, roll, width, height);
      MCH_EntitySeat[] arr$ = ac.getSeats();
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         MCH_EntitySeat s = arr$[i$];
         if(s != null) {
            this.renderEntitySimple(ac, s.riddenByEntity, tickTime, yaw, pitch, roll, width, height);
         }
      }

      GL11.glPopMatrix();
      MCH_ClientEventHook.setCancelRender(true);
   }

   public void renderEntitySimple(MCH_EntityAircraft ac, Entity entity, float tickTime, float yaw, float pitch, float roll, float width, float height) {
      if(entity != null) {
         boolean isPilot = ac.isPilot(entity);
         boolean isClientPlayer = W_Lib.isClientPlayer(entity);
         if(!isClientPlayer || !W_Lib.isFirstPerson() || isClientPlayer && isPilot && ac.getCameraId() > 0) {
            GL11.glPushMatrix();
            if(entity.ticksExisted == 0) {
               entity.lastTickPosX = entity.posX;
               entity.lastTickPosY = entity.posY;
               entity.lastTickPosZ = entity.posZ;
            }

            double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double)tickTime;
            double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)tickTime;
            double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double)tickTime;
            float f1 = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * tickTime;
            int i = entity.getBrightnessForRender(tickTime);
            if(entity.isBurning()) {
               i = 15728880;
            }

            int j = i % 65536;
            int k = i / 65536;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)j / 1.0F, (float)k / 1.0F);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            RenderManager var10001 = super.renderManager;
            double dx = x - RenderManager.renderPosX;
            var10001 = super.renderManager;
            double dy = y - RenderManager.renderPosY;
            var10001 = super.renderManager;
            double dz = z - RenderManager.renderPosZ;
            GL11.glTranslated(dx, dy, dz);
            GL11.glRotatef(yaw, 0.0F, -1.0F, 0.0F);
            GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(roll, 0.0F, 0.0F, 1.0F);
            GL11.glScaled((double)width, (double)height, (double)width);
            GL11.glRotatef(-yaw, 0.0F, -1.0F, 0.0F);
            GL11.glTranslated(-dx, -dy, -dz);
            boolean bk = renderingEntity;
            renderingEntity = true;
            Entity ridingEntity = entity.ridingEntity;
            if(!W_Lib.isEntityLivingBase(entity) && !(entity instanceof MCH_IEntityCanRideAircraft)) {
               entity.ridingEntity = null;
            }

            EntityLivingBase entityLiving = entity instanceof EntityLivingBase?(EntityLivingBase)entity:null;
            float bkYaw = 0.0F;
            float bkPrevYaw = 0.0F;
            float bkPitch = 0.0F;
            float bkPrevPitch = 0.0F;
            if(isPilot && entityLiving != null) {
               entityLiving.renderYawOffset = ac.getRotYaw();
               entityLiving.prevRenderYawOffset = ac.getRotYaw();
               if(ac.getCameraId() > 0) {
                  entityLiving.rotationYawHead = ac.getRotYaw();
                  entityLiving.prevRotationYawHead = ac.getRotYaw();
                  bkPitch = entityLiving.rotationPitch;
                  bkPrevPitch = entityLiving.prevRotationPitch;
                  entityLiving.rotationPitch = ac.getRotPitch();
                  entityLiving.prevRotationPitch = ac.getRotPitch();
               }
            }

            W_EntityRenderer.renderEntityWithPosYaw(super.renderManager, entity, dx, dy, dz, f1, tickTime, false);

            if(isPilot && entityLiving != null && ac.getCameraId() > 0) {
               entityLiving.rotationPitch = bkPitch;
               entityLiving.prevRotationPitch = bkPrevPitch;
            }

            entity.ridingEntity = ridingEntity;
            renderingEntity = bk;
            GL11.glPopMatrix();
         }
      }

   }

   public static void Test_Material(int light, float a, float b, float c) {
      GL11.glMaterial(1032, light, setColorBuffer(a, b, c, 1.0F));
   }

   public static void Test_Light(int light, float a, float b, float c) {
      GL11.glLight(16384, light, setColorBuffer(a, b, c, 1.0F));
      GL11.glLight(16385, light, setColorBuffer(a, b, c, 1.0F));
   }

   public abstract void renderAircraft(MCH_EntityAircraft var1, double var2, double var4, double var6, float var8, float var9, float var10, float var11);

   public float calcRot(float rot, float prevRot, float tickTime) {
      rot = MathHelper.wrapAngleTo180_float(rot);
      prevRot = MathHelper.wrapAngleTo180_float(prevRot);
      if(rot - prevRot < -180.0F) {
         prevRot -= 360.0F;
      } else if(prevRot - rot < -180.0F) {
         prevRot += 360.0F;
      }

      return prevRot + (rot - prevRot) * tickTime;
   }

   public void renderDebugHitBox(MCH_EntityAircraft e, double x, double y, double z, float yaw, float pitch) {
      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.TestMode.prmBool && debugModel != null) {
         GL11.glPushMatrix();
         GL11.glTranslated(x, y, z);
         GL11.glScalef(e.width, e.height, e.width);
         this.bindTexture("textures/hit_box.png");
         debugModel.renderAll();
         GL11.glPopMatrix();
         GL11.glPushMatrix();
         GL11.glTranslated(x, y, z);
         MCH_BoundingBox[] arr$ = e.extraBoundingBox;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_BoundingBox bb = arr$[i$];
            GL11.glPushMatrix();
            GL11.glTranslated(bb.rotatedOffset.xCoord, bb.rotatedOffset.yCoord, bb.rotatedOffset.zCoord);
            GL11.glPushMatrix();
            GL11.glScalef(bb.width, bb.height, bb.width);
            this.bindTexture("textures/bounding_box.png");
            debugModel.renderAll();
            GL11.glPopMatrix();
            this.drawHitBoxDetail(bb);
            GL11.glPopMatrix();
         }

         GL11.glPopMatrix();
      }

   }

   public void drawHitBoxDetail(MCH_BoundingBox bb) {
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      float f1 = 0.080000006F;
      String s = String.format("%.2f", new Object[]{Float.valueOf(bb.damegeFactor)});
      GL11.glPushMatrix();
      GL11.glTranslatef(0.0F, 0.5F + (float)(bb.offsetY * 0.0D + (double)bb.height), 0.0F);
      GL11.glNormal3f(0.0F, 1.0F, 0.0F);
      GL11.glRotatef(-super.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(super.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
      GL11.glScalef(-f1, -f1, f1);
      GL11.glDisable(2896);
      GL11.glEnable(3042);
      OpenGlHelper.glBlendFunc(770, 771, 1, 0);
      GL11.glDisable(3553);
      FontRenderer fontrenderer = this.getFontRendererFromRenderManager();
      Tessellator tessellator = Tessellator.instance;
      tessellator.startDrawingQuads();
      int i = fontrenderer.getStringWidth(s) / 2;
      tessellator.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.4F);
      tessellator.addVertex((double)(-i - 1), -1.0D, 0.1D);
      tessellator.addVertex((double)(-i - 1), 8.0D, 0.1D);
      tessellator.addVertex((double)(i + 1), 8.0D, 0.1D);
      tessellator.addVertex((double)(i + 1), -1.0D, 0.1D);
      tessellator.draw();
      GL11.glEnable(3553);
      GL11.glDepthMask(false);
      int color = bb.damegeFactor < 1.0F?'\uffff':(bb.damegeFactor > 1.0F?16711680:16777215);
      fontrenderer.drawString(s, -fontrenderer.getStringWidth(s) / 2, 0, -1073741824 | color);
      GL11.glDepthMask(true);
      GL11.glEnable(2896);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
   }

   public void renderDebugPilotSeat(MCH_EntityAircraft e, double x, double y, double z, float yaw, float pitch, float roll) {
      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.TestMode.prmBool && debugModel != null) {
         GL11.glPushMatrix();
         MCH_SeatInfo seat = e.getSeatInfo(0);
         GL11.glTranslated(x, y, z);
         GL11.glRotatef(yaw, 0.0F, -1.0F, 0.0F);
         GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(roll, 0.0F, 0.0F, 1.0F);
         GL11.glTranslated(seat.pos.xCoord, seat.pos.yCoord, seat.pos.zCoord);
         GL11.glScalef(1.0F, 1.0F, 1.0F);
         this.bindTexture("textures/seat_pilot.png");
         debugModel.renderAll();
         GL11.glPopMatrix();
      }

   }

   public static void renderBody(IModelCustom model) {
      if(model != null) {
         if(model instanceof W_ModelCustom) {
            if(((W_ModelCustom)model).containsPart("$body")) {
               model.renderPart("$body");
            } else {
               model.renderAll();
            }
         } else {
            model.renderAll();
         }
      }

   }

   public static void renderPart(IModelCustom model, IModelCustom modelBody, String partName) {
      if(model != null) {
         model.renderAll();
      } else if(modelBody instanceof W_ModelCustom && ((W_ModelCustom)modelBody).containsPart("$" + partName)) {
         modelBody.renderPart("$" + partName);
      }

   }

   public void renderCommonPart(MCH_EntityAircraft ac, MCH_AircraftInfo info, double x, double y, double z, float tickTime) {
      renderRope(ac, info, x, y, z, tickTime);
      renderWeapon(ac, info, tickTime);
      renderRotPart(ac, info, tickTime);
      renderHatch(ac, info, tickTime);
      renderTrackRoller(ac, info, tickTime);
      renderCrawlerTrack(ac, info, tickTime);
      renderSteeringWheel(ac, info, tickTime);
      renderLightHatch(ac, info, tickTime);
      renderWheel(ac, info, tickTime);
      renderThrottle(ac, info, tickTime);
      renderCamera(ac, info, tickTime);
      renderLandingGear(ac, info, tickTime);
      renderWeaponBay(ac, info, tickTime);
      renderCanopy(ac, info, tickTime);
   }

   public static void renderLightHatch(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
      if(info.lightHatchList.size() > 0) {
         float rot = ac.prevRotLightHatch + (ac.rotLightHatch - ac.prevRotLightHatch) * tickTime;
         Iterator i$ = info.lightHatchList.iterator();

         while(i$.hasNext()) {
            MCH_AircraftInfo.Hatch t = (MCH_AircraftInfo.Hatch)i$.next();
            GL11.glPushMatrix();
            GL11.glTranslated(t.pos.xCoord, t.pos.yCoord, t.pos.zCoord);
            GL11.glRotated((double)(rot * t.maxRot), t.rot.xCoord, t.rot.yCoord, t.rot.zCoord);
            GL11.glTranslated(-t.pos.xCoord, -t.pos.yCoord, -t.pos.zCoord);
            renderPart(t.model, info.model, t.modelName);
            GL11.glPopMatrix();
         }

      }
   }

   public static void renderSteeringWheel(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
      if(info.partSteeringWheel.size() > 0) {
         float rot = ac.prevRotYawWheel + (ac.rotYawWheel - ac.prevRotYawWheel) * tickTime;
         Iterator i$ = info.partSteeringWheel.iterator();

         while(i$.hasNext()) {
            MCH_AircraftInfo.PartWheel t = (MCH_AircraftInfo.PartWheel)i$.next();
            GL11.glPushMatrix();
            GL11.glTranslated(t.pos.xCoord, t.pos.yCoord, t.pos.zCoord);
            GL11.glRotated((double)(rot * t.rotDir), t.rot.xCoord, t.rot.yCoord, t.rot.zCoord);
            GL11.glTranslated(-t.pos.xCoord, -t.pos.yCoord, -t.pos.zCoord);
            renderPart(t.model, info.model, t.modelName);
            GL11.glPopMatrix();
         }

      }
   }

   public static void renderWheel(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
      if(info.partWheel.size() > 0) {
         float yaw = ac.prevRotYawWheel + (ac.rotYawWheel - ac.prevRotYawWheel) * tickTime;
         Iterator i$ = info.partWheel.iterator();

         while(i$.hasNext()) {
            MCH_AircraftInfo.PartWheel t = (MCH_AircraftInfo.PartWheel)i$.next();
            GL11.glPushMatrix();
            GL11.glTranslated(t.pos2.xCoord, t.pos2.yCoord, t.pos2.zCoord);
            GL11.glRotated((double)(yaw * t.rotDir), t.rot.xCoord, t.rot.yCoord, t.rot.zCoord);
            GL11.glTranslated(-t.pos2.xCoord, -t.pos2.yCoord, -t.pos2.zCoord);
            GL11.glTranslated(t.pos.xCoord, t.pos.yCoord, t.pos.zCoord);
            GL11.glRotatef(ac.prevRotWheel + (ac.rotWheel - ac.prevRotWheel) * tickTime, 1.0F, 0.0F, 0.0F);
            GL11.glTranslated(-t.pos.xCoord, -t.pos.yCoord, -t.pos.zCoord);
            renderPart(t.model, info.model, t.modelName);
            GL11.glPopMatrix();
         }

      }
   }

   public static void renderRotPart(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
      if(ac.haveRotPart()) {
         for(int i = 0; i < ac.rotPartRotation.length; ++i) {
            float rot = ac.rotPartRotation[i];
            float prevRot = ac.prevRotPartRotation[i];
            if(prevRot > rot) {
               rot += 360.0F;
            }

            rot = MCH_Lib.smooth(rot, prevRot, tickTime);
            MCH_AircraftInfo.RotPart h = (MCH_AircraftInfo.RotPart)info.partRotPart.get(i);
            GL11.glPushMatrix();
            GL11.glTranslated(h.pos.xCoord, h.pos.yCoord, h.pos.zCoord);
            GL11.glRotatef(rot, (float)h.rot.xCoord, (float)h.rot.yCoord, (float)h.rot.zCoord);
            GL11.glTranslated(-h.pos.xCoord, -h.pos.yCoord, -h.pos.zCoord);
            renderPart(h.model, info.model, h.modelName);
            GL11.glPopMatrix();
         }

      }
   }

   public static void renderWeapon(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
      MCH_WeaponSet beforeWs = null;
      Entity e = ac.getRiddenByEntity();
      int weaponIndex = 0;
      int cnt = 0;
      Iterator i$ = info.partWeapon.iterator();

      while(i$.hasNext()) {
         MCH_AircraftInfo.PartWeapon w = (MCH_AircraftInfo.PartWeapon)i$.next();
         MCH_WeaponSet ws = ac.getWeaponByName(w.name[0]);
         boolean var10000;
         if(ws != null && ws.getFirstWeapon().onTurret) {
            var10000 = true;
         } else {
            var10000 = false;
         }

         if(ws != beforeWs) {
            weaponIndex = 0;
            beforeWs = ws;
         }

         float rotYaw = 0.0F;
         float prevYaw = 0.0F;
         float rotPitch = 0.0F;
         float prevPitch = 0.0F;
         boolean rev_sign;
         int len$;
         if(w.hideGM && W_Lib.isFirstPerson()) {
            if(ws != null) {
               rev_sign = false;
               String[] i$1 = w.name;
               int wc = i$1.length;

               for(len$ = 0; len$ < wc; ++len$) {
                  String i$2 = i$1[len$];
                  if(W_Lib.isClientPlayer(ac.getWeaponUserByWeaponName(i$2))) {
                     rev_sign = true;
                     break;
                  }
               }

               if(rev_sign) {
                  continue;
               }
            } else if(ac.isMountedEntity(MCH_Lib.getClientPlayer())) {
               continue;
            }
         }

         GL11.glPushMatrix();
         float var22;
         if(w.turret) {
            GL11.glTranslated(info.turretPosition.xCoord, info.turretPosition.yCoord, info.turretPosition.zCoord);
            var22 = MCH_Lib.smooth(ac.getLastRiderYaw() - ac.getRotYaw(), ac.prevLastRiderYaw - ac.prevRotationYaw, tickTime);
            GL11.glRotatef(var22, 0.0F, -1.0F, 0.0F);
            GL11.glTranslated(-info.turretPosition.xCoord, -info.turretPosition.yCoord, -info.turretPosition.zCoord);
         }

         GL11.glTranslated(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord);
         if(w.yaw) {
            if(ws != null) {
               rotYaw = ws.rotationYaw - ws.defaultRotationYaw;
               prevYaw = ws.prevRotationYaw - ws.defaultRotationYaw;
            } else if(e != null) {
               rotYaw = e.rotationYaw - ac.getRotYaw();
               prevYaw = e.prevRotationYaw - ac.prevRotationYaw;
            } else {
               rotYaw = ac.getLastRiderYaw() - ac.rotationYaw;
               prevYaw = ac.prevLastRiderYaw - ac.prevRotationYaw;
            }

            if(rotYaw - prevYaw > 180.0F) {
               prevYaw += 360.0F;
            } else if(rotYaw - prevYaw < -180.0F) {
               prevYaw -= 360.0F;
            }

            GL11.glRotatef(prevYaw + (rotYaw - prevYaw) * tickTime, 0.0F, -1.0F, 0.0F);
         }

         if(w.turret) {
            var22 = MCH_Lib.smooth(ac.getLastRiderYaw() - ac.getRotYaw(), ac.prevLastRiderYaw - ac.prevRotationYaw, tickTime);
            var22 -= ws.rotationTurretYaw;
            GL11.glRotatef(-var22, 0.0F, -1.0F, 0.0F);
         }

         rev_sign = false;
         float var23;
         if(ws != null && (int)ws.defaultRotationYaw != 0) {
            var23 = MathHelper.wrapAngleTo180_float(ws.defaultRotationYaw);
            rev_sign = var23 >= 45.0F && var23 <= 135.0F || var23 <= -45.0F && var23 >= -135.0F;
            GL11.glRotatef(-ws.defaultRotationYaw, 0.0F, -1.0F, 0.0F);
         }

         if(w.pitch) {
            if(ws != null) {
               rotPitch = ws.rotationPitch;
               prevPitch = ws.prevRotationPitch;
            } else if(e != null) {
               rotPitch = e.rotationPitch;
               prevPitch = e.prevRotationPitch;
            } else {
               rotPitch = ac.getLastRiderPitch();
               prevPitch = ac.prevLastRiderPitch;
            }

            if(rev_sign) {
               rotPitch = -rotPitch;
               prevPitch = -prevPitch;
            }

            GL11.glRotatef(prevPitch + (rotPitch - prevPitch) * tickTime, 1.0F, 0.0F, 0.0F);
         }

         if(ws != null && w.recoilBuf != 0.0F) {
            MCH_WeaponSet.Recoil var24 = ws.recoilBuf[0];
            if(w.name.length > 1) {
               String[] var25 = w.name;
               len$ = var25.length;

               for(int var29 = 0; var29 < len$; ++var29) {
                  String wnm = var25[var29];
                  MCH_WeaponSet tws = ac.getWeaponByName(wnm);
                  if(tws != null && tws.recoilBuf[0].recoilBuf > var24.recoilBuf) {
                     var24 = tws.recoilBuf[0];
                  }
               }
            }

            float var26 = var24.prevRecoilBuf + (var24.recoilBuf - var24.prevRecoilBuf) * tickTime;
            GL11.glTranslated(0.0D, 0.0D, (double)(w.recoilBuf * var26));
         }

         if(ws != null) {
            GL11.glRotatef(ws.defaultRotationYaw, 0.0F, -1.0F, 0.0F);
            if(w.rotBarrel) {
               var23 = ws.prevRotBarrel + (ws.rotBarrel - ws.prevRotBarrel) * tickTime;
               GL11.glRotatef(var23, (float)w.rot.xCoord, (float)w.rot.yCoord, (float)w.rot.zCoord);
            }
         }

         GL11.glTranslated(-w.pos.xCoord, -w.pos.yCoord, -w.pos.zCoord);
         if(!w.isMissile || !ac.isWeaponNotCooldown(ws, weaponIndex)) {
            renderPart(w.model, info.model, w.modelName);
            Iterator var27 = w.child.iterator();

            while(var27.hasNext()) {
               MCH_AircraftInfo.PartWeaponChild var28 = (MCH_AircraftInfo.PartWeaponChild)var27.next();
               GL11.glPushMatrix();
               renderWeaponChild(ac, info, var28, ws, e, tickTime);
               GL11.glPopMatrix();
            }
         }

         GL11.glPopMatrix();
         ++weaponIndex;
         ++cnt;
      }

   }

   public static void renderWeaponChild(MCH_EntityAircraft ac, MCH_AircraftInfo info, MCH_AircraftInfo.PartWeaponChild w, MCH_WeaponSet ws, Entity e, float tickTime) {
      float rotYaw = 0.0F;
      float prevYaw = 0.0F;
      float rotPitch = 0.0F;
      float prevPitch = 0.0F;
      GL11.glTranslated(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord);
      if(w.yaw) {
         if(ws != null) {
            rotYaw = ws.rotationYaw - ws.defaultRotationYaw;
            prevYaw = ws.prevRotationYaw - ws.defaultRotationYaw;
         } else if(e != null) {
            rotYaw = e.rotationYaw - ac.getRotYaw();
            prevYaw = e.prevRotationYaw - ac.prevRotationYaw;
         } else {
            rotYaw = ac.getLastRiderYaw() - ac.rotationYaw;
            prevYaw = ac.prevLastRiderYaw - ac.prevRotationYaw;
         }

         if(rotYaw - prevYaw > 180.0F) {
            prevYaw += 360.0F;
         } else if(rotYaw - prevYaw < -180.0F) {
            prevYaw -= 360.0F;
         }

         GL11.glRotatef(prevYaw + (rotYaw - prevYaw) * tickTime, 0.0F, -1.0F, 0.0F);
      }

      boolean rev_sign = false;
      if(ws != null && (int)ws.defaultRotationYaw != 0) {
         float r = MathHelper.wrapAngleTo180_float(ws.defaultRotationYaw);
         rev_sign = r >= 45.0F && r <= 135.0F || r <= -45.0F && r >= -135.0F;
         GL11.glRotatef(-ws.defaultRotationYaw, 0.0F, -1.0F, 0.0F);
      }

      if(w.pitch) {
         if(ws != null) {
            rotPitch = ws.rotationPitch;
            prevPitch = ws.prevRotationPitch;
         } else if(e != null) {
            rotPitch = e.rotationPitch;
            prevPitch = e.prevRotationPitch;
         } else {
            rotPitch = ac.getLastRiderPitch();
            prevPitch = ac.prevLastRiderPitch;
         }

         if(rev_sign) {
            rotPitch = -rotPitch;
            prevPitch = -prevPitch;
         }

         GL11.glRotatef(prevPitch + (rotPitch - prevPitch) * tickTime, 1.0F, 0.0F, 0.0F);
      }

      if(ws != null && w.recoilBuf != 0.0F) {
         MCH_WeaponSet.Recoil var17 = ws.recoilBuf[0];
         if(w.name.length > 1) {
            String[] recoilBuf = w.name;
            int len$ = recoilBuf.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               String wnm = recoilBuf[i$];
               MCH_WeaponSet tws = ac.getWeaponByName(wnm);
               if(tws != null && tws.recoilBuf[0].recoilBuf > var17.recoilBuf) {
                  var17 = tws.recoilBuf[0];
               }
            }
         }

         float var18 = var17.prevRecoilBuf + (var17.recoilBuf - var17.prevRecoilBuf) * tickTime;
         GL11.glTranslated(0.0D, 0.0D, (double)(-w.recoilBuf * var18));
      }

      if(ws != null) {
         GL11.glRotatef(ws.defaultRotationYaw, 0.0F, -1.0F, 0.0F);
      }

      GL11.glTranslated(-w.pos.xCoord, -w.pos.yCoord, -w.pos.zCoord);
      renderPart(w.model, info.model, w.modelName);
   }

   public static void renderTrackRoller(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
      if(info.partTrackRoller.size() > 0) {
         float[] rot = ac.rotTrackRoller;
         float[] prevRot = ac.prevRotTrackRoller;
         Iterator i$ = info.partTrackRoller.iterator();

         while(i$.hasNext()) {
            MCH_AircraftInfo.TrackRoller t = (MCH_AircraftInfo.TrackRoller)i$.next();
            GL11.glPushMatrix();
            GL11.glTranslated(t.pos.xCoord, t.pos.yCoord, t.pos.zCoord);
            GL11.glRotatef(prevRot[t.side] + (rot[t.side] - prevRot[t.side]) * tickTime, 1.0F, 0.0F, 0.0F);
            GL11.glTranslated(-t.pos.xCoord, -t.pos.yCoord, -t.pos.zCoord);
            renderPart(t.model, info.model, t.modelName);
            GL11.glPopMatrix();
         }

      }
   }

   public static void renderCrawlerTrack(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
      if(info.partCrawlerTrack.size() > 0) {
         int prevWidth = GL11.glGetInteger(2833);
         Tessellator tessellator = Tessellator.instance;
         Iterator i$ = info.partCrawlerTrack.iterator();

         while(i$.hasNext()) {
            MCH_AircraftInfo.CrawlerTrack c = (MCH_AircraftInfo.CrawlerTrack)i$.next();
            GL11.glPointSize(c.len * 20.0F);
            MCH_Config var10000 = MCH_MOD.config;
            int L;
            if(MCH_Config.TestMode.prmBool) {
               GL11.glDisable(3553);
               GL11.glDisable(3042);
               tessellator.startDrawing(0);

               for(L = 0; L < c.cx.length; ++L) {
                  tessellator.setColorRGBA((int)(255.0F / (float)c.cx.length * (float)L), 80, 255 - (int)(255.0F / (float)c.cx.length * (float)L), 255);
                  tessellator.addVertex((double)c.z, c.cx[L], c.cy[L]);
               }

               tessellator.draw();
            }

            GL11.glEnable(3553);
            GL11.glEnable(3042);
            L = c.lp.size() - 1;
            double rc = ac != null?(double)ac.rotCrawlerTrack[c.side]:0.0D;
            double pc = ac != null?(double)ac.prevRotCrawlerTrack[c.side]:0.0D;

            for(int i = 0; i < L; ++i) {
               MCH_AircraftInfo.CrawlerTrackPrm cp = (MCH_AircraftInfo.CrawlerTrackPrm)c.lp.get(i);
               MCH_AircraftInfo.CrawlerTrackPrm np = (MCH_AircraftInfo.CrawlerTrackPrm)c.lp.get((i + 1) % L);
               double x1 = (double)cp.x;
               double x2 = (double)np.x;
               double r1 = (double)cp.r;
               double y1 = (double)cp.y;
               double y2 = (double)np.y;
               double r2 = (double)np.r;
               if(r2 - r1 < -180.0D) {
                  r2 += 360.0D;
               }

               if(r2 - r1 > 180.0D) {
                  r2 -= 360.0D;
               }

               double sx = x1 + (x2 - x1) * rc;
               double sy = y1 + (y2 - y1) * rc;
               double sr = r1 + (r2 - r1) * rc;
               double ex = x1 + (x2 - x1) * pc;
               double ey = y1 + (y2 - y1) * pc;
               double er = r1 + (r2 - r1) * pc;
               double x = sx + (ex - sx) * pc;
               double y = sy + (ey - sy) * pc;
               double r = sr + (er - sr) * pc;
               GL11.glPushMatrix();
               GL11.glTranslated(0.0D, x, y);
               GL11.glRotatef((float)r, -1.0F, 0.0F, 0.0F);
               renderPart(c.model, info.model, c.modelName);
               GL11.glPopMatrix();
            }
         }

         GL11.glEnable(3042);
         GL11.glPointSize((float)prevWidth);
      }
   }

   public static void renderHatch(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
      if(info.haveHatch() && ac.partHatch != null) {
         float rot = ac.getHatchRotation();
         float prevRot = ac.getPrevHatchRotation();
         Iterator i$ = info.hatchList.iterator();

         while(i$.hasNext()) {
            MCH_AircraftInfo.Hatch h = (MCH_AircraftInfo.Hatch)i$.next();
            GL11.glPushMatrix();
            if(h.isSlide) {
               float r = ac.partHatch.rotation / ac.partHatch.rotationMax;
               float pr = ac.partHatch.prevRotation / ac.partHatch.rotationMax;
               float f = pr + (r - pr) * tickTime;
               GL11.glTranslated(h.pos.xCoord * (double)f, h.pos.yCoord * (double)f, h.pos.zCoord * (double)f);
            } else {
               GL11.glTranslated(h.pos.xCoord, h.pos.yCoord, h.pos.zCoord);
               GL11.glRotatef((prevRot + (rot - prevRot) * tickTime) * h.maxRotFactor, (float)h.rot.xCoord, (float)h.rot.yCoord, (float)h.rot.zCoord);
               GL11.glTranslated(-h.pos.xCoord, -h.pos.yCoord, -h.pos.zCoord);
            }

            renderPart(h.model, info.model, h.modelName);
            GL11.glPopMatrix();
         }

      }
   }

   public static void renderThrottle(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
      if(info.havePartThrottle()) {
         float throttle = MCH_Lib.smooth((float)ac.getCurrentThrottle(), (float)ac.getPrevCurrentThrottle(), tickTime);
         Iterator i$ = info.partThrottle.iterator();

         while(i$.hasNext()) {
            MCH_AircraftInfo.Throttle h = (MCH_AircraftInfo.Throttle)i$.next();
            GL11.glPushMatrix();
            GL11.glTranslated(h.pos.xCoord, h.pos.yCoord, h.pos.zCoord);
            GL11.glRotatef(throttle * h.rot2, (float)h.rot.xCoord, (float)h.rot.yCoord, (float)h.rot.zCoord);
            GL11.glTranslated(-h.pos.xCoord, -h.pos.yCoord, -h.pos.zCoord);
            GL11.glTranslated(h.slide.xCoord * (double)throttle, h.slide.yCoord * (double)throttle, h.slide.zCoord * (double)throttle);
            renderPart(h.model, info.model, h.modelName);
            GL11.glPopMatrix();
         }

      }
   }

   public static void renderWeaponBay(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
      for(int i = 0; i < info.partWeaponBay.size(); ++i) {
         MCH_AircraftInfo.WeaponBay w = (MCH_AircraftInfo.WeaponBay)info.partWeaponBay.get(i);
         MCH_EntityAircraft.WeaponBay ws = ac.weaponBays[i];
         GL11.glPushMatrix();
         if(w.isSlide) {
            float r = ws.rot / 90.0F;
            float pr = ws.prevRot / 90.0F;
            float f = pr + (r - pr) * tickTime;
            GL11.glTranslated(w.pos.xCoord * (double)f, w.pos.yCoord * (double)f, w.pos.zCoord * (double)f);
         } else {
            GL11.glTranslated(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord);
            GL11.glRotatef((ws.prevRot + (ws.rot - ws.prevRot) * tickTime) * w.maxRotFactor, (float)w.rot.xCoord, (float)w.rot.yCoord, (float)w.rot.zCoord);
            GL11.glTranslated(-w.pos.xCoord, -w.pos.yCoord, -w.pos.zCoord);
         }

         renderPart(w.model, info.model, w.modelName);
         GL11.glPopMatrix();
      }

   }

   public static void renderCamera(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
      if(info.havePartCamera()) {
         float rotYaw = ac.camera.partRotationYaw;
         float prevRotYaw = ac.camera.prevPartRotationYaw;
         float rotPitch = ac.camera.partRotationPitch;
         float prevRotPitch = ac.camera.prevPartRotationPitch;
         float yaw = prevRotYaw + (rotYaw - prevRotYaw) * tickTime - ac.getRotYaw();
         float pitch = prevRotPitch + (rotPitch - prevRotPitch) * tickTime - ac.getRotPitch();
         Iterator i$ = info.cameraList.iterator();

         while(i$.hasNext()) {
            MCH_AircraftInfo.Camera c = (MCH_AircraftInfo.Camera)i$.next();
            GL11.glPushMatrix();
            GL11.glTranslated(c.pos.xCoord, c.pos.yCoord, c.pos.zCoord);
            if(c.yawSync) {
               GL11.glRotatef(yaw, 0.0F, -1.0F, 0.0F);
            }

            if(c.pitchSync) {
               GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
            }

            GL11.glTranslated(-c.pos.xCoord, -c.pos.yCoord, -c.pos.zCoord);
            renderPart(c.model, info.model, c.modelName);
            GL11.glPopMatrix();
         }

      }
   }

   public static void renderCanopy(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
      if(info.haveCanopy() && ac.partCanopy != null) {
         float rot = ac.getCanopyRotation();
         float prevRot = ac.getPrevCanopyRotation();
         Iterator i$ = info.canopyList.iterator();

         while(i$.hasNext()) {
            MCH_AircraftInfo.Canopy c = (MCH_AircraftInfo.Canopy)i$.next();
            GL11.glPushMatrix();
            if(c.isSlide) {
               float r = ac.partCanopy.rotation / ac.partCanopy.rotationMax;
               float pr = ac.partCanopy.prevRotation / ac.partCanopy.rotationMax;
               float f = pr + (r - pr) * tickTime;
               GL11.glTranslated(c.pos.xCoord * (double)f, c.pos.yCoord * (double)f, c.pos.zCoord * (double)f);
            } else {
               GL11.glTranslated(c.pos.xCoord, c.pos.yCoord, c.pos.zCoord);
               GL11.glRotatef((prevRot + (rot - prevRot) * tickTime) * c.maxRotFactor, (float)c.rot.xCoord, (float)c.rot.yCoord, (float)c.rot.zCoord);
               GL11.glTranslated(-c.pos.xCoord, -c.pos.yCoord, -c.pos.zCoord);
            }

            renderPart(c.model, info.model, c.modelName);
            GL11.glPopMatrix();
         }
      }

   }

   public static void renderLandingGear(MCH_EntityAircraft ac, MCH_AircraftInfo info, float tickTime) {
      if (info.haveLandingGear() && ac.partLandingGear != null) {
         float rot = ac.getLandingGearRotation();
         float prevRot = ac.getPrevLandingGearRotation();
         float revR = 90.0F - rot;
         float revPr = 90.0F - prevRot;
         float rot1 = prevRot + (rot - prevRot) * tickTime;
         float rot1Rev = revPr + (revR - revPr) * tickTime;
         float rotHatch = 90.0F * MathHelper.sin(rot1 * 2.0F * 3.1415927F / 180.0F) * 3.0F;
         if (rotHatch > 90.0F) {
            rotHatch = 90.0F;
         }

         Iterator i$ = info.landingGear.iterator();

         while (i$.hasNext()) {
            MCH_AircraftInfo.LandingGear n = (MCH_AircraftInfo.LandingGear) i$.next();
            GL11.glPushMatrix();
            GL11.glTranslated(n.pos.xCoord, n.pos.yCoord, n.pos.zCoord);
            if (!n.reverse) {
               if (!n.hatch) {
                  GL11.glRotatef(rot1 * n.maxRotFactor, (float) n.rot.xCoord, (float) n.rot.yCoord, (float) n.rot.zCoord);
               } else {
                  GL11.glRotatef(rotHatch * n.maxRotFactor, (float) n.rot.xCoord, (float) n.rot.yCoord, (float) n.rot.zCoord);
               }
            } else {
               GL11.glRotatef(rot1Rev * n.maxRotFactor, (float) n.rot.xCoord, (float) n.rot.yCoord, (float) n.rot.zCoord);
            }

            if (n.enableRot2) {
               if (!n.reverse) {
                  GL11.glRotatef(rot1 * n.maxRotFactor2, (float) n.rot2.xCoord, (float) n.rot2.yCoord, (float) n.rot2.zCoord);
               } else {
                  GL11.glRotatef(rot1Rev * n.maxRotFactor2, (float) n.rot2.xCoord, (float) n.rot2.yCoord, (float) n.rot2.zCoord);
               }
            }

            GL11.glTranslated(-n.pos.xCoord, -n.pos.yCoord, -n.pos.zCoord);
            if (n.slide != null) {
               float f = rot / 90.0F;
               if (n.reverse) {
                  f = 1.0F - f;
               }

               GL11.glTranslated((double) f * n.slide.xCoord, (double) f * n.slide.yCoord, (double) f * n.slide.zCoord);
            }

            renderPart(n.model, info.model, n.modelName);
            GL11.glPopMatrix();
         }
      }

   }

   public static void renderEntityMarker(Entity entity) {
      EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
      if(player != null) {
         if(!W_Entity.isEqual(player, entity)) {
            MCH_EntityAircraft ac = null; //玩家乘坐的实体
            if(player.ridingEntity instanceof MCH_EntityAircraft) {
               ac = (MCH_EntityAircraft)player.ridingEntity;
            } else if(player.ridingEntity instanceof MCH_EntitySeat) {
               ac = ((MCH_EntitySeat)player.ridingEntity).getParent();
            } else if(player.ridingEntity instanceof MCH_EntityUavStation) {
               ac = ((MCH_EntityUavStation)player.ridingEntity).getControlAircract();
            }

            if(ac != null) {
               if(!W_Entity.isEqual(ac, entity)) {
                  MCH_IGuidanceSystem guidanceSystem = ac.getCurrentWeapon(player).getCurrentWeapon().getGuidanceSystem();
                  MCH_WeaponInfo wi = ac.getCurrentWeapon(player).getCurrentWeapon().getInfo();
                  if(guidanceSystem == null) {
                     return;
                  }

                  if (guidanceSystem instanceof MCH_EntityGuidanceSystem) {
                     MCH_EntityGuidanceSystem gs = (MCH_EntityGuidanceSystem) guidanceSystem;
                     // 检查当前武器是否有引导系统，并且该引导系统是否能锁定目标实体
                     if(gs.canLockEntity(entity)) {
                        RenderManager rm = RenderManager.instance;
                        // 计算目标实体与玩家之间的平方距离
                        double dist = entity.getDistanceSqToEntity(rm.livingPlayer);
                        double distance = Math.sqrt(dist);
                        if(wi != null && wi.enableBVR && distance > wi.minRangeBVR) {
                           return;
                        }
//                     if(entity instanceof MCH_EntityFlare) {
//                        long worldTime = Minecraft.getMinecraft().theWorld.getTotalWorldTime();
//                        float blinkBaseFrequency = 1.0F; // 基本闪烁频率（每秒闪烁一次）
//                        float randomFrequencyFactor = 0.5F + rand.nextFloat() * 0.5F; // 随机频率范围 [0.5, 1.0]
//                        float blinkFrequency = blinkBaseFrequency * randomFrequencyFactor;
//                        float sinValue = (float) Math.sin(worldTime * blinkFrequency * Math.PI * 2.0F); // 正弦波函数
//                        boolean isFlareVisible = sinValue > 0.0F; // 通过正弦波的值来决定是否显示框
//                        if(!isFlareVisible) return;
//                     }
                        Vec3 src = Vec3.createVectorHelper(RenderManager.renderPosX, RenderManager.renderPosY, RenderManager.renderPosZ);
                        Vec3 dst = Vec3.createVectorHelper(entity.posX, entity.posY, entity.posZ);
                        MovingObjectPosition mop = player.worldObj.rayTraceBlocks(src, dst, true);
                        if(mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                           return;
                        }

                        // 计算实体相对于玩家的坐标偏移
                        double x = entity.posX - RenderManager.renderPosX;
                        double y = entity.posY - RenderManager.renderPosY;
                        double z = entity.posZ - RenderManager.renderPosZ;

                        // 如果目标实体与玩家的距离小于1000，则进行渲染
                        if(dist < 1000000.0D) {
                           float scl = 0.02666667F; // 缩放因子
                           GL11.glPushMatrix();
                           // 进行位置变换，将目标实体渲染到玩家视角中
                           GL11.glTranslatef((float)x, (float)y + entity.height + 2F, (float)z);
                           GL11.glNormal3f(0.0F, 1.0F, 0.0F);
                           GL11.glRotatef(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
                           GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
                           GL11.glScalef(-0.02666667F, -0.02666667F, 0.02666667F);
                           GL11.glDisable(2896); // 禁用深度测试
                           GL11.glTranslatef(0.0F, 9.374999F, 0.0F); // 上移一些偏移量
                           GL11.glDepthMask(false); // 禁用深度写入
                           GL11.glEnable(3042); // 启用混合
                           GL11.glBlendFunc(770, 771); // 设置混合模式
                           GL11.glDisable(3553); // 禁用纹理
                           GL11.glDisable(2929 /* GL_DEPTH_TEST */);

                           // 获取绘制前的屏幕宽度
                           int prevWidth = GL11.glGetInteger(2849);
                           // 设置目标实体大小（根据实体的宽度和高度进行调整）
                           float size1 = Math.max(entity.width, entity.height) * 20.0F;
                           if(entity instanceof MCH_EntityAircraft
                                   || entity instanceof MCH_EntityFlare
                                   || entity instanceof MCH_EntityChaff) {
                              size1 *= 2.0F; // 飞机类型实体大小加倍
                           }
                           float size = size1 + (float)((distance - 10.0D) / (300.0D - 10.0D)) * (300.0F - size1);
                           // 确保字体大小在size1和100之间
                           size = Math.max(size1, Math.min(300.0F, size));

                           // 创建Tessellator对象，用于绘制图形
                           Tessellator tessellator = Tessellator.instance;
                           tessellator.startDrawing(2); // 开始绘制线条
                           tessellator.setBrightness(240); // 设置亮度

                           Vector3f playerVelocity = new Vector3f(ac.motionX, ac.motionY, ac.motionZ);  // 玩家机体的速度向量
                           Vector3f targetVelocity = new Vector3f(entity.motionX, entity.motionY, entity.motionZ);  // 目标机体的速度向量
                           float angleInDegrees = 0;
                           if(playerVelocity.length() > 0.001 && targetVelocity.length() > 0.001) {
                              // 计算两个向量的点积
                              float dotProduct = Vector3f.dot(playerVelocity, targetVelocity);
                              // 计算两个向量的长度
                              float playerSpeed = playerVelocity.length();
                              float targetSpeed = targetVelocity.length();
                              // 计算夹角的余弦值
                              float cosAngle = dotProduct / (playerSpeed * targetSpeed);
                              // 确保夹角余弦值在合法范围内 [-1, 1]，避免浮动导致的异常值
                              cosAngle = Math.max(-1.0f, Math.min(1.0f, cosAngle));
                              // 计算夹角（弧度）
                              float angle = (float) Math.acos(cosAngle);
                              // 如果夹角大于90度，将其转换为锐角（90度以内）
                              if (angle > Math.PI / 2) {
                                 angle = (float) (Math.PI - angle);  // 转换为锐角
                              }
                              // 将角度转化为度数（可选）
                              angleInDegrees = (float) Math.toDegrees(angle);
                           }

                           // 检查当前是否锁定了目标实体
                           boolean isLockEntity = gs.isLockingEntity(entity);
                           float alpha = 1.0F;
                           if (angleInDegrees > ac.getCurrentWeapon(player).getCurrentWeapon().getInfo().pdHDNMaxDegree) {
                              //alpha = 0.4F * (float) (Math.sin(System.currentTimeMillis() * 1000) * MCH_ClientCommonTickHandler.smoothing + 1.0F);
                              alpha = 0.2F;
                           }
                           if (distance > ac.getCurrentWeapon(player).getCurrentWeapon().getInfo().maxLockOnRange) {
                              alpha = 0.2F;
                           }

                           if(isLockEntity) {
                              GL11.glLineWidth((float)MCH_Gui.scaleFactor * 2.5F); // 设置线宽
                              tessellator.setColorRGBA_F(1.0F, 0.0F, 0.0F, alpha); // 锁定状态下显示红色
                           } else {
                              GL11.glLineWidth((float)MCH_Gui.scaleFactor * 1.5F); // 设置线宽
                              tessellator.setColorRGBA_F(0.0F, 1.0F, 0.0F, alpha); // 绿色
                           }

                           // 绘制矩形框，表示锁定范围
                           tessellator.addVertex(-size - 1.0F, 0.0D, 0.0D);
                           tessellator.addVertex(-size - 1.0F, size * 2.0F, 0.0D);
                           tessellator.addVertex(size + 1.0F, size * 2.0F, 0.0D);
                           tessellator.addVertex(size + 1.0F, 0.0D, 0.0D);
                           tessellator.draw(); // 绘制线条

                           // 获取距离并绘制文字
                           String distanceText = String.format("%.1f", distance); // 格式化为一位小数

                           // 获取 FontRenderer 对象并设置颜色为绿色
                           FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
                           {
                              GL11.glPushMatrix();
                              GL11.glTranslatef(0.0F, size * 2.0F + 1.0F, 0.0F); // 将文本放置在矩形框的下方
                              float fontSize = 5.0F + (float)((distance - 10.0D) / (300.0D - 10.0D)) * (40.0F - 5.0F);
                              // 确保字体大小在5和40之间
                              fontSize = Math.max(5.0F, Math.min(40.0F, fontSize));
                              GL11.glScalef(fontSize, fontSize, fontSize);
                              // 绘制绿色文字，显示与目标的距离
                              String text = "";
                              if (gs.isHeatSeekerMissile) {
                                 text = "HEAT";
                              } else if (gs.isRadarMissile) {
                                 if (entity instanceof MCH_EntityAircraft) {
                                    MCH_EntityAircraft entityAircraft = (MCH_EntityAircraft) entity;
                                    text = entityAircraft.getNameOnOtherRadar(ac);
                                 } else {
                                    text = "?";
                                 }
                              }
                              text += " " + distanceText;

                              if(ac instanceof MCP_EntityPlane && entity instanceof MCP_EntityPlane && angleInDegrees != 0) {
                                 // 将角度值作为文本输出
                                 String angleText = String.format("%.1f", angleInDegrees);  // 保留一位小数
                                 text += " " + angleText;
                              }

                              fontRenderer.drawString(text, -fontRenderer.getStringWidth(text) / 2, 0, 0x00ff00);

                              GL11.glPopMatrix();
                           }

                           GL11.glPopMatrix();



                           // 如果实体是无人机且当前视角是第一人称视角，并且目标实体被锁定，则绘制连接线
                           if(!ac.isUAV() && isLockEntity && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
                              GL11.glPushMatrix();
                              tessellator.startDrawing(1); // 绘制点到点的连线
                              GL11.glLineWidth(1.0F);
                              tessellator.setColorRGBA_F(1.0F, 0.0F, 0.0F, 1.0F); // 设置颜色为红色
                              // 连接线从实体的中心到飞机的上一个位置
                              tessellator.addVertex(x, y + (double)(entity.height / 2.0F), z);
                              tessellator.addVertex(ac.lastTickPosX - RenderManager.renderPosX, ac.lastTickPosY - RenderManager.renderPosY - 1.0D, ac.lastTickPosZ - RenderManager.renderPosZ);
                              tessellator.setBrightness(240); // 设置亮度
                              tessellator.draw(); // 绘制线条
                              GL11.glPopMatrix(); // 恢复矩阵状态
                           }

                           // 恢复之前的线宽，启用纹理，恢复深度写入和深度测试
                           GL11.glLineWidth((float)prevWidth);
                           GL11.glEnable(3553);
                           GL11.glDepthMask(true);
                           GL11.glEnable(2896);
                           GL11.glDisable(3042);
                           GL11.glEnable(2929 /* GL_DEPTH_TEST */);
                           GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F); // 恢复默认颜色
                        }
                     }
                  }



               }
            }
         }
      }
   }

   public static void renderRope(MCH_EntityAircraft ac, MCH_AircraftInfo info, double x, double y, double z, float tickTime) {
      GL11.glPushMatrix();
      Tessellator tessellator = Tessellator.instance;
      if(ac.isRepelling()) {
         GL11.glDisable(3553);
         GL11.glDisable(2896);

         for(int i = 0; i < info.repellingHooks.size(); ++i) {
            tessellator.startDrawing(3);
            tessellator.setColorOpaque_I(0);
            tessellator.addVertex(((MCH_AircraftInfo.RepellingHook)info.repellingHooks.get(i)).pos.xCoord, ((MCH_AircraftInfo.RepellingHook)info.repellingHooks.get(i)).pos.yCoord, ((MCH_AircraftInfo.RepellingHook)info.repellingHooks.get(i)).pos.zCoord);
            tessellator.addVertex(((MCH_AircraftInfo.RepellingHook)info.repellingHooks.get(i)).pos.xCoord, ((MCH_AircraftInfo.RepellingHook)info.repellingHooks.get(i)).pos.yCoord + (double)ac.ropesLength, ((MCH_AircraftInfo.RepellingHook)info.repellingHooks.get(i)).pos.zCoord);
            tessellator.draw();
         }

         GL11.glEnable(2896);
         GL11.glEnable(3553);
      }

      GL11.glPopMatrix();
   }

}
