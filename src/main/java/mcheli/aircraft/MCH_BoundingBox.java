package mcheli.aircraft;

import mcheli.MCH_Lib;
import mcheli.wrapper.W_Vec3;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class MCH_BoundingBox {

   public final AxisAlignedBB boundingBox;
   public final AxisAlignedBB backupBoundingBox;
   public final double offsetX;
   public final double offsetY;
   public final double offsetZ;
   public final float width;
   public final float height;
   public Vec3 rotatedOffset;
   public Vec3 nowPos;
   public Vec3 prevPos;
   public final float damegeFactor;
   public EnumBoundingBoxType boundingBoxType = EnumBoundingBoxType.DEFAULT;
   private float lastYaw;
   private float lastPitch;
   private float lastRoll;


   public MCH_BoundingBox(double x, double y, double z, float w, float h, float df) {
      this.offsetX = x;
      this.offsetY = y;
      this.offsetZ = z;
      this.width = w;
      this.height = h;
      this.damegeFactor = df;
      this.boundingBox = AxisAlignedBB.getBoundingBox(x - (double)(w / 2.0F), y - (double)(h / 2.0F), z - (double)(w / 2.0F), x + (double)(w / 2.0F), y + (double)(h / 2.0F), z + (double)(w / 2.0F));
      this.backupBoundingBox = AxisAlignedBB.getBoundingBox(x - (double)(w / 2.0F), y - (double)(h / 2.0F), z - (double)(w / 2.0F), x + (double)(w / 2.0F), y + (double)(h / 2.0F), z + (double)(w / 2.0F));
      this.nowPos = Vec3.createVectorHelper(x, y, z);
      this.prevPos = Vec3.createVectorHelper(x, y, z);
      this.updatePosition(0.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0.0F);
   }


   public MCH_BoundingBox copy() {
      MCH_BoundingBox copy = new MCH_BoundingBox(this.offsetX, this.offsetY, this.offsetZ, this.width, this.height, this.damegeFactor);
      copy.boundingBoxType = this.boundingBoxType;
      return copy;
   }

   public wheelBoundingBox copy2() {
      wheelBoundingBox copy = new wheelBoundingBox(this.offsetX, this.offsetY, this.offsetZ, this.width, this.height, this.damegeFactor);
      copy.boundingBoxType = this.boundingBoxType;
      return copy;
   }

   public void updatePosition(double posX, double posY, double posZ, float yaw, float pitch, float roll) {
      this.lastYaw = yaw;
      this.lastPitch = pitch;
      this.lastRoll = roll;
      Vec3 v = Vec3.createVectorHelper(this.offsetX, this.offsetY, this.offsetZ);
      this.rotatedOffset = MCH_Lib.RotVec3(v, -yaw, -pitch, -roll);
      float w = this.width;
      float h = this.height;
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
      this.boundingBox.setBounds(x - (double)(w / 2.0F), y - (double)(h / 2.0F), z - (double)(w / 2.0F), x + (double)(w / 2.0F), y + (double)(h / 2.0F), z + (double)(w / 2.0F));
   }

   public Vec3 getWorldTopCenter() {
      Vec3 top = Vec3.createVectorHelper(0.0D, (double)this.height / 2.0D, 0.0D);
      top = MCH_Lib.RotVec3(top, -this.lastYaw, -this.lastPitch, -this.lastRoll);
      return Vec3.createVectorHelper(this.nowPos.xCoord + top.xCoord, this.nowPos.yCoord + top.yCoord, this.nowPos.zCoord + top.zCoord);
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
      double halfWidth = (double)this.width / 2.0D;
      double halfHeight = (double)this.height / 2.0D;
      return localFeet.xCoord > -halfWidth + horizontalInset
              && localFeet.xCoord < halfWidth - horizontalInset
              && localFeet.zCoord > -halfWidth + horizontalInset
              && localFeet.zCoord < halfWidth - horizontalInset
              && localFeet.yCoord >= halfHeight - belowTolerance
              && localFeet.yCoord <= halfHeight + aboveTolerance;
   }

   public double getTopSurfaceY() {
      return this.getWorldTopCenter().yCoord;
   }

   public double getPreviousTopSurfaceY() {
      return this.prevPos.yCoord + (this.getWorldTopCenter().yCoord - this.nowPos.yCoord);
   }

   public AxisAlignedBB getEnclosingAABB() {
      double halfWidth = (double)this.width / 2.0D;
      double halfHeight = (double)this.height / 2.0D;
      AxisAlignedBB enclosing = null;

      for(int x = -1; x <= 1; x += 2) {
         for(int y = -1; y <= 1; y += 2) {
            for(int z = -1; z <= 1; z += 2) {
               Vec3 corner = Vec3.createVectorHelper((double)x * halfWidth, (double)y * halfHeight, (double)z * halfWidth);
               corner = MCH_Lib.RotVec3(corner, -this.lastYaw, -this.lastPitch, -this.lastRoll);
               double worldX = this.nowPos.xCoord + corner.xCoord;
               double worldY = this.nowPos.yCoord + corner.yCoord;
               double worldZ = this.nowPos.zCoord + corner.zCoord;
               AxisAlignedBB point = AxisAlignedBB.getBoundingBox(worldX, worldY, worldZ, worldX, worldY, worldZ);
               enclosing = enclosing == null ? point : enclosing.func_111270_a(point);
            }
         }
      }

      return enclosing != null ? enclosing : this.boundingBox;
   }

   public double calculateDeckYOffset(AxisAlignedBB entityBox, double offset, double supportTolerance) {
      if(offset >= 0.0D || !this.isEntityOnTop(entityBox, 1.0E-4D, supportTolerance, supportTolerance)) {
         return offset;
      }

      double topY = this.getTopSurfaceY();
      double candidate = topY - entityBox.minY;
      return candidate > offset ? candidate : offset;
   }

   public double calculatePreviousDeckYOffset(AxisAlignedBB entityBox, double offset, double supportTolerance) {
      if(offset >= 0.0D || !this.isEntityOnTop(entityBox, 1.0E-4D, supportTolerance, supportTolerance)) {
         return offset;
      }

      double previousTopY = this.getPreviousTopSurfaceY();
      double candidate = previousTopY - entityBox.minY;
      return candidate > offset ? candidate : offset;
   }


   public boolean intersectsAABB(AxisAlignedBB aabb) {
      if(!this.getEnclosingAABB().intersectsWith(aabb)) {
         return false;
      }

      return this.intersectsAABBBySeparatingAxis(aabb);
   }

   private boolean containsWorldPoint(double x, double y, double z) {
      Vec3 local = this.toLocal(Vec3.createVectorHelper(x, y, z));
      double halfWidth = (double)this.width / 2.0D;
      double halfHeight = (double)this.height / 2.0D;
      return local.xCoord >= -halfWidth && local.xCoord <= halfWidth
              && local.yCoord >= -halfHeight && local.yCoord <= halfHeight
              && local.zCoord >= -halfWidth && local.zCoord <= halfWidth;
   }

   private boolean intersectsAABBBySeparatingAxis(AxisAlignedBB aabb) {
      Vec3 aabbCenter = Vec3.createVectorHelper((aabb.minX + aabb.maxX) / 2.0D, (aabb.minY + aabb.maxY) / 2.0D, (aabb.minZ + aabb.maxZ) / 2.0D);
      double[] aabbHalfExtents = new double[] {
              (aabb.maxX - aabb.minX) / 2.0D,
              (aabb.maxY - aabb.minY) / 2.0D,
              (aabb.maxZ - aabb.minZ) / 2.0D
      };
      double[] obbHalfExtents = new double[] {
              (double)this.width / 2.0D,
              (double)this.height / 2.0D,
              (double)this.width / 2.0D
      };
      Vec3[] aabbAxes = new Vec3[] {
              Vec3.createVectorHelper(1.0D, 0.0D, 0.0D),
              Vec3.createVectorHelper(0.0D, 1.0D, 0.0D),
              Vec3.createVectorHelper(0.0D, 0.0D, 1.0D)
      };
      Vec3[] obbAxes = new Vec3[] {
              MCH_Lib.RotVec3(Vec3.createVectorHelper(1.0D, 0.0D, 0.0D), -this.lastYaw, -this.lastPitch, -this.lastRoll),
              MCH_Lib.RotVec3(Vec3.createVectorHelper(0.0D, 1.0D, 0.0D), -this.lastYaw, -this.lastPitch, -this.lastRoll),
              MCH_Lib.RotVec3(Vec3.createVectorHelper(0.0D, 0.0D, 1.0D), -this.lastYaw, -this.lastPitch, -this.lastRoll)
      };
      Vec3 centerDelta = Vec3.createVectorHelper(aabbCenter.xCoord - this.nowPos.xCoord, aabbCenter.yCoord - this.nowPos.yCoord, aabbCenter.zCoord - this.nowPos.zCoord);

      for(int i = 0; i < 3; ++i) {
         if(this.hasSeparatingAxis(aabbAxes[i], centerDelta, aabbAxes, aabbHalfExtents, obbAxes, obbHalfExtents)) {
            return false;
         }
      }

      for(int i = 0; i < 3; ++i) {
         if(this.hasSeparatingAxis(obbAxes[i], centerDelta, aabbAxes, aabbHalfExtents, obbAxes, obbHalfExtents)) {
            return false;
         }
      }

      for(int i = 0; i < 3; ++i) {
         for(int j = 0; j < 3; ++j) {
            Vec3 axis = this.cross(aabbAxes[i], obbAxes[j]);
            if(this.lengthSq(axis) > 1.0E-12D && this.hasSeparatingAxis(axis, centerDelta, aabbAxes, aabbHalfExtents, obbAxes, obbHalfExtents)) {
               return false;
            }
         }
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

   private double dot(Vec3 a, Vec3 b) {
      return a.xCoord * b.xCoord + a.yCoord * b.yCoord + a.zCoord * b.zCoord;
   }

   private Vec3 cross(Vec3 a, Vec3 b) {
      return Vec3.createVectorHelper(
              a.yCoord * b.zCoord - a.zCoord * b.yCoord,
              a.zCoord * b.xCoord - a.xCoord * b.zCoord,
              a.xCoord * b.yCoord - a.yCoord * b.xCoord);
   }

   private double lengthSq(Vec3 v) {
      return v.xCoord * v.xCoord + v.yCoord * v.yCoord + v.zCoord * v.zCoord;
   }

   public double calculateXOffset(AxisAlignedBB entityBox, double offset) {
      return this.calculateAxisOffset(entityBox, offset, true);
   }

   public double calculateZOffset(AxisAlignedBB entityBox, double offset) {
      return this.calculateAxisOffset(entityBox, offset, false);
   }

   private double calculateAxisOffset(AxisAlignedBB entityBox, double offset, boolean xAxis) {
      if(offset == 0.0D) {
         return offset;
      }

      if(this.intersectsAABB(entityBox)) {
         return 0.0D;
      }

      AxisAlignedBB moved = xAxis ? entityBox.getOffsetBoundingBox(offset, 0.0D, 0.0D) : entityBox.getOffsetBoundingBox(0.0D, 0.0D, offset);
      if(!this.intersectsAABB(moved)) {
         return offset;
      }

      double safeOffset = this.calculateSweptAxisOffset(entityBox, offset, xAxis);
      double clear = safeOffset;
      double blocked = offset;

      // The swept OBB result is the authoritative collision time. This binary
      // search is only a deterministic refinement against the same SAT-backed
      // intersectsAABB() predicate used elsewhere, not a broad-phase AABB
      // approximation.
      for(int i = 0; i < 16; ++i) {
         double mid = (clear + blocked) / 2.0D;
         AxisAlignedBB test = xAxis ? entityBox.getOffsetBoundingBox(mid, 0.0D, 0.0D) : entityBox.getOffsetBoundingBox(0.0D, 0.0D, mid);
         if(this.intersectsAABB(test)) {
            blocked = mid;
         } else {
            clear = mid;
         }
      }

      return Math.abs(clear) < 1.0E-7D ? 0.0D : clear;
   }

   private double calculateSweptAxisOffset(AxisAlignedBB entityBox, double offset, boolean xAxis) {
      Vec3 center = this.toLocal(Vec3.createVectorHelper(
              (entityBox.minX + entityBox.maxX) / 2.0D,
              (entityBox.minY + entityBox.maxY) / 2.0D,
              (entityBox.minZ + entityBox.maxZ) / 2.0D));
      double[] aabbHalfExtents = new double[] {
              (entityBox.maxX - entityBox.minX) / 2.0D,
              (entityBox.maxY - entityBox.minY) / 2.0D,
              (entityBox.maxZ - entityBox.minZ) / 2.0D
      };
      double[] obbHalfExtents = new double[] {
              (double)this.width / 2.0D,
              (double)this.height / 2.0D,
              (double)this.width / 2.0D
      };
      Vec3[] obbAxes = new Vec3[] {
              Vec3.createVectorHelper(1.0D, 0.0D, 0.0D),
              Vec3.createVectorHelper(0.0D, 1.0D, 0.0D),
              Vec3.createVectorHelper(0.0D, 0.0D, 1.0D)
      };
      Vec3[] aabbAxes = new Vec3[] {
              this.toLocalDirection(1.0D, 0.0D, 0.0D),
              this.toLocalDirection(0.0D, 1.0D, 0.0D),
              this.toLocalDirection(0.0D, 0.0D, 1.0D)
      };
      Vec3 movement = xAxis ? this.toLocalDirection(offset, 0.0D, 0.0D) : this.toLocalDirection(0.0D, 0.0D, offset);
      double[] interval = new double[] {0.0D, 1.0D};

      for(int i = 0; i < 3; ++i) {
         if(!this.clipSweptSeparatingAxis(obbAxes[i], center, movement, obbAxes, obbHalfExtents, aabbAxes, aabbHalfExtents, interval)) return 0.0D;
         if(!this.clipSweptSeparatingAxis(aabbAxes[i], center, movement, obbAxes, obbHalfExtents, aabbAxes, aabbHalfExtents, interval)) return 0.0D;
      }

      for(int i = 0; i < 3; ++i) {
         for(int j = 0; j < 3; ++j) {
            Vec3 axis = this.cross(obbAxes[i], aabbAxes[j]);
            if(this.lengthSq(axis) > 1.0E-12D && !this.clipSweptSeparatingAxis(axis, center, movement, obbAxes, obbHalfExtents, aabbAxes, aabbHalfExtents, interval)) {
               return 0.0D;
            }
         }
      }

      double safe = offset * Math.max(0.0D, Math.min(1.0D, interval[0]));
      return Math.abs(safe) < 1.0E-7D ? 0.0D : safe;
   }

   private boolean clipSweptSeparatingAxis(Vec3 axis, Vec3 center, Vec3 movement, Vec3[] obbAxes, double[] obbHalfExtents, Vec3[] aabbAxes, double[] aabbHalfExtents, double[] interval) {
      double centerDistance = this.dot(center, axis);
      double velocity = this.dot(movement, axis);
      double radius = 0.0D;

      for(int i = 0; i < 3; ++i) {
         radius += obbHalfExtents[i] * Math.abs(this.dot(obbAxes[i], axis));
         radius += aabbHalfExtents[i] * Math.abs(this.dot(aabbAxes[i], axis));
      }

      if(Math.abs(velocity) < 1.0E-9D) {
         return Math.abs(centerDistance) <= radius + 1.0E-7D;
      }

      double enter = (-radius - centerDistance) / velocity;
      double exit = (radius - centerDistance) / velocity;
      if(enter > exit) {
         double tmp = enter;
         enter = exit;
         exit = tmp;
      }

      if(enter > interval[0]) interval[0] = enter;
      if(exit < interval[1]) interval[1] = exit;
      return interval[0] <= interval[1] && interval[1] >= 0.0D && interval[0] <= 1.0D;
   }

   private Vec3 toLocalDirection(double x, double y, double z) {
      Vec3 direction = Vec3.createVectorHelper(x, y, z);
      direction.rotateAroundY(this.lastYaw / 180.0F * 3.1415927F);
      direction.rotateAroundX(this.lastPitch / 180.0F * 3.1415927F);
      W_Vec3.rotateAroundZ(this.lastRoll / 180.0F * 3.1415927F, direction);
      return direction;
   }

   public Vec3 getHorizontalPushOut(AxisAlignedBB entityBox, double epsilon) {
      Vec3 localCenter = this.toLocal(Vec3.createVectorHelper(
              (entityBox.minX + entityBox.maxX) / 2.0D,
              (entityBox.minY + entityBox.maxY) / 2.0D,
              (entityBox.minZ + entityBox.maxZ) / 2.0D));
      Vec3 localExtent = this.getLocalHalfExtents(entityBox);
      double halfWidth = (double)this.width / 2.0D;
      double halfHeight = (double)this.height / 2.0D;

      if(Math.abs(localCenter.yCoord) >= halfHeight + localExtent.yCoord
              || Math.abs(localCenter.xCoord) >= halfWidth + localExtent.xCoord
              || Math.abs(localCenter.zCoord) >= halfWidth + localExtent.zCoord) {
         return null;
      }

      double pushX = halfWidth + localExtent.xCoord - Math.abs(localCenter.xCoord);
      double pushZ = halfWidth + localExtent.zCoord - Math.abs(localCenter.zCoord);
      Vec3 localPush = pushX < pushZ
              ? Vec3.createVectorHelper((localCenter.xCoord < 0.0D ? -pushX - epsilon : pushX + epsilon), 0.0D, 0.0D)
              : Vec3.createVectorHelper(0.0D, 0.0D, (localCenter.zCoord < 0.0D ? -pushZ - epsilon : pushZ + epsilon));
      return MCH_Lib.RotVec3(localPush, -this.lastYaw, -this.lastPitch, -this.lastRoll);
   }

   private Vec3 getLocalHalfExtents(AxisAlignedBB box) {
      double centerX = (box.minX + box.maxX) / 2.0D;
      double centerY = (box.minY + box.maxY) / 2.0D;
      double centerZ = (box.minZ + box.maxZ) / 2.0D;
      Vec3 center = this.toLocal(Vec3.createVectorHelper(centerX, centerY, centerZ));
      Vec3 x = this.toLocal(Vec3.createVectorHelper(box.maxX, centerY, centerZ));
      Vec3 y = this.toLocal(Vec3.createVectorHelper(centerX, box.maxY, centerZ));
      Vec3 z = this.toLocal(Vec3.createVectorHelper(centerX, centerY, box.maxZ));
      double halfX = Math.abs(x.xCoord - center.xCoord) + Math.abs(y.xCoord - center.xCoord) + Math.abs(z.xCoord - center.xCoord);
      double halfY = Math.abs(x.yCoord - center.yCoord) + Math.abs(y.yCoord - center.yCoord) + Math.abs(z.yCoord - center.yCoord);
      double halfZ = Math.abs(x.zCoord - center.zCoord) + Math.abs(y.zCoord - center.zCoord) + Math.abs(z.zCoord - center.zCoord);
      return Vec3.createVectorHelper(halfX, halfY, halfZ);
   }


   public MovingObjectPosition calculateIntercept(Vec3 start, Vec3 end) {
      Vec3 localStart = this.toLocal(start);
      Vec3 localEnd = this.toLocal(end);
      double dx = localEnd.xCoord - localStart.xCoord;
      double dy = localEnd.yCoord - localStart.yCoord;
      double dz = localEnd.zCoord - localStart.zCoord;
      double halfWidth = (double)this.width / 2.0D;
      double halfHeight = (double)this.height / 2.0D;
      double[] interval = new double[] {0.0D, 1.0D};

      if(!this.clipAxis(localStart.xCoord, dx, -halfWidth, halfWidth, interval)) return null;
      if(!this.clipAxis(localStart.yCoord, dy, -halfHeight, halfHeight, interval)) return null;
      if(!this.clipAxis(localStart.zCoord, dz, -halfWidth, halfWidth, interval)) return null;

      Vec3 hit = start.addVector((end.xCoord - start.xCoord) * interval[0], (end.yCoord - start.yCoord) * interval[0], (end.zCoord - start.zCoord) * interval[0]);
      return new MovingObjectPosition(0, 0, 0, 0, hit);
   }

   public Vec3 toLocal(Vec3 world) {
      Vec3 relative = Vec3.createVectorHelper(world.xCoord - this.nowPos.xCoord, world.yCoord - this.nowPos.yCoord, world.zCoord - this.nowPos.zCoord);
      relative.rotateAroundY(this.lastYaw / 180.0F * 3.1415927F);
      relative.rotateAroundX(this.lastPitch / 180.0F * 3.1415927F);
      W_Vec3.rotateAroundZ(this.lastRoll / 180.0F * 3.1415927F, relative);
      return relative;
   }

   private boolean clipAxis(double start, double delta, double min, double max, double[] interval) {
      if(Math.abs(delta) < 1.0E-7D) {
         return start >= min && start <= max;
      }

      double t1 = (min - start) / delta;
      double t2 = (max - start) / delta;
      if(t1 > t2) {
         double tmp = t1;
         t1 = t2;
         t2 = tmp;
      }

      if(t1 > interval[0]) interval[0] = t1;
      if(t2 < interval[1]) interval[1] = t2;
      return interval[0] <= interval[1];
   }
}
