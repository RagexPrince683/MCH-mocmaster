package mcheli.plane;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_ClientCommonTickHandler;
import mcheli.MCH_Config;
import mcheli.MCH_KeyName;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.MCH_Vector2;
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
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import mcheli.hud.layout.MCH_HudLayoutManager;
import org.lwjgl.util.glu.GLU;

@SideOnly(Side.CLIENT)
public class MCP_GuiPlane extends MCH_BaseVehicleCommonGui {

   private static final ResourceLocation PLANE_MOUSE_AIM_RETICLE_TEXTURE = new ResourceLocation("mcheli", "textures/gui/plane_crosshair.png");
   private static final double CCIP_SCREEN_SMOOTHING = 0.35D;
   private static final double CCIP_IMPACT_RESET_DISTANCE = 64.0D;
   private static final double CCIP_PIPPER_SCALE = 1.35D;
   private static final int CCIP_HYSTERESIS_GRACE_TICKS = 5;
   private static final int CCIP_IMPACT_GRACE_TICKS = MCP_ClientPlaneTickHandler.BOMB_RETICLE_IMPACT_GRACE_TICKS;
   private static final FloatBuffer CCIP_PROJECTED_COORDS = BufferUtils.createFloatBuffer(3);
   private static Field activeRenderModelViewField;
   private static Field activeRenderProjectionField;
   private static Field activeRenderViewportField;
   private static boolean activeRenderBufferLookupAttempted;
   private double ccipScreenX;
   private double ccipScreenY;
   private boolean hasSmoothedCCIPScreenPos;
   private String lastCCIPWeaponName = "";
   private int lastCCIPDimension = Integer.MIN_VALUE;
   private Vec3 lastCCIPImpact;
   private Vec3 lastValidCCIPImpact;
   private int lastValidCCIPEntityId = Integer.MIN_VALUE;
   private int lastValidCCIPDimension = Integer.MIN_VALUE;
   private String lastValidCCIPWeaponName = "";
   private int lastValidCCIPTick = Integer.MIN_VALUE;
   private boolean lastCCIPGraceUsed;
   private int cachedCCIPTick = Integer.MIN_VALUE;
   private int cachedCCIPEntityId = Integer.MIN_VALUE;
   private int cachedCCIPDimension = Integer.MIN_VALUE;
   private String cachedCCIPWeaponName = "";
   private MCP_PlaneCCIPHelper.Result cachedCCIPResult;
   private MCP_PlaneCCIPHelper.Result lastStableCCIPResult;
   private int lastStableCCIPTick = Integer.MIN_VALUE;
   private int lastStableCCIPEntityId = Integer.MIN_VALUE;
   private int lastStableCCIPDimension = Integer.MIN_VALUE;
   private String lastStableCCIPWeaponName = "";
   private String lastCCIPProjectionStatus = "invalid";
   private String lastCCIPProjectMode = "invalid";
   private double lastCCIPProjectWinZ = Double.NaN;
   private boolean lastCCIPFarPlaneRejected;
   private final CCIPProjectionDiagnostic lastCCIPProjectionDiagnostic = new CCIPProjectionDiagnostic();

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
         MCP_PlaneInfo info = plane.getPlaneInfo();
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
                  MCH_HudLayoutManager.renderBuiltin("plane", "plane.flight_readouts", () -> this.drawNewPlaneSimpleHud(plane));
                  MCH_HudLayoutManager.renderBuiltin("plane", "plane.weapon_list", () -> this.drawNewPlaneWeaponHud(plane, player));
                  MCH_HudLayoutManager.renderBuiltin("plane", "plane.debug", () -> this.drawNewPlaneDebugHud(plane));
               } else {
                  MCH_HudLayoutManager.renderBuiltin("plane", "plane.throttle", () -> this.drawNewFlightThrottleHud(plane));
               }
            }

            if(plane.getTVMissile() != null && (plane.getIsGunnerMode(player) || plane.isUAV())) {
               this.drawTvMissileNoise(plane, plane.getTVMissile());
            } else {
               MCH_HudLayoutManager.renderBuiltin("plane", "plane.keybinds", () -> this.drawKeybind(plane, player, seatID));
            }
         }

         // CCIP is a world-space impact cue, not part of the optional full HUD.
         // Keep it available in third person even when DisplayHUDThirdPerson is disabled.
         if(seatID == 0 && (!plane.getIsGunnerMode(player) || (!isThirdPersonView && info != null && info.hasBombSight && MCP_ClientPlaneTickHandler.isBombReticleMode(plane)))) {
            this.drawPlaneCCIPReticle(plane, player, plane.getIsGunnerMode(player));
         } else if(seatID == 0 && plane.getIsGunnerMode(player)) {
            this.resetCCIPState();
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
      lines.add(String.format("X: %+.1f", new Object[]{Double.valueOf(plane.posX)}));
      //lines.add(String.format("Y: %+.1f", new Object[]{Double.valueOf(plane.posY)})); altitude already covers this
      lines.add(String.format("Z: %+.1f", new Object[]{Double.valueOf(plane.posZ)}));
      lines.add(MCH_HudShared.formatVerticalSpeed(plane));
      lines.add(String.format("PITCH %+.0f\u00B0", new Object[]{Float.valueOf(this.getDisplayPitchDegrees(plane))}));
      lines.add(MCH_HudShared.formatFuelMinutes(plane));
      lines.add(MCH_HudShared.formatDamagePercent(plane));
      lines.add(this.formatSimpleHudGLoad(plane));
      lines.add(this.formatSimpleHudAoA(plane));

      List warnings = this.collectSimpleHudWarnings(plane);
      int x = MathHelper.clamp_int(MCH_Config.NewPlaneSimpleHudX.prmInt, 4, Math.max(4, super.width - 118));
      int mainPanelHeight = lines.size() * 10 + 8;
      int warningPanelHeight = warnings.isEmpty()?0:warnings.size() * 10 + 4;
      int panelStackHeight = mainPanelHeight + (warnings.isEmpty()?0:warningPanelHeight + 3);
      int y = MathHelper.clamp_int(MCH_Config.NewPlaneSimpleHudY.prmInt, 4, Math.max(4, super.height - panelStackHeight));
      this.drawHudPanel(x - 4, y - 4, 116, mainPanelHeight);
      this.drawHudLines(lines, x, y, 0xFFE8E8E8, 0x66303030);

      this.drawStickInputGauge(x + 126, y + 12);
      this.drawNewPlaneRadarGauge(plane, x + 126, y + 54);

      if(!warnings.isEmpty()) {
         int wy = y + lines.size() * 10 + 5;
         this.drawHudPanel(x - 4, wy - 2, 116, warnings.size() * 10 + 4);
         this.drawHudLines(warnings, x, wy, 0xFFFFD65A, 0x66FFAA00);
      }
   }


   private void drawNewPlaneRadarGauge(MCP_EntityPlane plane, int x, int y) {
      if(plane == null || !plane.hasRadar() || !plane.isRadarActive()) {
         return;
      }
      int size = 64;
      this.drawNewPlaneRadarTexture(plane, x, y, size);
      this.drawLine(new double[]{(double)x, (double)(y + size / 2), (double)(x + size), (double)(y + size / 2),
            (double)(x + size / 2), (double)y, (double)(x + size / 2), (double)(y + size)}, 0x80FFFFFF, 1);
      this.drawRadarPoints(plane.getRadarEntityList(), plane, x, y, size, -14101432);
      this.drawRadarPoints(plane.getRadarEnemyList(), plane, x, y, size, 0xFFDF0408);
   }

   private void drawNewPlaneRadarTexture(MCP_EntityPlane plane, int x, int y, int size) {
      GL11.glPushMatrix();
      boolean blend = GL11.glIsEnabled(3042);
      int srcBlend = GL11.glGetInteger(3041);
      int dstBlend = GL11.glGetInteger(3040);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      W_McClient.MOD_bindTexture("textures/gui/heli_hud.png");
      this.drawTexturedModalRectRotate((double)x, (double)y, (double)size, (double)size, 0.0D, 0.0D, 128.0D, 128.0D, 0.0F);
      this.drawTexturedModalRectRotate((double)(x + 16), (double)y, 32.0D, (double)size, 128.0D, 0.0D, 64.0D, 128.0D, (float)plane.getRadarRotate());
      GL11.glBlendFunc(srcBlend, dstBlend);
      if(!blend) {
         GL11.glDisable(3042);
      }
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
   }

   private void drawRadarPoints(ArrayList src, MCP_EntityPlane plane, int left, int top, int size, int color) {
      if(src == null || src.isEmpty()) {
         return;
      }
      double half = (double)size / 2.0D;
      double factor = (double)size / 64.0D;
      double[] points = new double[src.size() * 2];
      int idx = 0;
      for(Iterator it = src.iterator(); it.hasNext(); idx += 2) {
         MCH_Vector2 v = (MCH_Vector2)it.next();
         points[idx] = v.x / 2.0D * factor;
         points[idx + 1] = v.y / 2.0D * factor;
      }
      MCH_Lib.rotatePoints(points, -plane.getRotYaw() - 180.0F);
      ArrayList drawPoints = new ArrayList();
      for(int i = 0; i + 1 < points.length; i += 2) {
         if(points[i] > -half && points[i] < half && points[i + 1] > -half && points[i + 1] < half) {
            drawPoints.add(Double.valueOf(points[i] + (double)left + half));
            drawPoints.add(Double.valueOf(points[i + 1] + (double)top + half));
         }
      }
      this.drawPoints(drawPoints, color, Math.max(2, MCH_Gui.scaleFactor * 2));
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
      if(this.isPlaneFreelookIndicatorActive(plane)) {
         warnings.add("FREELOOK");
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

   private boolean isPlaneFreelookIndicatorActive(MCP_EntityPlane plane) {
      if(plane == null) {
         return false;
      }
      EntityPlayer player = super.mc != null?super.mc.thePlayer:null;
      return plane.isFreeLookMode() || MCP_PlaneChaseCamera.shouldUseHoldFreelookAsCameraOnly(plane, player);
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

   private void drawPlaneCCIPReticle(MCP_EntityPlane plane, EntityPlayer player, boolean bomberMode) {
      this.resetCCIPProjectionDiagnostic();
      MCP_PlaneInfo info = plane != null ? plane.getPlaneInfo() : null;
      if(plane == null || player == null || info == null || !info.hasBallisticComputer || plane.isDestroyed()) {
         this.resetCCIPState();
         return;
      }
      if(plane.getSeatIdByEntity(player) != 0 || plane.getRiddenByEntity() == null) {
         this.resetCCIPState();
         return;
      }

      MCH_WeaponSet ws = plane.getCurrentWeapon(player);
      MCH_WeaponBase weapon = ws != null ? ws.getCurrentWeapon() : null;
      boolean enabled = MCP_PlaneCCIPHelper.isBombWeapon(weapon);
      MCP_PlaneCCIPHelper.Result result = null;
      Vec3 aircraftMotion = Vec3.createVectorHelper(plane.motionX, plane.motionY, plane.motionZ);

      if(enabled) {
         String weaponName = weapon != null && weapon.getInfo() != null ? weapon.getInfo().name : "";
         int dimension = plane.worldObj != null && plane.worldObj.provider != null
               ? plane.worldObj.provider.dimensionId : Integer.MIN_VALUE;
         int entityId = plane.getEntityId();
         this.validateCCIPGraceCache(entityId, dimension, weaponName);
         this.lastCCIPGraceUsed = false;
         result = this.getOrUpdateCCIPPrediction(plane, ws, weapon, aircraftMotion);
         if(bomberMode) {
            // The hasBombSight reticle is a first-person optical sight attached to
            // the forced bomber-sight camera. Do not cull it through the world
            // impact projection: when the camera is correctly aimed at a very
            // distant focus point, the impact can fall outside the active render
            // frustum/far plane even though the local sight should remain visible.
            this.drawBombSightReticle((double)super.centerX, (double)super.centerY, false);
            this.resetCCIPSmoothing();
         } else if(result != null && result.valid && result.impact != null) {
            this.updateLastValidCCIPImpact(result.impact, entityId, dimension, weaponName, plane.ticksExisted);
            ScreenPoint projected = this.projectWorldToRenderCamera(result.impact, this.smoothCamPartialTicks);
            if(projected != null && projected.visible) {
               ScreenPoint smoothed = this.smoothCCIPScreenPoint(projected, result.impact, weapon);
               this.drawCCIPPipper(smoothed.x, smoothed.y, smoothed.clamped, false);
            } else if(!this.drawGraceCCIPPipper(plane, weapon, entityId, dimension, weaponName)) {
               this.resetCCIPSmoothing();
            }
         } else {
            this.lastCCIPProjectionStatus = "invalid";
            if(!this.drawGraceCCIPPipper(plane, weapon, entityId, dimension, weaponName)) {
               this.resetCCIPSmoothing();
            }
         }
      } else {
         this.resetCCIPState();
      }

      if(MCH_Config.PlaneMouseAimReticleDebug.prmBool || MCH_Config.DebugFlightControl.prmBool) {
         this.drawCCIPDebug(plane, weapon, enabled, result, aircraftMotion);
      }
   }


   private void validateCCIPGraceCache(int entityId, int dimension, String weaponName) {
      if(this.lastValidCCIPImpact != null && (this.lastValidCCIPEntityId != entityId
            || this.lastValidCCIPDimension != dimension || !weaponName.equals(this.lastValidCCIPWeaponName))) {
         this.clearCCIPGraceCache();
      }
   }

   private void updateLastValidCCIPImpact(Vec3 impact, int entityId, int dimension, String weaponName, int tick) {
      if(impact == null) {
         return;
      }
      this.lastValidCCIPImpact = Vec3.createVectorHelper(impact.xCoord, impact.yCoord, impact.zCoord);
      this.lastValidCCIPEntityId = entityId;
      this.lastValidCCIPDimension = dimension;
      this.lastValidCCIPWeaponName = weaponName;
      this.lastValidCCIPTick = tick;
   }

   private boolean drawGraceCCIPPipper(MCP_EntityPlane plane, MCH_WeaponBase weapon, int entityId, int dimension, String weaponName) {
      if(!this.canUseLastValidCCIPImpact(plane, entityId, dimension, weaponName)) {
         return false;
      }
      ScreenPoint projected = this.projectWorldToRenderCamera(this.lastValidCCIPImpact, this.smoothCamPartialTicks);
      if(projected == null || !projected.visible) {
         if(!this.hasSmoothedCCIPScreenPos) {
            return false;
         }
         projected = new ScreenPoint(this.ccipScreenX, this.ccipScreenY, true);
      }
      this.lastCCIPGraceUsed = true;
      ScreenPoint smoothed = this.smoothCCIPScreenPoint(projected, this.lastValidCCIPImpact, weapon);
      this.drawCCIPPipper(smoothed.x, smoothed.y, true, false, true);
      return true;
   }

   private boolean canUseLastValidCCIPImpact(MCP_EntityPlane plane, int entityId, int dimension, String weaponName) {
      return plane != null && this.lastValidCCIPImpact != null
            && plane.ticksExisted - this.lastValidCCIPTick <= CCIP_IMPACT_GRACE_TICKS
            && this.lastValidCCIPEntityId == entityId
            && this.lastValidCCIPDimension == dimension
            && weaponName.equals(this.lastValidCCIPWeaponName);
   }

   private void clearCCIPGraceCache() {
      this.lastValidCCIPImpact = null;
      this.lastValidCCIPEntityId = Integer.MIN_VALUE;
      this.lastValidCCIPDimension = Integer.MIN_VALUE;
      this.lastValidCCIPWeaponName = "";
      this.lastValidCCIPTick = Integer.MIN_VALUE;
      this.lastCCIPGraceUsed = false;
   }

   private MCP_PlaneCCIPHelper.Result getOrUpdateCCIPPrediction(MCP_EntityPlane plane, MCH_WeaponSet ws,
         MCH_WeaponBase weapon, Vec3 aircraftMotion) {
      String weaponName = weapon != null && weapon.getInfo() != null ? weapon.getInfo().name : "";
      int dimension = plane.worldObj != null && plane.worldObj.provider != null
            ? plane.worldObj.provider.dimensionId : Integer.MIN_VALUE;
      int entityId = plane.getEntityId();

      boolean refresh = this.cachedCCIPResult == null
            || this.cachedCCIPTick != plane.ticksExisted
            || this.cachedCCIPEntityId != entityId
            || this.cachedCCIPDimension != dimension
            || !weaponName.equals(this.cachedCCIPWeaponName);

      if(!refresh) {
         return this.cachedCCIPResult;
      }

      Vec3 shotOffset = weapon.getShotPos(plane);
      Vec3 release = Vec3.createVectorHelper(
            plane.posX + shotOffset.xCoord,
            plane.posY + shotOffset.yCoord,
            plane.posZ + shotOffset.zCoord);

      ReleaseKinematics k = this.getInitialBombVelocity(plane, ws, weapon, aircraftMotion);

      // MCH_WeaponDispenser advances the spawned entity by half of its initial
      // velocity before adding it to the world. Mirror that exact spawn position.
      if("DISPENSER_EJECTED".equals(k.releaseMode)) {
         release.xCoord += k.initialVelocity.xCoord * 0.5D;
         release.yCoord += k.initialVelocity.yCoord * 0.5D;
         release.zCoord += k.initialVelocity.zCoord * 0.5D;
      }

      MCP_PlaneCCIPHelper.Result result = MCP_PlaneCCIPHelper.predict(
            plane.worldObj, weapon.getInfo(), release, k.initialVelocity, aircraftMotion);
      result.releaseMode = k.releaseMode;
      result.ejectionVelocity = k.ejectionVelocity;
      result.initialVelocityDeltaFromAircraft = k.initialVelocityDeltaFromAircraft;
      result.initialVelocityUpDot = k.initialVelocityUpDot;
      result.initialVelocitySideDot = k.initialVelocitySideDot;
      result.warningImpossibleLaunch = k.warningImpossibleLaunch;

      if(result.valid && result.impact != null && !result.unloadedChunkFallback) {
         this.lastStableCCIPResult = result;
         this.lastStableCCIPTick = plane.ticksExisted;
         this.lastStableCCIPEntityId = entityId;
         this.lastStableCCIPDimension = dimension;
         this.lastStableCCIPWeaponName = weaponName;
      } else if(this.canReuseStableCCIP(plane.ticksExisted, entityId, dimension, weaponName)) {
         MCP_PlaneCCIPHelper.Result transientResult = result;
         result = this.copyCCIPResult(this.lastStableCCIPResult);
         result.hysteresisReused = true;
         result.hysteresisAgeTicks = plane.ticksExisted - this.lastStableCCIPTick;
         result.unloadedChunkFallback = transientResult.unloadedChunkFallback;
         result.firstUnloadedPosition = this.copyVec(transientResult.firstUnloadedPosition);
         result.firstUnloadedChunkX = transientResult.firstUnloadedChunkX;
         result.firstUnloadedChunkZ = transientResult.firstUnloadedChunkZ;
         result.fallbackReason = transientResult.fallbackReason;
         result.fallbackTargetY = transientResult.fallbackTargetY;
         result.syntheticFallback = transientResult.syntheticFallback;
         result.reasonInvalid = transientResult.reasonInvalid;
      }

      this.cachedCCIPTick = plane.ticksExisted;
      this.cachedCCIPEntityId = entityId;
      this.cachedCCIPDimension = dimension;
      this.cachedCCIPWeaponName = weaponName;
      this.cachedCCIPResult = result;
      return result;
   }

   private boolean canReuseStableCCIP(int tick, int entityId, int dimension, String weaponName) {
      return this.lastStableCCIPResult != null
            && this.lastStableCCIPResult.valid
            && this.lastStableCCIPResult.impact != null
            && tick - this.lastStableCCIPTick <= CCIP_HYSTERESIS_GRACE_TICKS
            && this.lastStableCCIPEntityId == entityId
            && this.lastStableCCIPDimension == dimension
            && weaponName.equals(this.lastStableCCIPWeaponName);
   }

   private MCP_PlaneCCIPHelper.Result copyCCIPResult(MCP_PlaneCCIPHelper.Result source) {
      MCP_PlaneCCIPHelper.Result copy = new MCP_PlaneCCIPHelper.Result();
      copy.valid = source.valid;
      copy.impact = this.copyVec(source.impact);
      copy.releasePos = this.copyVec(source.releasePos);
      copy.ticksSimulated = source.ticksSimulated;
      copy.impactDistance = source.impactDistance;
      copy.releaseAltitude = source.releaseAltitude;
      copy.initialVelocity = this.copyVec(source.initialVelocity);
      copy.finalVelocity = this.copyVec(source.finalVelocity);
      copy.aircraftMotion = this.copyVec(source.aircraftMotion);
      copy.gravity = source.gravity;
      copy.horizontalDrag = source.horizontalDrag;
      copy.accelerationFactor = source.accelerationFactor;
      copy.simulationTimeStep = source.simulationTimeStep;
      copy.speedDependsAircraft = source.speedDependsAircraft;
      copy.speedDependsAircraftApplied = source.speedDependsAircraftApplied;
      copy.speedAddedFromAircraft = source.speedAddedFromAircraft;
      copy.predictedAccelerationBeforeAircraft = source.predictedAccelerationBeforeAircraft;
      copy.predictedAccelerationAfterAircraft = source.predictedAccelerationAfterAircraft;
      copy.unloadedChunkFallback = source.unloadedChunkFallback;
      copy.firstUnloadedPosition = this.copyVec(source.firstUnloadedPosition);
      copy.firstUnloadedChunkX = source.firstUnloadedChunkX;
      copy.firstUnloadedChunkZ = source.firstUnloadedChunkZ;
      copy.fallbackReason = source.fallbackReason;
      copy.fallbackTargetY = source.fallbackTargetY;
      copy.hitRealTerrain = source.hitRealTerrain;
      copy.syntheticFallback = source.syntheticFallback;
      copy.ejectionVelocity = this.copyVec(source.ejectionVelocity);
      copy.initialVelocityDeltaFromAircraft = this.copyVec(source.initialVelocityDeltaFromAircraft);
      copy.initialVelocityUpDot = source.initialVelocityUpDot;
      copy.initialVelocitySideDot = source.initialVelocitySideDot;
      copy.warningImpossibleLaunch = source.warningImpossibleLaunch;
      copy.releaseMode = source.releaseMode;
      copy.reasonInvalid = source.reasonInvalid;
      return copy;
   }

   private Vec3 copyVec(Vec3 source) {
      return source != null ? Vec3.createVectorHelper(source.xCoord, source.yCoord, source.zCoord) : null;
   }

   private ReleaseKinematics getInitialBombVelocity(MCP_EntityPlane plane, MCH_WeaponSet ws,
         MCH_WeaponBase weapon, Vec3 aircraftMotion) {
      ReleaseKinematics k = new ReleaseKinematics();
      k.releaseMode = "GRAVITY_BOMB";
      k.ejectionVelocity = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
      k.initialVelocity = Vec3.createVectorHelper(
            aircraftMotion.xCoord, aircraftMotion.yCoord, aircraftMotion.zCoord);

      if(weapon != null && weapon.getInfo() != null && weapon.getInfo().type != null
            && weapon.getInfo().type.equalsIgnoreCase("dispenser")) {
         k.releaseMode = "DISPENSER_EJECTED";

         // Match MCH_WeaponSet.use + MCH_WeaponDispenser.shot exactly.
         float yaw = plane.rotationYaw + (ws != null ? ws.rotationYaw : 0.0F) + weapon.fixRotationYaw;
         float pitch = plane.rotationPitch + (ws != null ? ws.rotationPitch : 0.0F) + weapon.fixRotationPitch;
         float roll = plane.getRotRoll();
         Vec3 direction = mcheli.MCH_Lib.RotVec3(0.0D, 0.0D, 1.0D, -yaw, -pitch, -roll);
         double length = direction.lengthVector();
         if(length > 1.0E-7D) {
            double constructorSpeed = Math.min(3.9D, weapon.getEffectiveLaunchAcceleration());
            double ejectionScale = constructorSpeed * 0.5D / length;
            k.ejectionVelocity = Vec3.createVectorHelper(
                  direction.xCoord * ejectionScale,
                  direction.yCoord * ejectionScale,
                  direction.zCoord * ejectionScale);
            k.initialVelocity.xCoord += k.ejectionVelocity.xCoord;
            k.initialVelocity.yCoord += k.ejectionVelocity.yCoord;
            k.initialVelocity.zCoord += k.ejectionVelocity.zCoord;
         }
      }

      k.initialVelocityDeltaFromAircraft = Vec3.createVectorHelper(
            k.initialVelocity.xCoord - aircraftMotion.xCoord,
            k.initialVelocity.yCoord - aircraftMotion.yCoord,
            k.initialVelocity.zCoord - aircraftMotion.zCoord);
      k.initialVelocityUpDot = k.initialVelocityDeltaFromAircraft.yCoord;
      Vec3 side = mcheli.MCH_Lib.Rot2Vec3(plane.rotationYaw + 90.0F, 0.0F);
      k.initialVelocitySideDot = k.initialVelocityDeltaFromAircraft.xCoord * side.xCoord
            + k.initialVelocityDeltaFromAircraft.zCoord * side.zCoord;
      double delta = k.initialVelocityDeltaFromAircraft.lengthVector();
      boolean gravityBomb = "GRAVITY_BOMB".equals(k.releaseMode);
      k.warningImpossibleLaunch = (gravityBomb && (Math.abs(k.initialVelocitySideDot) > 0.05D
            || k.initialVelocityUpDot > 0.05D || delta > 0.10D))
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

   private void resetCCIPState() {
      this.resetCCIPSmoothing();
      this.cachedCCIPTick = Integer.MIN_VALUE;
      this.cachedCCIPEntityId = Integer.MIN_VALUE;
      this.cachedCCIPDimension = Integer.MIN_VALUE;
      this.cachedCCIPWeaponName = "";
      this.cachedCCIPResult = null;
      this.lastStableCCIPResult = null;
      this.lastStableCCIPTick = Integer.MIN_VALUE;
      this.lastStableCCIPEntityId = Integer.MIN_VALUE;
      this.lastStableCCIPDimension = Integer.MIN_VALUE;
      this.lastStableCCIPWeaponName = "";
      this.resetCCIPProjectionDiagnostic();
      this.clearCCIPGraceCache();
   }

   private void drawCCIPDebug(MCP_EntityPlane plane, MCH_WeaponBase weapon, boolean enabled, MCP_PlaneCCIPHelper.Result result, Vec3 aircraftMotion) {
      Entity camera = super.mc.renderViewEntity != null ? super.mc.renderViewEntity : super.mc.thePlayer;
      String name = weapon != null && weapon.getInfo() != null ? weapon.getInfo().name : "none";
      double horizontalSpeed = Math.sqrt(aircraftMotion.xCoord * aircraftMotion.xCoord + aircraftMotion.zCoord * aircraftMotion.zCoord);
      String msg1 = String.format("CCIP enabled=%s weapon=%s mode=%s valid=%s reason=%s ticksToImpact=%d simulatedImpactDistance=%.1f",
            Boolean.valueOf(enabled), name, result != null ? result.releaseMode : "-", Boolean.valueOf(result != null && result.valid),
            result != null ? result.reasonInvalid : (enabled ? "not_predicted" : "not_bomb_or_disabled"),
            Integer.valueOf(result != null ? result.ticksSimulated : 0), Double.valueOf(result != null ? result.impactDistance : 0.0D));
      String msg2 = String.format("releasePos=%s impactWorldPos=%s aircraftSpeedHorizontal=%.3f unloadedFallback=%s synthetic=%s realTerrain=%s",
            this.formatVec(result != null ? result.releasePos : null), this.formatVec(result != null ? result.impact : null),
            Double.valueOf(horizontalSpeed), Boolean.valueOf(result != null && result.unloadedChunkFallback),
            Boolean.valueOf(result != null && result.syntheticFallback), Boolean.valueOf(result != null && result.hitRealTerrain));
      String msg3 = String.format("aircraftMotion=%s ejectionVelocity=%s initialBombVelocity=%s deltaFromAircraft=%s",
            this.formatVec(aircraftMotion), this.formatVec(result != null ? result.ejectionVelocity : null),
            this.formatVec(result != null ? result.initialVelocity : null), this.formatVec(result != null ? result.initialVelocityDeltaFromAircraft : null));
      String msg4 = String.format("predictedGravity=%.4f predictedDrag=%.4f predictedTimestep=%.1f speedDependsAircraft=%s speedAddedFromAircraft=%.4f",
            Double.valueOf(result != null ? result.gravity : 0.0D), Double.valueOf(result != null ? result.horizontalDrag : 0.0D), Double.valueOf(result != null ? result.simulationTimeStep : 0.0D),
            Boolean.valueOf(result != null && result.speedDependsAircraft), Double.valueOf(result != null ? result.speedAddedFromAircraft : 0.0D));
      String msg5 = String.format("predictedAcceleration before/after=%.4f/%.4f speedDependsApplied=%s initialVelocityUpDot=%.3f initialVelocitySideDot=%.3f",
            Double.valueOf(result != null ? result.predictedAccelerationBeforeAircraft : 0.0D), Double.valueOf(result != null ? result.predictedAccelerationAfterAircraft : 0.0D),
            Boolean.valueOf(result != null && result.speedDependsAircraftApplied), Double.valueOf(result != null ? result.initialVelocityUpDot : 0.0D),
            Double.valueOf(result != null ? result.initialVelocitySideDot : 0.0D));
      String msg6 = String.format("warningImpossibleLaunch=%s projection=%s projectMode=%s winZ=%s farPlaneRejected=%s hysteresis=%s/%d ccipGrace=%s cameraYaw/Pitch=%.1f/%.1f planeYaw/Pitch/Roll=%.1f/%.1f/%.1f",
            Boolean.valueOf(result != null && result.warningImpossibleLaunch), this.lastCCIPProjectionStatus, this.lastCCIPProjectMode,
            this.formatProjectWinZ(this.lastCCIPProjectWinZ), Boolean.valueOf(this.lastCCIPFarPlaneRejected),
            Boolean.valueOf(result != null && result.hysteresisReused), Integer.valueOf(result != null ? result.hysteresisAgeTicks : 0), Boolean.valueOf(this.lastCCIPGraceUsed),
            Float.valueOf(camera != null ? camera.rotationYaw : 0.0F), Float.valueOf(camera != null ? camera.rotationPitch : 0.0F),
            Float.valueOf(plane.rotationYaw), Float.valueOf(plane.rotationPitch), Float.valueOf(plane.getRotRoll()));
      this.drawString(msg1, super.centerX - 170, super.centerY + 70, 0xFF55FF66);
      this.drawString(msg2, super.centerX - 170, super.centerY + 80, 0xFF55FF66);
      this.drawString(msg3, super.centerX - 170, super.centerY + 90, 0xFF55FF66);
      this.drawString(msg4, super.centerX - 170, super.centerY + 100, 0xFF55FF66);
      this.drawString(msg5, super.centerX - 170, super.centerY + 110, 0xFF55FF66);
      this.drawString(msg6, super.centerX - 170, super.centerY + 120, 0xFF55FF66);
      String msg7 = String.format("fallbackChunk=%d,%d firstUnloaded=%s fallbackReason=%s fallbackTargetY=%.1f",
            Integer.valueOf(result != null ? result.firstUnloadedChunkX : 0), Integer.valueOf(result != null ? result.firstUnloadedChunkZ : 0),
            this.formatVec(result != null ? result.firstUnloadedPosition : null), result != null && result.fallbackReason != null ? result.fallbackReason : "-",
            Double.valueOf(result != null ? result.fallbackTargetY : 0.0D));
      this.drawString(msg7, super.centerX - 170, super.centerY + 130, 0xFF55FF66);
      String msg8 = String.format("projectionDiag path=%s reason=%s screen=%s camDist=%s camDepth=%s winZ=%s",
            this.lastCCIPProjectionDiagnostic.path, this.lastCCIPProjectionDiagnostic.reason,
            this.formatScreenPoint(this.lastCCIPProjectionDiagnostic.screenX, this.lastCCIPProjectionDiagnostic.screenY),
            this.formatDiagnosticDouble(this.lastCCIPProjectionDiagnostic.cameraDistance),
            this.formatDiagnosticDouble(this.lastCCIPProjectionDiagnostic.cameraDepth),
            this.formatProjectWinZ(this.lastCCIPProjectionDiagnostic.winZ));
      this.drawString(msg8, super.centerX - 170, super.centerY + 140, 0xFF55FF66);
   }

   private String formatVec(Vec3 v) {
      return v == null ? "-" : String.format("%.2f,%.2f,%.2f", Double.valueOf(v.xCoord), Double.valueOf(v.yCoord), Double.valueOf(v.zCoord));
   }

   private String formatProjectWinZ(double winZ) {
      return this.isFinite(winZ) ? String.format("%.4f", Double.valueOf(winZ)) : "-";
   }

   private String formatDiagnosticDouble(double value) {
      return this.isFinite(value) ? String.format("%.2f", Double.valueOf(value)) : "-";
   }

   private String formatScreenPoint(double x, double y) {
      return this.isFinite(x) && this.isFinite(y) ? String.format("%.1f,%.1f", Double.valueOf(x), Double.valueOf(y)) : "-";
   }

   private void resetCCIPProjectionDiagnostic() {
      this.lastCCIPProjectionStatus = "invalid";
      this.lastCCIPProjectMode = "null";
      this.lastCCIPProjectWinZ = Double.NaN;
      this.lastCCIPFarPlaneRejected = false;
      this.lastCCIPProjectionDiagnostic.reset();
   }

   private ScreenPoint projectWorldToRenderCamera(Vec3 worldPos, float partialTicks) {
      this.resetCCIPProjectionDiagnostic();
      if(worldPos == null || super.mc == null) {
         this.recordCCIPProjectionFailure("null", "null");
         return null;
      }

      this.updateCCIPProjectionCameraMetrics(worldPos, partialTicks);
      ScreenPoint exact = this.projectWithActiveRenderMatrices(worldPos);
      if(exact != null) {
         this.recordCCIPProjectionResult(exact.clamped ? "clamped" : "exact", exact.clamped ? "offscreen_clamped" : "exact", exact);
         this.lastCCIPProjectMode = "gluProject";
         return exact;
      }

      boolean farPlaneRejected = this.lastCCIPFarPlaneRejected;
      ScreenPoint fallback = this.projectWorldToRenderCameraFallback(worldPos, partialTicks);
      if(fallback != null) {
         this.recordCCIPProjectionResult(fallback.clamped ? "clamped" : "fallback", fallback.clamped ? "offscreen_clamped" : "exact", fallback);
         this.lastCCIPProjectMode = farPlaneRejected ? "camera-vector-far" : "camera-vector";
      }
      return fallback;
   }

   private void updateCCIPProjectionCameraMetrics(Vec3 worldPos, float partialTicks) {
      Entity camera = super.mc.renderViewEntity != null ? super.mc.renderViewEntity : super.mc.thePlayer;
      if(!(camera instanceof EntityLivingBase)) {
         this.lastCCIPProjectionDiagnostic.reason = "no_camera";
         return;
      }

      Vec3 cameraPos = ActiveRenderInfo.projectViewFromEntity((EntityLivingBase)camera, (double)partialTicks);
      Vec3 relative = Vec3.createVectorHelper(worldPos.xCoord - cameraPos.xCoord, worldPos.yCoord - cameraPos.yCoord,
            worldPos.zCoord - cameraPos.zCoord);
      this.lastCCIPProjectionDiagnostic.cameraDistance = relative.lengthVector();
      float yaw = camera.prevRotationYaw + (camera.rotationYaw - camera.prevRotationYaw) * partialTicks;
      float pitch = camera.prevRotationPitch + (camera.rotationPitch - camera.prevRotationPitch) * partialTicks;
      if(super.mc.gameSettings.thirdPersonView == 2) {
         yaw += 180.0F;
         pitch = -pitch;
      }
      Vec3 forward = this.normalizeVec(mcheli.MCH_Lib.Rot2Vec3(yaw, pitch));
      this.lastCCIPProjectionDiagnostic.cameraDepth = relative.dotProduct(forward);
   }

   private void recordCCIPProjectionResult(String path, String reason, ScreenPoint point) {
      this.lastCCIPProjectionDiagnostic.path = path;
      this.lastCCIPProjectionDiagnostic.reason = reason;
      this.lastCCIPProjectionDiagnostic.winZ = this.lastCCIPProjectWinZ;
      if(point != null) {
         this.lastCCIPProjectionDiagnostic.screenX = point.x;
         this.lastCCIPProjectionDiagnostic.screenY = point.y;
      }
      this.lastCCIPProjectionStatus = reason;
   }

   private void recordCCIPProjectionFailure(String path, String reason) {
      this.lastCCIPProjectionDiagnostic.path = path;
      this.lastCCIPProjectionDiagnostic.reason = reason;
      this.lastCCIPProjectionDiagnostic.winZ = this.lastCCIPProjectWinZ;
      this.lastCCIPProjectionStatus = reason;
   }

   private ScreenPoint projectWorldToRenderCameraFallback(Vec3 worldPos, float partialTicks) {
      // Fallback for clients where another coremod replaces ActiveRenderInfo's
      // private buffers or when the exact GL projection is outside the current
      // viewport/far clip. This still uses the real third-person camera position.
      Entity camera = super.mc.renderViewEntity != null ? super.mc.renderViewEntity : super.mc.thePlayer;
      if(!(camera instanceof EntityLivingBase)) {
         this.recordCCIPProjectionFailure("null", "no_camera");
         return null;
      }

      Vec3 cameraPos = ActiveRenderInfo.projectViewFromEntity((EntityLivingBase)camera, (double)partialTicks);
      float yaw = camera.prevRotationYaw + (camera.rotationYaw - camera.prevRotationYaw) * partialTicks;
      float pitch = camera.prevRotationPitch + (camera.rotationPitch - camera.prevRotationPitch) * partialTicks;
      if(super.mc.gameSettings.thirdPersonView == 2) {
         yaw += 180.0F;
         pitch = -pitch;
      }

      Vec3 forward = mcheli.MCH_Lib.Rot2Vec3(yaw, pitch);
      forward = this.normalizeVec(forward);
      Vec3 right = this.normalizeVec(Vec3.createVectorHelper(-forward.zCoord, 0.0D, forward.xCoord));
      Vec3 up = this.normalizeVec(Vec3.createVectorHelper(
            right.yCoord * forward.zCoord - right.zCoord * forward.yCoord,
            right.zCoord * forward.xCoord - right.xCoord * forward.zCoord,
            right.xCoord * forward.yCoord - right.yCoord * forward.xCoord));

      Vec3 relative = Vec3.createVectorHelper(
            worldPos.xCoord - cameraPos.xCoord,
            worldPos.yCoord - cameraPos.yCoord,
            worldPos.zCoord - cameraPos.zCoord);
      double depth = relative.dotProduct(forward);
      if(!this.isFinite(depth) || depth <= 0.01D) {
         this.recordCCIPProjectionFailure("null", !this.isFinite(depth) ? "nonfinite" : "behind_camera");
         return null;
      }

      double fov = MathHelper.clamp_double((double)super.mc.gameSettings.fovSetting, 10.0D, 170.0D);
      double focalLength = ((double)super.height * 0.5D) / Math.tan(Math.toRadians(fov * 0.5D));
      double x = (double)super.centerX + relative.dotProduct(right) * focalLength / depth;
      double y = (double)super.centerY - relative.dotProduct(up) * focalLength / depth;
      if(!this.isFinite(x) || !this.isFinite(y)) {
         this.recordCCIPProjectionFailure("null", "nonfinite");
         return null;
      }
      if(x < 0.0D || x > (double)super.width || y < 0.0D || y > (double)super.height) {
         return this.clampScreenPointToEdge(x, y);
      }
      return new ScreenPoint(x, y, false, true);
   }

   private ScreenPoint clampScreenPointToEdge(double x, double y) {
      double dx = x - (double)super.centerX;
      double dy = y - (double)super.centerY;
      if(!this.isFinite(dx) || !this.isFinite(dy) || (Math.abs(dx) < 1.0E-6D && Math.abs(dy) < 1.0E-6D)) {
         return null;
      }

      double edgeX = dx > 0.0D ? (double)super.width : 0.0D;
      double edgeY = dy > 0.0D ? (double)super.height : 0.0D;
      double scaleX = Math.abs(dx) > 1.0E-6D ? (edgeX - (double)super.centerX) / dx : Double.POSITIVE_INFINITY;
      double scaleY = Math.abs(dy) > 1.0E-6D ? (edgeY - (double)super.centerY) / dy : Double.POSITIVE_INFINITY;
      double scale = Math.min(scaleX, scaleY);
      if(!this.isFinite(scale) || scale < 0.0D) {
         return null;
      }

      double clampedX = MathHelper.clamp_double((double)super.centerX + dx * scale, 0.0D, (double)super.width);
      double clampedY = MathHelper.clamp_double((double)super.centerY + dy * scale, 0.0D, (double)super.height);
      if(!this.isFinite(clampedX) || !this.isFinite(clampedY)) {
         return null;
      }
      return new ScreenPoint(clampedX, clampedY, true, true);
   }

   private ScreenPoint projectWithActiveRenderMatrices(Vec3 worldPos) {
      if(!this.resolveActiveRenderInfoBuffers()) {
         this.recordCCIPProjectionFailure("null", "no_active_matrices");
         return null;
      }

      try {
         FloatBuffer modelView = ((FloatBuffer)activeRenderModelViewField.get(null)).duplicate();
         FloatBuffer projection = ((FloatBuffer)activeRenderProjectionField.get(null)).duplicate();
         IntBuffer viewport = ((IntBuffer)activeRenderViewportField.get(null)).duplicate();
         modelView.rewind();
         projection.rewind();
         viewport.rewind();
         CCIP_PROJECTED_COORDS.clear();

         boolean projected = GLU.gluProject((float)worldPos.xCoord, (float)worldPos.yCoord,
               (float)worldPos.zCoord, modelView, projection, viewport, CCIP_PROJECTED_COORDS);
         if(!projected) {
            this.recordCCIPProjectionFailure("null", "null");
            return null;
         }

         double winX = (double)CCIP_PROJECTED_COORDS.get(0);
         double winY = (double)CCIP_PROJECTED_COORDS.get(1);
         double winZ = (double)CCIP_PROJECTED_COORDS.get(2);
         this.lastCCIPProjectWinZ = winZ;
         if(this.isFinite(winZ) && winZ > 1.0D) {
            this.lastCCIPFarPlaneRejected = true;
            this.recordCCIPProjectionFailure("null", "far_plane");
            return null;
         }
         if(!this.isFinite(winX) || !this.isFinite(winY) || !this.isFinite(winZ)) {
            this.recordCCIPProjectionFailure("null", "nonfinite");
            return null;
         }
         if(winZ < 0.0D) {
            this.recordCCIPProjectionFailure("null", "behind_camera");
            return null;
         }

         int viewportX = viewport.get(0);
         int viewportY = viewport.get(1);
         int viewportWidth = viewport.get(2);
         int viewportHeight = viewport.get(3);
         if(viewportWidth <= 0 || viewportHeight <= 0) {
            this.recordCCIPProjectionFailure("null", "nonfinite");
            return null;
         }

         double normalizedX = (winX - (double)viewportX) / (double)viewportWidth;
         double normalizedY = (winY - (double)viewportY) / (double)viewportHeight;
         double x = normalizedX * (double)super.width;
         double y = (1.0D - normalizedY) * (double)super.height;
         if(x < 0.0D || x > (double)super.width || y < 0.0D || y > (double)super.height) {
            this.recordCCIPProjectionFailure("null", "offscreen_clamped");
            return null;
         }
         return new ScreenPoint(x, y, false, true);
      } catch(Throwable ignored) {
         this.recordCCIPProjectionFailure("null", "null");
         return null;
      }
   }

   private boolean resolveActiveRenderInfoBuffers() {
      if(activeRenderModelViewField != null && activeRenderProjectionField != null
            && activeRenderViewportField != null) {
         return true;
      }
      if(activeRenderBufferLookupAttempted) {
         return false;
      }
      activeRenderBufferLookupAttempted = true;

      try {
         Field[] fields = ActiveRenderInfo.class.getDeclaredFields();
         List floatFields = new ArrayList();
         for(int i = 0; i < fields.length; ++i) {
            Field field = fields[i];
            if(!Modifier.isStatic(field.getModifiers())) {
               continue;
            }
            field.setAccessible(true);
            if(FloatBuffer.class.isAssignableFrom(field.getType())) {
               FloatBuffer buffer = (FloatBuffer)field.get(null);
               if(buffer != null && buffer.capacity() >= 16) {
                  floatFields.add(field);
                  String name = field.getName().toLowerCase();
                  if(name.indexOf("model") >= 0) activeRenderModelViewField = field;
                  if(name.indexOf("projection") >= 0) activeRenderProjectionField = field;
               }
            } else if(IntBuffer.class.isAssignableFrom(field.getType())) {
               IntBuffer buffer = (IntBuffer)field.get(null);
               if(buffer != null && buffer.capacity() >= 4) {
                  activeRenderViewportField = field;
               }
            }
         }

         // Production obfuscation may remove useful field names. Classify the two
         // 4x4 matrices by their values instead: a model-view matrix ends in 1,
         // while a perspective projection normally has m15=0 and m11=-1.
         for(int i = 0; i < floatFields.size(); ++i) {
            Field field = (Field)floatFields.get(i);
            FloatBuffer buffer = (FloatBuffer)field.get(null);
            float m11 = buffer.get(11);
            float m15 = buffer.get(15);
            if(activeRenderModelViewField == null && Math.abs(m15 - 1.0F) < 0.25F) {
               activeRenderModelViewField = field;
            }
            if(activeRenderProjectionField == null && Math.abs(m15) < 0.25F && Math.abs(m11) > 0.5F) {
               activeRenderProjectionField = field;
            }
         }

         if((activeRenderModelViewField == null || activeRenderProjectionField == null)
               && floatFields.size() >= 2) {
            if(activeRenderModelViewField == null) activeRenderModelViewField = (Field)floatFields.get(0);
            if(activeRenderProjectionField == null) {
               Field candidate = (Field)floatFields.get(1);
               if(candidate == activeRenderModelViewField && floatFields.size() > 2) candidate = (Field)floatFields.get(2);
               activeRenderProjectionField = candidate;
            }
         }
      } catch(Throwable ignored) {
         activeRenderModelViewField = null;
         activeRenderProjectionField = null;
         activeRenderViewportField = null;
      }

      return activeRenderModelViewField != null && activeRenderProjectionField != null
            && activeRenderViewportField != null;
   }

   private boolean isFinite(double value) {
      return !Double.isNaN(value) && !Double.isInfinite(value);
   }

   private Vec3 normalizeVec(Vec3 v) {
      double length = v != null ? v.lengthVector() : 0.0D;
      if(length < 1.0E-6D) {
         return Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
      }
      return Vec3.createVectorHelper(v.xCoord / length, v.yCoord / length, v.zCoord / length);
   }

   private void drawCCIPPipper(double x, double y, boolean clamped, boolean bomberMode) {
      this.drawCCIPPipper(x, y, clamped, bomberMode, false);
   }

   private void drawCCIPPipper(double x, double y, boolean clamped, boolean bomberMode, boolean grace) {
      if(bomberMode) {
         this.drawBombSightReticle(x, y, clamped);
         return;
      }
      int color = grace ? 0x6655FF66 : (clamped ? 0xAA55FF66 : 0xF055FF66);
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
      this.drawString(grace ? "CCIP GRACE" : "CCIP", (int)(x + r + 7.0D), (int)(y + r + 4.0D), color);
   }

   private void drawBombSightReticle(double x, double y, boolean clamped) {
      int color = clamped ? 0xAA000000 : 0xF0000000;
      double r = 30.0D;
      double inner = 8.0D;
      double outer = 46.0D;
      double[] circle = new double[66];
      for(int i = 0; i <= 32; ++i) {
         double a = Math.PI * 2.0D * (double)i / 32.0D;
         circle[i * 2] = x + Math.cos(a) * r;
         circle[i * 2 + 1] = y + Math.sin(a) * r;
      }
      this.drawLine(circle, color, 3);
      this.drawLine(new double[]{x - outer, y, x - inner, y, x + inner, y, x + outer, y,
            x, y - outer, x, y - inner, x, y + inner, x, y + outer}, color, 3);
      this.drawLine(new double[]{x - 3.0D, y, x + 3.0D, y, x, y - 3.0D, x, y + 3.0D}, color, 2);
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

   private static class CCIPProjectionDiagnostic {
      String path;
      String reason;
      double winZ;
      double screenX;
      double screenY;
      double cameraDistance;
      double cameraDepth;

      void reset() {
         this.path = "null";
         this.reason = "null";
         this.winZ = Double.NaN;
         this.screenX = Double.NaN;
         this.screenY = Double.NaN;
         this.cameraDistance = Double.NaN;
         this.cameraDepth = Double.NaN;
      }
   }

   private static class ScreenPoint {
      final double x;
      final double y;
      final boolean clamped;
      final boolean visible;

      ScreenPoint(double x, double y, boolean clamped) {
         this(x, y, clamped, true);
      }

      ScreenPoint(double x, double y, boolean clamped, boolean visible) {
         this.x = x;
         this.y = y;
         this.clamped = clamped;
         this.visible = visible;
      }
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

            if(seatID == 0 && plane.getIsGunnerMode(player) && info.hasBombSight && !Keyboard.isKeyDown(MCH_Config.KeyFreeLook.prmInt)) {
               if(super.mc != null && super.mc.gameSettings != null && super.mc.gameSettings.thirdPersonView != 0) {
                  this.drawCenteredString("Bomb sight: OFF Third Person", super.centerX, super.height - 42, colorInactive);
               } else {
                  boolean bombReticle = MCP_ClientPlaneTickHandler.isBombReticleMode(plane);
                  var12 = (new StringBuilder()).append(bombReticle ? "Bomb Sight Off : " : "Bomb Sight : ");
                  msg = var12.append(MCH_KeyName.getDescOrName(MCH_Config.KeyBombReticleMode.prmInt)).toString();
                  this.drawCenteredString(msg, super.centerX, super.height - 42, colorActive);
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
