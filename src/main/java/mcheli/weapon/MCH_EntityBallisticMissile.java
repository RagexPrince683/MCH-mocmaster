package mcheli.weapon;

import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MCH_EntityBallisticMissile extends MCH_EntityBaseBullet {
	//public double targetPosX;
	//public double targetPosY;
	//public double targetPosZ;
	public int current_target = 0;
	public double[][] targets = new double[4][3];
	
	public MCH_EntityBallisticMissile(World par1World) {
		super(par1World);
	}
	
	public void setTarget(double x, double y, double z) {
		//The last target is our *actual* target which we want to strike
		targets[3][0] = x;
		targets[3][1] = y;
		targets[3][2] = z;
		//The second target should be directly above it so the missile comes in vertically
		targets[2][0] = x;
		targets[2][1] = 255;
		targets[2][2] = z;
		//The third target is about midway between the initial and target positions
		targets[1][0] = (this.posX + x) / 2;
		targets[1][1] = 500;
		targets[1][2] = (this.posZ + z) / 2;
		//The first target is directly above our current position
		targets[0][0] = this.posX;
		targets[0][1] = 255;
		targets[0][2] = this.posZ;
		
	}
	
	public double getTargetX() {
		return targets[current_target][0];
	}
	
	public double getTargetY() {
		return targets[current_target][1];
	}
	
	public double getTargetZ() {
		return targets[current_target][2];
	}

	public float getGravity() {
		return this.getBomblet() == 1?-0.03F:super.getGravity();
	}

	public float getGravityInWater() {
		return this.getBomblet() == 1?-0.03F:super.getGravityInWater();
	}

	public void onUpdate() {
		super.onUpdate();
		if(this.getInfo() != null && !this.getInfo().disableSmoke && this.getBomblet() == 0) {
			this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 10.0F * this.getInfo().smokeSize * 0.5F);
		}

		if(this.getInfo() != null && !super.worldObj.isRemote && super.isBomblet != 1) {
			Block a = W_WorldFunc.getBlock(super.worldObj, (int)this.getTargetX(), (int)this.getTargetY(), (int)this.getTargetZ());
			if(a != null && a.isCollidable()) {
				double dist = this.getDistance(this.getTargetX(), this.getTargetY(), this.getTargetZ());
				if(dist < (double)this.getInfo().proximityFuseDist && this.current_target == 3) {
					if(this.getInfo().bomblet > 0) {
						for(int x = 0; x < this.getInfo().bomblet; ++x) {
							this.sprinkleBomblet();
						}
					} else {
						MovingObjectPosition var15 = new MovingObjectPosition(this);
						this.onImpact(var15, 1.0F);
					}

					this.setDead();
				} else {
					System.out.println("Guiding to tgt");
					guidanceToTarget(getTargetX(), getTargetY(), getTargetZ());
				}
				if(dist <= 10 && this.current_target < 3) {
					this.current_target ++;
				}
			}
		}

		double var14 = (double)((float)Math.atan2(super.motionZ, super.motionX));
		super.rotationYaw = (float)(var14 * 180.0D / 3.141592653589793D) - 90.0F;
		double r = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
		super.rotationPitch = -((float)(Math.atan2(super.motionY, r) * 180.0D / 3.141592653589793D));
		this.onUpdateBomblet();
	}

	public void sprinkleBomblet() {
		if(!super.worldObj.isRemote) {
			MCH_EntityASMissile e = new MCH_EntityASMissile(super.worldObj, super.posX, super.posY, super.posZ, super.motionX, super.motionY, super.motionZ, (float)super.rand.nextInt(360), 0.0F, super.acceleration);
			e.setParameterFromWeapon(this, super.shootingAircraft, super.shootingEntity);
			e.setName(this.getName());
			float MOTION = 0.5F;
			float RANDOM = this.getInfo().bombletDiff;
			e.motionX = super.motionX * 0.5D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM);
			e.motionY = super.motionY * 0.5D / 2.0D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM / 2.0F);
			e.motionZ = super.motionZ * 0.5D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM);
			e.setBomblet();
			super.worldObj.spawnEntityInWorld(e);
		}

	}

	public MCH_EntityBallisticMissile(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
		super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
	}

	public MCH_BulletModel getDefaultBulletModel() {
		return MCH_DefaultBulletModels.ASMissile;
	}
}
