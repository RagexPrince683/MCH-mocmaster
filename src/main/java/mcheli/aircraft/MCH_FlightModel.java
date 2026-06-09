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

   /** Returns the unsigned angle, in degrees, between the aircraft nose and its velocity. */
   public static double getAngleOfAttackDegrees(double forwardX, double forwardY, double forwardZ,
                                                double velocityX, double velocityY, double velocityZ) {
      double forwardLength = Math.sqrt(forwardX * forwardX + forwardY * forwardY + forwardZ * forwardZ);
      double speed = Math.sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ);
      if(forwardLength < 1.0E-6D || speed < 1.0E-6D) {
         return 0.0D;
      }

      double dot = (forwardX * velocityX + forwardY * velocityY + forwardZ * velocityZ)
            / (forwardLength * speed);
      return Math.toDegrees(Math.acos(clamp(dot, -1.0D, 1.0D)));
   }

   /** Resolves an absolute stall speed while retaining compatibility with StallSpeedFactor. */
   public static double getStallSpeed(float stallSpeed, float topSpeed, float stallSpeedFactor) {
      if(stallSpeed > 0.0F) {
         return stallSpeed;
      }
      return Math.max(0.05D, (double)topSpeed * (double)stallSpeedFactor);
   }

   /** Returns the stronger of the low-speed and excessive-AoA stall demands. */
   public static double getAerodynamicStallSeverity(double speed, double angleOfAttack,
                                                     double stallSpeed, float criticalAoA) {
      double speedSeverity = stallSpeed > 1.0E-6D
            ? clamp((stallSpeed - speed) / stallSpeed, 0.0D, 1.0D) : 0.0D;
      double critical = Math.max(1.0D, (double)criticalAoA);
      double aoaSeverity = clamp((Math.abs(angleOfAttack) - critical) / critical, 0.0D, 1.0D);
      return Math.max(speedSeverity, aoaSeverity);
   }

   /** Control surfaces lose authority progressively as the stall develops. */
   public static double getControlAuthority(double stallSeverity) {
      return clamp(1.0D - clamp(stallSeverity, 0.0D, 1.0D) * 0.75D, 0.25D, 1.0D);
   }

   /** Additional fractional drag caused by presenting the airframe to the airflow. */
   public static double getAngleOfAttackDrag(double angleOfAttack, float criticalAoA,
                                              float baseDrag, float aoaDragMultiplier) {
      double normalizedAoA = Math.abs(angleOfAttack) / Math.max(1.0D, (double)criticalAoA);
      return Math.max(0.0D, (double)baseDrag) * Math.max(0.0D, (double)aoaDragMultiplier)
            * normalizedAoA * normalizedAoA;
   }

   /** Returns a 0..1 severity value as airspeed falls below the stall threshold. */
   public static double getStallSeverity(double horizontalSpeed, float topSpeed, float stallSpeedFactor) {
      double stallSpeed = Math.max(0.05D, (double)topSpeed * (double)stallSpeedFactor);
      return clamp((stallSpeed - horizontalSpeed) / stallSpeed, 0.0D, 1.0D);
   }

   /**
    * Returns the fractional horizontal speed loss for one fixed-wing tick.
    * Inputs are normalized so content authors can tune coefficients directly.
    */
   public static double getEnergyDrag(double speed, double levelSpeed, double throttle, double turnLoad,
                                      double controlLoad, float baseDrag, float inducedDrag,
                                      float controlSurfaceDrag, float idleDrag) {
      double referenceSpeed = Math.max(0.05D, levelSpeed);
      double speedRatio = Math.max(0.0D, speed) / referenceSpeed;
      double power = clamp(throttle, 0.0D, 1.0D);
      double drag = Math.max(0.0D, (double)baseDrag) * (0.5D + 0.5D * speedRatio * speedRatio);
      drag += Math.max(0.0D, (double)inducedDrag) * clamp(turnLoad, 0.0D, 1.0D) * clamp(turnLoad, 0.0D, 1.0D);
      drag += Math.max(0.0D, (double)controlSurfaceDrag) * clamp(controlLoad, 0.0D, 1.0D);
      drag += Math.max(0.0D, (double)idleDrag) * (1.0D - power);

      // Full power can sustain maxLevelSpeed. Lower settings progressively reduce
      // the sustainable speed, so a fast aircraft cannot coast forever at idle.
      double sustainableSpeed = referenceSpeed * (0.35D + 0.65D * power);
      if(speed > sustainableSpeed) {
         drag += Math.max(0.0D, (double)baseDrag + (double)idleDrag)
               * clamp((speed - sustainableSpeed) / referenceSpeed, 0.0D, 2.0D);
      }
      return clamp(drag, 0.0D, 0.5D);
   }

   /** Positive values gain horizontal speed in a dive; negative values lose it in a climb. */
   public static double getVerticalEnergyChange(double verticalSpeed, float climbEnergyLoss, float diveEnergyGain) {
      double climb = clamp(verticalSpeed / 0.35D, 0.0D, 1.0D);
      double dive = clamp(-verticalSpeed / 0.35D, 0.0D, 1.0D);
      return dive * Math.max(0.0D, (double)diveEnergyGain)
            - climb * Math.max(0.0D, (double)climbEnergyLoss);
   }

   /** Diving raises the speed cap gradually, rather than creating an abrupt second limit. */
   public static double getDiveSpeedLimit(float topSpeed, float pitch, double verticalSpeed, float multiplier) {
      double noseDown = clamp((double)pitch / 60.0D, 0.0D, 1.0D);
      double descending = clamp(-verticalSpeed / 0.35D, 0.0D, 1.0D);
      double dive = Math.max(noseDown, descending);
      return (double)topSpeed * (1.0D + ((double)multiplier - 1.0D) * dive);
   }
}
