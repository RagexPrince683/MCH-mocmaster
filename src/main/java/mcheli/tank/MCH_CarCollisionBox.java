package mcheli.tank;

import java.util.ArrayList;
import java.util.List;
import mcheli.aircraft.MCH_BoundingBox;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

/** Translation-only SAT sweeps of the configured civilian body volume at its current pose. */
final class MCH_CarCollisionBox {
   static final double EPSILON = 1.0E-7D;
   final AxisAlignedBB bounds;
   private final double[][] axes;
   private final double[] radii;
   private double x, y, z;

   MCH_CarCollisionBox(AxisAlignedBB box) {
      this.bounds = box.copy();
      this.x = (box.minX + box.maxX) * 0.5;
      this.y = (box.minY + box.maxY) * 0.5;
      this.z = (box.minZ + box.maxZ) * 0.5;
      this.axes = new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
      this.radii = new double[]{(box.maxX - box.minX) * 0.5,
            (box.maxY - box.minY) * 0.5, (box.maxZ - box.minZ) * 0.5};
   }

   MCH_CarCollisionBox(MCH_BoundingBox box) {
      this.bounds = box.boundingBox.copy();
      this.x = box.nowPos.xCoord; this.y = box.nowPos.yCoord; this.z = box.nowPos.zCoord;
      Vec3[] corners = box.getCorners();
      double[][] edges = new double[3][3];
      int[] ends = {4, 2, 1};
      List<double[]> normals = new ArrayList<double[]>();
      normals.add(new double[]{1, 0, 0}); normals.add(new double[]{0, 1, 0}); normals.add(new double[]{0, 0, 1});
      for(int i = 0; i < 3; ++i) {
         Vec3 end = corners[ends[i]];
         double[] edge = {(end.xCoord - corners[0].xCoord) * 0.5,
               (end.yCoord - corners[0].yCoord) * 0.5, (end.zCoord - corners[0].zCoord) * 0.5};
         edges[i] = edge;
         addAxis(normals, edge[0], edge[1], edge[2]);
         // Cross each box edge with the three world axes.
         addAxis(normals, 0, edge[2], -edge[1]);
         addAxis(normals, -edge[2], 0, edge[0]);
         addAxis(normals, edge[1], -edge[0], 0);
      }
      this.axes = normals.toArray(new double[0][]);
      this.radii = new double[axes.length];
      for(int i = 0; i < axes.length; ++i) {
         for(double[] edge : edges) radii[i] += Math.abs(dot(edge, axes[i]));
      }
   }

   private MCH_CarCollisionBox(MCH_CarCollisionBox source) {
      this.bounds = source.bounds.copy(); this.x = source.x; this.y = source.y; this.z = source.z;
      this.axes = source.axes; this.radii = source.radii;
   }

   MCH_CarCollisionBox copy() { return new MCH_CarCollisionBox(this); }

   void offset(double dx, double dy, double dz) {
      x += dx; y += dy; z += dz; bounds.offset(dx, dy, dz);
   }

   boolean intersects(AxisAlignedBB obstacle) {
      for(int i = 0; i < axes.length; ++i) {
         if(radius(obstacle, i) - Math.abs(distance(obstacle, axes[i])) <= EPSILON) return false;
      }
      return true;
   }

   double clip(AxisAlignedBB obstacle, int direction, double requested) {
      return requested * clipFraction(obstacle, direction == 0 ? requested : 0,
            direction == 1 ? requested : 0, direction == 2 ? requested : 0);
   }

   double clipFraction(AxisAlignedBB obstacle, double dx, double dy, double dz) {
      if(dx == 0 && dy == 0 && dz == 0) return 1;
      double enter = Double.NEGATIVE_INFINITY, exit = Double.POSITIVE_INFINITY;
      double leastPenetration = Double.POSITIVE_INFINITY, outwardMovement = 0;
      for(int i = 0; i < axes.length; ++i) {
         double[] axis = axes[i];
         double d = distance(obstacle, axis), r = radius(obstacle, i);
         double v = dx * axis[0] + dy * axis[1] + dz * axis[2];
         double penetration = r - Math.abs(d);
         if(penetration < leastPenetration) {
            leastPenetration = penetration;
            outwardMovement = d >= 0 ? v : -v;
         }
         if(Math.abs(v) < 1.0E-12D) {
            // A touching face parallel to the request must permit sliding.
            if(penetration <= EPSILON) return 1;
         } else {
            double first = (-r - d) / v, last = (r - d) / v;
            enter = Math.max(enter, Math.min(first, last));
            exit = Math.min(exit, Math.max(first, last));
         }
      }
      if(enter >= exit || exit <= 0 || enter >= 1) return 1;
      // enter is a fraction of the request; penetration is a distance along a
      // normalized SAT axis. Use the same contact distance as intersects(), even
      // for tiny support probes, rather than comparing a fraction to that distance.
      if(enter >= 0) return enter;
      if(leastPenetration <= EPSILON) return outwardMovement < 0 ? 0 : 1;
      // A rotation can introduce overlap before translation. Prevent movement deeper
      // through its nearest face, but permit escape; a supported step must still clear
      // every component before it can be accepted.
      return outwardMovement < -EPSILON ? 0 : 1;
   }

   /** A separating face must move outward throughout an already overlapping upward escape. */
   double upwardEscape(AxisAlignedBB obstacle) {
      double distance = Double.POSITIVE_INFINITY;
      for(int i = 0; i < axes.length; ++i) {
         double d = distance(obstacle, axes[i]), vertical = axes[i][1];
         if(d * vertical > EPSILON) {
            distance = Math.min(distance, (radius(obstacle, i) - Math.abs(d)) / Math.abs(vertical));
         }
      }
      return distance;
   }

   private double distance(AxisAlignedBB box, double[] axis) {
      return (x - (box.minX + box.maxX) * 0.5) * axis[0]
            + (y - (box.minY + box.maxY) * 0.5) * axis[1]
            + (z - (box.minZ + box.maxZ) * 0.5) * axis[2];
   }

   private double radius(AxisAlignedBB box, int i) {
      return radii[i] + (box.maxX - box.minX) * 0.5 * Math.abs(axes[i][0])
            + (box.maxY - box.minY) * 0.5 * Math.abs(axes[i][1])
            + (box.maxZ - box.minZ) * 0.5 * Math.abs(axes[i][2]);
   }

   private static double dot(double[] a, double[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }

   private static void addAxis(List<double[]> axes, double x, double y, double z) {
      double length = Math.sqrt(x * x + y * y + z * z);
      if(length > EPSILON) axes.add(new double[]{x / length, y / length, z / length});
   }
}
