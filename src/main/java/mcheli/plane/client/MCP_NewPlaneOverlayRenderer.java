package mcheli.plane.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.plane.MCP_EntityPlane;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCP_NewPlaneOverlayRenderer {

   private static final ResourceLocation PLANE_MOUSE_AIM_RETICLE_TEXTURE = new ResourceLocation("mcheli", "textures/gui/plane_crosshair.png");
   private static long nextDebugLogTime;
   private static boolean reticleTextureChecked;
   private static boolean reticleTextureAvailable = true;
   private boolean registeredLogged;

   public MCP_NewPlaneOverlayRenderer() {
      MCH_Lib.Log("[MCHeli][NewPlaneOverlay] registered renderer instance", new Object[0]);
   }

   @SubscribeEvent
   public void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
      if(event == null || event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
         return;
      }

      OverlayState state = this.getOverlayState(Minecraft.getMinecraft());
      if(state.shouldSuppressCrosshair()) {
         state.crosshairSuppressed = true;
         if(state.plane != null) {
            state.plane.setMouseAimVanillaCrosshairSuppressed(true);
         }
         event.setCanceled(true);
      } else if(state.plane != null) {
         state.plane.setMouseAimVanillaCrosshairSuppressed(false);
      }
   }

   @SubscribeEvent
   public void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
      if(event == null || event.type != RenderGameOverlayEvent.ElementType.ALL) {
         return;
      }

      Minecraft mc = Minecraft.getMinecraft();
      OverlayState state = this.getOverlayState(mc);
      if(!state.hasPlane) {
         return;
      }

      ScaledResolution scaled = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
      state.width = scaled.getScaledWidth();
      state.height = scaled.getScaledHeight();
      state.centerX = (double)state.width / 2.0D;
      state.centerY = (double)state.height / 2.0D;
      state.noseX = state.centerX;
      state.noseY = state.centerY;

      if(state.shouldDrawReticle()) {
         this.updateAimScreenPosition(mc, state);
         this.drawMouseAimReticles(mc, state);
      } else {
         state.mouseX = state.centerX;
         state.mouseY = state.centerY;
      }

      if(MCH_Config.PlaneMouseAimReticleDebug.prmBool || MCH_Config.DebugFlightControl.prmBool) {
         this.drawProofOverlay(mc, state);

         this.drawDebugText(mc, state);
      }
      if(state.shouldDrawReticle()) {
         this.logDebugOncePerSecond(state);
      }
   }

   private OverlayState getOverlayState(Minecraft mc) {
      OverlayState state = new OverlayState();
      state.rendererRegistered = true;
      if(!this.registeredLogged) {
         this.registeredLogged = true;
         MCH_Lib.Log("[MCHeli][NewPlaneOverlay] first overlay state query reached", new Object[0]);
      }
      if(mc == null) {
         state.skipReason = "no_minecraft";
         return state;
      }
      EntityClientPlayerMP player = mc.thePlayer;
      if(player == null) {
         state.skipReason = "no_player";
         return state;
      }
      MCH_EntityBaseVehicle ac = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(player);
      if(!(ac instanceof MCP_EntityPlane)) {
         state.skipReason = "not_plane";
         return state;
      }
      state.hasPlane = true;
      state.player = player;
      state.plane = (MCP_EntityPlane)ac;
      if(!state.plane.isPilot(player)) {
         state.skipReason = "not_pilot";
      } else if(!state.plane.isNewFlightModelEnabled()) {
         state.skipReason = "not_new_flight";
      } else if(!state.plane.isMouseAimControlsActive()) {
         state.skipReason = "mouse_aim_disabled";
      } else if(!MCH_Config.EnablePlaneMouseAimReticle.prmBool) {
         state.skipReason = "reticle_config_disabled";
      } else {
         state.skipReason = "draw";
         state.qualifies = true;
      }
      state.crosshairSuppressed = state.plane.wasMouseAimVanillaCrosshairSuppressed();
      return state;
   }

   private void updateAimScreenPosition(Minecraft mc, OverlayState state) {
      Entity camera = mc.renderViewEntity != null?mc.renderViewEntity:state.player;
      state.cameraYaw = camera != null?camera.rotationYaw:state.player.rotationYaw;
      state.cameraPitch = camera != null?camera.rotationPitch:state.player.rotationPitch;
      state.desiredAimYaw = state.plane.getMouseAimDesiredYaw();
      state.desiredAimPitch = state.plane.getMouseAimDesiredPitch();
      state.cursorYawDelta = MathHelper.wrapAngleTo180_float((float)(state.desiredAimYaw - state.cameraYaw));
      state.cursorPitchDelta = MathHelper.wrapAngleTo180_float((float)(state.desiredAimPitch - state.cameraPitch));
      double safeRadius = Math.max(12.0D, Math.min((double)Math.min(state.width, state.height) * MCH_Config.PlaneMouseAimMaxScreenRadius.prmDouble,
            (double)Math.min(state.width, state.height) * 0.50D));
      double yawRange = Math.max(1.0D, MCH_Config.PlaneMouseAimYawVisualRange.prmDouble);
      double pitchRange = Math.max(1.0D, Math.max(MCH_Config.MouseAimMaxPitchUp.prmDouble, MCH_Config.MouseAimMaxPitchDown.prmDouble));
      state.mouseX = state.centerX + state.cursorYawDelta / yawRange * safeRadius;
      state.mouseY = state.centerY + state.cursorPitchDelta / pitchRange * safeRadius;
      this.clampToSafeRadius(state, true, safeRadius);

      Vec3 noseForward = MCH_Lib.Rot2Vec3(state.plane.getRotYaw(), state.plane.getRotPitch());
      double noseYaw = Math.atan2(-noseForward.xCoord, noseForward.zCoord) * 180.0D / Math.PI;
      double nosePitch = Math.asin(-noseForward.yCoord) * 180.0D / Math.PI;
      state.noseYawDelta = MathHelper.wrapAngleTo180_float((float)(noseYaw - state.cameraYaw));
      state.nosePitchDelta = MathHelper.wrapAngleTo180_float((float)(nosePitch - state.cameraPitch));
      state.noseX = state.centerX + state.noseYawDelta / yawRange * safeRadius;
      state.noseY = state.centerY + state.nosePitchDelta / pitchRange * safeRadius;
      this.clampToSafeRadius(state, false, safeRadius);
   }

   private void clampToSafeRadius(OverlayState state, boolean mouse, double safeRadius) {
      double x = mouse?state.mouseX:state.noseX;
      double y = mouse?state.mouseY:state.noseY;
      double dx = x - state.centerX;
      double dy = y - state.centerY;
      double dist = Math.sqrt(dx * dx + dy * dy);
      if(dist > safeRadius && dist > 1.0E-4D) {
         x = state.centerX + dx / dist * safeRadius;
         y = state.centerY + dy / dist * safeRadius;
         if(mouse) {
            state.mouseClamped = true;
         } else {
            state.noseClamped = true;
         }
      }
      if(mouse) {
         state.mouseX = x;
         state.mouseY = y;
      } else {
         state.noseX = x;
         state.noseY = y;
      }
   }

   private void drawProofOverlay(Minecraft mc, OverlayState state) {
      GL11.glPushMatrix();
      GL11.glDisable(2929);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3553);
      this.drawLineCross(state.noseX, state.noseY, 28.0D, 0xAA00FF00);
      GL11.glEnable(3553);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      mc.fontRenderer.drawStringWithShadow("NEW PLANE OVERLAY ACTIVE", 6, 6, 0x55FF66);
      GL11.glDisable(3042);
      GL11.glEnable(2929);
      GL11.glPopMatrix();
   }

   private void drawMouseAimReticles(Minecraft mc, OverlayState state) {
      GL11.glPushMatrix();
      GL11.glDisable(2929);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(3553);
      GL11.glColor4f(0.25F, 1.0F, 0.35F, (float)this.clamp(MCH_Config.PlaneMouseAimReticleOpacity.prmDouble, 0.0D, 1.0D));
      double mouseSize = 128.0D * this.clamp(MCH_Config.PlaneMouseAimReticleScale.prmDouble, 0.25D, 4.0D);
      if(this.bindPlaneMouseAimReticleTexture(mc)) {
         this.drawTexturedQuad(state.mouseX - mouseSize / 2.0D, state.mouseY - mouseSize / 2.0D, mouseSize, mouseSize);
      }
      GL11.glDisable(3553);
      if(!reticleTextureAvailable || MCH_Config.PlaneMouseAimReticleDebug.prmBool || MCH_Config.DebugFlightControl.prmBool) {
         this.drawLineCross(state.mouseX, state.mouseY, mouseSize * 0.35D, 0xEE55FF66);
      }
      double noseSize = 11.0D * this.clamp(MCH_Config.PlaneNoseReticleScale.prmDouble, 0.25D, 4.0D);
      double noseOpacity = this.clamp(MCH_Config.PlaneNoseReticleOpacity.prmDouble, 0.0D, 1.0D) * (state.noseClamped?0.45D:1.0D);
      int noseAlpha = ((int)(noseOpacity * 255.0D) & 255) << 24;
      this.drawLineCross(state.noseX, state.noseY, noseSize, noseAlpha | 0x00FFFFFF);
      this.drawLineBox(state.noseX, state.noseY, noseSize * 0.65D, noseAlpha | 0x00FFFFFF);
      GL11.glEnable(3553);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glDisable(3042);
      GL11.glEnable(2929);
      GL11.glPopMatrix();
   }

   private boolean bindPlaneMouseAimReticleTexture(Minecraft mc) {
      if(!reticleTextureChecked) {
         reticleTextureChecked = true;
         try {
            mc.getResourceManager().getResource(PLANE_MOUSE_AIM_RETICLE_TEXTURE);
            reticleTextureAvailable = true;
         } catch(Exception e) {
            reticleTextureAvailable = false;
            MCH_Lib.Log("[MCHeli][NewPlaneOverlay] failed to find reticle texture: %s", new Object[]{PLANE_MOUSE_AIM_RETICLE_TEXTURE});
         }
      }
      if(reticleTextureAvailable) {
         mc.getTextureManager().bindTexture(PLANE_MOUSE_AIM_RETICLE_TEXTURE);
      }
      return reticleTextureAvailable;
   }

   private void drawTexturedQuad(double x, double y, double width, double height) {
      Tessellator tess = Tessellator.instance;
      tess.startDrawingQuads();
      tess.addVertexWithUV(x, y + height, -90.0D, 0.0D, 1.0D);
      tess.addVertexWithUV(x + width, y + height, -90.0D, 1.0D, 1.0D);
      tess.addVertexWithUV(x + width, y, -90.0D, 1.0D, 0.0D);
      tess.addVertexWithUV(x, y, -90.0D, 0.0D, 0.0D);
      tess.draw();
   }

   private void drawLineCross(double x, double y, double radius, int color) {
      GL11.glColor4ub((byte)(color >> 16 & 255), (byte)(color >> 8 & 255), (byte)(color & 255), (byte)(color >> 24 & 255));
      Tessellator tess = Tessellator.instance;
      tess.startDrawing(1);
      tess.addVertex(x - radius, y, -89.0D);
      tess.addVertex(x + radius, y, -89.0D);
      tess.addVertex(x, y - radius, -89.0D);
      tess.addVertex(x, y + radius, -89.0D);
      tess.draw();
   }

   private void drawLineBox(double x, double y, double radius, int color) {
      GL11.glColor4ub((byte)(color >> 16 & 255), (byte)(color >> 8 & 255), (byte)(color & 255), (byte)(color >> 24 & 255));
      Tessellator tess = Tessellator.instance;
      tess.startDrawing(2);
      tess.addVertex(x - radius, y - radius, -89.0D);
      tess.addVertex(x + radius, y - radius, -89.0D);
      tess.addVertex(x + radius, y + radius, -89.0D);
      tess.addVertex(x - radius, y + radius, -89.0D);
      tess.draw();
   }

   private void drawDebugText(Minecraft mc, OverlayState state) {
      String debug = this.formatDebug(state);
      GL11.glPushMatrix();
      GL11.glEnable(3042);
      GL11.glEnable(3553);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      mc.fontRenderer.drawStringWithShadow(debug, 6, 20, 0x55FF66);
      GL11.glPopMatrix();
   }

   private void logDebugOncePerSecond(OverlayState state) {
      if(System.currentTimeMillis() < nextDebugLogTime) {
         return;
      }
      nextDebugLogTime = System.currentTimeMillis() + 1000L;
      MCH_Lib.Log("[MCHeli][NewPlaneOverlay] %s", new Object[]{this.formatDebug(state)});
   }

   private String formatDebug(OverlayState state) {
      return String.format("registered=%s reason=%s crosshairSuppressed=%s desiredAimYaw/Pitch=(%.2f,%.2f) cameraYaw/Pitch=(%.2f,%.2f) cursorYawDelta/PitchDelta=(%.2f,%.2f) screenX/Y=(%.1f,%.1f) nose=(%.1f,%.1f) noseDelta=(%.2f,%.2f) texture=%s",
            Boolean.valueOf(state.rendererRegistered), state.skipReason, Boolean.valueOf(state.crosshairSuppressed),
            Double.valueOf(state.desiredAimYaw), Double.valueOf(state.desiredAimPitch),
            Double.valueOf(state.cameraYaw), Double.valueOf(state.cameraPitch),
            Double.valueOf(state.cursorYawDelta), Double.valueOf(state.cursorPitchDelta),
            Double.valueOf(state.mouseX), Double.valueOf(state.mouseY), Double.valueOf(state.noseX), Double.valueOf(state.noseY),
            Double.valueOf(state.noseYawDelta), Double.valueOf(state.nosePitchDelta), Boolean.valueOf(reticleTextureAvailable));
   }

   private double clamp(double value, double min, double max) {
      return Math.max(min, Math.min(max, value));
   }

   private static class OverlayState {
      EntityClientPlayerMP player;
      MCP_EntityPlane plane;
      boolean rendererRegistered;
      boolean hasPlane;
      boolean qualifies;
      boolean crosshairSuppressed;
      String skipReason = "not_plane";
      int width;
      int height;
      double mouseX;
      double mouseY;
      double noseX;
      double noseY;
      double centerX;
      double centerY;
      double desiredAimYaw;
      double desiredAimPitch;
      double cameraYaw;
      double cameraPitch;
      double cursorYawDelta;
      double cursorPitchDelta;
      double noseYawDelta;
      double nosePitchDelta;
      boolean mouseClamped;
      boolean noseClamped;

      boolean shouldDrawReticle() {
         return this.qualifies;
      }

      boolean shouldSuppressCrosshair() {
         return this.qualifies && MCH_Config.HideVanillaCrosshairInPlaneMouseAim.prmBool;
      }
   }
}
