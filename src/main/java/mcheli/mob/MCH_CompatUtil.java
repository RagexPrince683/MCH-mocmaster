package mcheli.mob;

import net.minecraft.entity.Entity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MCH_CompatUtil {

    private static final List<Class<?>> targetMachineClasses = new ArrayList<Class<?>>();

    static {
        String[] classNames = {
                "com.hbm.entity.missiles.EntityMissileBaseNT",
                "com.hbm.entity.missiles.EntityMissileCustom",
                "com.hbm.entity.vehicle.EntityRailCarBase",
                "com.hbm.entity.vehicle.EntityBomber",
                "net.minecraft.entity.item.EntityMinecart"
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
                return true;
            }
        }
        return false;
    }

    public static boolean isMissileFalling(Entity entity) {
        try {
            if (entity.getClass().getName().equals("com.hbm.entity.missiles.EntityMissileBaseNT") ||
                    entity.getClass().getName().equals("com.hbm.entity.missiles.EntityMissileCustom")) {
                return entity.motionY < 0;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static boolean isRadarDetectableAndVisible(Entity entity, Entity checker) {
        try {
            Class<?> radarDetectable = Class.forName("com.hbm.interfaces.IRadarDetectableNT");
            if (radarDetectable.isAssignableFrom(entity.getClass())) {
                Method canBeSeenBy = radarDetectable.getMethod("canBeSeenBy", Entity.class);
                return (boolean) canBeSeenBy.invoke(entity, checker);
            }
        } catch (Exception ignored) {}
        return true; // assume visible if check fails
    }
}
