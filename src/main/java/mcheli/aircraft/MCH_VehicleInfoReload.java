package mcheli.aircraft;

import mcheli.MCH_InfoManagerBase;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.ship.MCH_EntityShip;
import mcheli.ship.MCH_ShipInfoManager;
import mcheli.tank.MCH_EntityTank;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.vehicle.MCH_EntityTurret;
import mcheli.vehicle.MCH_TurretInfoManager;

/** Single routing point for targeted vehicle definition reloads. */
public final class MCH_VehicleInfoReload {
   private MCH_VehicleInfoReload() {}

   public static MCH_InfoManagerBase managerFor(MCH_EntityBaseVehicle vehicle) {
      if(vehicle instanceof MCH_EntityHeli) return MCH_HeliInfoManager.getInstance();
      if(vehicle instanceof MCP_EntityPlane) return MCP_PlaneInfoManager.getInstance();
      if(vehicle instanceof MCH_EntityShip) return MCH_ShipInfoManager.getInstance();
      if(vehicle instanceof MCH_EntityTank) return MCH_TankInfoManager.getInstance();
      if(vehicle instanceof MCH_EntityTurret) return MCH_TurretInfoManager.getInstance();
      return null;
   }

   public static MCH_BaseVehicleInfo publishedInfo(MCH_EntityBaseVehicle vehicle, String name) {
      MCH_InfoManagerBase manager = managerFor(vehicle);
      return manager == null ? null : (MCH_BaseVehicleInfo)manager.getMap().get(name);
   }
}
