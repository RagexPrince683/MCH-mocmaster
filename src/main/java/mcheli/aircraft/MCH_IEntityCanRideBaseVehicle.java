package mcheli.aircraft;

import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_SeatRackInfo;

public interface MCH_IEntityCanRideBaseVehicle {

   boolean isSkipNormalRender();

   boolean canRideAircraft(MCH_EntityBaseVehicle var1, int var2, MCH_SeatRackInfo var3);
}
