package mcheli.aircraft;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_SeatRackInfo;

public interface MCH_IEntityCanRideAircraft {

   boolean isSkipNormalRender();

   boolean canRideAircraft(MCH_EntityAircraft var1, int var2, MCH_SeatRackInfo var3);
}
