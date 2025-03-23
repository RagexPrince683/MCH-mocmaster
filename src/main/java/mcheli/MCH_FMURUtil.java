package mcheli;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;

import java.lang.reflect.Method;

public class MCH_FMURUtil {

    private static Class<?> FMUR_APIClass;

    static {
        try {
            FMUR_APIClass = Class.forName("com.flansmod.api.FMUR_API");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static boolean bulletDestructedByAPS(Entity entity, EntityLivingBase user) {
        try {
            if (FMUR_APIClass != null) {
                Method method = FMUR_APIClass.getMethod("bulletDestructedByAPS", Entity.class, EntityLivingBase.class);
                return (boolean) method.invoke(null, entity, user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean grenadeDestructedByAPS(Entity entity, EntityLivingBase user) {
        try {
            if (FMUR_APIClass != null) {
                Method method = FMUR_APIClass.getMethod("grenadeDestructedByAPS", Entity.class, EntityLivingBase.class);
                return (boolean) method.invoke(null, entity, user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void sendAPSMarker(EntityPlayerMP playerMP) {
        try {
            if (FMUR_APIClass != null) {
                Method method = FMUR_APIClass.getMethod("sendAPSMarker", EntityPlayerMP.class);
                method.invoke(null, playerMP);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
