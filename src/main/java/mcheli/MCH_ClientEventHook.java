package mcheli;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import mcheli.MCH_ClientCommonTickHandler;
import mcheli.MCH_ClientTickHandlerBase;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.MCH_TextureManagerDummy;
import mcheli.MCH_ViewEntityDummy;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_RenderBaseVehicle;
import mcheli.lweapon.MCH_ClientLightWeaponTickHandler;
import mcheli.multiplay.MCH_GuiTargetMarker;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.plane.MCP_PlaneChaseCamera;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tool.rangefinder.MCH_ItemRangeFinder;
import mcheli.wrapper.W_ClientEventHook;
import mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent.Specials.Post;
import net.minecraftforge.client.event.RenderLivingEvent.Specials.Pre;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent.Unload;
import org.lwjgl.opengl.GL11;

//used in common tick handler, clientproxy and renderaircraft
public class MCH_ClientEventHook extends W_ClientEventHook {

   MCH_TextureManagerDummy dummyTextureManager = null;
   public static List haveSearchLightAircraft = new ArrayList();
   //I think this is for search lights
   private static final ResourceLocation ir_strobe = new ResourceLocation("mcheli", "textures/ir_strobe.png");
   private static boolean cancelRender = true;

   public static float smoothing;
   private static long nextPlaneMouseAimReticleDebugTime;
   private static final ResourceLocation PLANE_MOUSE_AIM_RETICLE_TEXTURE =
         new ResourceLocation("mcheli", "textures/gui/plane_crosshair.png");

