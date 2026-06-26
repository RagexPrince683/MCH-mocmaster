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
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_McClient;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCP_GuiPlane extends MCH_BaseVehicleCommonGui {

   private static final ResourceLocation PLANE_MOUSE_AIM_RETICLE_TEXTURE = new ResourceLocation("mcheli", "textures/gui/plane_crosshair.png");
   private static final double CCIP_SCREEN_SMOOTHING = 0.35D;
   private static final double CCIP_IMPACT_RESET_DISTANCE = 64.0D;
   private static final double CCIP_PIPPER_SCALE = 1.35D;
   private double ccipScreenX;
   private double ccipScreenY;
   private boolean hasSmoothedCCIPScreenPos;
   private String lastCCIPWeaponName = "";
   private int lastCCIPDimension = Integer.MIN_VALUE;
   private Vec3 lastCCIPImpact;

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
                  this.drawPlaneCCIPReticle(plane, player);
               } else {
                  this.drawNewFlightThrottleHud(plane);
                  this.drawPlaneCCIPReticle(plane, player);
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
      lines.add(String.format("spd raw=%.0f disp=%.0f km/h stall=%.3f", new Object[]{
            Double.valueOf(MCH_HudShared.getRawSpeedKmh(plane)), Double.valueOf(MCH_HudShared.getDisplaySpeedPlane(plane)), Double.valueOf(stallSpeed)}));
      lines.add(String.format("phys fwd=%.3f hor=%.3f", new Object[]{
            Double.valueOf(plane.getLastForwardAirspeed()), Double.valueOf(plane.getLastHorizontalSpeed())}));
      lines.add(String.format("aoa=%.1f demand=%.2f sev=%.2f/%.2f/%.2f", new Object[]{
            Double.valueOf(plane.getAngleOfAttackDegrees()), Double.valueOf(plane.getStallDemand()),
            Double.valueOf(plane.getSpeedStallSeverity()), Double.valueOf(plane.getAoAStallSeverity()),
            Double.valueOf(plane.getDeepStallSeverity())}));
      lines.add(String.format("auth ctrl=%.2f pitch=%.2f up=%.2f down=%.2f", new Object[]{
            Double.valueOf(plane.getLastControlAuthority()), Double.valueOf(plane.getLastFinalPitchAuthority()),
            Double.valueOf(plane.getLastPitchUpAuthority()), Double.valueOf(plane.getLastPitchDownAuthority())}));
      lines.add(String.format("mom stall=%.4f forced=%.4f finalAV=%.4f", new Object[]{
            Double.valueOf(plane.getLastStallPitchMoment()), Double.valueOf(plane.getLastForcedNoseDownPitchDelta()),
            Double.valueOf(plane.getLastFinalPitchAngularVelocity())}));
      lines.add(String.format("lift L/W=%.2f T/W=%.2f loss=%.2f validClimb=%s", new Object[]{
            Double.valueOf(plane.getLiftToWeightRatio()), Double.valueOf(plane.getThrustToWeightRatio()),
            Double.valueOf(plane.getLastLiftLoss()), Boolean.valueOf(plane.isLastValidClimb())}));
      lines.add(String.format("energy ratio=%.2f deficit=%.2f dE=%.4f", new Object[]{
            Double.valueOf(plane.getLastPitchEnvelopeEnergyRatio()), Double.valueOf(plane.getLastEnergyDeficitSeverity()),
            Double.valueOf(plane.getLastEnergyDelta())}));

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

   private void drawPlaneCCIPReticle(MCP_EntityPlane plane, EntityPlayer player) {
      MCP_PlaneInfo info = plane != null ? plane.getPlaneInfo() : null;
      if(plane == null || player == null || info == null || !info.hasBallisticComputer || plane.isDestroyed()) {
         this.resetCCIPSmoothing();
         return;
      }
      if(plane.getSeatIdByEntity(player) != 0 || plane.getRiddenByEntity() == null) {
         this.resetCCIPSmoothing();
         return;
      }

      MCH_WeaponSet ws = plane.getCurrentWeapon(player);
      MCH_WeaponBase weapon = ws != null ? ws.getCurrentWeapon() : null;
      boolean enabled = MCP_PlaneCCIPHelper.isBombWeapon(weapon);
      MCP_PlaneCCIPHelper.Result result = null;
      Vec3 aircraftMotion = Vec3.createVectorHelper(plane.motionX, plane.motionY, plane.motionZ);
      if(enabled) {
         Vec3 shotOfs = weapon.getShotPos(plane);
         Vec3 release = Vec3.createVectorHelper(plane.posX + shotOfs.xCoord, plane.posY + shotOfs.yCoord, plane.posZ + shotOfs.zCoord);
         ReleaseKinematics releaseKinematics = this.getInitialBombVelocity(plane, weapon, aircraftMotion);
         result = MCP_PlaneCCIPHelper.predict(plane.worldObj, weapon.getInfo(), release, releaseKinematics.initialVelocity);
         result.releaseMode = releaseKinematics.releaseMode;
         result.ejectionVelocity = releaseKinematics.ejectionVelocity;
         result.initialVelocityDeltaFromAircraft = releaseKinematics.initialVelocityDeltaFromAircraft;
         result.initialVelocityUpDot = releaseKinematics.initialVelocityUpDot;
         result.initialVelocitySideDot = releaseKinematics.initialVelocitySideDot;
         result.warningImpossibleLaunch = releaseKinematics.warningImpossibleLaunch;
         if(result.valid) {
            ScreenPoint projected = this.projectWorldToHud(result.impact);
            if(projected != null && projected.visible) {
               ScreenPoint p = this.smoothCCIPScreenPoint(projected, result.impact, weapon);
               this.drawCCIPPipper(p.x, p.y, p.clamped);
            } else {
               this.resetCCIPSmoothing();
            }
         } else {
            this.resetCCIPSmoothing();
         }
      } else {
         this.resetCCIPSmoothing();
      }

      if(MCH_Config.PlaneMouseAimReticleDebug.prmBool || MCH_Config.DebugFlightControl.prmBool) {
         this.drawCCIPDebug(plane, weapon, enabled, result, aircraftMotion);
      }
   }

   private ReleaseKinematics getInitialBombVelocity(MCP_EntityPlane plane, MCH_WeaponBase weapon, Vec3 aircraftMotion) {
      ReleaseKinematics k = new ReleaseKinematics();
      k.releaseMode = "GRAVITY_BOMB";
      k.ejectionVelocity = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
      k.initialVelocity = Vec3.createVectorHelper(aircraftMotion.xCoord, aircraftMotion.yCoord, aircraftMotion.zCoord);

      if(weapon != null && weapon.getInfo() != null && weapon.getInfo().type != null && weapon.getInfo().type.equalsIgnoreCase("dispenser")) {
         k.releaseMode = "DISPENSER_EJECTED";
         // Match MCH_WeaponDispenser: add half of the weapon's forward acceleration vector to aircraft motion.
         // This uses the aircraft/weapon firing angles only; camera/freelook and aircraft roll are deliberately not inputs.
         Vec3 eject = mcheli.MCH_Lib.Rot2Vec3(plane.rotationYaw + weapon.fixRotationYaw, plane.rotationPitch + weapon.fixRotationPitch);
         k.ejectionVelocity = Vec3.createVectorHelper(eject.xCoord * (double)weapon.getInfo().acceleration * 0.5D,
               eject.yCoord * (double)weapon.getInfo().acceleration * 0.5D,
               eject.zCoord * (double)weapon.getInfo().acceleration * 0.5D);
         k.initialVelocity.xCoord += k.ejectionVelocity.xCoord;
         k.initialVelocity.yCoord += k.ejectionVelocity.yCoord;
         k.initialVelocity.zCoord += k.ejectionVelocity.zCoord;
      }

      k.initialVelocityDeltaFromAircraft = Vec3.createVectorHelper(k.initialVelocity.xCoord - aircraftMotion.xCoord,
            k.initialVelocity.yCoord - aircraftMotion.yCoord, k.initialVelocity.zCoord - aircraftMotion.zCoord);
      k.initialVelocityUpDot = k.initialVelocityDeltaFromAircraft.yCoord;
      Vec3 side = mcheli.MCH_Lib.Rot2Vec3(plane.rotationYaw + 90.0F, 0.0F);
      k.initialVelocitySideDot = k.initialVelocityDeltaFromAircraft.xCoord * side.xCoord + k.initialVelocityDeltaFromAircraft.zCoord * side.zCoord;
      double delta = Math.sqrt(k.initialVelocityDeltaFromAircraft.xCoord * k.initialVelocityDeltaFromAircraft.xCoord
            + k.initialVelocityDeltaFromAircraft.yCoord * k.initialVelocityDeltaFromAircraft.yCoord
            + k.initialVelocityDeltaFromAircraft.zCoord * k.initialVelocityDeltaFromAircraft.zCoord);
      boolean gravityBomb = "GRAVITY_BOMB".equals(k.releaseMode);
      k.warningImpossibleLaunch = (gravityBomb && (Math.abs(k.initialVelocitySideDot) > 0.05D || k.initialVelocityUpDot > 0.05D || delta > 0.10D))
            || (!gravityBomb && delta > 4.0D);
      return k;
   }

   private ScreenPoint smoothCCIPScreenPoint(ScreenPoint projected, Vec3 impact, MCH_WeaponBase weapon) {
      String weaponName = weapon != null && weapon.getInfo() != null ? weapon.getInfo().name : "";
      int dimension = super.mc.theWorld != null && super.mc.theWorld.provider != null ? super.mc.theWorld.provider.dimensionId : Integer.MIN_VALUE;
      boolean reset = !this.hasSmoothedCCIPScreenPos || !weaponName.equals(this.lastCCIPWeaponName) || dimension != this.lastCCIPDimension;
      if(!reset && this.lastCCIPImpact != null && impact != null && this.lastCCIPImpact.distanceTo(impact) > CCIP_IMPACT_RESET_DISTANCE) {
         reset = true;
      }

      if(reset) {
         this.ccipScreenX = projected.x;
         this.ccipScreenY = projected.y;
      } else {
         this.ccipScreenX += (projected.x - this.ccipScreenX) * CCIP_SCREEN_SMOOTHING;
         this.ccipScreenY += (projected.y - this.ccipScreenY) * CCIP_SCREEN_SMOOTHING;
      }

      this.hasSmoothedCCIPScreenPos = true;
      this.lastCCIPWeaponName = weaponName;
      this.lastCCIPDimension = dimension;
      this.lastCCIPImpact = impact != null ? Vec3.createVectorHelper(impact.xCoord, impact.yCoord, impact.zCoord) : null;
      return new ScreenPoint(this.ccipScreenX, this.ccipScreenY, projected.clamped);
   }

   private void resetCCIPSmoothing() {
      this.hasSmoothedCCIPScreenPos = false;
      this.lastCCIPWeaponName = "";
      this.lastCCIPDimension = Integer.MIN_VALUE;
      this.lastCCIPImpact = null;
   }

   private void drawCCIPDebug(MCP_EntityPlane plane, MCH_WeaponBase weapon, boolean enabled, MCP_PlaneCCIPHelper.Result result, Vec3 aircraftMotion) {
      Entity camera = super.mc.renderViewEntity != null ? super.mc.renderViewEntity : super.mc.thePlayer;
      String name = weapon != null && weapon.getInfo() != null ? weapon.getInfo().name : "none";
      double horizontalSpeed = Math.sqrt(aircraftMotion.xCoord * aircraftMotion.xCoord + aircraftMotion.zCoord * aircraftMotion.zCoord);
      String msg1 = String.format("CCIP enabled=%s weapon=%s mode=%s valid=%s reason=%s ticksToImpact=%d simulatedImpactDistance=%.1f",
            Boolean.valueOf(enabled), name, result != null ? result.releaseMode : "-", Boolean.valueOf(result != null && result.valid),
            result != null ? result.reasonInvalid : (enabled ? "not_predicted" : "not_bomb_or_disabled"),
            Integer.valueOf(result != null ? result.ticksSimulated : 0), Double.valueOf(result != null ? result.impactDistance : 0.0D));
      String msg2 = String.format("releasePos=%s impactWorldPos=%s aircraftSpeedHorizontal=%.3f",
            this.formatVec(result != null ? result.releasePos : null), this.formatVec(result != null ? result.impact : null), Double.valueOf(horizontalSpeed));
      String msg3 = String.format("aircraftMotion=%s ejectionVelocity=%s initialBombVelocity=%s deltaFromAircraft=%s",
            this.formatVec(aircraftMotion), this.formatVec(result != null ? result.ejectionVelocity : null),
            this.formatVec(result != null ? result.initialVelocity : null), this.formatVec(result != null ? result.initialVelocityDeltaFromAircraft : null));
      String msg4 = String.format("predictedGravity=%.4f predictedDrag=%.4f predictedTimestep=%.1f initialVelocityUpDot=%.3f initialVelocitySideDot=%.3f",
            Double.valueOf(result != null ? result.gravity : 0.0D), Double.valueOf(result != null ? result.horizontalDrag : 0.0D), Double.valueOf(result != null ? result.simulationTimeStep : 0.0D),
            Double.valueOf(result != null ? result.initialVelocityUpDot : 0.0D), Double.valueOf(result != null ? result.initialVelocitySideDot : 0.0D));
      String msg5 = String.format("warningImpossibleLaunch=%s cameraYaw/Pitch=%.1f/%.1f planeYaw/Pitch/Roll=%.1f/%.1f/%.1f",
            Boolean.valueOf(result != null && result.warningImpossibleLaunch),
            Float.valueOf(camera != null ? camera.rotationYaw : 0.0F), Float.valueOf(camera != null ? camera.rotationPitch : 0.0F),
            Float.valueOf(plane.rotationYaw), Float.valueOf(plane.rotationPitch), Float.valueOf(plane.getRotRoll()));
      this.drawString(msg1, super.centerX - 170, super.centerY + 70, 0xFF55FF66);
      this.drawString(msg2, super.centerX - 170, super.centerY + 80, 0xFF55FF66);
      this.drawString(msg3, super.centerX - 170, super.centerY + 90, 0xFF55FF66);
      this.drawString(msg4, super.centerX - 170, super.centerY + 100, 0xFF55FF66);
      this.drawString(msg5, super.centerX - 170, super.centerY + 110, 0xFF55FF66);
   }

   private String formatVec(Vec3 v) {
      return v == null ? "-" : String.format("%.2f,%.2f,%.2f", Double.valueOf(v.xCoord), Double.valueOf(v.yCoord), Double.valueOf(v.zCoord));
   }

   private Vec3 getInterpolatedEntityPos(Entity entity, float partialTicks) {
      return Vec3.createVectorHelper(entity.prevPosX + (entity.posX - entity.prevPosX) * (double)partialTicks,
            entity.prevPosY + (entity.posY - entity.prevPosY) * (double)partialTicks,
            entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double)partialTicks);
   }


   private ScreenPoint projectWorldToHud(Vec3 pos) {
      Entity camera = super.mc.renderViewEntity != null ? super.mc.renderViewEntity : super.mc.thePlayer;
      if(pos == null || camera == null) {
         return null;
      }
      float partialTicks = this.smoothCamPartialTicks;
      Vec3 cameraPos = this.getInterpolatedEntityPos(camera, partialTicks);
      double dx = pos.xCoord - cameraPos.xCoord;
      double dy = pos.yCoord - (cameraPos.yCoord + (double)camera.getEyeHeight());
      double dz = pos.zCoord - cameraPos.zCoord;
      float yaw = camera.prevRotationYaw + MathHelper.wrapAngleTo180_float(camera.rotationYaw - camera.prevRotationYaw) * partialTicks;
      float pitch = camera.prevRotationPitch + (camera.rotationPitch - camera.prevRotationPitch) * partialTicks;
      Vec3 forward = mcheli.MCH_Lib.Rot2Vec3(yaw, pitch);
      Vec3 right = mcheli.MCH_Lib.Rot2Vec3(yaw + 90.0F, 0.0F);
      Vec3 up = Vec3.createVectorHelper(right.yCoord * forward.zCoord - right.zCoord * forward.yCoord,
            right.zCoord * forward.xCoord - right.xCoord * forward.zCoord,
            right.xCoord * forward.yCoord - right.yCoord * forward.xCoord);
      double localZ = dx * forward.xCoord + dy * forward.yCoord + dz * forward.zCoord;
      if(localZ <= 0.05D) return null;
      double localX = dx * right.xCoord + dy * right.yCoord + dz * right.zCoord;
      double localY = dx * up.xCoord + dy * up.yCoord + dz * up.zCoord;
      double scale = (double)super.height * 0.75D / localZ;
      double x = (double)super.centerX + localX * scale;
      double y = (double)super.centerY - localY * scale;
      if(x < 0.0D || x > (double)super.width || y < 0.0D || y > (double)super.height) {
         return null;
      }
      return new ScreenPoint(x, y, false);
   }

   private void drawCCIPPipper(double x, double y, boolean clamped) {
      int color = clamped ? 0xAA55FF66 : 0xF055FF66;
      double r = 10.0D * CCIP_PIPPER_SCALE;
      double tickInner = r + 2.0D * CCIP_PIPPER_SCALE;
      double tickOuter = r + 6.0D * CCIP_PIPPER_SCALE;
      double[] circle = new double[34];
      for(int i = 0; i <= 16; ++i) {
         double a = Math.PI * 2.0D * (double)i / 16.0D;
         circle[i * 2] = x + Math.cos(a) * r;
         circle[i * 2 + 1] = y + Math.sin(a) * r;
      }
      this.drawLine(circle, color, 2);
      this.drawLine(new double[]{x - 2.0D, y, x + 2.0D, y, x, y - 2.0D, x, y + 2.0D,
            x - tickOuter, y, x - tickInner, y, x + tickInner, y, x + tickOuter, y,
            x, y - tickOuter, x, y - tickInner, x, y + tickInner, x, y + tickOuter}, color);
      this.drawString("CCIP", (int)(x + r + 7.0D), (int)(y + r + 4.0D), color);
   }

   private static class ReleaseKinematics {
      Vec3 initialVelocity;
      Vec3 ejectionVelocity;
      Vec3 initialVelocityDeltaFromAircraft;
      double initialVelocityUpDot;
      double initialVelocitySideDot;
      boolean warningImpossibleLaunch;
      String releaseMode;
   }

   private static class ScreenPoint {
      final double x; final double y; final boolean clamped; final boolean visible = true;
      ScreenPoint(double x, double y, boolean clamped) { this.x = x; this.y = y; this.clamped = clamped; }
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
