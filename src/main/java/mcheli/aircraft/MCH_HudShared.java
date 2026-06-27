package mcheli.aircraft;

import java.util.ArrayList;
import java.util.List;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.weapon.MCH_WeaponSet;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

public final class MCH_HudShared {

   public static final double HUD_HELI_SPEED_MULTIPLIER = 1.0D;
   public static final double HUD_PLANE_SPEED_MULTIPLIER = 1.0D;

   private MCH_HudShared() {
   }

   public static boolean isNewHeliPilotHudActive(MCH_EntityBaseVehicle ac) {
      return isNewHeliPilotHudActive(ac, ac != null ? ac.getRiddenByEntity() : null);
   }

   public static boolean isNewHeliPilotHudActive(MCH_EntityBaseVehicle ac, Entity player) {
      if(!(ac instanceof MCH_EntityHeli) || player == null || ac.getSeatIdByEntity(player) != 0) {
         return false;
      }
      MCH_EntityHeli heli = (MCH_EntityHeli)ac;
      return heli.isNewHeliFlightModelEnabled() && !heli.isDestroyed();
   }

   public static String formatThrottleOrCollective(String label, MCH_EntityBaseVehicle ac) {
      return String.format("%s %5.1f%%", new Object[]{label, Double.valueOf(MathHelper.clamp_double(ac.getNormalizedThrottle() * 100.0D, 0.0D, 100.0D))});
   }

   public static double getRawSpeedKmh(MCH_EntityBaseVehicle ac) {
      if(ac == null) {
         return 0.0D;
      }
      return Math.sqrt(ac.motionX * ac.motionX + ac.motionY * ac.motionY + ac.motionZ * ac.motionZ) * 72.0D;
   }

   public static double getDisplaySpeedHeli(MCH_EntityBaseVehicle ac) {
      return getRawSpeedKmh(ac) * HUD_HELI_SPEED_MULTIPLIER;
   }

   public static double getDisplaySpeedPlane(MCH_EntityBaseVehicle ac) {
      return getRawSpeedKmh(ac) * HUD_PLANE_SPEED_MULTIPLIER;
   }

   public static double getDisplaySpeedKmh(MCH_EntityBaseVehicle ac) {
      return ac instanceof MCH_EntityHeli ? getDisplaySpeedHeli(ac) : getDisplaySpeedPlane(ac);
   }

   public static String formatSpeedKmh(MCH_EntityBaseVehicle ac) {
      double speedKmh = getDisplaySpeedKmh(ac);
      return String.format("SPD   %d km/h", new Object[]{Integer.valueOf(Math.max(0, (int)Math.round(speedKmh)))});
   }

   public static String formatAltitude(MCH_EntityBaseVehicle ac) {
      return String.format("ALT   %d m", new Object[]{Integer.valueOf(Math.max(0, (int)Math.round(ac.posY)))});
   }

   public static double getVerticalSpeedMotionY(MCH_EntityBaseVehicle ac) {
      return ac != null?ac.motionY:0.0D;
   }

   public static String formatVerticalSpeed(MCH_EntityBaseVehicle ac) {
      return String.format("VS    %+d m/s", new Object[]{Integer.valueOf((int)Math.round(getVerticalSpeedMotionY(ac) * 20.0D))});
   }

   public static String formatFuelMinutes(MCH_EntityBaseVehicle ac) {
      int seconds = ac != null ? ac.getFuelRemainingSeconds() : -1;
      if(seconds < 0) {
         return "FUEL  -- min";
      }
      int minutes = Math.max(0, (int)Math.round((double)seconds / 60.0D));
      return String.format("FUEL  %d min", new Object[]{Integer.valueOf(minutes)});
   }

   public static String formatDamagePercent(MCH_EntityBaseVehicle ac) {
      return String.format("DMG   %d%%", new Object[]{Integer.valueOf(getDamagePercent(ac))});
   }

   public static int getDamagePercent(MCH_EntityBaseVehicle ac) {
      int max = ac.getMaxHP();
      if(max <= 0) {
         return 100;
      }
      return MathHelper.clamp_int((int)Math.round((double)ac.getHP() * 100.0D / (double)max), 0, 100);
   }

   public static int estimateFuelMinutes(MCH_EntityBaseVehicle ac) {
      if(ac.getMaxFuel() <= 0 || ac.getFuel() <= 0 || ac.isInfinityFuel(ac.getRiddenByEntity(), true)) {
         return -1;
      }
      if(ac.getAcInfo() == null || ac.getAcInfo().fuelConsumption <= 0.0F) {
         return -1;
      }
      double throttle = MathHelper.clamp_double(ac.getNormalizedThrottle(), 0.0D, 1.0D);
      double burnPerSecond = Math.min(throttle * 1.4D, 1.0D) * (double)ac.getAcInfo().fuelConsumption * (double)ac.getFuelConsumptionFactor();
      if(burnPerSecond <= 0.01D) {
         return -1;
      }
      return Math.max(0, (int)Math.round((double)ac.getFuel() / burnPerSecond / 60.0D));
   }

   public static List collectWeaponAmmoLines(MCH_EntityBaseVehicle ac, Minecraft mc, int maxTextWidth, boolean includeSelector, int selectedWeaponId) {
      List lines = new ArrayList();
      if(ac == null || ac.weapons == null || mc == null || mc.fontRenderer == null) {
         return lines;
      }
      for(int i = 0; i < ac.weapons.length; ++i) {
         MCH_WeaponSet ws = ac.weapons[i];
         if(ws != null) {
            lines.add(formatWeaponAmmoLine(ws, mc, maxTextWidth, includeSelector, i == selectedWeaponId));
         }
      }
      return lines;
   }

   private static String formatWeaponAmmoLine(MCH_WeaponSet ws, Minecraft mc, int maxTextWidth, boolean includeSelector, boolean selected) {
      String prefix = includeSelector ? (selected ? "> " : "  ") : "";
      String ammo = getWeaponHudAmmo(ws);
      String name = getWeaponHudName(ws);
      String line = String.format("%s%-18s %5s", new Object[]{prefix, name, ammo});
      while(mc.fontRenderer.getStringWidth(line) > maxTextWidth && name.length() > 4) {
         name = name.substring(0, name.length() - 2) + "~";
         line = String.format("%s%-18s %5s", new Object[]{prefix, name, ammo});
      }
      return line;
   }

   private static String getWeaponHudName(MCH_WeaponSet ws) {
      String name = ws.getName();
      return name == null || name.length() == 0 ? "WEAPON" : name.toUpperCase();
   }

   private static String getWeaponHudAmmo(MCH_WeaponSet ws) {
      try {
         int ammo = ws.getAmmoNum() + ws.getRestAllAmmoNum();
         return ammo >= 0 ? String.valueOf(ammo) : "--";
      } catch(RuntimeException ex) {
         return "--";
      }
   }
}
