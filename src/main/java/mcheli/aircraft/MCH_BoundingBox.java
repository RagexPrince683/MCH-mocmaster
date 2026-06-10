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


   private boolean rotatingBoundsDisabled() {
      return mcheli.MCH_Config.EnableRotatingVehicleBounds == null || !mcheli.MCH_Config.EnableRotatingVehicleBounds.prmBool;
   }

   public AxisAlignedBB getLocalBox() {
      double hw = (double)this.width / 2.0D;
      double hh = (double)this.height / 2.0D;
      return AxisAlignedBB.getBoundingBox(this.offsetX - hw, this.offsetY - hh, this.offsetZ - hw, this.offsetX + hw, this.offsetY + hh, this.offsetZ + hw);
   }

   public Vec3 toWorld(double localX, double localY, double localZ, double posX, double posY, double posZ, float yaw, float pitch, float roll) {
      Vec3 v = MCH_Lib.RotVec3(localX, localY, localZ, -yaw, -pitch, -roll);
      return Vec3.createVectorHelper(posX + v.xCoord, posY + v.yCoord, posZ + v.zCoord);
   }

   public Vec3 toLocal(Vec3 world, double posX, double posY, double posZ, float yaw, float pitch, float roll) {
      double x = world.xCoord - posX;
      double y = world.yCoord - posY;
      double z = world.zCoord - posZ;
      Vec3 v = Vec3.createVectorHelper(x, y, z);
      v.rotateAroundY(yaw / 180.0F * 3.1415927F);
      v.rotateAroundX(pitch / 180.0F * 3.1415927F);
      mcheli.wrapper.W_Vec3.rotateAroundZ(roll / 180.0F * 3.1415927F, v);
      return v;
   }

   public AxisAlignedBB createRotatedEnclosingAABB(double posX, double posY, double posZ, float yaw, float pitch, float roll) {
      AxisAlignedBB local = this.getLocalBox();
      double minX = Double.MAX_VALUE;
      double minY = Double.MAX_VALUE;
      double minZ = Double.MAX_VALUE;
      double maxX = -Double.MAX_VALUE;
      double maxY = -Double.MAX_VALUE;
      double maxZ = -Double.MAX_VALUE;

      for(int ix = 0; ix < 2; ++ix) {
         double x = ix == 0?local.minX:local.maxX;
         for(int iy = 0; iy < 2; ++iy) {
            double y = iy == 0?local.minY:local.maxY;
            for(int iz = 0; iz < 2; ++iz) {
               double z = iz == 0?local.minZ:local.maxZ;
               Vec3 w = this.toWorld(x, y, z, posX, posY, posZ, yaw, pitch, roll);
               minX = Math.min(minX, w.xCoord);
               minY = Math.min(minY, w.yCoord);
               minZ = Math.min(minZ, w.zCoord);
               maxX = Math.max(maxX, w.xCoord);
               maxY = Math.max(maxY, w.yCoord);
               maxZ = Math.max(maxZ, w.zCoord);
            }
         }
      }

      // Minecraft 1.7.10 stores and queries only AxisAlignedBB values.  The
      // actual vehicle box is oriented, so this AABB is only the safe broad-phase
      // envelope used by the engine; precise tests transform probes back into
      // vehicle-local space and compare against the unrotated local box.
      return AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
   }

   public boolean intersectsRotated(AxisAlignedBB aabb, double posX, double posY, double posZ, float yaw, float pitch, float roll) {
      if(this.rotatingBoundsDisabled()) {
         return this.boundingBox.intersectsWith(aabb);
      }

      AxisAlignedBB local = this.getLocalBox();
      double minX = Double.MAX_VALUE;
      double minY = Double.MAX_VALUE;
      double minZ = Double.MAX_VALUE;
      double maxX = -Double.MAX_VALUE;
      double maxY = -Double.MAX_VALUE;
      double maxZ = -Double.MAX_VALUE;

      for(int ix = 0; ix < 2; ++ix) {
         double x = ix == 0?aabb.minX:aabb.maxX;
         for(int iy = 0; iy < 2; ++iy) {
            double y = iy == 0?aabb.minY:aabb.maxY;
            for(int iz = 0; iz < 2; ++iz) {
               double z = iz == 0?aabb.minZ:aabb.maxZ;
               Vec3 localPoint = this.toLocal(Vec3.createVectorHelper(x, y, z), posX, posY, posZ, yaw, pitch, roll);
               minX = Math.min(minX, localPoint.xCoord);
               minY = Math.min(minY, localPoint.yCoord);
               minZ = Math.min(minZ, localPoint.zCoord);
               maxX = Math.max(maxX, localPoint.xCoord);
               maxY = Math.max(maxY, localPoint.yCoord);
               maxZ = Math.max(maxZ, localPoint.zCoord);
            }
         }
      }

      return maxX > local.minX && minX < local.maxX && maxY > local.minY && minY < local.maxY && maxZ > local.minZ && minZ < local.maxZ;
   }

   public MovingObjectPosition calculateRotatedIntercept(Vec3 start, Vec3 end, double posX, double posY, double posZ, float yaw, float pitch, float roll) {
      if(this.rotatingBoundsDisabled()) {
         return this.boundingBox.calculateIntercept(start, end);
      }

      Vec3 localStart = this.toLocal(start, posX, posY, posZ, yaw, pitch, roll);
      Vec3 localEnd = this.toLocal(end, posX, posY, posZ, yaw, pitch, roll);
      MovingObjectPosition localHit = this.getLocalBox().calculateIntercept(localStart, localEnd);
      if(localHit == null) {
         return null;
      }

      Vec3 worldHit = this.toWorld(localHit.hitVec.xCoord, localHit.hitVec.yCoord, localHit.hitVec.zCoord, posX, posY, posZ, yaw, pitch, roll);
      return new MovingObjectPosition(0, 0, 0, localHit.sideHit, worldHit);
   }


   public MCH_BoundingBox copy() {
      return new MCH_BoundingBox(this.offsetX, this.offsetY, this.offsetZ, this.width, this.height, this.damegeFactor);
   }

   public wheelBoundingBox copy2() {
      return new wheelBoundingBox(this.offsetX, this.offsetY, this.offsetZ, this.width, this.height, this.damegeFactor);
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
      if(this.rotatingBoundsDisabled()) {
         this.boundingBox.setBounds(x - (double)(w / 2.0F), y - (double)(h / 2.0F), z - (double)(w / 2.0F), x + (double)(w / 2.0F), y + (double)(h / 2.0F), z + (double)(w / 2.0F));
      } else {
         try {
            this.boundingBox.setBB(this.createRotatedEnclosingAABB(posX, posY, posZ, yaw, pitch, roll));
         } catch(Throwable t) {
            // Safety fallback: any math/config problem restores the legacy
            // static world-axis sub-box instead of risking client/server desync.
            this.boundingBox.setBounds(x - (double)(w / 2.0F), y - (double)(h / 2.0F), z - (double)(w / 2.0F), x + (double)(w / 2.0F), y + (double)(h / 2.0F), z + (double)(w / 2.0F));
         }
      }
   }
}
