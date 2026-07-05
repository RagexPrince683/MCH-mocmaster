package mcheli.aircraft;

import mcheli.MCH_Lib;
import mcheli.wrapper.W_Vec3;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * Ship-only oriented bounding box collision primitive.
 *
 * Minecraft still requires AxisAlignedBB values for entity discovery/search.  Those broad-phase
 * boxes are exposed by getEnclosingAABB(); all physical ship checks should use this OBB directly.
 */
public class MCH_ShipOBB {
   private Vec3 center;
   private Vec3 previousCenter;
   private final double halfWidth;
   private final double halfHeight;
   private Vec3 yawAxis;
   private Vec3 pitchAxis;
   private Vec3 rollAxis;
   private float yaw;
   private float pitch;
   private float roll;

   public MCH_ShipOBB(float width, float height) {
      this.halfWidth = (double)width / 2.0D;
      this.halfHeight = (double)height / 2.0D;
      this.center = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
      this.previousCenter = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
      this.update(Vec3.createVectorHelper(0.0D, 0.0D, 0.0D), 0.0F, 0.0F, 0.0F);
   }

   public void update(Vec3 worldCenter, float yaw, float pitch, float roll) {
      this.previousCenter = this.center;
      this.center = Vec3.createVectorHelper(worldCenter.xCoord, worldCenter.yCoord, worldCenter.zCoord);
      this.yaw = yaw;
      this.pitch = pitch;
      this.roll = roll;
      this.yawAxis = MCH_Lib.RotVec3(Vec3.createVectorHelper(1.0D, 0.0D, 0.0D), -yaw, -pitch, -roll);
      this.pitchAxis = MCH_Lib.RotVec3(Vec3.createVectorHelper(0.0D, 1.0D, 0.0D), -yaw, -pitch, -roll);
      this.rollAxis = MCH_Lib.RotVec3(Vec3.createVectorHelper(0.0D, 0.0D, 1.0D), -yaw, -pitch, -roll);
   }

   public Vec3 getCenter() { return this.center; }
   public Vec3 getPreviousCenter() { return this.previousCenter; }
   public double getHalfWidth() { return this.halfWidth; }
   public double getHalfHeight() { return this.halfHeight; }
   public Vec3 getYawAxis() { return this.yawAxis; }
   public Vec3 getPitchAxis() { return this.pitchAxis; }
   public Vec3 getRollAxis() { return this.rollAxis; }

   public Vec3 getWorldTopCenter() {
      return this.localToWorld(Vec3.createVectorHelper(0.0D, this.halfHeight, 0.0D));
   }

   public Vec3 localToWorld(Vec3 local) {
      Vec3 rotated = MCH_Lib.RotVec3(local, -this.yaw, -this.pitch, -this.roll);
      return Vec3.createVectorHelper(this.center.xCoord + rotated.xCoord, this.center.yCoord + rotated.yCoord, this.center.zCoord + rotated.zCoord);
   }

   public Vec3 toLocal(Vec3 world) {
      Vec3 relative = Vec3.createVectorHelper(world.xCoord - this.center.xCoord, world.yCoord - this.center.yCoord, world.zCoord - this.center.zCoord);
      relative.rotateAroundY(this.yaw / 180.0F * 3.1415927F);
      relative.rotateAroundX(this.pitch / 180.0F * 3.1415927F);
      W_Vec3.rotateAroundZ(this.roll / 180.0F * 3.1415927F, relative);
      return relative;
   }

   /**
    * Returns the world AABB enclosing this OBB for Minecraft broad-phase entity discovery/search only.
    * Do not use the returned AABB as a ship physical collision volume.
    */
   public AxisAlignedBB getEnclosingAABB() {
      AxisAlignedBB enclosing = null;
      for(int x = -1; x <= 1; x += 2) {
         for(int y = -1; y <= 1; y += 2) {
            for(int z = -1; z <= 1; z += 2) {
               Vec3 corner = this.localToWorld(Vec3.createVectorHelper((double)x * this.halfWidth, (double)y * this.halfHeight, (double)z * this.halfWidth));
               AxisAlignedBB point = AxisAlignedBB.getBoundingBox(corner.xCoord, corner.yCoord, corner.zCoord, corner.xCoord, corner.yCoord, corner.zCoord);
               enclosing = enclosing == null ? point : enclosing.func_111270_a(point);
            }
         }
      }
      return enclosing;
   }