   public void renderLivingEventSpecialsPre(Pre event) {
      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.DisableRenderLivingSpecials.prmBool) {
         MCH_EntityBaseVehicle ac = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(Minecraft.getMinecraft().thePlayer);
         if(ac != null && ac.isMountedEntity(event.entity)) {
            event.setCanceled(true);
            return;
         }
      }

   }

   public void renderLivingEventSpecialsPost(Post event) {}

   private void renderIRStrobe(EntityLivingBase entity, Post event) {
      int cm = MCH_ClientCommonTickHandler.cameraMode;
      if(cm != 0) {
         int ticks = entity.ticksExisted % 20;
         if(ticks < 4) {
            float alpha = ticks != 2 && ticks != 1?0.5F:1.0F;
            EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
            if(player != null) {
               if(player.isOnSameTeam(entity)) {
                  short j = 240;
                  short k = 240;
                  OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)j / 1.0F, (float)k / 1.0F);
                  GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                  float f1 = 0.080000006F;
                  GL11.glPushMatrix();
                  GL11.glTranslated(event.x, event.y + (double)((float)((double)entity.height * 0.75D)), event.z);
                  GL11.glNormal3f(0.0F, 1.0F, 0.0F);
                  GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
                  GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
                  GL11.glScalef(-f1, -f1, f1);
                  GL11.glEnable(3042);
                  OpenGlHelper.glBlendFunc(770, 771, 1, 0);
                  GL11.glEnable(3553);
                  RenderManager.instance.renderEngine.bindTexture(ir_strobe);
                  GL11.glAlphaFunc(516, 0.003921569F);
                  Tessellator tessellator = Tessellator.instance;
                  tessellator.startDrawingQuads();
                  tessellator.setColorRGBA_F(1.0F, 1.0F, 1.0F, alpha * (cm == 1?0.9F:0.5F));
                  int i = (int)Math.max(entity.width, entity.height) * 20;
                  tessellator.addVertexWithUV((double)(-i), (double)(-i), 0.1D, 0.0D, 0.0D);
                  tessellator.addVertexWithUV((double)(-i), (double)i, 0.1D, 0.0D, 1.0D);
                  tessellator.addVertexWithUV((double)i, (double)i, 0.1D, 1.0D, 1.0D);
                  tessellator.addVertexWithUV((double)i, (double)(-i), 0.1D, 1.0D, 0.0D);
                  tessellator.draw();
                  GL11.glEnable(2896);
                  GL11.glPopMatrix();
               }
            }
         }
      }
   }

   public void mouseEvent(MouseEvent event) {
      if(MCH_ClientTickHandlerBase.updateMouseWheel(event.dwheel)) {
         event.setCanceled(true);
      }

   }

   public static void setCancelRender(boolean cancel) {
      cancelRender = cancel;
   }

   public void renderLivingEventPre(net.minecraftforge.client.event.RenderLivingEvent.Pre event) {
      Iterator rm = haveSearchLightAircraft.iterator();

      while(rm.hasNext()) {
         MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)rm.next();
         //System.out.println("what the hell does this do");
         //spam
         OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, ac.getSearchLightValue(event.entity), 240.0F);
      }

      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.EnableModEntityRender.prmBool && cancelRender && (event.entity.ridingEntity instanceof MCH_EntityBaseVehicle || event.entity.ridingEntity instanceof MCH_EntitySeat)) {
         event.setCanceled(true);
      } else {
         var10000 = MCH_MOD.config;
         if(MCH_Config.EnableReplaceTextureManager.prmBool) {
            RenderManager rm1 = W_Reflection.getRenderManager(event.renderer);
            if(rm1 != null && !(rm1.renderEngine instanceof MCH_TextureManagerDummy)) {
               if(this.dummyTextureManager == null) {
                  this.dummyTextureManager = new MCH_TextureManagerDummy(rm1.renderEngine);
               }

               rm1.renderEngine = this.dummyTextureManager;
            }
         }

      }
   }

   public void renderLivingEventPost(net.minecraftforge.client.event.RenderLivingEvent.Post event) {
      MCH_GuiTargetMarker.addMarkEntityPos(2, event.entity, event.x, event.y + (double)event.entity.height + 0.5D, event.z);
      MCH_ClientLightWeaponTickHandler.markEntity(event.entity, event.x, event.y + (double)(event.entity.height / 2.0F), event.z);
      MCH_RenderBaseVehicle.renderEntityMarker(event.entity);
   }

   public void renderPlayerPre(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {
      if(event.entity != null) {
         if(event.entity.ridingEntity instanceof MCH_EntityBaseVehicle) {
            MCH_EntityBaseVehicle v = (MCH_EntityBaseVehicle)event.entity.ridingEntity;
            if(v.getAcInfo() != null && v.getAcInfo().hideEntity) {
               event.setCanceled(true);
               return;
            }
         }

      }
   }

   public void renderPlayerPost(net.minecraftforge.client.event.RenderPlayerEvent.Post event) {}

   public void worldEventUnload(Unload event) {
      MCH_ViewEntityDummy.onUnloadWorld();
   }

   public void entityJoinWorldEvent(EntityJoinWorldEvent event) {
      if(event.entity.isEntityEqual(MCH_Lib.getClientPlayer())) {
         MCH_Lib.DbgLog(true, "MCH_ClientEventHook.entityJoinWorldEvent : " + event.entity, new Object[0]);
         MCH_ItemRangeFinder.mode = Minecraft.getMinecraft().isSingleplayer()?1:0;
         MCH_ParticlesUtil.clearMarkPoint();
      }

   }

   @SubscribeEvent
   public void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
      if(event == null || event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
         return;
      }

      EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
      if(player == null) {
         return;
      }
      MCH_EntityBaseVehicle ac = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(player);
      if(ac instanceof MCP_EntityPlane && ((MCP_EntityPlane)ac).shouldSuppressVanillaCrosshair(player)) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
      if(event == null || event.type != RenderGameOverlayEvent.ElementType.ALL) {
         return;
      }

      Minecraft mc = Minecraft.getMinecraft();
      EntityClientPlayerMP player = mc.thePlayer;
      ReticleState state = this.getPlaneMouseAimReticleState(mc, player);
      boolean debug = MCH_Config.DebugFlightControl.prmBool || MCH_Config.PlaneMouseAimReticleDebug.prmBool;
      if(!state.shouldDraw) {
         if(debug) {
            this.logPlaneMouseAimReticleDebug(mc, state, false);
         }
         return;
      }

      ScaledResolution scaled = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
      int width = scaled.getScaledWidth();
      int height = scaled.getScaledHeight();
      double centerX = (double)width / 2.0D;
      double centerY = (double)height / 2.0D;
      double safeRadius = Math.max(12.0D, Math.min((double)Math.min(width, height) * MCH_Config.PlaneMouseAimMaxScreenRadius.prmDouble,
            (double)Math.min(width, height) * 0.50D));
      double yawRange = Math.max(1.0D, MCH_Config.PlaneMouseAimYawVisualRange.prmDouble);
      double pitchRange = Math.max(1.0D, Math.max(MCH_Config.MouseAimMaxPitchUp.prmDouble, MCH_Config.MouseAimMaxPitchDown.prmDouble));
      double aimX = centerX + (double)state.plane.getMouseAimYawError() / yawRange * safeRadius;
      double aimY = centerY - (double)state.plane.getMouseAimPitchError() / pitchRange * safeRadius;
      double dx = aimX - centerX;
      double dy = aimY - centerY;
      double dist = Math.sqrt(dx * dx + dy * dy);
      if(dist > safeRadius && dist > 1.0E-4D) {
         aimX = centerX + dx / dist * safeRadius;
         aimY = centerY + dy / dist * safeRadius;
      }

      this.drawPlaneMouseAimOverlay(mc, aimX, aimY, centerX, centerY);
      state.mouseX = aimX;
      state.mouseY = aimY;
      state.noseX = centerX;
      state.noseY = centerY;
      if(debug) {
         String text = this.getPlaneMouseAimReticleDebugText(state, true);
         mc.fontRenderer.drawStringWithShadow(text, 6, height / 2 + 64, 0x55FF66);
      }
      this.logPlaneMouseAimReticleDebug(mc, state, true);
   }

   private ReticleState getPlaneMouseAimReticleState(Minecraft mc, EntityClientPlayerMP player) {
      ReticleState state = new ReticleState();
      if(mc != null && mc.gameSettings != null && mc.gameSettings.hideGUI) {
         state.reason = "hud_hidden";
         return state;
      }
      if(player == null) {
         state.reason = "no_player";
         return state;
      }

      MCH_EntityBaseVehicle ac = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(player);
      if(!(ac instanceof MCP_EntityPlane)) {
         state.reason = "not_plane";
         return state;
      }

      MCP_EntityPlane plane = (MCP_EntityPlane)ac;
      state.plane = plane;
      if(!plane.isPilot(player)) {
         state.reason = "not_pilot";
      } else if(!plane.isNewFlightModelEnabled()) {
         state.reason = "not_new_flight";
      } else if(!plane.isMouseAimControlsEnabled()) {
         state.reason = "mouse_aim_disabled";
      } else if(!MCH_Config.EnablePlaneMouseAimReticle.prmBool) {
         state.reason = "reticle_config_disabled";
      } else {
         state.reason = "draw";
         state.shouldDraw = true;
      }
      return state;
   }

   private void drawPlaneMouseAimOverlay(Minecraft mc, double mouseX, double mouseY, double noseX, double noseY) {
      GL11.glPushMatrix();
      GL11.glDisable(2929);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(3553);
      GL11.glColor4f(0.25F, 1.0F, 0.35F, (float)Math.max(0.0D, Math.min(1.0D, MCH_Config.PlaneMouseAimReticleOpacity.prmDouble)));
      mc.renderEngine.bindTexture(getPlaneMouseAimReticleTexture());
      double mouseSize = 96.0D * Math.max(0.25D, Math.min(4.0D, MCH_Config.PlaneMouseAimReticleScale.prmDouble));
      this.drawTexturedOverlayQuad(mouseX - mouseSize / 2.0D, mouseY - mouseSize / 2.0D, mouseSize, mouseSize, 1024.0D, 1024.0D);
      GL11.glDisable(3553);
      this.drawOverlayLineReticle(mouseX, mouseY, mouseSize * 0.35D, 0xEE55FF66);
      double noseSize = 10.0D * Math.max(0.25D, Math.min(4.0D, MCH_Config.PlaneNoseReticleScale.prmDouble));
      int noseAlpha = ((int)(Math.max(0.0D, Math.min(1.0D, MCH_Config.PlaneNoseReticleOpacity.prmDouble)) * 255.0D) & 255) << 24;
      this.drawOverlayLineReticle(noseX, noseY, noseSize, noseAlpha | 0x00FFFFFF);
      GL11.glEnable(3553);
      GL11.glDisable(3042);
      GL11.glEnable(2929);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
   }

   private static ResourceLocation getPlaneMouseAimReticleTexture() {
      String path = MCH_Config.PlaneMouseAimReticleTexture != null ? MCH_Config.PlaneMouseAimReticleTexture.prmString : "";
      if(path == null || path.trim().length() <= 0) {
         return PLANE_MOUSE_AIM_RETICLE_TEXTURE;
      }
      path = path.trim();
      int domainSep = path.indexOf(':');
      if(domainSep >= 0) {
         return new ResourceLocation(path.substring(0, domainSep), path.substring(domainSep + 1));
      }
      if(path.startsWith("assets/mcheli/")) {
         path = path.substring("assets/mcheli/".length());
      }
      return new ResourceLocation("mcheli", path);
   }

   private void drawTexturedOverlayQuad(double x, double y, double width, double height, double texWidth, double texHeight) {
      Tessellator tess = Tessellator.instance;
      tess.startDrawingQuads();
      tess.addVertexWithUV(x, y + height, -90.0D, 0.0D, texHeight / texHeight);
      tess.addVertexWithUV(x + width, y + height, -90.0D, texWidth / texWidth, texHeight / texHeight);
      tess.addVertexWithUV(x + width, y, -90.0D, texWidth / texWidth, 0.0D);
      tess.addVertexWithUV(x, y, -90.0D, 0.0D, 0.0D);
      tess.draw();
   }

   private void drawOverlayLineReticle(double x, double y, double radius, int color) {
      GL11.glColor4ub((byte)(color >> 16 & 255), (byte)(color >> 8 & 255), (byte)(color & 255), (byte)(color >> 24 & 255));
      Tessellator tess = Tessellator.instance;
      tess.startDrawing(1);
      tess.addVertex(x - radius, y, -89.0D);
      tess.addVertex(x - radius * 0.35D, y, -89.0D);
      tess.addVertex(x + radius * 0.35D, y, -89.0D);
      tess.addVertex(x + radius, y, -89.0D);
      tess.addVertex(x, y - radius, -89.0D);
      tess.addVertex(x, y - radius * 0.35D, -89.0D);
      tess.addVertex(x, y + radius * 0.35D, -89.0D);
      tess.addVertex(x, y + radius, -89.0D);
      tess.draw();
   }

   private void logPlaneMouseAimReticleDebug(Minecraft mc, ReticleState state, boolean rendered) {
      if(System.currentTimeMillis() < nextPlaneMouseAimReticleDebugTime) {
         return;
      }
      nextPlaneMouseAimReticleDebugTime = System.currentTimeMillis() + 1000L;
      String text = this.getPlaneMouseAimReticleDebugText(state, rendered);
      MCH_Lib.Log("[MCHeli][PlaneMouseAimReticle] %s", new Object[]{text});
   }

   private String getPlaneMouseAimReticleDebugText(ReticleState state, boolean rendered) {
      MCP_EntityPlane plane = state.plane;
      return String.format("rendered=%s reason=%s mouse=(%.1f,%.1f) nose=(%.1f,%.1f) desired=(%.2f,%.2f) error=(%.2f,%.2f) crosshairSuppressed=%s",
            Boolean.valueOf(rendered), state.reason, Double.valueOf(state.mouseX), Double.valueOf(state.mouseY),
            Double.valueOf(state.noseX), Double.valueOf(state.noseY),
            Float.valueOf(plane != null ? plane.getMouseAimDesiredYaw() : 0.0F),
            Float.valueOf(plane != null ? plane.getMouseAimDesiredPitch() : 0.0F),
            Float.valueOf(plane != null ? plane.getMouseAimYawError() : 0.0F),
            Float.valueOf(plane != null ? plane.getMouseAimPitchError() : 0.0F),
            Boolean.valueOf(plane != null && plane.wasMouseAimVanillaCrosshairSuppressed()));
   }

   private static class ReticleState {
      MCP_EntityPlane plane;
      boolean shouldDraw;
      String reason = "unknown";
      double mouseX;
      double mouseY;
      double noseX;
      double noseY;
   }

   @SubscribeEvent
   public void onFovUpdate(FOVUpdateEvent event) {
      if(event != null && MCP_PlaneChaseCamera.isFovOverrideActive()) {
         event.newfov = MCP_PlaneChaseCamera.applyFovOverride(event.newfov);
      }
   }

   @SubscribeEvent
   public void renderTick(TickEvent.RenderTickEvent event) {
      switch (event.phase) {
         case START:
            smoothing = event.renderTickTime;
            MCP_PlaneChaseCamera.applyRenderStartCamera(Minecraft.getMinecraft());
            MCP_PlaneChaseCamera.beginOrientCameraBypass(Minecraft.getMinecraft(), event.renderTickTime);
            break;
         case END:
            MCP_PlaneChaseCamera.endOrientCameraBypass(Minecraft.getMinecraft());
            break;
      }
   }

}
