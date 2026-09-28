package mcheli.tank;

/** Optional civilian reverse controls; velocities are in blocks per 20 Hz tick. */
final class MCH_CarReverseControl {
   // Match the maximum forward thrust (currentThrottle / 10), not a speed limit.
   static final float MAX_REVERSE_THROTTLE = 0.1F;

   private MCH_CarReverseControl() {}

   static float parseSpeed(String data) {
      try {
         float value = Float.parseFloat(data.trim());
         return Float.isNaN(value) || Float.isInfinite(value) ? 0.0F
                 : Math.max(0.0F, Math.min(4.0F, value));
      } catch(NumberFormatException ex) {
         return 0.0F;
      }
   }

   static float boundThrottle(float throttle) {
      return Float.isNaN(throttle) ? 0.0F
              : Math.max(0.0F, Math.min(MAX_REVERSE_THROTTLE, throttle));
   }

   static boolean isReverseInput(float limit, boolean throttleDown, double forwardThrottle) {
      return limit > 0.0F && throttleDown && forwardThrottle <= 0.0D;
   }

   /** Scale horizontal speed only when powered motion points behind the current heading. */
   static double speedScale(double motionX, double motionZ, float yaw, float limit, boolean powered) {
      if(!powered || limit <= 0.0F || Float.isNaN(limit) || Float.isInfinite(limit)) return 1.0D;
      double angle = Math.toRadians(yaw);
      double forwardSpeed = -motionX * Math.sin(angle) + motionZ * Math.cos(angle);
      double speed = Math.hypot(motionX, motionZ);
      return forwardSpeed < 0.0D && speed > limit ? limit / speed : 1.0D;
   }
}
