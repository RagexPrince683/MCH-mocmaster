package mcheli.aircraft;

import java.util.List;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;

/**
 * Caches calculated vehicle collision / hit box positions.
 *
 * The current MCHeli collision path still consumes AxisAlignedBB-backed MCH_BoundingBox objects.
 * This cache is deliberately behavior-preserving: it updates those same boxes only when the
 * vehicle transform or source definitions change.  It is the abstraction boundary where future
 * rotating/oriented vehicle box calculations should be implemented.
 */
public class MCH_VehicleBoxCache {
   private static final double POSITION_EPSILON = 1.0E-4D;
   private static final float ROTATION_EPSILON = 1.0E-3F;
   private static final double ROUND_TRIP_EPSILON = 1.0E-5D;

   private boolean dirty = true;
   private String dirtyReason = "initial";
   private double posX;
   private double posY;
   private double posZ;
   private float yaw;
   private float pitch;
   private float roll;
   private MCH_BaseVehicleInfo acInfo;
   private List sourceBoxes;
   private MCH_BoundingBox[] boxes;
   private int sourceBoxCount = -1;
   private long lastUpdateTick = -1L;

   public void markDirty(String reason) {
      this.dirty = true;
      this.dirtyReason = reason != null ? reason : "unknown";
   }

   public MCH_BoundingBox[] getBoxes(MCH_EntityBaseVehicle vehicle) {
      if(vehicle == null) {
         return new MCH_BoundingBox[0];
      }

      MCH_BoundingBox[] currentBoxes = vehicle.extraBoundingBox != null ? vehicle.extraBoundingBox : new MCH_BoundingBox[0];
      MCH_BaseVehicleInfo currentInfo = vehicle.getAcInfo();
      List currentSourceBoxes = currentInfo != null ? currentInfo.extraBoundingBox : null;
      int currentSourceBoxCount = currentSourceBoxes != null ? currentSourceBoxes.size() : -1;
      double currentPosX = vehicle.posX;
      double currentPosY = vehicle.posY;
      double currentPosZ = vehicle.posZ;
      float currentYaw = vehicle.getRotYaw();
      float currentPitch = vehicle.getRotPitch();
      float currentRoll = vehicle.getRotRoll();

      String reason = this.getInvalidationReason(currentBoxes, currentInfo, currentSourceBoxes, currentSourceBoxCount,
              currentPosX, currentPosY, currentPosZ, currentYaw, currentPitch, currentRoll);
      if(reason != null) {
         this.markDirty(reason);
      }

      if(!this.dirty) {
         this.debug(vehicle, "hit", "reusing", currentBoxes.length);
         return currentBoxes;
      }

      for(int i = 0; i < currentBoxes.length; ++i) {
         currentBoxes[i].updatePosition(currentPosX, currentPosY, currentPosZ, currentYaw, currentPitch, currentRoll);
         this.validateTransformRoundTrip(vehicle, currentBoxes[i], i);
      }

      this.boxes = currentBoxes;
      this.acInfo = currentInfo;
      this.sourceBoxes = currentSourceBoxes;
      this.sourceBoxCount = currentSourceBoxCount;
      this.posX = currentPosX;
      this.posY = currentPosY;
      this.posZ = currentPosZ;
      this.yaw = currentYaw;
      this.pitch = currentPitch;
      this.roll = currentRoll;
      this.lastUpdateTick = vehicle.worldObj != null ? vehicle.worldObj.getTotalWorldTime() : -1L;
      reason = this.dirtyReason;
      this.dirty = false;
      this.dirtyReason = null;
      this.debug(vehicle, "rebuild", reason, currentBoxes.length);
      return currentBoxes;
   }

   private String getInvalidationReason(MCH_BoundingBox[] currentBoxes, MCH_BaseVehicleInfo currentInfo, List currentSourceBoxes,
                                        int currentSourceBoxCount, double currentPosX, double currentPosY, double currentPosZ,
                                        float currentYaw, float currentPitch, float currentRoll) {
      if(this.boxes != currentBoxes) return "box array changed";
      if(this.acInfo != currentInfo) return "vehicle config changed";
      if(this.sourceBoxes != currentSourceBoxes) return "source box list changed";
      if(this.sourceBoxCount != currentSourceBoxCount) return "source box count changed";
      if(Math.abs(this.posX - currentPosX) > POSITION_EPSILON || Math.abs(this.posY - currentPosY) > POSITION_EPSILON || Math.abs(this.posZ - currentPosZ) > POSITION_EPSILON) return "vehicle moved";
      if(Math.abs(this.yaw - currentYaw) > ROTATION_EPSILON || Math.abs(this.pitch - currentPitch) > ROTATION_EPSILON || Math.abs(this.roll - currentRoll) > ROTATION_EPSILON) return "vehicle rotated";
      return null;
   }

   private void validateTransformRoundTrip(MCH_EntityBaseVehicle vehicle, MCH_BoundingBox box, int index) {
      if(MCH_Config.DebugVehicleBoxCache != null && MCH_Config.DebugVehicleBoxCache.prmBool) {
         double error = box.getTransformRoundTripError();
         if(error > ROUND_TRIP_EPSILON) {
            String name = vehicle.getAcInfo() != null ? vehicle.getAcInfo().name : vehicle.getClass().getSimpleName();
            if(vehicle.worldObj != null) {
               MCH_Lib.DbgLog(vehicle.worldObj, "VehicleBoxCache %s transform round-trip error box=%d error=%.9f epsilon=%.9f", name, Integer.valueOf(index), Double.valueOf(error), Double.valueOf(ROUND_TRIP_EPSILON));
            } else {
               MCH_Lib.DbgLog(false, "VehicleBoxCache %s transform round-trip error box=%d error=%.9f epsilon=%.9f", name, Integer.valueOf(index), Double.valueOf(error), Double.valueOf(ROUND_TRIP_EPSILON));
            }
         }
      }
   }

   private void debug(MCH_EntityBaseVehicle vehicle, String action, String reason, int count) {
      if(MCH_Config.DebugVehicleBoxCache != null && MCH_Config.DebugVehicleBoxCache.prmBool) {
         String name = vehicle.getAcInfo() != null ? vehicle.getAcInfo().name : vehicle.getClass().getSimpleName();
         if(vehicle.worldObj != null) {
            MCH_Lib.DbgLog(vehicle.worldObj, "VehicleBoxCache %s %s reason=%s boxes=%d tick=%d", name, action, reason, Integer.valueOf(count), Long.valueOf(this.lastUpdateTick));
         } else {
            MCH_Lib.DbgLog(false, "VehicleBoxCache %s %s reason=%s boxes=%d tick=%d", name, action, reason, Integer.valueOf(count), Long.valueOf(this.lastUpdateTick));
         }
      }
   }
}
