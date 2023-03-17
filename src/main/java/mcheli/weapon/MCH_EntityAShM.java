package mcheli.weapon;

import cuchaz.ships.EntityShip;
import mcheli.MCH_Lib;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class MCH_EntityAShM extends MCH_EntityBaseBullet {

	public int current_target = 0;
	public ArrayList<Vec3> targets = new ArrayList<Vec3>();
	private int cruise_alt = 65;
	private double switchAlt = 0;
	
   public MCH_EntityAShM(World par1World) {
      super(par1World);
   }

   public MCH_EntityAShM(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
   }
   
   public double getTargetX() {
		return targets.get(current_target).xCoord;
	}
	
	public double getTargetY() {
		return targets.get(current_target).yCoord;
	}
	
	public double getTargetZ() {
		return targets.get(current_target).zCoord;
	}
	
	public void setTarget(double x, double z, double x2, double z2) {
		//The first target will be to get us to cruise altitude
		targets.add(Vec3.createVectorHelper(x2, cruise_alt, z2));
		
		//The last target is our *actual* target which we want to strike
		targets.add(Vec3.createVectorHelper(x, cruise_alt, z));
		switchAlt = (posY - cruise_alt)*0.75+cruise_alt;
		//System.out.println("TGT 1 INFO. X " + x+  " Z " + z);
	}

	public void scanForTgt() {
		if(this.current_target==0) {return;}
		Entity target = getTarget();
		if(target == null) {return;}
		targets.set(1, Vec3.createVectorHelper(target.posX, target.posY,target.posZ));
	}
	
	public double getBearingToEntity(Entity e) {
		double delta_x = e.posX- this.posX;
		double delta_z = posZ - e.posZ;
		double angle = Math.atan2(delta_x, delta_z);
		angle = Math.toDegrees(angle);
		if(angle < 0) { angle += 360;}
		
		return angle;
	}
	
	public double getYaw() {
		double yaw = MCH_Lib.getRotate360(this.rotationYaw)-180;
		if(yaw < 0) { yaw += 360;}
		return yaw;
	}
	
	public Entity getTarget() {
		double range = 64;
		List<Entity> list = worldObj.getEntitiesWithinAABBExcludingEntity(this,this.boundingBox.expand(range, range, range));
		for(Entity e : list) {
			if(e instanceof EntityShip) {
				double result = Math.abs(getBearingToEntity(e) - getYaw());
				//System.out.println("Yeet: " + result);
				if(result <= 70) {
					return e;
				}
			}
		}
		return null;
	}
	
	@Override
	public void onImpact(MovingObjectPosition m, float damageFactor) {
		super.onImpact(m,  damageFactor);
		setDead();
	}
	
   public void onUpdate() {
		super.onUpdate();
		if(this.getInfo() != null && !this.getInfo().disableSmoke && this.getBomblet() == 0) {
			this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 10.0F * this.getInfo().smokeSize * 0.5F);
		}

		if(this.getInfo() != null && !super.worldObj.isRemote && super.isBomblet != 1) {
		//	Block a = W_WorldFunc.getBlock(super.worldObj, (int)this.getTargetX(), (int)this.getTargetY(), (int)this.getTargetZ());
			//if(a != null && a.isCollidable()) {
				double dist = this.getDistance(this.getTargetX(), this.getTargetY(), this.getTargetZ());
				if(dist < (double)this.getInfo().proximityFuseDist && this.current_target == 1) {
					if(this.getInfo().bomblet > 0) {
						for(int x = 0; x < this.getInfo().bomblet; ++x) {
							this.sprinkleBomblet();
						}
					} else {
						MovingObjectPosition var15 = new MovingObjectPosition(this);
						System.out.println("Impacting");
						this.onImpact(var15, 1.0F);
						this.setDead();
					}
					System.out.println("Esplodey");
					
					this.setDead();
				}
		}
		
		if(this.posY <= switchAlt && this.current_target < 1) {
			this.current_target ++;
		}
		
		scanForTgt();
		guidanceToTarget(getTargetX(), getTargetY(), getTargetZ());
		this.onUpdateBomblet();
	}

   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.ASMissile;
   }
}
