package mcheli.aircraft;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import cpw.mods.fml.common.Loader;

import mcheli.MCH_ClientCommonTickHandler;
import mcheli.MCH_ClientEventHook;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.flare.MCH_EntityChaff;
import mcheli.flare.MCH_EntityFlare;
import mcheli.gui.MCH_Gui;
import mcheli.lweapon.MCH_ClientLightWeaponTickHandler;
import mcheli.lod.MCH_VehicleLODProjection;
import mcheli.lod.MCH_VehicleLODManager;
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
import mcheli.tank.MCH_TurretPopModelCache;
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

public abstract class MCH_RenderBaseVehicle extends W_Render {

   public static boolean renderingEntity = false;
   public static IModelCustom debugModel = null;
   private static ResourceLocation activeSkinOverlayTexture = null;
   private static ResourceLocation activeBaseTexture = null;
   private static final boolean ANGELICA_DYNAMIC_PART_COMPAT = Loader.isModLoaded("angelica");
   private static final boolean DEBUG_ANGELICA_DYNAMIC_PART_RENDER = Boolean.getBoolean("mcheli.debugAngelicaDynamicPartRender");
   private static final Set angelicaDynamicPartRenderDiagnostics = new HashSet();
   private static final Map vehicleLODDiagnosticTimes = new HashMap();
   /** Shared immutable input for crawler tracks rendered without a live vehicle. */
   private static final float[] STATIC_CRAWLER_TRACK_STATE = new float[]{0.0F, 0.0F};

   public static Random rand = new Random();


   public void doRender(Entity entity, double posX, double posY, double posZ, float par8, float tickTime) {



      MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)entity;
      //this will fire like constantly so yay emoji
      //if(ac.getAcInfo() != null) {
      //   ac.getAcInfo().reload();
      //   ac.changeType(ac.getAcInfo().name);
      //   ac.onAcInfoReloaded();
      //}
      MCH_BaseVehicleInfo info = ac.getAcInfo();
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

            if(this.shouldRenderAircraftLOD(ac, posX, posY, posZ)) {
               this.renderAircraftLOD(ac, info, posX, posY, posZ, yaw, pitch, roll, tickTime);
            } else {
               beginSkinOverlayRender(info, ac);
               this.renderBaseVehicle(ac, posX, posY, posZ, yaw, pitch, roll, tickTime);
               this.renderCommonPart(ac, info, posX, posY, posZ, tickTime);
               endSkinOverlayRender();
               renderLight(posX, posY, posZ, tickTime, ac, info);
            }

