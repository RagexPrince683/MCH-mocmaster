package mcheli.weapon;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public abstract class MCH_GuidanceSystem {

	protected World worldObj;
	   public Object lastLockEntity;
	   public Object targetEntity;
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

	protected abstract void update();

	protected abstract Entity getLastLockEntity();
}
