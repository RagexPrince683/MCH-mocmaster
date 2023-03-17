package mcheli.weapon;

import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.flare.MCH_EntityFlare;
import mcheli.sensors.MCH_RadarContact;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class MCH_EntityAAMissile extends MCH_EntityBaseBullet {
	public String type; 

	public MCH_EntityAAMissile(World par1World) {
		super(par1World);
		super.targetEntity = null;
	}

	public MCH_EntityAAMissile(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
		super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
	}

	public void checkFlares() {
		double range = 50.0;
		List<Entity> list = worldObj.getEntitiesWithinAABBExcludingEntity(this,this.boundingBox.expand(range, range, range));
		for(Entity e : list) {
			if(e instanceof MCH_EntityFlare && Math.random() >= 0.999) {
				this.setTargetEntity(e);
				System.out.println("Have been decieved");
			}else if(e instanceof MCH_EntityFlare) {
				System.out.println("Not decieved");
			}
		}
	}

	public void onUpdate() {
		super.onUpdate();

		if(this.getCountOnUpdate() > 4 && this.getInfo() != null && !this.getInfo().disableSmoke) {
			this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 7.0F * this.getInfo().smokeSize * 0.5F);
		}

		if(!super.worldObj.isRemote && this.getInfo() != null) {
			if(super.shootingEntity != null && super.targetEntity != null && !super.targetEntity.isDead) {
				//if(this.getCountOnUpdate() > this.getInfo().rigidityTime) {
					checkProximityFuse();
					doGuidance();
				//}
			}
		}
	}

	public void doGuidance() {
	//	this.rotationYaw += 10;
		if(type == "ir") {
			checkFlares();
			guidanceToTarget(targetEntity);
			//this.guidanceToTarget(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ, this.getInfo().rigidityTime);
		}else if(type == "sarh") {
			MCH_RadarContact target = getRadarTarget();
			if(target != null) {
				//System.out.println("x " + target.x + " y " + target.y + " z " + target.z + " remote " + this.worldObj.isRemote);
				
				this.guidanceToTarget(target.x, target.y, target.z, this.getInfo().rigidityTime);
			}else {
				//System.out.println("Error " + this.worldObj.isRemote);
				//((MCH_EntityAircraft)this.shootingAircraft).print("Lost target, dying");
				this.setDead();
			}
		}
	}
	

	//Returns vec3 predicted position of entity target after t ticks
	Vec3 predictPos(Entity target, int t) {
		double x = target.posX + target.motionX * t;
		double y = target.posY + target.motionY * t;
		double z = target.posZ + target.motionZ * t;
		return Vec3.createVectorHelper(x, y, z);
	}
	
	double burnTime = 20;
	public void guidanceToTarget(Entity targetEntity) {
		double maxSpeed = 2.0;
		
		//if(burnTime > 0) {
			burnTime --;
			Vec3 orientation = MCH_Lib.Rot2Vec3(rotationYaw, rotationPitch).normalize();
			this.setVelocity(orientation.xCoord, orientation.yCoord, orientation.zCoord);
		//}
		
		for(int t = 0; t <= 200; t++) {
			Vec3 pos = predictPos(targetEntity, t);
			pos = Vec3.createVectorHelper(targetEntity.posX, targetEntity.posY,targetEntity.posZ);
			double dist = this.getDistance(pos.xCoord, pos.yCoord, pos.zCoord);

			if(Math.abs(dist - (maxSpeed * t)) <= 3) {
				//System.out.println("Intercept found at " + pos.xCoord + " " + pos.zCoord);
				guidanceToPos(pos);
				return;
			}
		}
	}
	
	public void guidanceToPos(Vec3 pos) {
		double aspect = getAspect(pos);
		double dAngle = 2.0;
		//this.rotationYaw += 10;
		//if(true) {return;}
		if(aspect > 0) {
			this.rotationYaw += Math.min(dAngle, aspect);
		}else if(aspect < 0) {
			this.rotationYaw -= Math.max(dAngle, -aspect);
		}
		
		double pitchAspect = getPitchAspect(pos);
		if(pitchAspect > 0) {
			this.rotationPitch -= Math.min(dAngle, pitchAspect);
		}else if(pitchAspect < 0) {
			this.rotationPitch += Math.max(dAngle, pitchAspect);
		}
		
		System.out.println("Aspect " + aspect + " pitchAspect " + pitchAspect);
	}

	
	 private double getPitchAspect(Vec3 target) {
			double dist = getDistance(target.xCoord, posY, target.zCoord);
			double dY = target.yCoord - posY;
			//a.print("Dist: " + dist + " dY " + dY);
			return Math.toDegrees(Math.atan(dY / dist)) + rotationPitch;
		}
	
	private double getAspect(Vec3 target) {
		return getBearingToEntity(target) - rotationYaw - 180;
	}
	
	public float getBearingToEntity(Vec3 target) {
		double delta_x = target.xCoord- this.posX;
		double delta_z = posZ - target.zCoord;
		double angle = Math.atan2(delta_x, delta_z);
		angle = Math.toDegrees(angle);
		if(angle < 0) { angle += 360;}
		return (float)angle;
	}
	
	public MCH_RadarContact getRadarTarget() {
		if(this.shootingAircraft instanceof MCH_EntityAircraft) {
			MCH_EntityAircraft ac = (MCH_EntityAircraft)shootingAircraft;
			if(ac.radarTarget != null) {
				return ac.radarTarget;
			}
		}
		return null;
	}


	public void checkProximityFuse() {
		double x = super.posX - super.targetEntity.posX;
		double y = super.posY - super.targetEntity.posY;
		double z = super.posZ - super.targetEntity.posZ;
		double d = x * x + y * y + z * z;
		if(this.getInfo().proximityFuseDist >= 0.1F && d < (double)this.getInfo().proximityFuseDist) {
			MovingObjectPosition mop = new MovingObjectPosition(super.targetEntity);
			super.posX = (super.targetEntity.posX + super.posX) / 2.0D;
			super.posY = (super.targetEntity.posY + super.posY) / 2.0D;
			super.posZ = (super.targetEntity.posZ + super.posZ) / 2.0D;
			this.onImpact(mop, 1.0F);
		}
	}

	public MCH_BulletModel getDefaultBulletModel() {
		return MCH_DefaultBulletModels.AAMissile;
	}
}