   public boolean isEntityOnTop(AxisAlignedBB entityBox, double horizontalInset, double belowTolerance, double aboveTolerance) {
      double centerX = (entityBox.minX + entityBox.maxX) / 2.0D;
      double centerZ = (entityBox.minZ + entityBox.maxZ) / 2.0D;
      return this.isFootPointOnTop(centerX, entityBox.minY, centerZ, horizontalInset, belowTolerance, aboveTolerance)
              || this.isFootPointOnTop(entityBox.minX, entityBox.minY, entityBox.minZ, horizontalInset, belowTolerance, aboveTolerance)
              || this.isFootPointOnTop(entityBox.minX, entityBox.minY, entityBox.maxZ, horizontalInset, belowTolerance, aboveTolerance)
              || this.isFootPointOnTop(entityBox.maxX, entityBox.minY, entityBox.minZ, horizontalInset, belowTolerance, aboveTolerance)
              || this.isFootPointOnTop(entityBox.maxX, entityBox.minY, entityBox.maxZ, horizontalInset, belowTolerance, aboveTolerance);
   }

   private boolean isFootPointOnTop(double x, double y, double z, double horizontalInset, double belowTolerance, double aboveTolerance) {
      Vec3 localFeet = this.toLocal(Vec3.createVectorHelper(x, y, z));
      return localFeet.xCoord > -this.halfWidth + horizontalInset
              && localFeet.xCoord < this.halfWidth - horizontalInset
              && localFeet.zCoord > -this.halfWidth + horizontalInset
              && localFeet.zCoord < this.halfWidth - horizontalInset
              && localFeet.yCoord >= this.halfHeight - belowTolerance
              && localFeet.yCoord <= this.halfHeight + aboveTolerance;
   }

   public double getTopSurfaceY() { return this.getWorldTopCenter().yCoord; }
   public double getPreviousTopSurfaceY() { return this.previousCenter.yCoord + (this.getWorldTopCenter().yCoord - this.center.yCoord); }

   public double calculateDeckYOffset(AxisAlignedBB entityBox, double offset, double supportTolerance) {
      if(offset >= 0.0D || !this.isEntityOnTop(entityBox, 1.0E-4D, supportTolerance, supportTolerance)) return offset;
      double candidate = this.getTopSurfaceY() - entityBox.minY;
      return candidate > offset ? candidate : offset;
   }

   public double calculatePreviousDeckYOffset(AxisAlignedBB entityBox, double offset, double supportTolerance) {
      if(offset >= 0.0D || !this.isEntityOnTop(entityBox, 1.0E-4D, supportTolerance, supportTolerance)) return offset;
      double candidate = this.getPreviousTopSurfaceY() - entityBox.minY;
      return candidate > offset ? candidate : offset;
   }

   public boolean intersectsAABB(AxisAlignedBB aabb) {
      return this.getEnclosingAABB().intersectsWith(aabb) && this.intersectsAABBBySeparatingAxis(aabb);
   }

