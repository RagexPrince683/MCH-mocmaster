package mcheli.mob;

import net.minecraft.entity.Entity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MCH_CompatUtil {

    private static final List<Class<?>> targetMachineClasses = new ArrayList<Class<?>>();

    /**
     * if(targetMachines) {
     *
     * 			if(e instanceof IRadarDetectableNT && !((IRadarDetectableNT)e).canBeSeenBy(this)) return false;
     * 			if(e instanceof EntityMissileBaseNT) return e.motionY < 0;
     * 			if(e instanceof EntityMissileCustom) return e.motionY < 0;
     * 			if(e instanceof EntityMinecart) return true;
     * 			if(e instanceof EntityRailCarBase) return true;
     * 			if(e instanceof EntityBomber) return true;
     * 			for(Class c : CompatExternal.turretTargetMachine) if(c.isAssignableFrom(e.getClass())) return true;
     *                }
     */

    static {
        String[] classNames = {
                "com.hbm.entity.missile.EntityMissileBaseNT",
                "com.hbm.entity.missile.EntityMissileCustom",
                //"com.hbm.entity.vehicle.EntityRailCarBase",
                "com.hbm.entity.logic.EntityBomber"
                //"net.minecraft.entity.item.EntityMinecart"
        };

        for (String name : classNames) {
            try {
                Class<?> clazz = Class.forName(name);
                targetMachineClasses.add(clazz);
            } catch (ClassNotFoundException ignored) {}
        }
    }

    public static boolean isTargetMachine(Entity entity) {
        for (Class<?> clazz : targetMachineClasses) {
            if (clazz.isAssignableFrom(entity.getClass())) {
                System.out.println("MCH_CompatUtil: Detected target machine entity: " + entity.getClass().getName());
                return true;
            }
        }
        return false;
    }

    public static boolean isMissileFalling(Entity entity) {
        try {
            if (entity.getClass().getName().equals("com.hbm.entity.missile.EntityMissileBaseNT") ||
                    entity.getClass().getName().equals("com.hbm.entity.missile.EntityMissileCustom")) {
                return entity.motionY < 0;
            }
        } catch (Exception ignored) {
            System.out.println("MCH_CompatUtil: Error checking if entity is missile falling: " + ignored.getMessage());
        }
        return false;
    }

    public static boolean isRadarDetectableAndVisible(Entity entity, Entity checker) {
        try {
            Class<?> radarDetectable = Class.forName("api.hbm.entity.IRadarDetectableNT");
            if (radarDetectable.isAssignableFrom(entity.getClass())) {
                Method canBeSeenBy = radarDetectable.getMethod("canBeSeenBy", Entity.class);
                return (boolean) canBeSeenBy.invoke(entity, checker);
            }
        } catch (Exception ignored) {
            System.out.println("MCH_CompatUtil: Error checking radar detectability or visibility: " + ignored.getMessage());
        }
        return true; // assume visible if check fails
    }
}
