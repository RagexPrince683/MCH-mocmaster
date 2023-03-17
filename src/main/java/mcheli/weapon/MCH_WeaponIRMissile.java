package mcheli.weapon;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class MCH_WeaponIRMissile extends MCH_WeaponEntitySeeker {
//MCH_WeaponGuidanceSystem guidanceSystem;
public Entity target = null;
public MCH_WeaponIRMissile(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.power = 12;
      super.acceleration = 2.5F;
      super.explosionPower = 4;
      super.interval = 5;
      if(w.isRemote) {
         super.interval += 5;
      }
     // this.guidanceSystem = new MCH_WeaponGuidanceSystem(this.worldObj);
     // this.guidanceSystem.canLockInAir = true;
     // this.guidanceSystem.ridableOnly = wi.ridableOnly;
   }

   public boolean isCooldownCountReloadTime() {
      return true;
   }

   double getYawToTarget(Entity c) {
	   double dX = aircraft.posX - c.posX;
	   double dZ = aircraft.posZ - c.posZ;
	   double yaw = Math.toDegrees(Math.atan(dX/dZ));
	   return yaw - aircraft.rotationYaw;
   }
   
   double getPitchToTarget(Entity c) {
	   double dist = aircraft.getDistance(c.posX, aircraft.posY, c.posZ);
	   double dY = c.posY - aircraft.posY;
	//   aircraft.print("dist: " + dist + " dy " + dY);
	   double pitch = Math.toDegrees(Math.atan(dY/dist));
	   return pitch + aircraft.rotationPitch;
   }
   
   private double getAltAngle(MCH_EntityAircraft a, MCH_EntityAircraft b) {
		double dist = a.getDistance(b.posX, a.posY, b.posZ);
		double dY = b.posY - a.posY;
		//a.print("Dist: " + dist + " dY " + dY);
		return Math.toDegrees(Math.atan(dY / dist));
	}
	
	private double getAspect(MCH_EntityAircraft a, MCH_EntityAircraft b) {
		return getAngle(a, b) - a.rotationYaw;
	}
	
	private double getAngle(MCH_EntityAircraft a, MCH_EntityAircraft b) {
		return a.getBearingToEntity(b);
	}
   
   public void update(int countWait) {
      super.update(countWait);
      EntityPlayer p = aircraft.getFirstMountPlayer();
      if(p == null) {return;}
      if(aircraft.getCurrentWeapon(p).getCurrentWeapon() == this) {
    	  
    	  double lockRange = 200;
    	  List<Entity> contacts = aircraft.worldObj.getEntitiesWithinAABBExcludingEntity(aircraft, aircraft.boundingBox.expand(lockRange, lockRange, lockRange));
    	  for(Entity c : contacts) {
    		  if(c instanceof MCH_EntityAircraft && !c.onGround) {
	    		  double yaw = getAspect(aircraft, (MCH_EntityAircraft)c) - 180;
	    		  double pitch = getAltAngle(aircraft, (MCH_EntityAircraft)c) + aircraft.rotationPitch;
	    //		  aircraft.print("Yaw: " + yaw + " pitch: " + pitch);
	    		  if(Math.abs(yaw) <= 30 && Math.abs(pitch) <= 45) {
	    			  if(target == null) {
		    			  this.target = c;
		    			  
	    			  }else if(Math.abs(yaw) + Math.abs(pitch) < Math.abs(getPitchToTarget(target)) + Math.abs(getYawToTarget(target))){
	    				  target = c;
	    			  }
	    		  }else {
	    			  target = null;
	    		  }
    		  }
    	  }
    	  if(target != null) {
    		  this.playSound(p, "ir_lock_tone");
    		  //aircraft.print("Target!");
    	  }else {
    		  this.playSound(p, "ir_basic_tone");
    	  }

      }
   }
   

   public boolean shot(MCH_WeaponParam prm) {
      boolean result = false;
      if(!super.worldObj.isRemote) {
         Entity tgtEnt = prm.user.worldObj.getEntityByID(prm.option1);
         if(tgtEnt != null && !tgtEnt.isDead) {
            this.playSound(prm.entity);
            float yaw = prm.entity.rotationYaw + super.fixRotationYaw;
            float pitch = prm.entity.rotationPitch + super.fixRotationPitch;
            double tX = (double)(-MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
            double tZ = (double)(MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
            double tY = (double)(-MathHelper.sin(pitch / 180.0F * 3.1415927F));
            MCH_EntityAAMissile e = new MCH_EntityAAMissile(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, (double)super.acceleration);
            e.type = "ir";
            e.setName(super.name);
            e.setParameterFromWeapon(this, prm.entity, prm.user);
            e.setTargetEntity(tgtEnt);
            super.worldObj.spawnEntityInWorld(e);
            result = true;
         }
      } else if(target != null) {
         result = true;
         //System.out.println("Firing " +this.weaponInfo.soundFileName);
         super.optionParameter1 = W_Entity.getEntityId(target);
      }else {
    	  //System.out.println("Yeet!");
      }

      return result;
   }
}
