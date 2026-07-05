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
