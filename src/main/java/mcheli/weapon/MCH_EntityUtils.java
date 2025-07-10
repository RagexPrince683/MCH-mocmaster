package mcheli.weapon;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntityHitBox;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.mob.MCH_EntityGunner;
import mcheli.plane.MCP_EntityPlane;
import mcheli.ship.MCH_EntityShip;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vehicle.MCH_EntityVehicle;
import net.minecraft.entity.Entity;

public class MCH_EntityUtils {

    public static boolean isMCHVehicleRelated(Entity entity) {
        return entity instanceof MCH_EntityAircraft ||
                entity instanceof MCH_EntitySeat ||
                entity instanceof MCH_EntityVehicle ||
                entity instanceof MCH_EntityShip ||
                entity instanceof MCH_EntityHitBox ||
                entity instanceof MCH_EntityBaseBullet ||
                entity instanceof MCH_EntityGunner ||
                entity instanceof MCH_DummyEntityPlayer ||
                entity instanceof MCH_EntityHeli ||
                entity instanceof MCP_EntityPlane ||
                entity instanceof MCH_EntityUavStation;
    }

    public static boolean isRiding(Entity entity) {
        return entity.ridingEntity != null ; //&& isMCHVehicleRelated(entity.ridingEntity)
        //no more bullet motion resets for riding things (even vanilla)
    }

    public static void resetMotionIfNotMCHVehicle(Entity entity) {
        if (!isMCHVehicleRelated(entity) && !isRiding(entity)) {
            System.out.println("reset entity motion" + entity.getClass().getSimpleName() + " " + entity.getEntityId());
            entity.motionX = 0.0D;
            entity.motionY = 0.0D;
            entity.motionZ = 0.0D;
        }
    }
}
