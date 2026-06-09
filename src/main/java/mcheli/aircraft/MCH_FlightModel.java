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

   /**
    * Integrates one local-axis body rate. Torque and damping are divided by inertia,
    * so heavier aircraft take longer to reach the same configured control authority.
    */
   public static float updateAngularVelocity(float velocity, float control, float torque, float damping,
                                             float inertiaMultiplier, float delta) {
      float inertia = Math.max(0.05F, inertiaMultiplier);
      float step = Math.max(0.0F, delta);
      float drag = Math.max(0.0F, damping);
      float force = control * Math.max(0.0F, torque);
      if(drag <= 1.0E-4F) {
         return velocity + force / inertia * step;
      }

      float targetVelocity = force / drag;
      float response = 1.0F - (float)Math.exp((double)(-drag * step / inertia));
      return velocity + (targetVelocity - velocity) * response;
   }

   /** Moves engine output toward commanded throttle without an instantaneous thrust step. */
   public static double approachEngineOutput(double output, double target, float acceleration, float drag) {
      double difference = clamp(target, 0.0D, 1.0D) - clamp(output, 0.0D, 1.0D);
      double limit = difference >= 0.0D ? Math.max(0.0D, (double)acceleration) : Math.max(0.0D, (double)drag);
      if(Math.abs(difference) <= limit) {
         return clamp(target, 0.0D, 1.0D);
      }
      return clamp(output + (difference > 0.0D ? limit : -limit), 0.0D, 1.0D);
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