   private boolean intersectsAABBBySeparatingAxis(AxisAlignedBB aabb) {
      Vec3 aabbCenter = Vec3.createVectorHelper((aabb.minX + aabb.maxX) / 2.0D, (aabb.minY + aabb.maxY) / 2.0D, (aabb.minZ + aabb.maxZ) / 2.0D);
      double[] aabbHalfExtents = new double[] {(aabb.maxX - aabb.minX) / 2.0D, (aabb.maxY - aabb.minY) / 2.0D, (aabb.maxZ - aabb.minZ) / 2.0D};
      double[] obbHalfExtents = new double[] {this.halfWidth, this.halfHeight, this.halfWidth};
      Vec3[] aabbAxes = new Vec3[] {Vec3.createVectorHelper(1.0D, 0.0D, 0.0D), Vec3.createVectorHelper(0.0D, 1.0D, 0.0D), Vec3.createVectorHelper(0.0D, 0.0D, 1.0D)};
      Vec3[] obbAxes = new Vec3[] {this.yawAxis, this.pitchAxis, this.rollAxis};
      Vec3 centerDelta = Vec3.createVectorHelper(aabbCenter.xCoord - this.center.xCoord, aabbCenter.yCoord - this.center.yCoord, aabbCenter.zCoord - this.center.zCoord);
      for(int i = 0; i < 3; ++i) if(this.hasSeparatingAxis(aabbAxes[i], centerDelta, aabbAxes, aabbHalfExtents, obbAxes, obbHalfExtents)) return false;
      for(int i = 0; i < 3; ++i) if(this.hasSeparatingAxis(obbAxes[i], centerDelta, aabbAxes, aabbHalfExtents, obbAxes, obbHalfExtents)) return false;
      for(int i = 0; i < 3; ++i) for(int j = 0; j < 3; ++j) {
         Vec3 axis = this.cross(aabbAxes[i], obbAxes[j]);
         if(this.lengthSq(axis) > 1.0E-12D && this.hasSeparatingAxis(axis, centerDelta, aabbAxes, aabbHalfExtents, obbAxes, obbHalfExtents)) return false;
      }
      return true;
   }

   private boolean hasSeparatingAxis(Vec3 axis, Vec3 centerDelta, Vec3[] aabbAxes, double[] aabbHalfExtents, Vec3[] obbAxes, double[] obbHalfExtents) {
      double centerDistance = Math.abs(this.dot(centerDelta, axis));
      double aabbProjection = 0.0D;
      double obbProjection = 0.0D;
      for(int i = 0; i < 3; ++i) {
         aabbProjection += aabbHalfExtents[i] * Math.abs(this.dot(aabbAxes[i], axis));
         obbProjection += obbHalfExtents[i] * Math.abs(this.dot(obbAxes[i], axis));
      }
      return centerDistance > aabbProjection + obbProjection + 1.0E-7D;
   }

   public double calculateXOffset(AxisAlignedBB entityBox, double offset) { return this.calculateAxisOffset(entityBox, offset, true); }
   public double calculateZOffset(AxisAlignedBB entityBox, double offset) { return this.calculateAxisOffset(entityBox, offset, false); }

   private double calculateAxisOffset(AxisAlignedBB entityBox, double offset, boolean xAxis) {
      if(offset == 0.0D || this.intersectsAABB(entityBox)) return offset;
      AxisAlignedBB moved = xAxis ? entityBox.getOffsetBoundingBox(offset, 0.0D, 0.0D) : entityBox.getOffsetBoundingBox(0.0D, 0.0D, offset);
      if(!this.intersectsAABB(moved)) return offset;
      double clear = 0.0D;
      double blocked = offset;
      for(int i = 0; i < 32; ++i) {
         double mid = (clear + blocked) / 2.0D;
         AxisAlignedBB test = xAxis ? entityBox.getOffsetBoundingBox(mid, 0.0D, 0.0D) : entityBox.getOffsetBoundingBox(0.0D, 0.0D, mid);
         if(this.intersectsAABB(test)) blocked = mid; else clear = mid;
      }
      return Math.abs(clear) < 1.0E-7D ? 0.0D : clear;
   }

   public Vec3 getHorizontalPushOut(AxisAlignedBB entityBox, double epsilon) {
      Vec3 localCenter = this.toLocal(Vec3.createVectorHelper((entityBox.minX + entityBox.maxX) / 2.0D, (entityBox.minY + entityBox.maxY) / 2.0D, (entityBox.minZ + entityBox.maxZ) / 2.0D));
      Vec3 localExtent = this.getLocalHalfExtents(entityBox);
      if(Math.abs(localCenter.yCoord) >= this.halfHeight + localExtent.yCoord || Math.abs(localCenter.xCoord) >= this.halfWidth + localExtent.xCoord || Math.abs(localCenter.zCoord) >= this.halfWidth + localExtent.zCoord) return null;
      double pushX = this.halfWidth + localExtent.xCoord - Math.abs(localCenter.xCoord);
      double pushZ = this.halfWidth + localExtent.zCoord - Math.abs(localCenter.zCoord);
      Vec3 localPush = pushX < pushZ ? Vec3.createVectorHelper((localCenter.xCoord < 0.0D ? -pushX - epsilon : pushX + epsilon), 0.0D, 0.0D) : Vec3.createVectorHelper(0.0D, 0.0D, (localCenter.zCoord < 0.0D ? -pushZ - epsilon : pushZ + epsilon));
      return this.localToWorld(localPush).addVector(-this.center.xCoord, -this.center.yCoord, -this.center.zCoord);
   }

