package mcheli.aircraft;

import mcheli.MCH_PacketNotifyLock;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import java.util.List;

public class MCH_MissileDetector {

    private static Class<?> hmgBulletClass = null;
    private static java.lang.reflect.Field hasVTField = null;
    private static boolean reflectionInitialized = false;

    private static void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;

        try {
            hmgBulletClass = Class.forName("handmadeguns.entity.bullets.HMGEntityBulletBase");
            hasVTField = hmgBulletClass.getDeclaredField("hasVT");
            hasVTField.setAccessible(true);
        } catch (Exception e) {
            hmgBulletClass = null; // HMG not installed
        }
    }

    public static final int SEARCH_RANGE = 60;
    private MCH_EntityAircraft ac;
    private World world;
    private int alertCount;


    public MCH_MissileDetector(MCH_EntityAircraft aircraft, World w) {
        this.world = w;
        this.ac = aircraft;
        this.alertCount = 0;
    }

    public void update() {
        if (this.ac.haveFlare()) {
            if (this.alertCount > 0) {
                --this.alertCount;
            }

            boolean isLocked = this.ac.getEntityData().getBoolean("Tracking");
            if (isLocked) {
                this.ac.getEntityData().setBoolean("Tracking", false);
            }

            if (this.ac.getEntityData().getBoolean("LockOn")) {
                if (this.alertCount == 0) {
                    this.alertCount = 10;
                    if (this.ac != null && this.ac.haveFlare() && !this.ac.isDestroyed()) {
                        for (int rider = 0; rider < 2; ++rider) {
                            Entity entity = this.ac.getEntityBySeatId(rider);
                            if (entity instanceof EntityPlayerMP) {
                                MCH_PacketNotifyLock.sendToPlayer((EntityPlayerMP) entity);
                            }
                        }
                    }
                }

                this.ac.getEntityData().setBoolean("LockOn", false);
            }

            if (!this.ac.isDestroyed()) {
                Entity var4 = this.ac.getRiddenByEntity();
                if (var4 == null) {
                    var4 = this.ac.getEntityBySeatId(1);
                }

                if (var4 != null) {
                    if (this.ac.isFlareUsing()) {

                        this.destroyMissile();
                    } else if (!this.ac.isUAV() && !this.world.isRemote) {

                        //if (this.hasalert())

                            if (this.alertCount == 0 && ((isLocked || this.isLockedByMissile() || this.isLockedByHMGVT())) && this.hasalert()) {
                                this.alertCount = 20;
                                W_WorldFunc.MOD_playSoundAtEntity(this.ac, "alert", 50.0F, 1.0F);
                            }
                    } else if (this.ac.isUAV() && this.world.isRemote && this.alertCount == 0 && ((isLocked || this.isLockedByMissile() || this.isLockedByHMGVT())) && this.hasalert()) {
                        this.alertCount = 20;
                        if (W_Lib.isClientPlayer(var4)) {
                            W_McClient.MOD_playSoundFX("alert", 50.0F, 1.0F);
                        }
                    }

                }



            }
        }
    }

    public boolean isLockedByHMGVT() {

        initReflection();

        if (hmgBulletClass == null)
            return false; // HMG not installed

        List list = this.world.getEntitiesWithinAABB(
                hmgBulletClass,
                this.ac.boundingBox.expand(400.0D, 400.0D, 400.0D)
        );

        if (list == null || list.isEmpty())
            return false;

        for (Object obj : list) {

            System.out.println("Checking entity: " + obj.getClass().getName() + " (ID: " + ((Entity) obj).getEntityId() + ")");

            if (obj == null)
                continue;

            try {
                Boolean hasVT = (Boolean) hasVTField.get(obj);
                if (hasVT == null || !hasVT)
                    continue;

                Entity bullet = (Entity) obj;

                if (bullet.isDead)
                    continue;

                // Optional: check if moving toward aircraft
                double dx = this.ac.posX - bullet.posX;
                double dy = (this.ac.posY + this.ac.height * 0.5) - bullet.posY;
                double dz = this.ac.posZ - bullet.posZ;

                double dot =
                        (bullet.motionX * dx) +
                                (bullet.motionY * dy) +
                                (bullet.motionZ * dz);

                if (dot > 0) {
                    return true;
                }

            } catch (Exception ignored) {}
        }

        return false;
    }

    public void destroyMissile() {
        List list = this.world.getEntitiesWithinAABB(MCH_EntityBaseBullet.class, this.ac.boundingBox.expand(300.0D, 300.0D, 300.0D));
        if (list == null) {
            return;
        }
        for (Object o : list) {
            MCH_EntityBaseBullet msl = (MCH_EntityBaseBullet) o;
            if (msl.targetEntity != null && (this.ac.isMountedEntity(msl.targetEntity) || msl.targetEntity.equals(this.ac))) {
                if (msl.getInfo().isHeatSeekerMissile) {
                    if (msl.getInfo().antiFlareCount > 0) {
                        if (msl.antiFlareTick > msl.getInfo().antiFlareCount) {
                            msl.targetEntity = null;
                            msl.antiFlareTick = 0;
                        } else {
                            msl.antiFlareTick++;
                        }
                    } else {
                        msl.targetEntity = null;
                    }
                    //msl.setDead();
                } else {


                }
            }
        }

    }

    public boolean isLockedByMissile() {
        List list = this.world.getEntitiesWithinAABB(MCH_EntityBaseBullet.class, this.ac.boundingBox.expand(300.0D, 300.0D, 300.0D));
        if (list != null) {
            for (int i = 0; i < list.size(); ++i) {
                MCH_EntityBaseBullet msl = (MCH_EntityBaseBullet) list.get(i);
                if (msl.targetEntity != null && (this.ac.isMountedEntity(msl.targetEntity) || msl.targetEntity.equals(this.ac))) {
                    return true;
                }
            }
        }

        return false;
    }


    public boolean hasalert() {
        if (this.ac.hasalert()) {
            return true;
        }

        return false;
    }

}