            this.restoreCommonRenderParam();
         }

         GL11.glPopMatrix();
         MCH_GuiTargetMarker.addMarkEntityPos(1, entity, posX, posY + (double)info.markerHeight, posZ);
         MCH_ClientLightWeaponTickHandler.markEntity(entity, posX, posY, posZ);
         renderEntityMarker(ac);
      }

   }

   protected boolean shouldRenderAircraftLOD(MCH_EntityBaseVehicle ac, double posX, double posY, double posZ) {
      if(MCH_Config.EnableAircraftLODRender == null || !MCH_Config.EnableAircraftLODRender.prmBool) {
         ac.isRenderingLOD = false;
         return false;
      }

      double startDistance = MCH_Config.AircraftLODStartDistance != null?MCH_Config.AircraftLODStartDistance.prmDouble:0.0D;
      if(startDistance <= 0.0D) {
         ac.isRenderingLOD = false;
         return false;
      }

      Minecraft mc = Minecraft.getMinecraft();
      Entity camera = mc.renderViewEntity;
      if(camera == null || camera.isDead || camera.worldObj != ac.worldObj || mc.theWorld != ac.worldObj) {
         ac.isRenderingLOD = false;
         return false;
      }

      // Do not use a player (or the renderer coordinates derived from one) cached
      // before respawn.  The render-view entity is replaceable and is authoritative
      // for the current frame's distance.
      double dx = ac.posX - camera.posX;
      double dy = ac.posY - camera.posY;
      double dz = ac.posZ - camera.posZ;
      double hysteresis = 16.0D;
      double exitDistance = Math.max(0.0D, startDistance - hysteresis);
      double threshold = ac.isRenderingLOD?exitDistance:startDistance;
      boolean shouldRenderLOD = dx * dx + dy * dy + dz * dz >= threshold * threshold;
      ac.isRenderingLOD = shouldRenderLOD;
      return shouldRenderLOD;
   }

   protected void renderAircraftLOD(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, double posX, double posY, double posZ, float yaw, float pitch, float roll, float tickTime) {
      /*
       * This used to draw a tiny debug-like line silhouette (a plus sign with a box).
       * That proved hard to see and made distant vehicles look like placeholders, so
       * the far pass now renders the real vehicle model and simply omits the expensive
       * common extras/lights that are only useful up close.
       *
       * Fog is still disabled only around this far-model pass.  Otherwise Minecraft
       * fades the model into the horizon color right at the distance where the LOD is
       * supposed to become useful.  The matrix/attribute pushes keep this isolated from
       * normal close-range aircraft and world rendering.
       */
      double realDistance = Math.sqrt(posX * posX + posY * posY + posZ * posZ);
      MCH_VehicleLODProjection.Context projection = MCH_VehicleLODProjection.capture(Minecraft.getMinecraft(), realDistance);
      double depthScale = projection.depthScale;
      diagnoseTrackedLOD(ac, posX, posY, posZ, realDistance, projection);
      int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glPushMatrix();
      try {
         GL11.glDisable(GL11.GL_FOG);
         GL11.glEnable(GL11.GL_BLEND);
         GL11.glDisable(GL11.GL_ALPHA_TEST);
         GL11.glEnable(GL11.GL_DEPTH_TEST);
         GL11.glDepthMask(true);
         GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
         // Pattern A: this wrapper owns world placement; every vehicle renderer was
         // audited to translate by its position arguments and accepts zero here.
         GL11.glTranslated(posX * depthScale, posY * depthScale, posZ * depthScale);
         GL11.glScaled(depthScale, depthScale, depthScale);
         this.renderBaseVehicle(ac, 0.0D, 0.0D, 0.0D, yaw, pitch, roll, tickTime);
         this.renderAircraftLODParts(ac, info, 0.0D, 0.0D, 0.0D, tickTime);
      } finally {
         GL11.glPopMatrix();
         GL11.glPopAttrib();
         GL11.glMatrixMode(previousMatrixMode);
      }
   }

   private static void diagnoseTrackedLOD(MCH_EntityBaseVehicle ac, double posX, double posY, double posZ,
      double realDistance, MCH_VehicleLODProjection.Context projection) {
      if(MCH_Config.DebugVehicleLODVisibility == null || !MCH_Config.DebugVehicleLODVisibility.prmBool) return;
      long now = System.currentTimeMillis();
      Integer id = Integer.valueOf(ac.getEntityId());
      Long previous = (Long)vehicleLODDiagnosticTimes.get(id);
      if(previous != null && now - previous.longValue() < 1000L) return;
      vehicleLODDiagnosticTimes.put(id, Long.valueOf(now));
      Minecraft mc = Minecraft.getMinecraft();
      Entity camera = mc.renderViewEntity;
      double dx = camera == null ? posX : ac.posX - camera.posX;
      double dy = camera == null ? posY : ac.posY - camera.posY;
      double dz = camera == null ? posZ : ac.posZ - camera.posZ;
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      boolean eligible = ac.isInRangeToRenderDist(realDistance * realDistance);
      boolean snapshotReceived = MCH_VehicleLODManager.INSTANCE.hasSnapshotFor(ac);
      int glError = GL11.glGetError();
      String type = ac.getAcInfo() == null ? ac.getClass().getName() : ac.getAcInfo().name;
      MCH_Lib.DbgLog(true,
         "TrackedVehicleLOD id=%d type=%s camera=(%.2f,%.2f,%.2f) vehicle=(%.2f,%.2f,%.2f) horizontal=%.2f vertical=%.2f distance3d=%.2f renderer=(%.2f,%.2f,%.2f) doRender=true path=lod eligible=%s renderChunks=%d projectionValid=%s projection=[%.6f,%.6f,%.6f,%.6f] farPlane=%.2f safeDepth=%.2f depthScale=%.6f watchedChunk=true snapshotReceived=%s snapshotSuppressed=%s final=(%.2f,%.2f,%.2f) modelScale=%.6f glError=%d",
         new Object[]{id, type, Double.valueOf(camera == null ? Double.NaN : camera.posX),
            Double.valueOf(camera == null ? Double.NaN : camera.posY), Double.valueOf(camera == null ? Double.NaN : camera.posZ),
            Double.valueOf(ac.posX), Double.valueOf(ac.posY), Double.valueOf(ac.posZ), Double.valueOf(horizontal),
            Double.valueOf(Math.abs(dy)), Double.valueOf(realDistance), Double.valueOf(posX), Double.valueOf(posY),
            Double.valueOf(posZ), Boolean.valueOf(eligible), Integer.valueOf(mc.gameSettings.renderDistanceChunks),
            Boolean.valueOf(projection.validProjection), Float.valueOf(projection.projection[0]),
            Float.valueOf(projection.projection[5]), Float.valueOf(projection.projection[10]),
            Float.valueOf(projection.projection[14]), Double.valueOf(projection.farPlane),
            Double.valueOf(projection.safeProxyDepth), Double.valueOf(projection.depthScale),
            Boolean.valueOf(snapshotReceived), Boolean.valueOf(snapshotReceived),
            Double.valueOf(posX * projection.depthScale), Double.valueOf(posY * projection.depthScale),
            Double.valueOf(posZ * projection.depthScale), Double.valueOf(projection.depthScale), Integer.valueOf(glError)});
   }

   /**
    * Renders dynamic parts required by a vehicle's far-model pass.  The base model
    * renderer intentionally leaves the vehicle translation and interpolated hull
    * rotations on the current matrix, just as it does before the normal common-part
    * pass.  Subclasses can therefore reuse the normal part renderers without
    * duplicating animation calculations.
    */
   protected void renderAircraftLODParts(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, double posX, double posY, double posZ, float tickTime) {
   }

   public static boolean shouldSkipRender(Entity entity) {
      if(entity instanceof MCH_IEntityCanRideBaseVehicle) {
         MCH_IEntityCanRideBaseVehicle e = (MCH_IEntityCanRideBaseVehicle)entity;
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

   public static void renderLight(double x, double y, double z, float tickTime, MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info) {
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
               MCH_BaseVehicleInfo.SearchLight sl = (MCH_BaseVehicleInfo.SearchLight)i$.next();
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

   protected void bindTexture(String path, MCH_EntityBaseVehicle ac) {
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
         try {
            activeBaseTexture = new ResourceLocation(W_MOD.DOMAIN, getBaseTexturePath(path));
            activeBaseTexture = mcheli.texture.MCH_ModelTextureRepairManager.resolve(activeBaseTexture,
                  ac.getAcInfo() != null ? ac.getAcInfo().model : null,
                  ac.getAcInfo() != null ? ac.getAcInfo().getDirectoryName() + "/" + ac.getAcInfo().name : "unknown");
            super.bindTexture(activeBaseTexture);
         } catch (Exception var4) {
            System.out.println("Error loading texture: " + path + " (" + var4.getMessage() + ")"); //why the fuck is this happening
            super.bindTexture(new ResourceLocation(W_MOD.DOMAIN, "textures/test.png"));
         }
      }
   }

   protected void renderBodyWithSkinOverlay(IModelCustom model, String directory, MCH_EntityBaseVehicle ac) {
      renderBody(model);
   }

   private static void beginSkinOverlayRender(MCH_BaseVehicleInfo info, MCH_EntityBaseVehicle ac) {
      beginSkinOverlayRender(info.getDirectoryName(), ac.getTextureName());
   }

   public static void beginSkinOverlayRender(String directory, String textureName) {
      activeSkinOverlayTexture = null;
      activeBaseTexture = null;
      String overlayTextureName = getSkinOverlayTextureName(textureName);
      if(overlayTextureName != null && !overlayTextureName.isEmpty()) {
         activeSkinOverlayTexture = new ResourceLocation(W_MOD.DOMAIN, MCH_EntityBaseVehicle.getTexturePath(directory, overlayTextureName));
      }
   }

   public static void endSkinOverlayRender() {
      activeSkinOverlayTexture = null;
      activeBaseTexture = null;
   }

   private static void renderSkinOverlayPass(RenderRunnable renderer) {
      if(activeSkinOverlayTexture == null) {
         return;
      }

      GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_POLYGON_BIT);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glDisable(GL11.GL_ALPHA_TEST);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glDepthMask(false);
      GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
      GL11.glPolygonOffset(-1.0F, -1.0F);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      Minecraft.getMinecraft().renderEngine.bindTexture(activeSkinOverlayTexture);
      renderer.render();
      GL11.glPopAttrib();
      GL11.glDepthMask(true);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      if(activeBaseTexture != null) {
         Minecraft.getMinecraft().renderEngine.bindTexture(activeBaseTexture);
      }
   }

   private interface RenderRunnable {
      void render();
   }

   public static String getBaseTexturePath(String path) {
      int overlaySeparator = path != null ? path.indexOf("|skinoverlays/") : -1;
      return overlaySeparator >= 0 ? path.substring(0, overlaySeparator) + ".png" : path;
   }

   public static String getBaseTextureName(String textureName) {
      int overlaySeparator = textureName != null ? textureName.indexOf("|skinoverlays/") : -1;
      return overlaySeparator >= 0 ? textureName.substring(0, overlaySeparator) : textureName;
   }

   private static String getSkinOverlayTextureName(String textureName) {
      int overlaySeparator = textureName != null ? textureName.indexOf("|skinoverlays/") : -1;
      return overlaySeparator >= 0 ? textureName.substring(overlaySeparator + 1) : null;
   }

   public void renderRiddenEntity(MCH_EntityBaseVehicle ac, float tickTime, float yaw, float pitch, float roll, float width, float height) {
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

   public void renderEntitySimple(MCH_EntityBaseVehicle ac, Entity entity, float tickTime, float yaw, float pitch, float roll, float width, float height) {
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
            if(!W_Lib.isEntityLivingBase(entity) && !(entity instanceof MCH_IEntityCanRideBaseVehicle)) {
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

   public abstract void renderBaseVehicle(MCH_EntityBaseVehicle var1, double var2, double var4, double var6, float var8, float var9, float var10, float var11);

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

   public void renderDebugHitBox(MCH_EntityBaseVehicle e, double x, double y, double z, float yaw, float pitch, float roll) {
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
         GL11.glRotatef(yaw, 0.0F, -1.0F, 0.0F);
         GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(roll, 0.0F, 0.0F, 1.0F);
         MCH_BoundingBox[] arr$ = e.getCalculatedExtraBoundingBoxes();
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_BoundingBox bb = arr$[i$];
            GL11.glPushMatrix();
            GL11.glTranslated(bb.offsetX, bb.offsetY, bb.offsetZ);
            GL11.glPushMatrix();
            GL11.glScalef(bb.width, bb.height, bb.depth);
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

   public void renderDebugPilotSeat(MCH_EntityBaseVehicle e, double x, double y, double z, float yaw, float pitch, float roll) {
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

   public static void renderBody(final IModelCustom model) {
      if(model != null) {
         renderBodyModel(model);
         renderSkinOverlayPass(new RenderRunnable() {
            public void render() {
               renderBodyModel(model);
            }
         });
      }

   }

   /**
    * Renders a popped tank's chassis without the canonical turret or the configured
    * main-gun groups. The same runnable is used for the base texture and skin overlay,
    * so neither pass can put the attached assembly back on the wreck.
   */
   public static void renderTankBodyWithoutPoppedTurret(final IModelCustom model,
         MCH_BaseVehicleInfo.PartWeapon turretRoot) {
      if(model == null) return;
      final String[] excluded = getDetachedTankTurretGroups(model, turretRoot);
      renderTankBodyModelWithoutPoppedTurret(model, excluded);
      renderSkinOverlayPass(new RenderRunnable() {
         public void render() {
            renderTankBodyModelWithoutPoppedTurret(model, excluded);
         }
      });
   }

   private static void renderTankBodyModelWithoutPoppedTurret(IModelCustom model, String[] excluded) {
      if(model instanceof W_ModelCustom) {
         W_ModelCustom custom = (W_ModelCustom)model;
         if(custom.containsPart("$body")) {
            custom.renderPart("$body");
         } else {
            custom.renderAllExcept(excluded);
         }
      } else {
         // Models without named-part support cannot contain the canonical $turret.
         model.renderAll();
      }
   }

   private static String[] getDetachedTankTurretGroups(IModelCustom model,
         MCH_BaseVehicleInfo.PartWeapon root) {
      java.util.List groups = new java.util.ArrayList();
      if(root != null && model instanceof W_ModelCustom) {
         W_ModelCustom custom = (W_ModelCustom)model;
         String rootName = "$" + root.modelName;
         if(custom.containsPart(rootName)) groups.add(rootName);
         for(Object object : root.child) {
            String childName = "$" + ((MCH_BaseVehicleInfo.PartWeaponChild)object).modelName;
            if(custom.containsPart(childName)) groups.add(childName);
         }
         // Some packs provide a separate shell around the configured main gun.
         if(custom.containsPart("$turret")) groups.add("$turret");
      }
      return (String[])groups.toArray(new String[groups.size()]);
   }

   /** True only when rendering the body separately cannot also draw named dynamic groups. */
   public static boolean hasSeparableBody(IModelCustom model) {
      return model instanceof W_ModelCustom && ((W_ModelCustom)model).containsPart("$body");
   }

   public static void renderSnapshotWeapon(MCH_BaseVehicleInfo info, MCH_BaseVehicleInfo.PartWeapon w,
         mcheli.network.packets.PacketVehicleLODSnapshot.WeaponPose pose, float tickTime) {
      if(info == null || w == null || pose == null || !pose.visible) return;
      GL11.glPushMatrix();
      try {
         float turretYaw = interpolateSnapshotAngle(pose.prevTurretYaw, pose.turretYaw, tickTime);
         if(w.turret) {
            GL11.glTranslated(info.turretPosition.xCoord, info.turretPosition.yCoord, info.turretPosition.zCoord);
            GL11.glRotatef(turretYaw, 0.0F, -1.0F, 0.0F);
            GL11.glTranslated(-info.turretPosition.xCoord, -info.turretPosition.yCoord, -info.turretPosition.zCoord);
         }
         GL11.glTranslated(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord);
         if(w.yaw) GL11.glRotatef(interpolateSnapshotAngle(pose.prevYaw, pose.yaw, tickTime), 0.0F, -1.0F, 0.0F);
         if(w.turret) GL11.glRotatef(-(turretYaw - pose.rotationTurretYaw), 0.0F, -1.0F, 0.0F);
         boolean reversePitch = false;
         if((int)pose.defaultRotationYaw != 0) {
            float wrapped = MathHelper.wrapAngleTo180_float(pose.defaultRotationYaw);
            reversePitch = wrapped >= 45.0F && wrapped <= 135.0F || wrapped <= -45.0F && wrapped >= -135.0F;
            GL11.glRotatef(-pose.defaultRotationYaw, 0.0F, -1.0F, 0.0F);
         }
         if(w.pitch) {
            float pitch = pose.prevPitch + (pose.pitch - pose.prevPitch) * tickTime;
            GL11.glRotatef(reversePitch ? -pitch : pitch, 1.0F, 0.0F, 0.0F);
         }
         if(w.recoilBuf != 0.0F) {
            float recoil = pose.prevRecoil + (pose.recoil - pose.prevRecoil) * tickTime;
            GL11.glTranslated(0.0D, 0.0D, (double)(w.recoilBuf * recoil));
         }
         GL11.glRotatef(pose.defaultRotationYaw, 0.0F, -1.0F, 0.0F);
         if(w.rotBarrel) {
            GL11.glRotatef(interpolateSnapshotAngle(pose.prevBarrelRotation, pose.barrelRotation, tickTime),
               (float)w.rot.xCoord, (float)w.rot.yCoord, (float)w.rot.zCoord);
         }
         GL11.glTranslated(-w.pos.xCoord, -w.pos.yCoord, -w.pos.zCoord);
         renderPart(w.model, info.model, w.modelName);
         for(int i = 0; i < w.child.size(); ++i) {
            MCH_BaseVehicleInfo.PartWeaponChild child = (MCH_BaseVehicleInfo.PartWeaponChild)w.child.get(i);
            GL11.glPushMatrix();
            try {
               renderSnapshotWeaponChild(info, child, pose, tickTime);
            } finally {
               GL11.glPopMatrix();
            }
         }
      } finally {
         GL11.glPopMatrix();
      }
   }

   private static void renderSnapshotWeaponChild(MCH_BaseVehicleInfo info, MCH_BaseVehicleInfo.PartWeaponChild w,
         mcheli.network.packets.PacketVehicleLODSnapshot.WeaponPose pose, float tickTime) {
      GL11.glTranslated(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord);
      if(w.yaw) GL11.glRotatef(interpolateSnapshotAngle(pose.prevYaw, pose.yaw, tickTime), 0.0F, -1.0F, 0.0F);
      float wrapped = MathHelper.wrapAngleTo180_float(pose.defaultRotationYaw);
      boolean reversePitch = wrapped >= 45.0F && wrapped <= 135.0F || wrapped <= -45.0F && wrapped >= -135.0F;
      if((int)pose.defaultRotationYaw != 0) GL11.glRotatef(-pose.defaultRotationYaw, 0.0F, -1.0F, 0.0F);
      if(w.pitch) {
         float pitch = pose.prevPitch + (pose.pitch - pose.prevPitch) * tickTime;
         GL11.glRotatef(reversePitch ? -pitch : pitch, 1.0F, 0.0F, 0.0F);
      }
      if(w.recoilBuf != 0.0F) {
         float recoil = pose.prevRecoil + (pose.recoil - pose.prevRecoil) * tickTime;
         GL11.glTranslated(0.0D, 0.0D, (double)(-w.recoilBuf * recoil));
      }
      GL11.glRotatef(pose.defaultRotationYaw, 0.0F, -1.0F, 0.0F);
      GL11.glTranslated(-w.pos.xCoord, -w.pos.yCoord, -w.pos.zCoord);
      renderPart(w.model, info.model, w.modelName);
   }

   private static float interpolateSnapshotAngle(float previous, float current, float partial) {
      float delta = MathHelper.wrapAngleTo180_float(current - previous);
      return previous + delta * partial;
   }

   public static void renderAllModel(final IModelCustom model) {
      if(model != null) {
         model.renderAll();
         renderSkinOverlayPass(new RenderRunnable() {
            public void render() {
               model.renderAll();
            }
         });
      }

   }

   private static void renderBodyModel(IModelCustom model) {
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

   public static void renderPart(final IModelCustom model, final IModelCustom modelBody, final String partName) {
      renderPartModelTransformed(model, modelBody, partName);
      renderSkinOverlayPass(new RenderRunnable() {
         public void render() {
            renderPartModelTransformed(model, modelBody, partName);
         }
      });

   }

   private static void renderPartModel(IModelCustom model, IModelCustom modelBody, String partName) {
      if(model != null) {
         model.renderAll();
      } else if(modelBody instanceof W_ModelCustom && ((W_ModelCustom)modelBody).containsPart("$" + partName)) {
         modelBody.renderPart("$" + partName);
      }
   }

   private static void renderPartModelTransformed(IModelCustom model, IModelCustom modelBody, String partName) {
      if(ANGELICA_DYNAMIC_PART_COMPAT && model instanceof W_ModelCustom) {
         logAngelicaDynamicPartRender((W_ModelCustom)model, partName, "external", "compat-tessellator");
         ((W_ModelCustom)model).renderAllTransformed();
      } else if(model != null) {
         logAngelicaDynamicPartRender(model, partName, "external", "vbo");
         model.renderAll();
      } else if(modelBody instanceof W_ModelCustom && ((W_ModelCustom)modelBody).containsPart("$" + partName)) {
         if(ANGELICA_DYNAMIC_PART_COMPAT) {
            logAngelicaDynamicPartRender(modelBody, partName, "body-part", "compat-tessellator");
            ((W_ModelCustom)modelBody).renderPartTransformed("$" + partName);
         } else {
            logAngelicaDynamicPartRender(modelBody, partName, "body-part", "vbo");
            modelBody.renderPart("$" + partName);
         }
      }
   }

   private static void logAngelicaDynamicPartRender(IModelCustom model, String partName, String modelType, String path) {
      if(!DEBUG_ANGELICA_DYNAMIC_PART_RENDER || !ANGELICA_DYNAMIC_PART_COMPAT) {
         return;
      }

      String key = model.getClass().getName() + ":" + modelType + ":" + partName + ":" + path;
      if(angelicaDynamicPartRenderDiagnostics.add(key)) {
         MCH_Lib.Log("[AngelicaDynamicPartRender] angelica=true model=%s modelType=%s group=%s transformedDynamicPart=true path=%s", new Object[]{model.getClass().getName(), modelType, partName, path});
      }
   }

   public void renderCommonPart(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, double x, double y, double z, float tickTime) {
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

   public static void renderLightHatch(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, float tickTime) {
      if(info.lightHatchList.size() > 0) {
         float rot = ac.prevRotLightHatch + (ac.rotLightHatch - ac.prevRotLightHatch) * tickTime;
         Iterator i$ = info.lightHatchList.iterator();

         while(i$.hasNext()) {
            MCH_BaseVehicleInfo.Hatch t = (MCH_BaseVehicleInfo.Hatch)i$.next();
            GL11.glPushMatrix();
            GL11.glTranslated(t.pos.xCoord, t.pos.yCoord, t.pos.zCoord);
            GL11.glRotated((double)(rot * t.maxRot), t.rot.xCoord, t.rot.yCoord, t.rot.zCoord);
            GL11.glTranslated(-t.pos.xCoord, -t.pos.yCoord, -t.pos.zCoord);
            renderPart(t.model, info.model, t.modelName);
            GL11.glPopMatrix();
         }

      }
   }

   public static void renderSteeringWheel(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, float tickTime) {
      if(info.partSteeringWheel.size() > 0) {
         float rot = ac.prevRotYawWheel + (ac.rotYawWheel - ac.prevRotYawWheel) * tickTime;
         Iterator i$ = info.partSteeringWheel.iterator();

         while(i$.hasNext()) {
            MCH_BaseVehicleInfo.PartWheel t = (MCH_BaseVehicleInfo.PartWheel)i$.next();
            GL11.glPushMatrix();
            GL11.glTranslated(t.pos.xCoord, t.pos.yCoord, t.pos.zCoord);
            GL11.glRotated((double)(rot * t.rotDir), t.rot.xCoord, t.rot.yCoord, t.rot.zCoord);
            GL11.glTranslated(-t.pos.xCoord, -t.pos.yCoord, -t.pos.zCoord);
            renderPart(t.model, info.model, t.modelName);
            GL11.glPopMatrix();
         }

      }
   }

   public static void renderWheel(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, float tickTime) {
      renderWheel(info, ac.rotWheel, ac.prevRotWheel, ac.rotYawWheel, ac.prevRotYawWheel, tickTime);
   }

   public static void renderWheel(MCH_BaseVehicleInfo info, float wheelRotation, float previousWheelRotation,
         float wheelYaw, float previousWheelYaw, float tickTime) {
      if(info.partWheel.size() > 0) {
         float yaw = interpolateSnapshotAngle(previousWheelYaw, wheelYaw, tickTime);
         float rotation = interpolateSnapshotAngle(previousWheelRotation, wheelRotation, tickTime);
         Iterator i$ = info.partWheel.iterator();

         while(i$.hasNext()) {
            MCH_BaseVehicleInfo.PartWheel t = (MCH_BaseVehicleInfo.PartWheel)i$.next();
            GL11.glPushMatrix();
            GL11.glTranslated(t.pos2.xCoord, t.pos2.yCoord, t.pos2.zCoord);
            GL11.glRotated((double)(yaw * t.rotDir), t.rot.xCoord, t.rot.yCoord, t.rot.zCoord);
            GL11.glTranslated(-t.pos2.xCoord, -t.pos2.yCoord, -t.pos2.zCoord);
            GL11.glTranslated(t.pos.xCoord, t.pos.yCoord, t.pos.zCoord);
            GL11.glRotatef(rotation, 1.0F, 0.0F, 0.0F);
            GL11.glTranslated(-t.pos.xCoord, -t.pos.yCoord, -t.pos.zCoord);
            renderPart(t.model, info.model, t.modelName);
            GL11.glPopMatrix();
         }

      }
   }

   public static void renderRotPart(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, float tickTime) {
      if(ac.haveRotPart()) {
         for(int i = 0; i < ac.rotPartRotation.length; ++i) {
            float rot = ac.rotPartRotation[i];
            float prevRot = ac.prevRotPartRotation[i];
            if(prevRot > rot) {
               rot += 360.0F;
            }

            rot = MCH_Lib.smooth(rot, prevRot, tickTime);
            MCH_BaseVehicleInfo.RotPart h = (MCH_BaseVehicleInfo.RotPart)info.partRotPart.get(i);
            GL11.glPushMatrix();
            GL11.glTranslated(h.pos.xCoord, h.pos.yCoord, h.pos.zCoord);
            GL11.glRotatef(rot, (float)h.rot.xCoord, (float)h.rot.yCoord, (float)h.rot.zCoord);
            GL11.glTranslated(-h.pos.xCoord, -h.pos.yCoord, -h.pos.zCoord);
            renderPart(h.model, info.model, h.modelName);
            GL11.glPopMatrix();
         }

      }
   }

   public static void renderWeapon(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, float tickTime) {
      renderWeapon(ac, info, tickTime, false);
   }

   public static void renderDetachedTurretWeapon(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, float tickTime) {
      renderWeapon(ac, info, tickTime, true);
   }

   /** Draws only cached detached groups with their frozen destruction pose. */
   public static void renderDetachedTankTurret(final mcheli.tank.MCH_EntityTank tank,
         final MCH_TurretPopModelCache.Entry entry) {
      if(entry == null) return;
      renderDetachedTankTurretModel(tank, entry);
      renderSkinOverlayPass(new RenderRunnable() {
         public void render() { renderDetachedTankTurretModel(tank, entry); }
      });
   }

   private static void renderDetachedTankTurretModel(mcheli.tank.MCH_EntityTank tank,
         MCH_TurretPopModelCache.Entry entry) {
      MCH_BaseVehicleInfo.PartWeapon root = entry.mainGun;
      GL11.glPushMatrix();
      try {
         GL11.glRotatef(tank.turretPopFrozenYaw, 0.0F, -1.0F, 0.0F);
         // The world origin is already the flying pivot; recenter source geometry
         // instead of translating to turretPosition and applying that pivot twice.
         GL11.glTranslated(-entry.pivot.xCoord, -entry.pivot.yCoord, -entry.pivot.zCoord);
         entry.detached.renderPart("$turret");
         if(root == null) return;
         GL11.glPushMatrix();
         try {
            applyFrozenTurretPitch(root.pos, root.pitch, tank.turretPopFrozenPitch);
            if(entry.detached.containsPart("$" + root.modelName)) entry.detached.renderPart("$" + root.modelName);
            // Children inherit the root transform, matching renderWeaponChild.
            for(Object object : root.child) {
               MCH_BaseVehicleInfo.PartWeaponChild child = (MCH_BaseVehicleInfo.PartWeaponChild)object;
               if(!entry.detached.containsPart("$" + child.modelName)) continue;
               GL11.glPushMatrix();
               try {
                  applyFrozenTurretPitch(child.pos, child.pitch, tank.turretPopFrozenPitch);
                  entry.detached.renderPart("$" + child.modelName);
               } finally { GL11.glPopMatrix(); }
            }
         } finally { GL11.glPopMatrix(); }
      } finally { GL11.glPopMatrix(); }
   }

   private static void applyFrozenTurretPitch(Vec3 pivot, boolean pitches, float frozenPitch) {
      if(!pitches) return;
      GL11.glTranslated(pivot.xCoord, pivot.yCoord, pivot.zCoord);
      GL11.glRotatef(frozenPitch, 1.0F, 0.0F, 0.0F);
      GL11.glTranslated(-pivot.xCoord, -pivot.yCoord, -pivot.zCoord);
   }

   private static void renderWeapon(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, float tickTime, boolean detachedOnly) {
      MCH_WeaponSet beforeWs = null;
      Entity e = ac.getRiddenByEntity();
      int weaponIndex = 0;
      int cnt = 0;
      Iterator i$ = info.partWeapon.iterator();

      while(i$.hasNext()) {
         MCH_BaseVehicleInfo.PartWeapon w = (MCH_BaseVehicleInfo.PartWeapon)i$.next();
         if(ac instanceof mcheli.tank.MCH_EntityTank && ((mcheli.tank.MCH_EntityTank)ac).turretPopStarted) {
            mcheli.tank.MCH_EntityTank tank = (mcheli.tank.MCH_EntityTank)ac;
            MCH_BaseVehicleInfo.PartWeapon root = tank.getTurretPopRoot();
            MCH_TurretPopModelCache.Entry popModel = info instanceof mcheli.tank.MCH_TankInfo
                  ? MCH_TurretPopModelCache.get((mcheli.tank.MCH_TankInfo)info, root) : null;
            if(popModel != null && ((detachedOnly && w != root) || (!detachedOnly && w == root))) continue;
            if(popModel == null && detachedOnly) continue;
         } else if(detachedOnly) {
            continue;
         }
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
               MCH_BaseVehicleInfo.PartWeaponChild var28 = (MCH_BaseVehicleInfo.PartWeaponChild)var27.next();
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

   public static void renderWeaponChild(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, MCH_BaseVehicleInfo.PartWeaponChild w, MCH_WeaponSet ws, Entity e, float tickTime) {
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

   public static void renderTrackRoller(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, float tickTime) {
      renderTrackRoller(info, ac.rotTrackRoller, ac.prevRotTrackRoller, tickTime);
   }

   public static void renderTrackRoller(MCH_BaseVehicleInfo info, float[] rot, float[] prevRot, float tickTime) {
      if(info.partTrackRoller.size() > 0) {
         Iterator i$ = info.partTrackRoller.iterator();

         while(i$.hasNext()) {
            MCH_BaseVehicleInfo.TrackRoller t = (MCH_BaseVehicleInfo.TrackRoller)i$.next();
            GL11.glPushMatrix();
            GL11.glTranslated(t.pos.xCoord, t.pos.yCoord, t.pos.zCoord);
            GL11.glRotatef(interpolateSnapshotAngle(prevRot[t.side], rot[t.side], tickTime), 1.0F, 0.0F, 0.0F);
            GL11.glTranslated(-t.pos.xCoord, -t.pos.yCoord, -t.pos.zCoord);
            renderPart(t.model, info.model, t.modelName);
            GL11.glPopMatrix();
         }

      }
   }

   public static void renderCrawlerTrack(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, float tickTime) {
      if(ac == null) {
         renderCrawlerTrack(info, tickTime);
         return;
      }

      float[] direction = new float[2];
      for(int side = 0; side < 2; ++side) direction[side] = wrappedPhaseDelta(ac.prevRotCrawlerTrack[side], ac.rotCrawlerTrack[side], 0.0F);
      renderCrawlerTrack(info, ac.rotCrawlerTrack, ac.prevRotCrawlerTrack, direction, tickTime);
   }

   public static void renderCrawlerTrack(MCH_BaseVehicleInfo info, float tickTime) {
      renderCrawlerTrack(info, STATIC_CRAWLER_TRACK_STATE, STATIC_CRAWLER_TRACK_STATE,
         STATIC_CRAWLER_TRACK_STATE, tickTime);
   }

   public static void renderCrawlerTrack(MCH_BaseVehicleInfo info, float[] phase, float[] previousPhase,
         float[] movementDirection, float tickTime) {
      if(info.partCrawlerTrack.size() > 0) {
         GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_POINT_BIT);
         Tessellator tessellator = Tessellator.instance;
         Iterator i$ = info.partCrawlerTrack.iterator();
         try {
         while(i$.hasNext()) {
            MCH_BaseVehicleInfo.CrawlerTrack c = (MCH_BaseVehicleInfo.CrawlerTrack)i$.next();
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
            double trackPhase = previousPhase[c.side]
               + wrappedPhaseDelta(previousPhase[c.side], phase[c.side], movementDirection[c.side]) * tickTime;
            trackPhase -= Math.floor(trackPhase);

            for(int i = 0; i < L; ++i) {
               MCH_BaseVehicleInfo.CrawlerTrackPrm cp = (MCH_BaseVehicleInfo.CrawlerTrackPrm)c.lp.get(i);
               MCH_BaseVehicleInfo.CrawlerTrackPrm np = (MCH_BaseVehicleInfo.CrawlerTrackPrm)c.lp.get((i + 1) % L);
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

               double x = x1 + (x2 - x1) * trackPhase;
               double y = y1 + (y2 - y1) * trackPhase;
               double r = r1 + (r2 - r1) * trackPhase;
               GL11.glPushMatrix();
               GL11.glTranslated(0.0D, x, y);
               GL11.glRotatef((float)r, -1.0F, 0.0F, 0.0F);
               renderPart(c.model, info.model, c.modelName);
               GL11.glPopMatrix();
            }
         }

         } finally {
            GL11.glPopAttrib();
         }
      }
   }

   private static float wrappedPhaseDelta(float previous, float current, float movementDirection) {
      float delta = current - previous;
      while(delta > 0.5F) delta -= 1.0F;
      while(delta < -0.5F) delta += 1.0F;
      if(Math.abs(Math.abs(delta) - 0.5F) < 0.0001F && movementDirection != 0.0F) {
         delta = movementDirection > 0.0F ? 0.5F : -0.5F;
      }
      return delta;
   }

   public static void renderHatch(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, float tickTime) {
      if(info.haveHatch() && ac.partHatch != null) {
         float rot = ac.getHatchRotation();
         float prevRot = ac.getPrevHatchRotation();
         Iterator i$ = info.hatchList.iterator();

         while(i$.hasNext()) {
            MCH_BaseVehicleInfo.Hatch h = (MCH_BaseVehicleInfo.Hatch)i$.next();
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

   public static void renderThrottle(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, float tickTime) {
      if(info.havePartThrottle()) {
         float throttle = MCH_Lib.smooth((float)ac.getCurrentThrottle(), (float)ac.getPrevCurrentThrottle(), tickTime);
         Iterator i$ = info.partThrottle.iterator();

         while(i$.hasNext()) {
            MCH_BaseVehicleInfo.Throttle h = (MCH_BaseVehicleInfo.Throttle)i$.next();
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

   public static void renderWeaponBay(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, float tickTime) {
      for(int i = 0; i < info.partWeaponBay.size(); ++i) {
         MCH_BaseVehicleInfo.WeaponBay w = (MCH_BaseVehicleInfo.WeaponBay)info.partWeaponBay.get(i);
         MCH_EntityBaseVehicle.WeaponBay ws = ac.weaponBays[i];
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

   public static void renderCamera(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, float tickTime) {
      if(info.havePartCamera()) {
         float rotYaw = ac.camera.partRotationYaw;
         float prevRotYaw = ac.camera.prevPartRotationYaw;
         float rotPitch = ac.camera.partRotationPitch;
         float prevRotPitch = ac.camera.prevPartRotationPitch;
         float yaw = prevRotYaw + (rotYaw - prevRotYaw) * tickTime - ac.getRotYaw();
         float pitch = prevRotPitch + (rotPitch - prevRotPitch) * tickTime - ac.getRotPitch();
         Iterator i$ = info.cameraList.iterator();

         while(i$.hasNext()) {
            MCH_BaseVehicleInfo.Camera c = (MCH_BaseVehicleInfo.Camera)i$.next();
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

   public static void renderCanopy(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, float tickTime) {
      if(info.haveCanopy() && ac.partCanopy != null) {
         float rot = ac.getCanopyRotation();
         float prevRot = ac.getPrevCanopyRotation();
         Iterator i$ = info.canopyList.iterator();

         while(i$.hasNext()) {
            MCH_BaseVehicleInfo.Canopy c = (MCH_BaseVehicleInfo.Canopy)i$.next();
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

   public static void renderLandingGear(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, float tickTime) {
      if (info.haveLandingGear() && ac.partLandingGear != null) {
         renderLandingGear(info, ac.getLandingGearRotation(), ac.getPrevLandingGearRotation(), tickTime);
      }
   }

   public static void renderLandingGear(MCH_BaseVehicleInfo info, float rot, float prevRot, float tickTime) {
      if (info.haveLandingGear()) {
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
            MCH_BaseVehicleInfo.LandingGear n = (MCH_BaseVehicleInfo.LandingGear) i$.next();
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
            MCH_EntityBaseVehicle ac = null; //Entity ridden by the player
            if(player.ridingEntity instanceof MCH_EntityBaseVehicle) {
               ac = (MCH_EntityBaseVehicle)player.ridingEntity;
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
                     // Checks whether the current weapon has a guidance system and whether it can lock the target entity
                     if(gs.canLockEntity(entity)) {
                        RenderManager rm = RenderManager.instance;
                        // Calculates squared distance between target entity and player
                        double dist = entity.getDistanceSqToEntity(rm.livingPlayer);
                        double distance = Math.sqrt(dist);
                        if(wi != null && wi.enableBVR && distance > wi.minRangeBVR) {
                           return;
                        }
//                     if(entity instanceof MCH_EntityFlare) {
//                        long worldTime = Minecraft.getMinecraft().theWorld.getTotalWorldTime();
//                        float blinkBaseFrequency = 1.0F; // Base blink frequency (blinks once per second)
//                        float randomFrequencyFactor = 0.5F + rand.nextFloat() * 0.5F; // Random frequency range [0.5, 1.0]
//                        float blinkFrequency = blinkBaseFrequency * randomFrequencyFactor;
//                        float sinValue = (float) Math.sin(worldTime * blinkFrequency * Math.PI * 2.0F); // Sine wave function
//                        boolean isFlareVisible = sinValue > 0.0F; // Determines whether to show the box from the sine value
//                        if(!isFlareVisible) return;
//                     }
                        Vec3 src = Vec3.createVectorHelper(RenderManager.renderPosX, RenderManager.renderPosY, RenderManager.renderPosZ);
                        Vec3 dst = Vec3.createVectorHelper(entity.posX, entity.posY, entity.posZ);
                        MovingObjectPosition mop = player.worldObj.rayTraceBlocks(src, dst, true);
                        if(mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                           return;
                        }

                        // Calculates coordinate offset of the entity relative to the player
                        double x = entity.posX - RenderManager.renderPosX;
                        double y = entity.posY - RenderManager.renderPosY;
                        double z = entity.posZ - RenderManager.renderPosZ;

                        // Renders if target entity is within 1000 units of the player
                        if(dist < 1000000.0D) {
                           float scl = 0.02666667F; // Scale factor
                           GL11.glPushMatrix();
                           // Transforms position to render target entity in the player view
                           GL11.glTranslatef((float)x, (float)y + entity.height + 2F, (float)z);
                           GL11.glNormal3f(0.0F, 1.0F, 0.0F);
                           GL11.glRotatef(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
                           GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
                           GL11.glScalef(-0.02666667F, -0.02666667F, 0.02666667F);
                           GL11.glDisable(2896); // Disables depth testing
                           GL11.glTranslatef(0.0F, 9.374999F, 0.0F); // Moves up by an offset
                           GL11.glDepthMask(false); // Disables depth writes
                           GL11.glEnable(3042); // Enables blending
                           GL11.glBlendFunc(770, 771); // Sets blend mode
                           GL11.glDisable(3553); // Disables textures
                           GL11.glDisable(2929 /* GL_DEPTH_TEST */);

                           // Gets screen width before drawing
                           int prevWidth = GL11.glGetInteger(2849);
                           // Sets target entity size (adjusted based on entity width and height)
                           float size1 = Math.max(entity.width, entity.height) * 20.0F;
                           if(entity instanceof MCH_EntityBaseVehicle
                                   || entity instanceof MCH_EntityFlare
                                   || entity instanceof MCH_EntityChaff) {
                              size1 *= 2.0F; // Doubles size for aircraft-type entities
                           }
                           float size = size1 + (float)((distance - 10.0D) / (300.0D - 10.0D)) * (300.0F - size1);
                           // Ensures font size is between size1 and 100
                           size = Math.max(size1, Math.min(300.0F, size));

                           // Creates a Tessellator object for drawing graphics
                           Tessellator tessellator = Tessellator.instance;
                           tessellator.startDrawing(2); // Starts drawing lines
                           tessellator.setBrightness(240); // Sets brightness

                           Vector3f playerVelocity = new Vector3f(ac.motionX, ac.motionY, ac.motionZ);  // Velocity vector of the player aircraft
                           Vector3f targetVelocity = new Vector3f(entity.motionX, entity.motionY, entity.motionZ);  // Velocity vector of the target aircraft
                           float angleInDegrees = 0;
                           if(playerVelocity.length() > 0.001 && targetVelocity.length() > 0.001) {
                              // Calculates the dot product of the two vectors
                              float dotProduct = Vector3f.dot(playerVelocity, targetVelocity);
                              // Calculates lengths of the two vectors
                              float playerSpeed = playerVelocity.length();
                              float targetSpeed = targetVelocity.length();
                              // Calculates the cosine of the angle
                              float cosAngle = dotProduct / (playerSpeed * targetSpeed);
                              // Ensures the angle cosine is within the valid range [-1, 1],avoids abnormal values caused by floating-point error
                              cosAngle = Math.max(-1.0f, Math.min(1.0f, cosAngle));
                              // Calculates the angle (radians)
                              float angle = (float) Math.acos(cosAngle);
                              // If the angle is greater than 90 degrees, converts it to an acute angle (within 90 degrees)
                              if (angle > Math.PI / 2) {
                                 angle = (float) (Math.PI - angle);  // Converts to an acute angle
                              }
                              // Converts angle to degrees (optional)
                              angleInDegrees = (float) Math.toDegrees(angle);
                           }

                           // Checks whether the target entity is currently locked
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
                              GL11.glLineWidth((float)MCH_Gui.scaleFactor * 2.5F); // Sets line width
                              tessellator.setColorRGBA_F(1.0F, 0.0F, 0.0F, alpha); // Shows red while locked
                           } else {
                              GL11.glLineWidth((float)MCH_Gui.scaleFactor * 1.5F); // Sets line width
                              tessellator.setColorRGBA_F(0.0F, 1.0F, 0.0F, alpha); // Green
                           }

                           // Draws a rectangle representing the lock range
                           tessellator.addVertex(-size - 1.0F, 0.0D, 0.0D);
                           tessellator.addVertex(-size - 1.0F, size * 2.0F, 0.0D);
                           tessellator.addVertex(size + 1.0F, size * 2.0F, 0.0D);
                           tessellator.addVertex(size + 1.0F, 0.0D, 0.0D);
                           tessellator.draw(); // Draws lines

                           // Gets distance and draws text
                           String distanceText = String.format("%.1f", distance); // Formats with one decimal place

                           // Gets the FontRenderer object and sets the color to green
                           FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
                           {
                              GL11.glPushMatrix();
                              GL11.glTranslatef(0.0F, size * 2.0F + 1.0F, 0.0F); // Places text below the rectangle
                              float fontSize = 5.0F + (float)((distance - 10.0D) / (300.0D - 10.0D)) * (40.0F - 5.0F);
                              // Ensures font size is between 5 and 40
                              fontSize = Math.max(5.0F, Math.min(40.0F, fontSize));
                              GL11.glScalef(fontSize, fontSize, fontSize);
                              // Draws green text showing distance to target
                              String text = "";
                              if (gs.isHeatSeekerMissile) {
                                 text = "HEAT";
                              } else if (gs.isRadarMissile) {
                                 if (entity instanceof MCH_EntityBaseVehicle) {
                                    MCH_EntityBaseVehicle entityAircraft = (MCH_EntityBaseVehicle) entity;
                                    text = entityAircraft.getNameOnOtherRadar(ac);
                                 } else {
                                    text = "?";
                                 }
                              }
                              text += " " + distanceText;

                              if(ac instanceof MCP_EntityPlane && entity instanceof MCP_EntityPlane && angleInDegrees != 0) {
                                 // Outputs the angle value as text
                                 String angleText = String.format("%.1f", angleInDegrees);  // Keeps one decimal place
                                 text += " " + angleText;
                              }

                              fontRenderer.drawString(text, -fontRenderer.getStringWidth(text) / 2, 0, 0x00ff00);

                              GL11.glPopMatrix();
                           }

                           GL11.glPopMatrix();



                           // Draws a connecting line if the entity is a UAV, current view is first person, and target entity is locked
                           if(!ac.isUAV() && isLockEntity && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
                              GL11.glPushMatrix();
                              tessellator.startDrawing(1); // Draws a point-to-point line
                              GL11.glLineWidth(1.0F);
                              tessellator.setColorRGBA_F(1.0F, 0.0F, 0.0F, 1.0F); // Sets color to red
                              // Connecting line goes from entity center to aircraft previous position
                              tessellator.addVertex(x, y + (double)(entity.height / 2.0F), z);
                              tessellator.addVertex(ac.lastTickPosX - RenderManager.renderPosX, ac.lastTickPosY - RenderManager.renderPosY - 1.0D, ac.lastTickPosZ - RenderManager.renderPosZ);
                              tessellator.setBrightness(240); // Sets brightness
                              tessellator.draw(); // Draws lines
                              GL11.glPopMatrix(); // Restores matrix state
                           }

                           // Restores previous line width, enables textures, restores depth writes and depth testing
                           GL11.glLineWidth((float)prevWidth);
                           GL11.glEnable(3553);
                           GL11.glDepthMask(true);
                           GL11.glEnable(2896);
                           GL11.glDisable(3042);
                           GL11.glEnable(2929 /* GL_DEPTH_TEST */);
                           GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F); // Restores default color
                        }
                     }
                  }



               }
            }
         }
      }
   }

   public static void renderRope(MCH_EntityBaseVehicle ac, MCH_BaseVehicleInfo info, double x, double y, double z, float tickTime) {
      GL11.glPushMatrix();
      Tessellator tessellator = Tessellator.instance;
      if(ac.isRepelling()) {
         GL11.glDisable(3553);
         GL11.glDisable(2896);

         for(int i = 0; i < info.repellingHooks.size(); ++i) {
            tessellator.startDrawing(3);
            tessellator.setColorOpaque_I(0);
            tessellator.addVertex(((MCH_BaseVehicleInfo.RepellingHook)info.repellingHooks.get(i)).pos.xCoord, ((MCH_BaseVehicleInfo.RepellingHook)info.repellingHooks.get(i)).pos.yCoord, ((MCH_BaseVehicleInfo.RepellingHook)info.repellingHooks.get(i)).pos.zCoord);
            tessellator.addVertex(((MCH_BaseVehicleInfo.RepellingHook)info.repellingHooks.get(i)).pos.xCoord, ((MCH_BaseVehicleInfo.RepellingHook)info.repellingHooks.get(i)).pos.yCoord + (double)ac.ropesLength, ((MCH_BaseVehicleInfo.RepellingHook)info.repellingHooks.get(i)).pos.zCoord);
            tessellator.draw();
         }

         GL11.glEnable(2896);
         GL11.glEnable(3553);
      }

      GL11.glPopMatrix();
   }

}
