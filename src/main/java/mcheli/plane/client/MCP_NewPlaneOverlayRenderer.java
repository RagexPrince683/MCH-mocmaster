package mcheli.plane.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_PlaneChaseCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCP_NewPlaneOverlayRenderer {

   private static final ResourceLocation PLANE_MOUSE_AIM_RETICLE_TEXTURE = new ResourceLocation("mcheli", "textures/gui/plane_crosshair.png");
   private static long nextDebugLogTime;
   private static long nextEventLogTime;
   private static boolean reticleTextureChecked;
   private static boolean reticleTextureAvailable = true;
   private boolean registeredLogged;
   private boolean postEnteredLogged;

   public MCP_NewPlaneOverlayRenderer() {
      MCH_Lib.Log("[MCHeli][NewPlaneOverlay] created renderer helper driven by MCH_ClientEventHook", new Object[0]);
   }

   @SubscribeEvent
   public void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
      if(event == null || event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
         return;
      }

      OverlayState state = this.getOverlayState(Minecraft.getMinecraft(), event.type, "pre_crosshairs");
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
      if(event == null) {
         return;
      }
      if(!this.postEnteredLogged || (this.isDebugEnabled() && event.type == RenderGameOverlayEvent.ElementType.ALL && System.currentTimeMillis() >= nextEventLogTime)) {
         this.postEnteredLogged = true;
         nextEventLogTime = System.currentTimeMillis() + 1000L;
         MCH_Lib.Log("[MCHeli][NewPlaneOverlay] post event entered type=%s", new Object[]{event.type});
      }
      if(event.type != RenderGameOverlayEvent.ElementType.ALL) {
         return;
      }

      Minecraft mc = Minecraft.getMinecraft();
      OverlayState state = this.getOverlayState(mc, event.type, "post_all");
      this.populateScreen(mc, state);
      this.populateStageDecisions(state);

      if(state.proofShouldDraw) {
         this.drawProofOverlay(mc, state);
         state.proofDrawn = true;
      }

      if(state.overlayActive) {
         this.updateAimScreenPosition(mc, state);
      } else {
         state.mouseX = state.centerX;
         state.mouseY = state.centerY;
         state.noseX = state.centerX;
         state.noseY = state.centerY;
      }

      if(state.cursorShouldDraw) {
         this.drawMouseAimCursor(mc, state);
         state.cursorDrawn = true;
      }
      if(state.noseShouldDraw) {
         this.drawNoseReticle(state);
         state.noseDrawn = true;
      }
      if(state.freelookIndicatorShouldDraw) {
         this.drawFreelookIndicator(mc, state);
         state.freelookIndicatorDrawn = true;
      }
      if(state.debugEnabled && state.hasPlane) {
         this.drawDebugText(mc, state);
      }
      this.logDebugOncePerSecond(state);
   }

   private OverlayState getOverlayState(Minecraft mc, RenderGameOverlayEvent.ElementType eventType, String eventStage) {
      OverlayState state = new OverlayState();
      state.rendererRegistered = true;
      state.eventStage = eventStage;
      state.eventType = eventType != null?eventType.name():"null";
      state.debugEnabled = this.isDebugEnabled();
      if(!this.registeredLogged) {
         this.registeredLogged = true;
         MCH_Lib.Log("[MCHeli][NewPlaneOverlay] first overlay state query reached", new Object[0]);
      }
      if(mc == null) {
         state.skipReason = "no_minecraft";
         return state;
      }
      state.renderViewEntityClass = mc.renderViewEntity != null?mc.renderViewEntity.getClass().getName():"null";
      EntityClientPlayerMP player = mc.thePlayer;
      if(player == null) {
         state.skipReason = "no_player";
         return state;
      }
      state.player = player;
      state.playerEntityClass = player.getClass().getName();
      MCH_EntityBaseVehicle ac = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(player);
      state.ridingEntityClass = ac != null?ac.getClass().getName():(player.ridingEntity != null?player.ridingEntity.getClass().getName():"null");
      if(!(ac instanceof MCP_EntityPlane)) {
         state.skipReason = "not_plane";
         return state;
      }
      state.hasPlane = true;
      state.plane = (MCP_EntityPlane)ac;
      state.newFlight = state.plane.isNewFlightModelEnabled();
      state.mouseAimEnabled = state.plane.isMouseAimControlsActive();
      if(!state.plane.isPilot(player)) {
         state.skipReason = "not_pilot";
      } else if(!state.newFlight) {
         state.skipReason = "not_new_flight";
      } else {
         state.skipReason = "qualifying_plane";
         state.qualifies = true;
      }
      state.crosshairSuppressed = state.plane.wasMouseAimVanillaCrosshairSuppressed();
      state.freelookActive = state.plane.isFreeLookMode() || MCP_PlaneChaseCamera.shouldUseHoldFreelookAsCameraOnly(state.plane, player);
      return state;
   }

   private void populateScreen(Minecraft mc, OverlayState state) {
      if(mc == null) {
         state.width = 0;
         state.height = 0;
      } else {
         ScaledResolution scaled = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
         state.width = scaled.getScaledWidth();
         state.height = scaled.getScaledHeight();
      }
      state.centerX = (double)state.width / 2.0D;
      state.centerY = (double)state.height / 2.0D;
      state.mouseX = state.centerX;
      state.mouseY = state.centerY;
      state.noseX = state.centerX;
      state.noseY = state.centerY;
   }

   private void populateStageDecisions(OverlayState state) {
      state.overlayActive = state.qualifies && (state.mouseAimEnabled || state.debugEnabled);
      state.proofShouldDraw = state.debugEnabled && state.qualifies;
      state.cursorShouldDraw = state.qualifies && (state.mouseAimEnabled || state.debugEnabled);
      state.noseShouldDraw = state.overlayActive || (state.debugEnabled && state.qualifies);
      state.freelookIndicatorShouldDraw = state.qualifies && state.freelookActive;
      state.proofSkipReason = state.proofShouldDraw?"draw":"debug_or_plane_gate";
      state.cursorSkipReason = state.cursorShouldDraw?"draw":"mouse_aim_or_debug_gate";
      state.noseSkipReason = state.noseShouldDraw?"draw":"overlay_gate";
   }

   private void updateAimScreenPosition(Minecraft mc, OverlayState state) {
      Entity camera = mc != null && mc.renderViewEntity != null?mc.renderViewEntity:state.player;
      state.cameraYaw = camera != null?camera.rotationYaw:0.0D;
      state.cameraPitch = camera != null?camera.rotationPitch:0.0D;
      state.desiredAimYaw = state.plane != null?state.plane.getMouseAimDesiredYaw():state.cameraYaw;
      state.desiredAimPitch = state.plane != null?state.plane.getMouseAimDesiredPitch():state.cameraPitch;
      state.cursorYawDelta = MathHelper.wrapAngleTo180_float((float)(state.desiredAimYaw - state.cameraYaw));
      state.cursorPitchDelta = MathHelper.wrapAngleTo180_float((float)(state.desiredAimPitch - state.cameraPitch));
      double safeRadius = Math.max(12.0D, Math.min((double)Math.min(state.width, state.height) * MCH_Config.PlaneMouseAimMaxScreenRadius.prmDouble,
            (double)Math.min(state.width, state.height) * 0.50D));
      double yawRange = Math.max(1.0D, MCH_Config.PlaneMouseAimYawVisualRange.prmDouble);
      double pitchRange = Math.max(1.0D, Math.max(MCH_Config.MouseAimMaxPitchUp.prmDouble, MCH_Config.MouseAimMaxPitchDown.prmDouble));
      state.mouseX = state.centerX + state.cursorYawDelta / yawRange * safeRadius;
      state.mouseY = state.centerY + state.cursorPitchDelta / pitchRange * safeRadius;
      if(!this.validNumbers(state.cameraYaw, state.cameraPitch, state.desiredAimYaw, state.desiredAimPitch, state.cursorYawDelta, state.cursorPitchDelta, state.mouseX, state.mouseY)) {
         state.cursorBadMath = true;
         state.cursorSkipReason = "bad_math_center_fallback";
         MCH_Lib.Log("[MCHeli][NewPlaneOverlay] bad cursor math camera=(%.3f,%.3f) desired=(%.3f,%.3f) delta=(%.3f,%.3f) screen=(%.3f,%.3f)",
               new Object[]{Double.valueOf(state.cameraYaw), Double.valueOf(state.cameraPitch), Double.valueOf(state.desiredAimYaw), Double.valueOf(state.desiredAimPitch), Double.valueOf(state.cursorYawDelta), Double.valueOf(state.cursorPitchDelta), Double.valueOf(state.mouseX), Double.valueOf(state.mouseY)});
         state.mouseX = state.centerX;
         state.mouseY = state.centerY;
      }
      this.clampToSafeRadius(state, true, safeRadius);

      Vec3 noseForward = state.plane != null?MCH_Lib.Rot2Vec3(state.plane.getRotYaw(), state.plane.getRotPitch()):Vec3.createVectorHelper(0.0D, 0.0D, 1.0D);
      double noseYaw = Math.atan2(-noseForward.xCoord, noseForward.zCoord) * 180.0D / Math.PI;
      double nosePitch = Math.asin(-noseForward.yCoord) * 180.0D / Math.PI;
      state.noseYawDelta = MathHelper.wrapAngleTo180_float((float)(noseYaw - state.cameraYaw));
      state.nosePitchDelta = MathHelper.wrapAngleTo180_float((float)(nosePitch - state.cameraPitch));
      state.noseX = state.centerX + state.noseYawDelta / yawRange * safeRadius;
      state.noseY = state.centerY + state.nosePitchDelta / pitchRange * safeRadius;
      if(!this.validNumbers(noseYaw, nosePitch, state.noseYawDelta, state.nosePitchDelta, state.noseX, state.noseY)) {
         state.noseBadMath = true;
         state.noseSkipReason = "bad_math_center_fallback";
         MCH_Lib.Log("[MCHeli][NewPlaneOverlay] bad nose math nose=(%.3f,%.3f) delta=(%.3f,%.3f) screen=(%.3f,%.3f)",
               new Object[]{Double.valueOf(noseYaw), Double.valueOf(nosePitch), Double.valueOf(state.noseYawDelta), Double.valueOf(state.nosePitchDelta), Double.valueOf(state.noseX), Double.valueOf(state.noseY)});
         state.noseX = state.centerX;
         state.noseY = state.centerY;
      }
      this.clampToSafeRadius(state, false, safeRadius);
   }

   private void clampToSafeRadius(OverlayState state, boolean mouse, double safeRadius) {
      double x = mouse?state.mouseX:state.noseX;
      double y = mouse?state.mouseY:state.noseY;
      double dx = x - state.centerX;
      double dy = y - state.centerY;
      double dist = Math.sqrt(dx * dx + dy * dy);
      if(!this.validNumbers(x, y, dx, dy, dist)) {
         x = state.centerX;
         y = state.centerY;
      } else if(dist > safeRadius && dist > 1.0E-4D) {
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
      this.beginOverlayGl();
      GL11.glDisable(3553);
      GL11.glColor4f(1.0F, 1.0F, 0.0F, 1.0F);
      this.drawLineCross(state.centerX, state.centerY, 34.0D, 0xFFFFFF00);
      GL11.glEnable(3553);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      if(mc != null && mc.fontRenderer != null) {
         mc.fontRenderer.drawStringWithShadow("NEW PLANE OVERLAY ACTIVE", 6, 6, 0xFFFF00);
      }
      this.endOverlayGl();
   }

   private void drawMouseAimCursor(Minecraft mc, OverlayState state) {
      this.beginOverlayGl();
      double mouseSize = 128.0D * this.clamp(MCH_Config.PlaneMouseAimReticleScale.prmDouble, 0.25D, 4.0D);
      boolean textureBound = this.bindPlaneMouseAimReticleTexture(mc);
      if(textureBound) {
         GL11.glEnable(3553);
         GL11.glColor4f(0.25F, 1.0F, 0.35F, (float)this.clamp(MCH_Config.PlaneMouseAimReticleOpacity.prmDouble, 0.0D, 1.0D));
         this.drawTexturedQuad(state.mouseX - mouseSize / 2.0D, state.mouseY - mouseSize / 2.0D, mouseSize, mouseSize);
      }
      GL11.glDisable(3553);
      if(!textureBound || state.debugEnabled) {
         this.drawLineCross(state.mouseX, state.mouseY, mouseSize * 0.35D, textureBound?0xAA55FF66:0xEE55FF66);
      }
      state.textureAvailable = textureBound;
      this.endOverlayGl();
   }

   private void drawFreelookIndicator(Minecraft mc, OverlayState state) {
      if(mc == null || mc.fontRenderer == null) {
         return;
      }
      this.beginOverlayGl();
      GL11.glEnable(3553);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      String text = "FREELOOK";
      int textWidth = mc.fontRenderer.getStringWidth(text);
      int x = (state.width - textWidth) / 2;
      int y = Math.max(8, (int)state.centerY - 42);
      mc.fontRenderer.drawStringWithShadow(text, x, y, 0x55FF66);
      this.endOverlayGl();
   }

   private void drawNoseReticle(OverlayState state) {
      this.beginOverlayGl();
      GL11.glDisable(3553);
      double noseSize = 11.0D * this.clamp(MCH_Config.PlaneNoseReticleScale.prmDouble, 0.25D, 4.0D);
      double noseOpacity = this.clamp(MCH_Config.PlaneNoseReticleOpacity.prmDouble, 0.0D, 1.0D) * (state.noseClamped?0.45D:1.0D);
      int noseAlpha = ((int)(noseOpacity * 255.0D) & 255) << 24;
      this.drawLineCross(state.noseX, state.noseY, noseSize, noseAlpha | 0x00FFFFFF);
      this.drawLineBox(state.noseX, state.noseY, noseSize * 0.65D, noseAlpha | 0x00FFFFFF);
      this.endOverlayGl();
   }

   private void beginOverlayGl() {
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
      GL11.glPushMatrix();
      GL11.glDisable(2929);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void endOverlayGl() {
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
      GL11.glPopAttrib();
   }

   private boolean bindPlaneMouseAimReticleTexture(Minecraft mc) {
      if(mc == null) {
         reticleTextureAvailable = false;
         return false;
      }
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
         try {
            mc.getTextureManager().bindTexture(PLANE_MOUSE_AIM_RETICLE_TEXTURE);
         } catch(Exception e) {
            reticleTextureAvailable = false;
            MCH_Lib.Log("[MCHeli][NewPlaneOverlay] failed to bind reticle texture: %s", new Object[]{PLANE_MOUSE_AIM_RETICLE_TEXTURE});
         }
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
      if(mc == null || mc.fontRenderer == null) {
         return;
      }
      this.beginOverlayGl();
      GL11.glEnable(3553);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      mc.fontRenderer.drawStringWithShadow(this.formatDebug(state), 6, 20, 0x55FF66);
      this.endOverlayGl();
   }

   private void logDebugOncePerSecond(OverlayState state) {
      if(!state.debugEnabled && !state.overlayActive) {
         return;
      }
      if(System.currentTimeMillis() < nextDebugLogTime) {
         return;
      }
      nextDebugLogTime = System.currentTimeMillis() + 1000L;
      MCH_Lib.Log("[MCHeli][NewPlaneOverlay] %s", new Object[]{this.formatDebug(state)});
   }

   private String formatDebug(OverlayState state) {
      return String.format("eventEntered=true stage=%s type=%s player=%s riding=%s planeQualifies=%s reason=%s renderView=%s mouseAim=%s debugReticle=%s debugFlight=%s size=%dx%d texture=%s proof=%s(%s) cursor=%s(%s) nose=%s(%s) freelookIndicator=%s freelook=%s crosshairSuppressed=%s desiredAimYaw/Pitch=(%.2f,%.2f) cameraYaw/Pitch=(%.2f,%.2f) cursorYawDelta/PitchDelta=(%.2f,%.2f) screenX/Y=(%.1f,%.1f) noseXY=(%.1f,%.1f) noseDelta=(%.2f,%.2f)",
            state.eventStage, state.eventType, state.playerEntityClass, state.ridingEntityClass, Boolean.valueOf(state.qualifies), state.skipReason,
            state.renderViewEntityClass, Boolean.valueOf(state.mouseAimEnabled), Boolean.valueOf(MCH_Config.PlaneMouseAimReticleDebug.prmBool), Boolean.valueOf(MCH_Config.DebugFlightControl.prmBool),
            Integer.valueOf(state.width), Integer.valueOf(state.height), Boolean.valueOf(reticleTextureAvailable), Boolean.valueOf(state.proofDrawn), state.proofSkipReason,
            Boolean.valueOf(state.cursorDrawn), state.cursorSkipReason, Boolean.valueOf(state.noseDrawn), state.noseSkipReason,
            Boolean.valueOf(state.freelookIndicatorDrawn), Boolean.valueOf(state.freelookActive), Boolean.valueOf(state.crosshairSuppressed), Double.valueOf(state.desiredAimYaw), Double.valueOf(state.desiredAimPitch),
            Double.valueOf(state.cameraYaw), Double.valueOf(state.cameraPitch), Double.valueOf(state.cursorYawDelta), Double.valueOf(state.cursorPitchDelta),
            Double.valueOf(state.mouseX), Double.valueOf(state.mouseY), Double.valueOf(state.noseX), Double.valueOf(state.noseY),
            Double.valueOf(state.noseYawDelta), Double.valueOf(state.nosePitchDelta));
   }

   private boolean isDebugEnabled() {
      return MCH_Config.PlaneMouseAimReticleDebug.prmBool || MCH_Config.DebugFlightControl.prmBool;
   }

   private boolean validNumbers(double a) {
      return !Double.isNaN(a) && !Double.isInfinite(a);
   }

   private boolean validNumbers(double a, double b, double c, double d, double e) {
      return this.validNumbers(a) && this.validNumbers(b) && this.validNumbers(c) && this.validNumbers(d) && this.validNumbers(e);
   }

   private boolean validNumbers(double a, double b, double c, double d, double e, double f) {
      return this.validNumbers(a) && this.validNumbers(b) && this.validNumbers(c) && this.validNumbers(d) && this.validNumbers(e) && this.validNumbers(f);
   }

   private boolean validNumbers(double a, double b, double c, double d, double e, double f, double g, double h) {
      return this.validNumbers(a) && this.validNumbers(b) && this.validNumbers(c) && this.validNumbers(d) && this.validNumbers(e) && this.validNumbers(f) && this.validNumbers(g) && this.validNumbers(h);
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
      boolean newFlight;
      boolean mouseAimEnabled;
      boolean debugEnabled;
      boolean overlayActive;
      boolean freelookActive;
      boolean crosshairSuppressed;
      String skipReason = "not_plane";
      String eventStage = "unknown";
      String eventType = "unknown";
      String playerEntityClass = "null";
      String ridingEntityClass = "null";
      String renderViewEntityClass = "null";
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
      boolean cursorBadMath;
      boolean noseBadMath;
      boolean textureAvailable = true;
      boolean proofShouldDraw;
      boolean proofDrawn;
      String proofSkipReason = "not_evaluated";
      boolean cursorShouldDraw;
      boolean cursorDrawn;
      String cursorSkipReason = "not_evaluated";
      boolean noseShouldDraw;
      boolean noseDrawn;
      String noseSkipReason = "not_evaluated";
      boolean freelookIndicatorShouldDraw;
      boolean freelookIndicatorDrawn;

      boolean shouldSuppressCrosshair() {
         return this.qualifies && this.mouseAimEnabled && MCH_Config.HideVanillaCrosshairInPlaneMouseAim.prmBool;
      }
   }
}
