package mcheli.weapon;

import net.minecraft.entity.Entity;

public abstract class MCH_EntityGuidanceSystem implements MCH_IGuidanceSystem {

    public int lockCount;
    public int lockSoundCount;
    public int continueLockCount;
    public int lockCountMax;
    public int prevLockCount;
    public boolean canLockInWater;
    public boolean canLockOnGround;
    public boolean canLockInAir;
    public boolean ridableOnly;
    public double lockRange;
    public int lockAngle;
    public MCH_IEntityLockChecker checker;

    /**
     * Whether this is an IR missile and can be distracted by flares
     */
    public boolean isHeatSeekerMissile = true;

    /**
     * Whether this is a radar missile and can be distracted by chaff
     */
    public boolean isRadarMissile = false;

    /**
     * Semi-active radar missiles need continuous guidance
     */
    public boolean passiveRadar = false;

    /**
     * Unlock timer after semi-active radar missile loses guidance
     */
    public int passiveRadarLockOutCount = 20;
    /**
     * Velocity-gate radar maximum angle; exceeding this angle breaks lock (can also be used for rear-aspect IR attacks)
     */
    public float pdHDNMaxDegree = 1000f;
    /**
     * Velocity-gate radar unlock interval; after exceeding max angle, missile unlocks after this tick count
     */
    public int pdHDNMaxDegreeLockOutCount = 10;
    /**
     * Missile countermeasure resistance duration; -1 means no resistance
     */
    public int antiFlareCount = -1;

    /**
     * Radar missile multipath clutter detection height; aircraft below this height makes radar missiles lose lock
     */
    public int lockMinHeight = 12;
    /**
     * Whether missile entities can be locked
     */
    public boolean canLockMissile = false;


    public boolean canLockEntity(Entity entity) {
        return false;
    }

    public boolean isLockingEntity(Entity entity) {
        return false;
    }

    protected abstract void setLockCountMax(int i);

    protected abstract boolean lock(Entity user);

    protected abstract int getLockCount();

    protected abstract int getLockCountMax();

    protected abstract Entity getLastLockEntity();
}