   private Vec3 getLocalHalfExtents(AxisAlignedBB box) {
      double centerX = (box.minX + box.maxX) / 2.0D;
      double centerY = (box.minY + box.maxY) / 2.0D;
      double centerZ = (box.minZ + box.maxZ) / 2.0D;
      Vec3 center = this.toLocal(Vec3.createVectorHelper(centerX, centerY, centerZ));
      Vec3 x = this.toLocal(Vec3.createVectorHelper(box.maxX, centerY, centerZ));
      Vec3 y = this.toLocal(Vec3.createVectorHelper(centerX, box.maxY, centerZ));
      Vec3 z = this.toLocal(Vec3.createVectorHelper(centerX, centerY, box.maxZ));
      return Vec3.createVectorHelper(Math.abs(x.xCoord - center.xCoord) + Math.abs(y.xCoord - center.xCoord) + Math.abs(z.xCoord - center.xCoord), Math.abs(x.yCoord - center.yCoord) + Math.abs(y.yCoord - center.yCoord) + Math.abs(z.yCoord - center.yCoord), Math.abs(x.zCoord - center.zCoord) + Math.abs(y.zCoord - center.zCoord) + Math.abs(z.zCoord - center.zCoord));
   }

   public MovingObjectPosition calculateIntercept(Vec3 start, Vec3 end) {
      Vec3 localStart = this.toLocal(start);
      Vec3 localEnd = this.toLocal(end);
      double dx = localEnd.xCoord - localStart.xCoord;
      double dy = localEnd.yCoord - localStart.yCoord;
      double dz = localEnd.zCoord - localStart.zCoord;
      double[] interval = new double[] {0.0D, 1.0D};
      if(!this.clipAxis(localStart.xCoord, dx, -this.halfWidth, this.halfWidth, interval)) return null;
      if(!this.clipAxis(localStart.yCoord, dy, -this.halfHeight, this.halfHeight, interval)) return null;
      if(!this.clipAxis(localStart.zCoord, dz, -this.halfWidth, this.halfWidth, interval)) return null;
      Vec3 hit = start.addVector((end.xCoord - start.xCoord) * interval[0], (end.yCoord - start.yCoord) * interval[0], (end.zCoord - start.zCoord) * interval[0]);
      return new MovingObjectPosition(0, 0, 0, 0, hit);
   }

   private boolean clipAxis(double start, double delta, double min, double max, double[] interval) {
      if(Math.abs(delta) < 1.0E-7D) return start >= min && start <= max;
      double t1 = (min - start) / delta;
      double t2 = (max - start) / delta;
      if(t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
      if(t1 > interval[0]) interval[0] = t1;
      if(t2 < interval[1]) interval[1] = t2;
      return interval[0] <= interval[1];
   }

   private double dot(Vec3 a, Vec3 b) { return a.xCoord * b.xCoord + a.yCoord * b.yCoord + a.zCoord * b.zCoord; }
   private Vec3 cross(Vec3 a, Vec3 b) { return Vec3.createVectorHelper(a.yCoord * b.zCoord - a.zCoord * b.yCoord, a.zCoord * b.xCoord - a.xCoord * b.zCoord, a.xCoord * b.yCoord - a.yCoord * b.xCoord); }
   private double lengthSq(Vec3 v) { return v.xCoord * v.xCoord + v.yCoord * v.yCoord + v.zCoord * v.zCoord; }
}
