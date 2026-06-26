package mcheli.plane;

import java.util.ArrayList;
import java.util.List;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_ClientCommonTickHandler;
import mcheli.MCH_Config;
import mcheli.MCH_KeyName;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_BaseVehicleCommonGui;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_FlightModel;
import mcheli.aircraft.MCH_HudShared;
import mcheli.gui.MCH_Gui;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.weapon.MCH_WeaponSet;
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

            if(!this.shouldDrawNewPlaneSimpleHud(plane, seatID)) {
               if(seatID == 0 && plane.getIsGunnerMode(player)) {
                  this.drawHud(ac, player, 1);
               } else {
                  this.drawHud(ac, player, seatID);
               }
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
               if(this.shouldDrawNewPlaneSimpleHud(plane, seatID)) {
                  this.drawNewPlaneSimpleHud(plane);
                  this.drawNewPlaneWeaponHud(plane, player);
                  this.drawNewPlaneDebugHud(plane);
               } else {
                  this.drawNewFlightThrottleHud(plane);
               }
            }

            if(plane.getTVMissile() != null && (plane.getIsGunnerMode(player) || plane.isUAV())) {
               this.drawTvMissileNoise(plane, plane.getTVMissile());
            } else {
               this.drawKeybind(plane, player, seatID);
            }
         }


         this.drawHitBullet(plane, -14101432, seatID);
      }
   }

   private boolean shouldDrawNewPlaneSimpleHud(MCP_EntityPlane plane, int seatID) {
      return seatID == 0 && MCH_Config.EnableNewPlaneSimpleHud.prmBool && plane != null && !plane.isDestroyed()
            && plane.isNewFlightModelEnabled();
   }

   private void drawNewPlaneSimpleHud(MCP_EntityPlane plane) {
      List lines = new ArrayList();
      lines.add(MCH_HudShared.formatThrottleOrCollective("THR  ", plane));
      lines.add(MCH_HudShared.formatSpeedKmh(plane));
      lines.add(MCH_HudShared.formatAltitude(plane));
      lines.add(MCH_HudShared.formatVerticalSpeed(plane));
      lines.add(String.format("PITCH %+.0f\u00B0", new Object[]{Float.valueOf(this.getDisplayPitchDegrees(plane))}));
      lines.add(MCH_HudShared.formatFuelMinutes(plane));
      lines.add(MCH_HudShared.formatDamagePercent(plane));
      lines.add(this.formatSimpleHudGLoad(plane));
      lines.add(this.formatSimpleHudAoA(plane));

      List warnings = this.collectSimpleHudWarnings(plane);
      int x = MathHelper.clamp_int(MCH_Config.NewPlaneSimpleHudX.prmInt, 4, Math.max(4, super.width - 118));
      int y = MathHelper.clamp_int(MCH_Config.NewPlaneSimpleHudY.prmInt, 4, Math.max(4, super.height - 102));
      this.drawHudPanel(x - 4, y - 4, 116, lines.size() * 10 + 8);
      this.drawHudLines(lines, x, y, 0xFFE8E8E8, 0x66303030);

      this.drawStickInputGauge(x + 126, y + 12);

      if(!warnings.isEmpty()) {
         int wy = y + lines.size() * 10 + 5;
         this.drawHudPanel(x - 4, wy - 2, 116, warnings.size() * 10 + 4);
         this.drawHudLines(warnings, x, wy, 0xFFFFD65A, 0x66FFAA00);
      }
   }



   private float getDisplayPitchDegrees(MCP_EntityPlane plane) {
      // Minecraft aircraft pitch is inverted for pilot-facing attitude readouts:
      // negative rotation pitch is nose-up and positive rotation pitch is nose-down.
      // Invert only the displayed HUD value; do not feed this back into physics/control.
      return plane != null?-plane.getRotPitch():0.0F;
   }

   private void drawStickInputGauge(int x, int y) {
      if(!MCH_Config.EnableNewVehicleStickInputGauge.prmBool) {
         return;
      }
      int size = 34;
      int half = size / 2;
      double max = Math.max(1.0D, MCH_ClientCommonTickHandler.getMaxStickLength());
      int sx = x + half + (int)Math.round(MathHelper.clamp_double(MCH_ClientCommonTickHandler.getCurrentStickX() / max, -1.0D, 1.0D) * (double)(half - 3));
      int sy = y + half - (int)Math.round(MathHelper.clamp_double(MCH_ClientCommonTickHandler.getCurrentStickY() / max, -1.0D, 1.0D) * (double)(half - 3));
      this.drawHudPanel(x - 2, y - 2, size + 4, size + 4);
      this.drawLine(new double[]{(double)x, (double)(y + half), (double)(x + size), (double)(y + half), (double)(x + half), (double)y, (double)(x + half), (double)(y + size)}, 0x8855FF66, 1);
      drawRect(sx - 2, sy - 2, sx + 3, sy + 3, 0xFF55FF66);
   }

   private void drawNewPlaneDebugHud(MCP_EntityPlane plane) {
      if(!MCH_Config.DebugFlightControl.prmBool && !MCH_Config.TestMode.prmBool) {
         return;
      }

      MCP_PlaneInfo info = plane.getPlaneInfo();
      if(info == null) {
         return;
      }

      double stallSpeed = MCH_FlightModel.getStallSpeed(info.stallSpeed, plane.getMaxSpeed(), info.stallSpeedFactor);
      List lines = new ArrayList();
      lines.add(String.format("NF DBG  stall=%s recover=%s flap=%s", new Object[]{
            Boolean.valueOf(plane.getStallSeverity() > 0.0D), Boolean.valueOf(plane.isStallRecovering()),
            Boolean.valueOf(plane.isCombatFlapsDeployed())}));
      lines.add(String.format("SPD side=%s path=%s currentField=%.3f control=%.3f true=%.3f bodyFwd=%.3f horiz=%.3f", new Object[]{
            plane.isLastSetAnglesRemote() ? "client" : "server", Boolean.valueOf(plane.isLastNewFlightControlPathActive()),
            Double.valueOf(plane.currentSpeed), Double.valueOf(plane.getLastControlSpeed()),
            Double.valueOf(plane.getTrueAirspeed()), Double.valueOf(plane.getForwardAirspeed()),
            Double.valueOf(plane.getLastHorizontalSpeed())}));
      lines.add(String.format("LIM dt=%.3f mAdd y=%.2f p=%.2f r=%.2f in=(%.2f,%.2f)", new Object[]{
            Float.valueOf(plane.getLastSetAnglesPartialTicks()), Double.valueOf(plane.getLastYawMAdd()),
            Double.valueOf(plane.getLastPitchMAdd()), Double.valueOf(plane.getLastRollMAdd()),
            Double.valueOf(plane.getLastInputXBeforeClamp()), Double.valueOf(plane.getLastInputYBeforeClamp())}));
      lines.add(String.format("CLAMP pitch %.2f->%.2f roll %.2f->%.2f", new Object[]{
            Double.valueOf(plane.getLastPitchBeforeClamp()), Double.valueOf(plane.getLastPitchAfterClamp()),
            Double.valueOf(plane.getLastRollBeforeClamp()), Double.valueOf(plane.getLastRollAfterClamp())}));
      lines.add(String.format("SPD raw=%.0f display=%.0f km/h stall=%.3f", new Object[]{
            Double.valueOf(MCH_HudShared.getRawSpeedKmh(plane)), Double.valueOf(MCH_HudShared.getDisplaySpeedPlane(plane)), Double.valueOf(stallSpeed)}));
      lines.add(String.format("aoa=%.1f demand=%.2f sev=%.2f/%.2f/%.2f", new Object[]{
            Double.valueOf(plane.getAngleOfAttackDegrees()), Double.valueOf(plane.getStallDemand()),
            Double.valueOf(plane.getSpeedStallSeverity()), Double.valueOf(plane.getAoAStallSeverity()),
            Double.valueOf(plane.getDeepStallSeverity())}));
      lines.add(String.format("auth pitch=%.2f roll=%.2f finalPitch=%.4f finalRollAV=%.4f", new Object[]{
            Double.valueOf(plane.getLastFinalPitchAuthority()), Double.valueOf(plane.getLastRollAuthority()),
            Double.valueOf(plane.getLastPitchInputAfterAuthority()), Float.valueOf(plane.getRollAngularVelocity())}));
      lines.add(String.format("auth ctrl=%.2f up=%.2f down=%.2f yaw=%.2f", new Object[]{
            Double.valueOf(plane.getLastControlAuthority()), Double.valueOf(plane.getLastPitchUpAuthority()),
            Double.valueOf(plane.getLastPitchDownAuthority()), Double.valueOf(plane.getLastYawAuthority())}));
      lines.add(String.format("mom stall=%.4f forced=%.4f finalAV=%.4f", new Object[]{
            Double.valueOf(plane.getLastStallPitchMoment()), Double.valueOf(plane.getLastForcedNoseDownPitchDelta()),
            Double.valueOf(plane.getLastFinalPitchAngularVelocity())}));
      lines.add(String.format("lift L/W=%.2f T/W=%.2f loss=%.2f validClimb=%s", new Object[]{
            Double.valueOf(plane.getLiftToWeightRatio()), Double.valueOf(plane.getThrustToWeightRatio()),
            Double.valueOf(plane.getLastLiftLoss()), Boolean.valueOf(plane.isLastValidClimb())}));
      lines.add(String.format("energy ratio=%.2f deficit=%.2f dE=%.4f reason=%s", new Object[]{
            Double.valueOf(plane.getLastPitchEnvelopeEnergyRatio()), Double.valueOf(plane.getLastEnergyDeficitSeverity()),
            Double.valueOf(plane.getLastEnergyDelta()), plane.getLastLowHorizontalSpeedWarning()}));

      int width = Math.min(super.width - 16, Math.max(220, this.getMaxHudLineWidth(lines) + 8));
      int x = MathHelper.clamp_int(MCH_Config.NewPlaneSimpleHudX.prmInt, 4, Math.max(4, super.width - width - 8));
      int y = MathHelper.clamp_int(MCH_Config.NewPlaneSimpleHudY.prmInt + 125, 4, Math.max(4, super.height - (lines.size() * 10 + 12)));
      this.drawHudPanel(x - 4, y - 4, width, lines.size() * 10 + 8);
      this.drawHudLines(lines, x, y, 0xFF66FF66, 0x66005500);
   }

   private String formatSimpleHudThrottle(MCP_EntityPlane plane) {
      int throttle = MathHelper.clamp_int(plane.getThrottlePercent(), 0, 100);
      return String.format("THR   %d%%", new Object[]{Integer.valueOf(throttle)});
   }


   private String formatSimpleHudVerticalSpeed(MCP_EntityPlane plane) {
      double vs = plane.motionY * 20.0D;
      return String.format("VS    %+d m/s", new Object[]{Integer.valueOf((int)Math.round(vs))});
   }

   private String formatSimpleHudDamage(MCP_EntityPlane plane) {
      return String.format("DMG   %d%%", new Object[]{Integer.valueOf(MCH_HudShared.getDamagePercent(plane))});
   }

   private String formatSimpleHudGLoad(MCP_EntityPlane plane) {
      return "G     --";
   }

   private String formatSimpleHudAoA(MCP_EntityPlane plane) {
      double aoa = plane.getAngleOfAttackDegrees();
      if(Double.isNaN(aoa) || Double.isInfinite(aoa)) {
         return "AOA   --";
      }
      return String.format("AOA   %d\u00B0", new Object[]{Integer.valueOf((int)Math.round(aoa))});
   }

   private int getDamagePercent(MCP_EntityPlane plane) {
      int max = plane.getMaxHP();
      if(max <= 0) {
         return 100;
      }
      return MathHelper.clamp_int((int)Math.round((double)plane.getHP() * 100.0D / (double)max), 0, 100);
   }

   private List collectSimpleHudWarnings(MCP_EntityPlane plane) {
      List warnings = new ArrayList();
      if(plane.getStallSeverity() > 0.15D || plane.getStallDemand() > 0.35D) {
         warnings.add("STALL");
      }
      if(plane.isOverspeeding()) {
         warnings.add("OVERSPEED");
      }
      if(plane.getMaxFuel() > 0 && plane.getFuelP() < 0.10F && !plane.isInfinityFuel(plane.getRiddenByEntity(), true)) {
         warnings.add("LOW FUEL");
      }
      int dmg = MCH_HudShared.getDamagePercent(plane);
      if(dmg < 25) {
         warnings.add("CRITICAL DAMAGE");
      } else if(dmg < 50) {
         warnings.add("DAMAGED");
      }
      return warnings;
   }

   private String formatSimpleHudFuel(MCP_EntityPlane plane) {
      int minutes = this.getEstimatedFuelMinutes(plane);
      return minutes < 0 ? "FUEL  -- min" : String.format("FUEL  %d min", new Object[]{Integer.valueOf(minutes)});
   }

   private int getEstimatedFuelMinutes(MCP_EntityPlane plane) {
      if(plane.getMaxFuel() <= 0 || plane.getFuel() <= 0 || plane.isInfinityFuel(plane.getRiddenByEntity(), true)) {
         return -1;
      }

      if(plane.getAcInfo() == null || plane.getAcInfo().fuelConsumption <= 0.0F) {
         return -1;
      }

      double burnPerSecond = Math.min(plane.getNormalizedThrottle() * 1.4D, 1.0D)
            * (double)plane.getAcInfo().fuelConsumption * (double)plane.getFuelConsumptionFactor();
      if(burnPerSecond <= 0.01D) {
         return -1;
      }

      return Math.max(0, (int)Math.round((double)plane.getFuel() / burnPerSecond / 60.0D));
   }

   private String formatSimpleHudAltitude(MCP_EntityPlane plane) {
      return String.format("ALT   %d m", new Object[]{Integer.valueOf(this.getSimpleHudAltitudeMeters(plane))});
   }

   private int getSimpleHudAltitudeMeters(MCP_EntityPlane plane) {
      return Math.max(0, (int)Math.round(plane.posY));
   }


   private void drawNewPlaneWeaponHud(MCP_EntityPlane plane, EntityPlayer player) {
      if(!MCH_Config.EnableNewPlaneWeaponHud.prmBool) {
         return;
      }
      int leftMargin = 12;
      int rightMargin = Math.max(24, MCH_Config.NewPlaneWeaponHudRightMargin.prmInt);
      int maxTextWidth = Math.max(80, super.width - leftMargin - rightMargin - 8);
      List lines = MCH_HudShared.collectWeaponAmmoLines(plane, super.mc, maxTextWidth, false, -1);
      if(lines.isEmpty()) {
         return;
      }

      int width = Math.min(maxTextWidth + 8, Math.max(148, this.getMaxHudLineWidth(lines) + 8));
      int x = MathHelper.clamp_int(super.width - width - rightMargin, leftMargin, Math.max(leftMargin, super.width - width - rightMargin));
      int y = MathHelper.clamp_int(MCH_Config.NewPlaneWeaponHudY.prmInt, 12, Math.max(12, super.height - (lines.size() * 10 + 16)));
      int selected = plane.getCurrentWeaponID(player);
      this.drawHudPanel(x - 4, y - 4, width, lines.size() * 10 + 8);
      this.drawWeaponOverheatBars(plane, x, y + lines.size() * 10 + 2, width - 8);
      for(int i = 0; i < lines.size(); ++i) {
         int color = i == selected ? 0xFFFFFFFF : 0xFF9A9A9A;
         int glow = i == selected ? 0x66555555 : 0x55202020;
         this.drawHudText((String)lines.get(i), x, y + i * 10, color, glow);
      }
   }


   private void drawWeaponOverheatBars(MCH_EntityBaseVehicle ac, int x, int y, int width) {
      if(ac == null || ac.weapons == null) {
         return;
      }
      int row = 0;
      for(int i = 0; i < ac.weapons.length; ++i) {
         MCH_WeaponSet ws = ac.weapons[i];
         if(ws != null && ws.getCurrentWeapon() != null && ws.getCurrentWeapon().getInfo() != null) {
            int maxHeat = ws.getCurrentWeapon().getInfo().maxHeatCount;
            if(maxHeat > 0) {
               int by = y + row * 5;
               int fill = MathHelper.clamp_int((int)Math.round((double)Math.max(0, ws.currentHeat) * (double)width / (double)maxHeat), 0, width);
               drawRect(x, by, x + width, by + 3, 0x66303030);
               drawRect(x, by, x + fill, by + 3, ws.currentHeat >= maxHeat ? 0xFFFF3030 : 0xFFFFAA30);
               ++row;
            }
         }
      }
   }

   private List collectWeaponAmmoLines(MCP_EntityPlane plane, int maxTextWidth) {
      List lines = new ArrayList();
      if(plane.weapons == null) {
         return lines;
      }
      for(int i = 0; i < plane.weapons.length; ++i) {
         MCH_WeaponSet ws = plane.weapons[i];
         if(ws != null) {
            lines.add(this.formatWeaponAmmoLine(ws, maxTextWidth));
         }
      }
      return lines;
   }

   private String formatWeaponAmmoLine(MCH_WeaponSet ws, int maxTextWidth) {
      String ammo = this.getWeaponHudAmmo(ws);
      String name = this.getWeaponHudName(ws);
      String line = String.format("%-18s %5s", new Object[]{name, ammo});
      while(super.mc.fontRenderer.getStringWidth(line) > maxTextWidth && name.length() > 4) {
         name = name.substring(0, name.length() - 2) + "~";
         line = String.format("%-18s %5s", new Object[]{name, ammo});
      }
      return line;
   }

   private String getWeaponHudName(MCH_WeaponSet ws) {
      String name = ws.getName();
      return name == null || name.length() == 0 ? "WEAPON" : name.toUpperCase();
   }

   private String getWeaponHudAmmo(MCH_WeaponSet ws) {
      int ammo = ws.getAmmoNum() + ws.getRestAllAmmoNum();
      return ammo >= 0 ? String.valueOf(ammo) : "--";
   }

   private int getMaxHudLineWidth(List lines) {
      int width = 0;
      for(int i = 0; i < lines.size(); ++i) {
         width = Math.max(width, super.mc.fontRenderer.getStringWidth((String)lines.get(i)));
      }
      return width;
   }

   private void drawHudLines(List lines, int x, int y, int color, int glowColor) {
      for(int i = 0; i < lines.size(); ++i) {
         this.drawHudText((String)lines.get(i), x, y + i * 10, color, glowColor);
      }
   }

   private void drawHudText(String text, int x, int y, int color, int glowColor) {
      if(this.isVehicleHudGlowEnabled()) {
         this.drawString(text, x + 1, y + 1, glowColor);
      }
      this.drawString(text, x, y, color);
   }

   private void drawHudPanel(int x, int y, int width, int height) {
      if(this.isVehicleHudGlowEnabled()) {
         GL11.glPushMatrix();
         boolean blend = GL11.glIsEnabled(3042);
         int srcBlend = GL11.glGetInteger(3041);
         int dstBlend = GL11.glGetInteger(3040);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         drawRect(x, y, x + width, y + height, 0x66000000);
         GL11.glBlendFunc(srcBlend, dstBlend);
         if(!blend) {
            GL11.glDisable(3042);
         }
         GL11.glPopMatrix();
      }
   }

   private boolean isVehicleHudGlowEnabled() {
      return MCH_Config.EnableNewVehicleHudGlow != null ? MCH_Config.EnableNewVehicleHudGlow.prmBool : MCH_Config.EnableNewPlaneHudGlow.prmBool;
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
