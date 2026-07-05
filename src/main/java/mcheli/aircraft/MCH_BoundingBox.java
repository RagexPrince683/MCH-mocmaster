package mcheli.aircraft;

import mcheli.MCH_Lib;
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
   private final MCH_ShipOBB shipOBB;


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
      this.shipOBB = new MCH_ShipOBB(w, h);
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
      this.shipOBB.update(this.nowPos, yaw, pitch, roll);
   }

   public MCH_ShipOBB getShipOBB() {
      return this.shipOBB;
   }

   public Vec3 getWorldTopCenter() {
      return this.shipOBB.getWorldTopCenter();
   }

   public boolean isEntityOnTop(AxisAlignedBB entityBox, double horizontalInset, double belowTolerance, double aboveTolerance) {
      return this.shipOBB.isEntityOnTop(entityBox, horizontalInset, belowTolerance, aboveTolerance);
   }

   public double getTopSurfaceY() {
      return this.shipOBB.getTopSurfaceY();
   }

   public double getPreviousTopSurfaceY() {
      return this.shipOBB.getPreviousTopSurfaceY();
   }

   /**
    * Returns the AABB that encloses the ship OBB for Minecraft entity discovery/search only.
    * Ship physical collision, ray hits, and deck support should use getShipOBB() / OBB helpers instead.
    */
   public AxisAlignedBB getEnclosingAABB() {
      return this.shipOBB.getEnclosingAABB();
   }

   public double calculateDeckYOffset(AxisAlignedBB entityBox, double offset, double supportTolerance) {
      return this.shipOBB.calculateDeckYOffset(entityBox, offset, supportTolerance);
   }

   public double calculatePreviousDeckYOffset(AxisAlignedBB entityBox, double offset, double supportTolerance) {
      return this.shipOBB.calculatePreviousDeckYOffset(entityBox, offset, supportTolerance);
   }

   public boolean intersectsAABB(AxisAlignedBB aabb) {
      return this.shipOBB.intersectsAABB(aabb);
   }

   public double calculateXOffset(AxisAlignedBB entityBox, double offset) {
      return this.shipOBB.calculateXOffset(entityBox, offset);
   }

   public double calculateZOffset(AxisAlignedBB entityBox, double offset) {
      return this.shipOBB.calculateZOffset(entityBox, offset);
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
      return this.shipOBB.getHorizontalPushOut(entityBox, epsilon);
   }

   public MovingObjectPosition calculateIntercept(Vec3 start, Vec3 end) {
      return this.shipOBB.calculateIntercept(start, end);
   }

   public Vec3 toLocal(Vec3 world) {
      return this.shipOBB.toLocal(world);
   }
}
