package mcheli.plane;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_KeyName;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_BaseVehicleCommonGui;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.gui.MCH_Gui;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.wrapper.W_McClient;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCP_GuiPlane extends MCH_BaseVehicleCommonGui {

   private static final ResourceLocation PLANE_MOUSE_AIM_RETICLE_TEXTURE = new ResourceLocation("mcheli", "textures/gui/plane_crosshair.png");

   public MCP_GuiPlane(Minecraft minecraft) {
      super(minecraft);
   }

   public boolean isDrawGui(EntityPlayer player) {
      return MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(player) instanceof MCP_EntityPlane;
   }

   public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
      MCH_EntityBaseVehicle ac = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(player);
      if(ac instanceof MCP_EntityPlane && !ac.isDestroyed()) {
         MCP_EntityPlane plane = (MCP_EntityPlane)ac;
         int seatID = ac.getSeatIdByEntity(player);
         GL11.glLineWidth((float)MCH_Gui.scaleFactor);
         if(plane.getCameraMode(player) == 1) {
            this.drawNightVisionNoise();
         }

         MCH_Config var10000;
         label50: {
            if(isThirdPersonView) {
               var10000 = MCH_MOD.config;
               if(!MCH_Config.DisplayHUDThirdPerson.prmBool) {
                  break label50;
               }
            }

            if(seatID == 0 && plane.getIsGunnerMode(player)) {
               this.drawHud(ac, player, 1);
            } else {
               this.drawHud(ac, player, seatID);
            }
         }

         label51: {
            this.drawDebugtInfo(plane);
            if(isThirdPersonView) {
               var10000 = MCH_MOD.config;
               if(!MCH_Config.DisplayHUDThirdPerson.prmBool) {
                  break label51;
               }
            }

            if(seatID == 0) {
               this.drawNewFlightThrottleHud(plane);
            }

            if(plane.getTVMissile() != null && (plane.getIsGunnerMode(player) || plane.isUAV())) {
               this.drawTvMissileNoise(plane, plane.getTVMissile());
            } else {
               this.drawKeybind(plane, player, seatID);
            }
         }

         if(seatID == 0) {
            this.drawMouseAimReticles(plane, player);
         }

         this.drawHitBullet(plane, -14101432, seatID);
      }
   }

   private void drawNewFlightThrottleHud(MCP_EntityPlane plane) {
      MCP_PlaneInfo info = plane.getPlaneInfo();
      if(info == null || !plane.isNewFlightModelEnabled() || !info.newFlightThrottleHudDisplay) {
         return;
      }

      int color = plane.isOverspeeding() ? -65536 : -1;
      String flap = plane.canUseCombatFlaps() ? (plane.isCombatFlapsDeployed() ? " FLP" : "") : "";
      this.drawString(String.format("THR %3d%%%s", new Object[]{Integer.valueOf(plane.getThrottlePercent()), flap}),
            super.centerX - 35, super.centerY + 42, color);
   }

   private void drawMouseAimReticles(MCP_EntityPlane plane, EntityPlayer player) {
      if(!plane.shouldDrawMouseAimReticle(player)) {
         return;
      }

      double safeRadius = Math.max(8.0D, Math.min((double)Math.min(super.centerX, super.centerY),
            (double)Math.min(super.width, super.height) * MCH_Config.PlaneMouseAimMaxScreenRadius.prmDouble));
      double yawRange = Math.max(1.0D, MCH_Config.PlaneMouseAimYawVisualRange.prmDouble);
      double pitchRange = Math.max(1.0D, Math.max(MCH_Config.MouseAimMaxPitchUp.prmDouble, MCH_Config.MouseAimMaxPitchDown.prmDouble));
      double aimX = (double)super.centerX + (double)plane.getMouseAimYawError() / yawRange * safeRadius;
      double aimY = (double)super.centerY - (double)plane.getMouseAimPitchError() / pitchRange * safeRadius;
      double dx = aimX - (double)super.centerX;
      double dy = aimY - (double)super.centerY;
      double distance = Math.sqrt(dx * dx + dy * dy);
      if(distance > safeRadius && distance > 1.0E-4D) {
         aimX = (double)super.centerX + dx / distance * safeRadius;
         aimY = (double)super.centerY + dy / distance * safeRadius;
      }

      double noseX = (double)super.centerX;
      double noseY = (double)super.centerY;
      this.drawNoseReticle(noseX, noseY);
      this.drawMouseAimReticle(aimX, aimY);

      if(MCH_Config.PlaneMouseAimReticleDebug.prmBool || MCH_Config.DebugFlightControl.prmBool) {
         String debug = String.format("MAIM x=%.1f y=%.1f nose=%.1f,%.1f yaw=%.1f pitch=%.1f err=%.1f,%.1f xhair=%s",
               Double.valueOf(aimX), Double.valueOf(aimY), Double.valueOf(noseX), Double.valueOf(noseY),
               Float.valueOf(plane.getMouseAimDesiredYaw()), Float.valueOf(plane.getMouseAimDesiredPitch()),
               Float.valueOf(plane.getMouseAimYawError()), Float.valueOf(plane.getMouseAimPitchError()),
               Boolean.valueOf(plane.wasMouseAimVanillaCrosshairSuppressed()));
         this.drawString(debug, super.centerX - 120, super.centerY + 58, -16711936);
         if(plane.ticksExisted % 20 == 0) {
            System.out.println("[MCHeli][PlaneMouseAimReticle] " + debug);
         }
      }
   }

   private void drawMouseAimReticle(double x, double y) {
      double scale = MathHelper.clamp_double(MCH_Config.PlaneMouseAimReticleScale.prmDouble, 0.25D, 4.0D);
      float opacity = (float)MathHelper.clamp_double(MCH_Config.PlaneMouseAimReticleOpacity.prmDouble, 0.0D, 1.0D);
      double size = 32.0D * scale;
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glColor4f(0.25F, 1.0F, 0.35F, opacity);
      super.mc.renderEngine.bindTexture(getPlaneMouseAimReticleTexture());
      this.drawTexturedRect(x - size / 2.0D, y - size / 2.0D, size, size, 0.0D, 0.0D, 1024.0D, 1024.0D, 1024.0D, 1024.0D);
      GL11.glDisable(3553);
      this.drawLine(new double[]{x - size * 0.65D, y, x - size * 0.25D, y, x + size * 0.25D, y, x + size * 0.65D, y,
            x, y - size * 0.65D, x, y - size * 0.25D, x, y + size * 0.25D, x, y + size * 0.65D}, 0xCC55FF66);
      GL11.glEnable(3553);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
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

   private void drawNoseReticle(double x, double y) {
      double scale = MathHelper.clamp_double(MCH_Config.PlaneNoseReticleScale.prmDouble, 0.25D, 4.0D);
      float opacity = (float)MathHelper.clamp_double(MCH_Config.PlaneNoseReticleOpacity.prmDouble, 0.0D, 1.0D);
      int alpha = (int)(opacity * 255.0F) << 24;
      int color = alpha | 0x00FFFFFF;
      double r = 8.0D * scale;
      GL11.glEnable(3042);
      this.drawLine(new double[]{x - r, y, x - r * 0.35D, y, x + r * 0.35D, y, x + r, y,
            x, y - r, x, y - r * 0.35D, x, y + r * 0.35D, x, y + r}, color);
      this.drawLine(new double[]{x - r * 0.55D, y - r * 0.55D, x + r * 0.55D, y - r * 0.55D,
            x + r * 0.55D, y + r * 0.55D, x - r * 0.55D, y + r * 0.55D}, color, 2);
   }

   public void drawKeybind(MCP_EntityPlane plane, EntityPlayer player, int seatID) {
      MCH_Config var10000 = MCH_MOD.config;
      if(!MCH_Config.HideKeybind.prmBool) {
         MCP_PlaneInfo info = plane.getPlaneInfo();
         if(info != null) {
            int colorActive = -1342177281;
            int colorInactive = -1349546097;
            int RX = super.centerX + 120;
            int LX = super.centerX - 200;
            this.drawKeyBind(plane, info, player, seatID, RX, LX, colorActive, colorInactive);
            String msg;
            StringBuilder var12;
            MCH_Config var10001;
            if(seatID == 0 && info.isEnableGunnerMode) {
               var10000 = MCH_MOD.config;
               if(!Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
                  int c = plane.isHoveringMode()?colorInactive:colorActive;
                  var12 = (new StringBuilder()).append(plane.getIsGunnerMode(player)?"Normal":"Gunner").append(" : ");
                  var10001 = MCH_MOD.config;
                  msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchMode.prmInt)).toString();
                  this.drawString(msg, RX, super.centerY - 70, c);
               }
            }

            if(seatID > 0 && plane.canSwitchGunnerModeOtherSeat(player)) {
               var12 = (new StringBuilder()).append(plane.getIsGunnerMode(player)?"Normal":"Camera").append(" : ");
               var10001 = MCH_MOD.config;
               msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchMode.prmInt)).toString();
               this.drawString(msg, RX, super.centerY - 40, colorActive);
            }

            if(seatID == 0 && plane.canUseCombatFlaps() && !info.isEnableVtol) {
               var10000 = MCH_MOD.config;
               if(!Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
                  var12 = (new StringBuilder()).append(plane.isCombatFlapsDeployed()?"Flaps Up : ":"Combat Flaps : ");
                  var10001 = MCH_MOD.config;
                  msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeyExtra.prmInt)).toString();
                  this.drawString(msg, RX, super.centerY - 60, colorActive);
               }
            }

            if(seatID == 0 && info.isEnableVtol) {
               var10000 = MCH_MOD.config;
               if(!Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
                  int stat = plane.getVtolMode();
                  if(stat != 1) {
                     var12 = (new StringBuilder()).append(stat == 0?"VTOL : ":"Normal : ");
                     var10001 = MCH_MOD.config;
                     msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeyExtra.prmInt)).toString();
                     this.drawString(msg, RX, super.centerY - 60, colorActive);
                  }
               }
            }

            if(plane.canEjectSeat(player)) {
               var12 = (new StringBuilder()).append("Eject seat: ");
               var10001 = MCH_MOD.config;
               msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwitchHovering.prmInt)).toString();
               this.drawString(msg, RX, super.centerY - 30, colorActive);
            }

            if(plane.getIsGunnerMode(player) && info.cameraZoom > 1) {
               var12 = (new StringBuilder()).append("Zoom : ");
               var10001 = MCH_MOD.config;
               msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt)).toString();
               this.drawString(msg, LX, super.centerY - 80, colorActive);
            } else if(seatID == 0) {
               if(!plane.canFoldWing() && !plane.canUnfoldWing()) {
                  if(plane.canFoldHatch() || plane.canUnfoldHatch()) {
                     var12 = (new StringBuilder()).append("OpenHatch : ");
                     var10001 = MCH_MOD.config;
                     msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt)).toString();
                     this.drawString(msg, LX, super.centerY - 80, colorActive);
                  }
               } else {
                  var12 = (new StringBuilder()).append("FoldWing : ");
                  var10001 = MCH_MOD.config;
                  msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt)).toString();
                  this.drawString(msg, LX, super.centerY - 80, colorActive);
               }
            }

         }
      }
   }
}
