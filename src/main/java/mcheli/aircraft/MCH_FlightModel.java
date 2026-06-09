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

   /**
    * Approximates the standard atmosphere's density change with altitude.
    * The returned value is relative to sea-level density and is bounded so
    * unusual world heights cannot remove aerodynamic control entirely.
    */
   public static double getAirDensityFactor(double altitude) {
      double heightAboveSeaLevel = altitude - 64.0D;
      return clamp(Math.exp(-heightAboveSeaLevel / 8500.0D), 0.25D, 1.1D);
   }

   /**
    * Calculates speed lost to aerodynamic drag during one tick.
    *
    * Parasitic drag follows the physical v^2 relationship. motionFactor is
    * used as the aircraft's drag calibration: at referenceSpeed and sea-level
    * density this produces the same loss as the old linear damping. Banking
    * adds induced drag from the increased lift/load requirement, while
    * sideslip exposes more of the airframe to the airflow during a turn.
    */
   public static double getAerodynamicDragLoss(double speed, float referenceSpeed, float motionFactor, double airDensityFactor, float bankAngle, double sideslip) {
      if(speed <= 0.0D || referenceSpeed <= 0.0F) {
         return 0.0D;
      }

      double baseDrag = clamp(1.0D - (double)motionFactor, 0.0D, 1.0D);
      double density = clamp(airDensityFactor, 0.25D, 1.1D);
      double quadraticDrag = baseDrag * density * speed * speed / (double)referenceSpeed;

      // Limit the load-factor calculation to a 60-degree coordinated turn. Steeper
      // visual banks must not make drag approach infinity as cos(bank) approaches 0.
      double bankRadians = Math.toRadians(clamp(Math.abs((double)bankAngle), 0.0D, 60.0D));
      double loadFactor = 1.0D / Math.max(0.5D, Math.cos(bankRadians));
      double inducedDragPenalty = Math.min(0.24D, (loadFactor * loadFactor - 1.0D) * 0.08D);

      // Heading naturally leads the velocity vector while an aircraft starts turning.
      // Ignore that small lag and only penalize substantial uncoordinated sideslip.
      double sideslipExcess = clamp((Math.abs(sideslip) - 0.2D) / 0.8D, 0.0D, 1.0D);
      double sideslipDragPenalty = sideslipExcess * 0.16D;
      double turnDragMultiplier = 1.0D + Math.min(0.4D, inducedDragPenalty + sideslipDragPenalty);

      // Keep drag gradual even for low-drag aircraft after collisions or abrupt rotations.
      return Math.min(speed * 0.05D, quadraticDrag * turnDragMultiplier);
   }

   /** Diving raises the speed cap gradually, rather than creating an abrupt second limit. */
   public static double getDiveSpeedLimit(float topSpeed, float pitch, double verticalSpeed, float multiplier) {
      double noseDown = clamp((double)pitch / 60.0D, 0.0D, 1.0D);
      double descending = clamp(-verticalSpeed / 0.35D, 0.0D, 1.0D);
      double dive = Math.max(noseDown, descending);
      return (double)topSpeed * (1.0D + ((double)multiplier - 1.0D) * dive);
   }
}
