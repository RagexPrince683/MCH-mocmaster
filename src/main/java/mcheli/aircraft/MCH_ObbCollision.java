package mcheli.aircraft;

/** Allocation-light full 3D OBB/AABB SAT used by vehicle transform validation. */
public final class MCH_ObbCollision {
   private static final double AXIS_EPSILON = 1.0E-8D;

   public static final class Contact {
      public final double normalX, normalY, normalZ;
      public final double penetration;

      Contact(double x, double y, double z, double penetration) {
         this.normalX = x;
         this.normalY = y;
         this.normalZ = z;
         this.penetration = penetration;
      }
   }

   private MCH_ObbCollision() {}

   /** Tests the three OBB axes, three block axes, and nine cross-product axes. */
   public static Contact intersect(double[] center, double[][] axes, double[] half,
                            double[] blockCenter, double[] blockHalf) {
      double dx = center[0] - blockCenter[0];
      double dy = center[1] - blockCenter[1];
      double dz = center[2] - blockCenter[2];
      double best = Double.MAX_VALUE;
      double bestX = 0.0D, bestY = 0.0D, bestZ = 0.0D;

      for(int i = 0; i < 3; ++i) {
         double[] result = testAxis(axes[i][0], axes[i][1], axes[i][2], dx, dy, dz,
                 axes, half, blockHalf);
         if(result == null) return null;
         if(result[0] < best) { best = result[0]; bestX = result[1]; bestY = result[2]; bestZ = result[3]; }
      }
      for(int i = 0; i < 3; ++i) {
         double ax = i == 0 ? 1.0D : 0.0D;
         double ay = i == 1 ? 1.0D : 0.0D;
         double az = i == 2 ? 1.0D : 0.0D;
         double[] result = testAxis(ax, ay, az, dx, dy, dz, axes, half, blockHalf);
         if(result == null) return null;
         if(result[0] < best) { best = result[0]; bestX = result[1]; bestY = result[2]; bestZ = result[3]; }
      }
      for(int i = 0; i < 3; ++i) for(int j = 0; j < 3; ++j) {
         double bx = j == 0 ? 1.0D : 0.0D;
         double by = j == 1 ? 1.0D : 0.0D;
         double bz = j == 2 ? 1.0D : 0.0D;
         double ax = axes[i][1] * bz - axes[i][2] * by;
         double ay = axes[i][2] * bx - axes[i][0] * bz;
         double az = axes[i][0] * by - axes[i][1] * bx;
         double length = Math.sqrt(ax * ax + ay * ay + az * az);
         if(length < AXIS_EPSILON) continue;
         double[] result = testAxis(ax / length, ay / length, az / length, dx, dy, dz,
                 axes, half, blockHalf);
         if(result == null) return null;
         if(result[0] < best) { best = result[0]; bestX = result[1]; bestY = result[2]; bestZ = result[3]; }
      }
      return new Contact(bestX, bestY, bestZ, best);
   }

   private static double[] testAxis(double x, double y, double z, double dx, double dy, double dz,
                                    double[][] axes, double[] half, double[] blockHalf) {
      double obbRadius = 0.0D;
      for(int i = 0; i < 3; ++i) {
         obbRadius += half[i] * Math.abs(x * axes[i][0] + y * axes[i][1] + z * axes[i][2]);
      }
      double blockRadius = blockHalf[0] * Math.abs(x) + blockHalf[1] * Math.abs(y) + blockHalf[2] * Math.abs(z);
      double signedCenter = dx * x + dy * y + dz * z;
      double overlap = obbRadius + blockRadius - Math.abs(signedCenter);
      if(overlap <= 1.0E-7D) return null;
      double sign = signedCenter < 0.0D ? -1.0D : 1.0D;
      return new double[]{overlap, x * sign, y * sign, z * sign};
   }
}
