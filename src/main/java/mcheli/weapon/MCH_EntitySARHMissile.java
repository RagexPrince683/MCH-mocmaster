package mcheli.weapon;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.sensors.MCH_RadarContact;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MCH_EntitySARHMissile extends MCH_EntityBaseBullet {

   public MCH_EntitySARHMissile(World par1World) {
      super(par1World);
      super.targetEntity = null;
   }

   public MCH_EntitySARHMissile(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
   }

   
   public void onUpdate() {
      super.onUpdate();
      
       
      if(this.getCountOnUpdate() > 4 && this.getInfo() != null && !this.getInfo().disableSmoke) {
    	  System.out.println("Spawning particle " + this.worldObj.isRemote);
          this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 7.0F * this.getInfo().smokeSize * 0.5F);
       }else {
    	   System.out.println("Count: " + this.getCountOnUpdate() + " info null: " + this.getInfo() == null);
       }
   
      if(!super.worldObj.isRemote && this.getInfo() != null) {
    	  MCH_RadarContact target = getRadarTarget();	
         if(super.shootingEntity != null && target != null) {
            double x = super.posX - target.x;
            double y = super.posY - target.y;
            double z = super.posZ - target.z;
            double d = x * x + y * y + z * z;
            if(d < 0) {//> 3422500.0D) {
            	System.out.println("Dying");
              // this.setDead();
            } else if(this.getCountOnUpdate() > this.getInfo().rigidityTime) {
               if(this.getInfo().proximityFuseDist >= 0.1F && d < (double)this.getInfo().proximityFuseDist) {
                  MovingObjectPosition mop = new MovingObjectPosition(super.targetEntity);
                  super.posX = (super.targetEntity.posX + super.posX) / 2.0D;
                  super.posY = (super.targetEntity.posY + super.posY) / 2.0D;
                  super.posZ = (super.targetEntity.posZ + super.posZ) / 2.0D;
                  this.onImpact(mop, 1.0F);
               } else {
                  this.guidanceToTarget(target.x, target.y, target.z);
               }
            }
         } else {
        	 System.out.println("Dying 2");
            //this.setDead();
         }
      }

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

   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.AAMissile;
   }
}
