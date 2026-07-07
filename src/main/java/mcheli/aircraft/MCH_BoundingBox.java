package mcheli.aircraft;

import mcheli.MCH_Lib;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class MCH_BoundingBox {

   /** Broad-phase compatibility AABB containing the oriented box. */
   public final AxisAlignedBB boundingBox;
   public final AxisAlignedBB backupBoundingBox;
   public final double offsetX;
   public final double offsetY;
   public final double offsetZ;
   public final float width;
   public final float height;
   public final float depth;
   public Vec3 rotatedOffset;
   public Vec3 nowPos;
   public Vec3 prevPos;
   public final float damegeFactor;
   public EnumBoundingBoxType boundingBoxType = EnumBoundingBoxType.DEFAULT;
   private Vec3 axisX = Vec3.createVectorHelper(1.0D, 0.0D, 0.0D);
   private Vec3 axisY = Vec3.createVectorHelper(0.0D, 1.0D, 0.0D);
   private Vec3 axisZ = Vec3.createVectorHelper(0.0D, 0.0D, 1.0D);
   private final double halfWidth;
   private final double halfHeight;
   private final double halfDepth;

   public MCH_BoundingBox(double x, double y, double z, float w, float h, float df) {
      this(x, y, z, w, h, w, df);
   }

   public MCH_BoundingBox(double x, double y, double z, float w, float h, float d, float df) {
      this.offsetX = x;
      this.offsetY = y;
      this.offsetZ = z;
      this.width = w;
      this.height = h;
      this.depth = d;
      this.halfWidth = (double)(w / 2.0F);
      this.halfHeight = (double)(h / 2.0F);
      this.halfDepth = (double)(d / 2.0F);
      this.damegeFactor = df;
      this.boundingBox = AxisAlignedBB.getBoundingBox(x - this.halfWidth, y - this.halfHeight, z - this.halfDepth, x + this.halfWidth, y + this.halfHeight, z + this.halfDepth);
      this.backupBoundingBox = AxisAlignedBB.getBoundingBox(x - this.halfWidth, y - this.halfHeight, z - this.halfDepth, x + this.halfWidth, y + this.halfHeight, z + this.halfDepth);
      this.nowPos = Vec3.createVectorHelper(x, y, z);
      this.prevPos = Vec3.createVectorHelper(x, y, z);
      this.updatePosition(0.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0.0F);
   }

   public MCH_BoundingBox copy() {
      MCH_BoundingBox copy = new MCH_BoundingBox(this.offsetX, this.offsetY, this.offsetZ, this.width, this.height, this.depth, this.damegeFactor);
      copy.boundingBoxType = this.boundingBoxType;
      return copy;
   }

   public wheelBoundingBox copy2() {
      wheelBoundingBox copy = new wheelBoundingBox(this.offsetX, this.offsetY, this.offsetZ, this.width, this.height, this.depth, this.damegeFactor);
      copy.boundingBoxType = this.boundingBoxType;
      return copy;
   }

   public void updatePosition(double posX, double posY, double posZ, float yaw, float pitch, float roll) {
      Vec3 v = Vec3.createVectorHelper(this.offsetX, this.offsetY, this.offsetZ);
      this.rotatedOffset = MCH_Lib.RotVec3(v, -yaw, -pitch, -roll);
      this.axisX = MCH_Lib.RotVec3(Vec3.createVectorHelper(1.0D, 0.0D, 0.0D), -yaw, -pitch, -roll).normalize();
      this.axisY = MCH_Lib.RotVec3(Vec3.createVectorHelper(0.0D, 1.0D, 0.0D), -yaw, -pitch, -roll).normalize();
      this.axisZ = MCH_Lib.RotVec3(Vec3.createVectorHelper(0.0D, 0.0D, 1.0D), -yaw, -pitch, -roll).normalize();
      double x = posX + this.rotatedOffset.xCoord;
      double y = posY + this.rotatedOffset.yCoord;
      double z = posZ + this.rotatedOffset.zCoord;
      this.prevPos.xCoord = this.nowPos.xCoord;
      this.prevPos.yCoord = this.nowPos.yCoord;
      this.prevPos.zCoord = this.nowPos.zCoord;
      this.nowPos.xCoord = x;
      this.nowPos.yCoord = y;
      this.nowPos.zCoord = z;
      this.backupBoundingBox.setBB(this.boundingBox);
      this.updateEnclosingAabb();
   }

   public boolean contains(Vec3 point) {
      Vec3 d = Vec3.createVectorHelper(point.xCoord - this.nowPos.xCoord, point.yCoord - this.nowPos.yCoord, point.zCoord - this.nowPos.zCoord);
      return Math.abs(dot(d, this.axisX)) <= this.halfWidth
              && Math.abs(dot(d, this.axisY)) <= this.halfHeight
              && Math.abs(dot(d, this.axisZ)) <= this.halfDepth;
   }

   public double distanceTo(Vec3 point) {
      Vec3 d = Vec3.createVectorHelper(point.xCoord - this.nowPos.xCoord, point.yCoord - this.nowPos.yCoord, point.zCoord - this.nowPos.zCoord);
      double dx = Math.max(Math.abs(dot(d, this.axisX)) - this.halfWidth, 0.0D);
      double dy = Math.max(Math.abs(dot(d, this.axisY)) - this.halfHeight, 0.0D);
      double dz = Math.max(Math.abs(dot(d, this.axisZ)) - this.halfDepth, 0.0D);
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   public boolean intersectsWith(AxisAlignedBB aabb) {
      if(aabb == null || !this.boundingBox.intersectsWith(aabb)) {
         return false;
      }
      Vec3 aabbCenter = Vec3.createVectorHelper((aabb.minX + aabb.maxX) / 2.0D, (aabb.minY + aabb.maxY) / 2.0D, (aabb.minZ + aabb.maxZ) / 2.0D);
      double[] a = new double[]{this.halfWidth, this.halfHeight, this.halfDepth};
      double[] b = new double[]{(aabb.maxX - aabb.minX) / 2.0D, (aabb.maxY - aabb.minY) / 2.0D, (aabb.maxZ - aabb.minZ) / 2.0D};
      Vec3[] u = new Vec3[]{this.axisX, this.axisY, this.axisZ};
      Vec3[] v = new Vec3[]{Vec3.createVectorHelper(1.0D, 0.0D, 0.0D), Vec3.createVectorHelper(0.0D, 1.0D, 0.0D), Vec3.createVectorHelper(0.0D, 0.0D, 1.0D)};
      return intersectsObb(aabbCenter, a, b, u, v);
   }

   public MovingObjectPosition calculateIntercept(Vec3 start, Vec3 end) {
      if(this.boundingBox.calculateIntercept(start, end) == null && !this.contains(start)) {
         return null;
      }
      Vec3 dir = Vec3.createVectorHelper(end.xCoord - start.xCoord, end.yCoord - start.yCoord, end.zCoord - start.zCoord);
      Vec3 p = Vec3.createVectorHelper(start.xCoord - this.nowPos.xCoord, start.yCoord - this.nowPos.yCoord, start.zCoord - this.nowPos.zCoord);
      double[] s = new double[]{dot(p, this.axisX), dot(p, this.axisY), dot(p, this.axisZ)};
      double[] d = new double[]{dot(dir, this.axisX), dot(dir, this.axisY), dot(dir, this.axisZ)};
      double[] e = new double[]{this.halfWidth, this.halfHeight, this.halfDepth};
      double tMin = 0.0D;
      double tMax = 1.0D;
      for(int i = 0; i < 3; ++i) {
         if(Math.abs(d[i]) < 1.0E-7D) {
            if(s[i] < -e[i] || s[i] > e[i]) return null;
         } else {
            double t1 = (-e[i] - s[i]) / d[i];
            double t2 = (e[i] - s[i]) / d[i];
            if(t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            if(t1 > tMin) tMin = t1;
            if(t2 < tMax) tMax = t2;
            if(tMin > tMax) return null;
         }
      }
      Vec3 hit = Vec3.createVectorHelper(start.xCoord + dir.xCoord * tMin, start.yCoord + dir.yCoord * tMin, start.zCoord + dir.zCoord * tMin);
      return new MovingObjectPosition(0, 0, 0, 0, hit);
   }

   private void updateEnclosingAabb() {
      Vec3[] corners = this.getCorners();
      double minX = corners[0].xCoord, minY = corners[0].yCoord, minZ = corners[0].zCoord;
      double maxX = minX, maxY = minY, maxZ = minZ;
      for(int i = 1; i < corners.length; ++i) {
         minX = Math.min(minX, corners[i].xCoord); minY = Math.min(minY, corners[i].yCoord); minZ = Math.min(minZ, corners[i].zCoord);
         maxX = Math.max(maxX, corners[i].xCoord); maxY = Math.max(maxY, corners[i].yCoord); maxZ = Math.max(maxZ, corners[i].zCoord);
      }
      this.boundingBox.setBounds(minX, minY, minZ, maxX, maxY, maxZ);
   }

   public Vec3[] getCorners() {
      Vec3[] corners = new Vec3[8];
      int i = 0;
      for(int sx = -1; sx <= 1; sx += 2) for(int sy = -1; sy <= 1; sy += 2) for(int sz = -1; sz <= 1; sz += 2) {
         corners[i++] = Vec3.createVectorHelper(this.nowPos.xCoord + this.axisX.xCoord * this.halfWidth * sx + this.axisY.xCoord * this.halfHeight * sy + this.axisZ.xCoord * this.halfDepth * sz,
                 this.nowPos.yCoord + this.axisX.yCoord * this.halfWidth * sx + this.axisY.yCoord * this.halfHeight * sy + this.axisZ.yCoord * this.halfDepth * sz,
                 this.nowPos.zCoord + this.axisX.zCoord * this.halfWidth * sx + this.axisY.zCoord * this.halfHeight * sy + this.axisZ.zCoord * this.halfDepth * sz);
      }
      return corners;
   }

   private boolean intersectsObb(Vec3 centerB, double[] a, double[] b, Vec3[] u, Vec3[] v) {
      double[][] r = new double[3][3];
      double[][] absR = new double[3][3];
      for(int i = 0; i < 3; ++i) for(int j = 0; j < 3; ++j) { r[i][j] = dot(u[i], v[j]); absR[i][j] = Math.abs(r[i][j]) + 1.0E-7D; }
      Vec3 tVec = Vec3.createVectorHelper(centerB.xCoord - this.nowPos.xCoord, centerB.yCoord - this.nowPos.yCoord, centerB.zCoord - this.nowPos.zCoord);
      double[] t = new double[]{dot(tVec, u[0]), dot(tVec, u[1]), dot(tVec, u[2])};
      for(int i = 0; i < 3; ++i) if(Math.abs(t[i]) > a[i] + b[0] * absR[i][0] + b[1] * absR[i][1] + b[2] * absR[i][2]) return false;
      for(int j = 0; j < 3; ++j) if(Math.abs(t[0] * r[0][j] + t[1] * r[1][j] + t[2] * r[2][j]) > b[j] + a[0] * absR[0][j] + a[1] * absR[1][j] + a[2] * absR[2][j]) return false;
      for(int i = 0; i < 3; ++i) for(int j = 0; j < 3; ++j) {
         double ra = a[(i + 1) % 3] * absR[(i + 2) % 3][j] + a[(i + 2) % 3] * absR[(i + 1) % 3][j];
         double rb = b[(j + 1) % 3] * absR[i][(j + 2) % 3] + b[(j + 2) % 3] * absR[i][(j + 1) % 3];
         if(Math.abs(t[(i + 2) % 3] * r[(i + 1) % 3][j] - t[(i + 1) % 3] * r[(i + 2) % 3][j]) > ra + rb) return false;
      }
      return true;
   }

   private static double dot(Vec3 a, Vec3 b) {
      return a.xCoord * b.xCoord + a.yCoord * b.yCoord + a.zCoord * b.zCoord;
   }
}
