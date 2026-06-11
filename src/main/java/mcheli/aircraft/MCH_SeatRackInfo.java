package mcheli.aircraft;

import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.aircraft.MCH_SeatInfo;
import net.minecraft.util.Vec3;

public class MCH_SeatRackInfo extends MCH_SeatInfo {

   public final float range;
   public final float openParaAlt;
   public final String[] names;
   public final boolean launchRack;


   public MCH_SeatRackInfo(String[] entityNames, double x, double y, double z, MCH_BaseVehicleInfo.CameraPosition ep, float rng, float paraAlt, float yaw, float pitch, boolean rotSeat) {
      this(entityNames, x, y, z, ep, rng, paraAlt, yaw, pitch, rotSeat, false);
   }

   public MCH_SeatRackInfo(String[] entityNames, double x, double y, double z, MCH_BaseVehicleInfo.CameraPosition ep, float rng, float paraAlt, float yaw, float pitch, boolean rotSeat, boolean launchRack) {
      super(Vec3.createVectorHelper(x, y, z), ep, yaw, pitch, rotSeat);
      this.range = rng;
      this.openParaAlt = paraAlt;
      this.names = entityNames;
      this.launchRack = launchRack;
   }

   public Vec3 getEntryPos() {
      return this.getCamPos().pos;
   }
}
