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
    * Maximum pitch/yaw turn rate allowed by a load-factor limit.
    * This intentionally limits pitch/yaw turning, not roll rate.
    *
    * Minecraft units:
    * speed = blocks/tick
    * gravity = blocks/tick^2
    * result = degrees/tick
    */
   public static double getTurnRateLimitDegreesPerTick(double speed, double gravity, double maxG) {
      double v = Math.max(0.01D, speed);
      double g = Math.max(1.0E-6D, gravity);
      double n = Math.max(1.0D, maxG);

      if(n <= 1.0001D) {
         return 0.0D;
      }

      double lateralAcceleration = g * Math.sqrt(n * n - 1.0D);
      double turnRateRadians = lateralAcceleration / v;
      return Math.toDegrees(turnRateRadians);
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


   /**
    * Converts render-loop deltas into a bounded tick fraction for legacy client-side
    * control prediction. Rendering may happen at 30, 60, 144, or 240+ FPS, but the
    * sum of these fractions over one Minecraft tick remains one simulation tick.
    */
   public static float getBoundedTickDelta(float delta) {
      if(Float.isNaN(delta) || Float.isInfinite(delta)) {
         return 0.0F;
      }
      if(delta < 0.0F) {
         return 0.0F;
      }
      return delta > 1.0F ? 1.0F : delta;
   }

   /** Exponential decay that gives the same result for one full tick regardless of render FPS. */
   public static float decayPerTick(float value, float retainedPerTick, float deltaTicks) {
      float retained = (float)clamp((double)retainedPerTick, 0.0D, 1.0D);
      float delta = getBoundedTickDelta(deltaTicks);
      return value * (float)Math.pow((double)retained, (double)delta);
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
      if(forwardLength < 1.0E-6D) {
         return 0.0D;
      }

      // At very low airspeed the velocity vector becomes numerically unstable, but
      // a nose-high fixed-wing aircraft should not look aerodynamically clean. Blend
      // toward the nose-vs-horizontal attitude so stalled, nearly suspended aircraft
      // continue to accumulate AoA/stall demand instead of escaping recovery logic.
      double horizontalForward = Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);
      double attitudeAoA = Math.toDegrees(Math.atan2(Math.abs(forwardY), horizontalForward));
      if(speed < 1.0E-6D) {
         return attitudeAoA;
      }

      double dot = (forwardX * velocityX + forwardY * velocityY + forwardZ * velocityZ)
            / (forwardLength * speed);
      double velocityAoA = Math.toDegrees(Math.acos(clamp(dot, -1.0D, 1.0D)));
      if(speed >= 0.08D) {
         return velocityAoA;
      }

      double velocityBlend = clamp(speed / 0.08D, 0.0D, 1.0D);
      return attitudeAoA * (1.0D - velocityBlend) + velocityAoA * velocityBlend;
   }

   /** Resolves an absolute stall speed while retaining compatibility with StallSpeedFactor. */
   public static double getStallSpeed(float stallSpeed, float topSpeed, float stallSpeedFactor) {
      if(stallSpeed > 0.0F) {
         return stallSpeed;
      }
      return Math.max(0.05D, (double)topSpeed * (double)stallSpeedFactor);
   }

   /** Returns the low-speed portion of stall demand. */
   public static double getSpeedStallSeverity(double speed, double stallSpeed) {
      return stallSpeed > 1.0E-6D ? clamp((stallSpeed - speed) / stallSpeed, 0.0D, 1.0D) : 0.0D;
   }

   /** Returns the excessive-AoA portion of stall demand. */
   public static double getAoAStallSeverity(double angleOfAttack, float criticalAoA) {
      double critical = Math.max(1.0D, (double)criticalAoA);
      return clamp((Math.abs(angleOfAttack) - critical) / critical, 0.0D, 1.0D);
   }

   /** Returns the stronger of the low-speed and excessive-AoA stall demands. */
   public static double getAerodynamicStallSeverity(double speed, double angleOfAttack,
                                                     double stallSpeed, float criticalAoA) {
      return Math.max(getSpeedStallSeverity(speed, stallSpeed), getAoAStallSeverity(angleOfAttack, criticalAoA));
   }

   /** Control surfaces lose authority progressively as the stall develops. */
   public static double getControlAuthority(double stallSeverity) {
      double severity = clamp(stallSeverity, 0.0D, 1.0D);
      return clamp(1.0D - severity * 0.75D, 0.25D, 1.0D);
   }

   /** Additional fractional drag caused by presenting the airframe to the airflow. */
   public static double getAngleOfAttackDrag(double angleOfAttack, float criticalAoA,
                                              float baseDrag, float aoaDragMultiplier) {
      double normalizedAoA = Math.abs(angleOfAttack) / Math.max(1.0D, (double)criticalAoA);
      return Math.max(0.0D, (double)baseDrag) * Math.max(0.0D, (double)aoaDragMultiplier)
            * normalizedAoA * normalizedAoA;
   }

   /**
    * Signed coefficient-like lift curve: linear through usable AoA, then progressive post-stall loss.
    * Positive AoA produces normal lift, negative AoA produces inverted/downward lift.
    */
   public static double getLiftCoefficientLikeCurve(double signedAoA, double criticalAoA, double stallSeverity) {
      double critical = Math.max(1.0D, criticalAoA);
      double normalized = clamp(signedAoA / critical, -3.0D, 3.0D);
      double sign = normalized < 0.0D ? -1.0D : 1.0D;
      double abs = Math.abs(normalized);
      double lift;
      if(abs <= 1.0D) {
         lift = abs;
      } else {
         double post = clamp((abs - 1.0D) / 2.0D, 0.0D, 1.0D);
         lift = 1.0D - post * (0.65D + 0.25D * clamp(stallSeverity, 0.0D, 1.0D));
      }
      return sign * clamp(lift, 0.08D, 1.0D);
   }

   /** Coefficient-like AoA drag curve with a strong post-critical rise. */
   public static double getAoADragCoefficientLikeCurve(double absAoA, double criticalAoA, double baseDrag, double aoaDragMultiplier) {
      double critical = Math.max(1.0D, criticalAoA);
      double normalized = Math.max(0.0D, absAoA) / critical;
      double preStall = normalized * normalized;
      double postStall = normalized > 1.0D ? (normalized - 1.0D) * (normalized - 1.0D) * 3.0D : 0.0D;
      return Math.max(0.0D, baseDrag) * Math.max(0.0D, aoaDragMultiplier) * (preStall + postStall);
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

   /**
    * Approximates felt load from airspeed and aircraft turn rate. Minecraft motion is
    * measured per tick, so centripetal acceleration is compared with vanilla gravity.
    */
   public static double getApproximateGForce(double speed, double turnRateDegreesPerTick) {
      double turnRate = Math.toRadians(Math.abs(turnRateDegreesPerTick));
      double lateralAcceleration = Math.max(0.0D, speed) * turnRate;
      return Math.sqrt(1.0D + lateralAcceleration * lateralAcceleration / (0.08D * 0.08D));
   }

   /** Progressive control loss between the comfortable and structural load limits. */
   public static double getHighGControlAuthority(double gForce, float comfortableG, float structuralG,
                                                  float controlPenalty) {
      double comfortable = Math.max(1.0D, (double)comfortableG);
      double structural = Math.max(comfortable + 0.01D, (double)structuralG);
      double severity = clamp((gForce - comfortable) / (structural - comfortable), 0.0D, 1.0D);
      return clamp(1.0D - severity * clamp((double)controlPenalty, 0.0D, 1.0D), 0.05D, 1.0D);
   }

   /** Pitch authority fades progressively above the compressibility threshold. */
   public static double getCompressibilityPitchAuthority(double speed, double compressibilitySpeed,
                                                           double maxSafeSpeed, float pitchPenalty) {
      if(compressibilitySpeed <= 0.0D || speed <= compressibilitySpeed) {
         return 1.0D;
      }

      double range = Math.max(0.05D, maxSafeSpeed - compressibilitySpeed);
      double severity = clamp((speed - compressibilitySpeed) / range, 0.0D, 1.0D);
      return clamp(1.0D - severity * clamp((double)pitchPenalty, 0.0D, 1.0D), 0.05D, 1.0D);
   }

   /** Relative overspeed above the safe limit; 1 means twice the safe speed. */
   public static double getOverspeedSeverity(double speed, double maxSafeSpeed) {
      return maxSafeSpeed > 0.0D ? Math.max(0.0D, speed / maxSafeSpeed - 1.0D) : 0.0D;
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
