package mcheli.aircraft;

/**
 * Small, stateless helpers shared by fixed-wing and rotorcraft physics.
 * Keeping these calculations here makes the server flight loops easier to tune.
 */
public final class MCH_FlightModel {

   private MCH_FlightModel() {
   }

   public static double clamp(double value, double min, double max) {
      return value < min ? min : (value > max ? max : value);
   }

   /** Returns 1 below the ceiling fade band and 0 at or above the ceiling. */
   public static double getCeilingLiftFactor(double altitude, float ceiling, float fadeRange) {
      if(ceiling <= 0.0F) {
         return 1.0D;
      }

      double range = Math.max(1.0D, (double)fadeRange);
      return clamp((ceiling - altitude) / range, 0.0D, 1.0D);
   }

   /** Returns a 0..1 severity value as airspeed falls below the stall threshold. */
   public static double getStallSeverity(double horizontalSpeed, float topSpeed, float stallSpeedFactor) {
      double stallSpeed = Math.max(0.05D, (double)topSpeed * (double)stallSpeedFactor);
      return clamp((stallSpeed - horizontalSpeed) / stallSpeed, 0.0D, 1.0D);
   }

   /** Diving raises the speed cap gradually, rather than creating an abrupt second limit. */
   public static double getDiveSpeedLimit(float topSpeed, float pitch, double verticalSpeed, float multiplier) {
      double noseDown = clamp((double)pitch / 60.0D, 0.0D, 1.0D);
      double descending = clamp(-verticalSpeed / 0.35D, 0.0D, 1.0D);
      double dive = Math.max(noseDown, descending);
      return (double)topSpeed * (1.0D + ((double)multiplier - 1.0D) * dive);
   }
}
