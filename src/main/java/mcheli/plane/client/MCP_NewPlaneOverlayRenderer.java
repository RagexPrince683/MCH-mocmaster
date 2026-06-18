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
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCP_NewPlaneOverlayRenderer {

   private static final ResourceLocation PLANE_MOUSE_AIM_RETICLE_TEXTURE = new ResourceLocation("mcheli", "textures/gui/plane_crosshair.png");
   private static long nextDebugLogTime;
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
      state.noseX = (double)state.width / 2.0D;
      state.noseY = (double)state.height / 2.0D;
      this.drawProofOverlay(mc, state);

      if(state.shouldDrawReticle()) {
         this.updateAimScreenPosition(state);
         this.drawMouseAimReticles(mc, state);
      } else {
         state.mouseX = state.noseX;
         state.mouseY = state.noseY;
      }

      if(MCH_Config.PlaneMouseAimReticleDebug.prmBool || MCH_Config.DebugFlightControl.prmBool) {
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

   private void updateAimScreenPosition(OverlayState state) {
      double safeRadius = Math.max(12.0D, Math.min((double)Math.min(state.width, state.height) * MCH_Config.PlaneMouseAimMaxScreenRadius.prmDouble,
            (double)Math.min(state.width, state.height) * 0.50D));
      double yawRange = Math.max(1.0D, MCH_Config.PlaneMouseAimYawVisualRange.prmDouble);
      double pitchRange = Math.max(1.0D, Math.max(MCH_Config.MouseAimMaxPitchUp.prmDouble, MCH_Config.MouseAimMaxPitchDown.prmDouble));
      state.mouseX = state.noseX + (double)state.plane.getMouseAimYawError() / yawRange * safeRadius;
      state.mouseY = state.noseY - (double)state.plane.getMouseAimPitchError() / pitchRange * safeRadius;
      double dx = state.mouseX - state.noseX;
      double dy = state.mouseY - state.noseY;
      double dist = Math.sqrt(dx * dx + dy * dy);
      if(dist > safeRadius && dist > 1.0E-4D) {
         state.mouseX = state.noseX + dx / dist * safeRadius;
         state.mouseY = state.noseY + dy / dist * safeRadius;
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
      mc.renderEngine.bindTexture(PLANE_MOUSE_AIM_RETICLE_TEXTURE);
      double mouseSize = 96.0D * this.clamp(MCH_Config.PlaneMouseAimReticleScale.prmDouble, 0.25D, 4.0D);
      this.drawTexturedQuad(state.mouseX - mouseSize / 2.0D, state.mouseY - mouseSize / 2.0D, mouseSize, mouseSize);
      GL11.glDisable(3553);
      this.drawLineCross(state.mouseX, state.mouseY, mouseSize * 0.35D, 0xEE55FF66);
      double noseSize = 11.0D * this.clamp(MCH_Config.PlaneNoseReticleScale.prmDouble, 0.25D, 4.0D);
      int noseAlpha = ((int)(this.clamp(MCH_Config.PlaneNoseReticleOpacity.prmDouble, 0.0D, 1.0D) * 255.0D) & 255) << 24;
      this.drawLineCross(state.noseX, state.noseY, noseSize, noseAlpha | 0x00FFFFFF);
      this.drawLineBox(state.noseX, state.noseY, noseSize * 0.65D, noseAlpha | 0x00FFFFFF);
      GL11.glEnable(3553);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glDisable(3042);
      GL11.glEnable(2929);
      GL11.glPopMatrix();
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
      MCP_EntityPlane plane = state.plane;
      return String.format("registered=%s reason=%s crosshairSuppressed=%s aim=(%.1f,%.1f) nose=(%.1f,%.1f) desired=(%.2f,%.2f) error=(%.2f,%.2f)",
            Boolean.valueOf(state.rendererRegistered), state.skipReason, Boolean.valueOf(state.crosshairSuppressed),
            Double.valueOf(state.mouseX), Double.valueOf(state.mouseY), Double.valueOf(state.noseX), Double.valueOf(state.noseY),
            Float.valueOf(plane != null ? plane.getMouseAimDesiredYaw() : 0.0F),
            Float.valueOf(plane != null ? plane.getMouseAimDesiredPitch() : 0.0F),
            Float.valueOf(plane != null ? plane.getMouseAimYawError() : 0.0F),
            Float.valueOf(plane != null ? plane.getMouseAimPitchError() : 0.0F));
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

      boolean shouldDrawReticle() {
         return this.qualifies;
      }

      boolean shouldSuppressCrosshair() {
         return this.qualifies && MCH_Config.HideVanillaCrosshairInPlaneMouseAim.prmBool;
      }
   }
}
