package mcheli.tank;

import java.util.List;
import net.minecraft.util.AxisAlignedBB;

/** Read-only collision geometry for civilian body pitch; never moves a wheel or body. */
final class MCH_CarTerrainPitch {
   static final double EPSILON = 1.0E-5D;
   static final float MAX_PITCH = 45.0F;

   private MCH_CarTerrainPitch() {}

   /** Missing support is NaN, including an unloaded column, rather than world height zero. */
   static double highestSurface(List<AxisAlignedBB> boxes, AxisAlignedBB column,
         double referenceY, double stepHeight, double clearanceHeight) {
      double highest = Double.NaN;
      for(AxisAlignedBB box : boxes) {
         double surface = box.maxY;
         if(box.maxX <= column.minX || box.minX >= column.maxX
               || box.maxZ <= column.minZ || box.minZ >= column.maxZ
               || surface < referenceY - stepHeight - EPSILON
               || surface > referenceY + stepHeight + EPSILON) continue;

         // A stair's individual collision boxes supply its half/full-height tread.
         // Reject buried treads and low ceilings before accepting even a reachable top.
         AxisAlignedBB clearance = AxisAlignedBB.getBoundingBox(column.minX, surface + EPSILON,
               column.minZ, column.maxX, surface + clearanceHeight, column.maxZ);
         boolean clear = true;
         for(AxisAlignedBB obstacle : boxes) {
            if(obstacle.intersectsWith(clearance)) {
               clear = false;
               break;
            }
         }
         if(clear && (Double.isNaN(highest) || surface > highest)) highest = surface;
      }
      return highest;
   }

   static float angle(double front, double rear, double wheelbase) {
      // The model faces +Z. GL's positive X rotation lowers +Z, so nose-up is negative.
      return (float)Math.max(-MAX_PITCH, Math.min(MAX_PITCH,
            -Math.toDegrees(Math.atan2(front - rear, Math.max(0.5D, wheelbase)))));
   }

   static float approach(float previous, float pitch, float smoothing) {
      if(Float.isNaN(pitch)) return previous * 0.94F;
      // Do not spend transition ticks pointing downhill after reaching a valid climb.
      if(previous * pitch < 0.0F) previous = 0.0F;
      float result = previous + (pitch - previous) * smoothing;
      return Math.abs(pitch) < 0.01F && Math.abs(result) < 0.02F ? 0.0F : result;
   }
}
