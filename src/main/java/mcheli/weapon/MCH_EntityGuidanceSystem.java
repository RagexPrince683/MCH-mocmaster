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
     * 是否为红外弹，会受到热焰弹干扰
     */
    public boolean isHeatSeekerMissile = true;

    /**
     * 是否为雷达弹，会受到箔条干扰
     */
    public boolean isRadarMissile = false;

    /**
     * 半主动雷达弹需要持续引导
     */
    public boolean passiveRadar = false;

    /**
     * 半主动雷达弹脱离引导后脱锁计时
     */
    public int passiveRadarLockOutCount = 20;
    /**
     * 速度门雷达最大角度，超过此角度将脱锁 (也可用于红外弹尾后攻击)
     */
    public float pdHDNMaxDegree = 1000f;
    /**
     * 速度门雷达脱锁间隔，超过最大角度后，在该tick后导弹脱锁
     */
    public int pdHDNMaxDegreeLockOutCount = 10;
    /**
     * 导弹抗干扰时长，-1为不抗干扰
     */
    public int antiFlareCount = -1;

    /**
     * 雷达弹多径杂波检测高度，飞机低于这个高度将使雷达弹脱锁
     */
    public int lockMinHeight = 12;
    /**
     * 是否可以锁定导弹实体
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
