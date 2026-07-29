package mcheli;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;

/** Optional FMUR integration. Absence or API mismatch must never affect MCHeli. */
public class MCH_FMURUtil {
    private static Class<?> apiClass;
    private static Method bulletMethod;
    private static Method grenadeMethod;
    private static Method markerMethod;
    private static boolean lookupComplete;

    private static void lookup() {
        if (lookupComplete) return;
        lookupComplete = true;
        try {
            apiClass = Class.forName("com.flansmod.api.FMUR_API");
            bulletMethod = apiClass.getMethod("bulletDestructedByAPS", Entity.class, EntityLivingBase.class);
            grenadeMethod = apiClass.getMethod("grenadeDestructedByAPS", Entity.class, EntityLivingBase.class);
            markerMethod = apiClass.getMethod("sendAPSMarker", EntityPlayerMP.class);
        } catch (ClassNotFoundException e) {
            apiClass = null;
        } catch (NoSuchMethodException e) {
            apiClass = null;
        } catch (SecurityException e) {
            apiClass = null;
        }
    }

    public static boolean isAPSThreat(Entity entity) {
        lookup();
        return apiClass != null && entity != null
                && entity.getClass().getName().startsWith("com.flansmod.")
                && (entity.getClass().getSimpleName().indexOf("Bullet") >= 0
                    || entity.getClass().getSimpleName().indexOf("Grenade") >= 0);
    }

    public static boolean destroyAPSThreat(Entity entity, EntityLivingBase user) {
        if (!isAPSThreat(entity) || user == null) return false;
        Method method = entity.getClass().getSimpleName().indexOf("Grenade") >= 0 ? grenadeMethod : bulletMethod;
        try {
            return method != null && ((Boolean)method.invoke(null, entity, user)).booleanValue();
        } catch (ReflectiveOperationException e) {
            return false;
        } catch (ClassCastException e) {
            return false;
        }
    }

    /** Best-effort owner lookup for optional projectiles without linking their classes. */
    public static Entity getAPSOwner(Entity entity) {
        if (!isAPSThreat(entity)) return null;
        String[] names = {"shootingEntity", "owner", "thrower"};
        for (String name : names) {
            try {
                Field field = entity.getClass().getField(name);
                Object value = field.get(entity);
                if (value instanceof Entity) return (Entity)value;
            } catch (NoSuchFieldException e) {
                // Try the next known optional API field.
            } catch (IllegalAccessException e) {
                return null;
            } catch (SecurityException e) {
                return null;
            }
        }
        return null;
    }

    public static void sendAPSMarker(EntityPlayerMP player) {
        lookup();
        if (markerMethod == null || player == null) return;
        try {
            markerMethod.invoke(null, player);
        } catch (ReflectiveOperationException e) {
            // Optional visual feedback is allowed to fail silently once FMUR changes its API.
        }
    }
}
