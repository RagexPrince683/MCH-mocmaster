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
