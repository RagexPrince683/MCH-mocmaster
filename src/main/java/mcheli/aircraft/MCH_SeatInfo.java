package mcheli.aircraft;

import mcheli.aircraft.MCH_AircraftInfo;
import net.minecraft.util.Vec3;

public class MCH_SeatInfo
         {
      public final Vec3 pos;
      public final boolean gunner;
      private final MCH_AircraftInfo.CameraPosition camPos;
      public boolean invCamPos;
      public final boolean switchgunner;
      public final boolean fixRot;
      public final float fixYaw;
      public final float fixPitch;
      public final float minPitch;
      public final float maxPitch;
      public final boolean rotSeat;

      public MCH_SeatInfo(Vec3 p, boolean g, MCH_AircraftInfo.CameraPosition cp, boolean icp, boolean sg, boolean fr, float yaw, float pitch, float pmin, float pmax, boolean rotSeat) {
           this.camPos = cp;
           this.pos = p;
           this.gunner = g;
           this.invCamPos = icp;
           this.switchgunner = sg;
           this.fixRot = fr;
           this.fixYaw = yaw;
           this.fixPitch = pitch;
           this.minPitch = pmin;
           this.maxPitch = pmax;
           this.rotSeat = rotSeat;
         }

      public MCH_SeatInfo(Vec3 p, boolean g, MCH_AircraftInfo.CameraPosition cp, boolean icp, boolean sg, boolean fr, float yaw, float pitch, boolean rotSeat) {
           this(p, g, cp, icp, sg, fr, yaw, pitch, -30.0F, 70.0F, rotSeat);
         }

      public MCH_SeatInfo(Vec3 p, MCH_AircraftInfo.CameraPosition cp, float yaw, float pitch, boolean rotSeat) {
           this(p, false, cp, false, false, false, yaw, pitch, -30.0F, 70.0F, rotSeat);
         }

         public MCH_SeatInfo(Vec3 p, boolean rotSeat) {
          this(p, false, (MCH_AircraftInfo.CameraPosition)null, false, false, false, 0.0F, 0.0F, -30.0F, 70.0F, rotSeat);
      }

      public MCH_AircraftInfo.CameraPosition getCamPos() {
           return this.camPos;
         }
    }
