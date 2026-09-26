package mcheli.tank;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small, tick-based car slip model. All constants are gameplay tuning, not measured friction. */
public final class MCH_CarTireGrip {
   public static final float DEFAULT_GRIP = 0.06F;
   public static final float MAX_GRIP = 0.25F;
   private static final double SLIP_DAMPING = 0.25D;
   private static final double LONGITUDINAL_REFERENCE = 0.20D;
   private static final Pattern SIZE = Pattern.compile("(\\d{3})(?:/(\\d{2,3}))?Z?R(\\d{2}(?:\\.5)?)", Pattern.CASE_INSENSITIVE);

   private MCH_CarTireGrip() {}

   /** Unknown sizes have neutral response; no fictional factory size is substituted. */
   public static TireSize parseTireSize(String value) {
      if(value == null) return null;
      Matcher m = SIZE.matcher(value.replaceAll("\\s+", ""));
      if(!m.matches()) return null;
      int width = Integer.parseInt(m.group(1));
      int aspect = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
      double rim = Double.parseDouble(m.group(3));
      if(width < 100 || width > 500 || (aspect != 0 && (aspect < 20 || aspect > 100)) || rim < 10.0D || rim > 30.0D) return null;
      // A full-profile size such as 175R14 deliberately has no inferred aspect ratio.
      if(m.group(2) != null && aspect == 0) return null;
      return new TireSize(width, aspect, rim);
   }

   public static double response(TireSize size) {
      if(size == null) return 1.0D;
      double widthTerm = 0.02D * clamp(Math.log(size.widthMm / 205.0D), -1.0D, 1.0D);
      double sidewallTerm = 0.0D;
      if(size.aspectPercent > 0) {
         double sidewall = size.widthMm * size.aspectPercent / 100.0D;
         double diameter = size.rimInches * 25.4D + 2.0D * sidewall;
         double reference = 112.75D / 631.9D; // neutral 205/55R16 geometry, not a default fitment
         sidewallTerm = -0.03D * clamp((sidewall / diameter - reference) / reference, -1.0D, 1.0D);
      }
      return clamp(1.0D + widthTerm + sidewallTerm, 0.95D, 1.05D);
   }

   /** Signed sideways delta to subtract, never reversing slip or modifying forward velocity/yaw. */
   public static double lateralCorrection(double sideways, double grip, double contact, double response, double longitudinalDemand) {
      if(!finite(sideways) || !finite(grip) || !finite(contact) || !finite(response) || !finite(longitudinalDemand)) return 0.0D;
      double usage = clamp(Math.abs(longitudinalDemand) / LONGITUDINAL_REFERENCE, 0.0D, 0.95D);
      double limit = clamp(grip, 0.0D, MAX_GRIP) * clamp(contact, 0.0D, 1.0D) * Math.sqrt(1.0D - usage * usage);
      double requested = Math.abs(sideways) * SLIP_DAMPING * clamp(response, 0.95D, 1.05D) * clamp(contact, 0.0D, 1.0D);
      return Math.copySign(Math.min(requested, limit), sideways);
   }

   private static boolean finite(double value) {
      return !Double.isNaN(value) && !Double.isInfinite(value);
   }

   private static double clamp(double value, double min, double max) {
      return Math.max(min, Math.min(max, value));
   }

   public static final class TireSize {
      public final int widthMm;
      public final int aspectPercent;
      public final double rimInches;

      private TireSize(int width, int aspect, double rim) {
         this.widthMm = width;
         this.aspectPercent = aspect;
         this.rimInches = rim;
      }
   }
}
