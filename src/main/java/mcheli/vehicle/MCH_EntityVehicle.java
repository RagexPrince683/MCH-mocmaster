package mcheli.vehicle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipLocator;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_PacketStatusRequest;
import mcheli.sensors.MCH_RadarContact;
import mcheli.weapon.MCH_EntityAAMissile;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponParam;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class MCH_EntityVehicle extends MCH_EntityAircraft {

   private MCH_VehicleInfo vehicleInfo = null;
   public boolean isUsedPlayer;
   public float lastRiderYaw;
   public float lastRiderPitch;
   public String team;
   public ArrayList<MCH_EntityAAMissile> missiles = new ArrayList<MCH_EntityAAMissile>();
   public int firingTimer = 0;
   
   public MCH_EntityVehicle(World world) {
      super(world);
      super.currentSpeed = 0.07D;
      super.preventEntitySpawning = true;
      this.setSize(2.0F, 0.7F);
      super.yOffset = super.height / 2.0F;
      super.motionX = 0.0D;
      super.motionY = 0.0D;
      super.motionZ = 0.0D;
      this.isUsedPlayer = false;
      this.lastRiderYaw = 0.0F;
      this.lastRiderPitch = 0.0F;
      super.weapons = this.createWeapon(0);
   }

   public String getKindName() {
      return "vehicles";
   }

   public String getEntityType() {
      return "Vehicle";
   }

   public MCH_VehicleInfo getVehicleInfo() {
      return this.vehicleInfo;
   }

   public void changeType(String type) {
      if(!type.isEmpty()) {
         this.vehicleInfo = MCH_VehicleInfoManager.get(type);
      }

      if(this.vehicleInfo == null) {
         MCH_Lib.Log((Entity)this, "##### MCH_EntityVehicle changeVehicleType() Vehicle info null %d, %s, %s", new Object[]{Integer.valueOf(W_Entity.getEntityId(this)), type, this.getEntityName()});
         this.setDead();
      } else {
         this.setAcInfo(this.vehicleInfo);
         this.newSeats(this.getAcInfo().getNumSeatAndRack());
         super.weapons = this.createWeapon(1 + this.getSeatNum());
         this.initPartRotation(super.rotationYaw, super.rotationPitch);
      }

   }

   public boolean canMountWithNearEmptyMinecart() {
      MCH_Config var10000 = MCH_MOD.config;
      return MCH_Config.MountMinecartVehicle.prmBool;
   }

   protected void entityInit() {
      super.entityInit();
   }

   protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
      super.writeEntityToNBT(par1NBTTagCompound);
      try {
	   par1NBTTagCompound.setString("team", this.team);
      }catch(Exception e) {}

   }

   protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
      super.readEntityFromNBT(par1NBTTagCompound);
      try {
    	  this.team = par1NBTTagCompound.getString("team");
      }catch(Exception e) {}
      if(this.vehicleInfo == null) {
         this.vehicleInfo = MCH_VehicleInfoManager.get(this.getTypeName());
         if(this.vehicleInfo == null) {
            MCH_Lib.Log((Entity)this, "##### MCH_EntityVehicle readEntityFromNBT() Vehicle info null %d, %s", new Object[]{Integer.valueOf(W_Entity.getEntityId(this)), this.getEntityName()});
            this.setDead();
         } else {
            this.setAcInfo(this.vehicleInfo);
         }
      }

   }

   public Item getItem() {
      return this.getVehicleInfo() != null?this.getVehicleInfo().item:null;
   }

   public void setDead() {
      super.setDead();
   }

   public float getSoundVolume() {
      return (float)this.getCurrentThrottle() * 2.0F;
   }

   public float getSoundPitch() {
      return (float)(this.getCurrentThrottle() * 0.5D);
   }

   public String getDefaultSoundName() {
      return "";
   }

   @SideOnly(Side.CLIENT)
   public void zoomCamera() {
      if(this.canZoom()) {
         float z = super.camera.getCameraZoom();
         ++z;
         super.camera.setCameraZoom((double)z <= (double)this.getZoomMax() + 0.01D?z:1.0F);
      }

   }

   public void _updateCameraRotate(float yaw, float pitch) {
      super.camera.prevRotationYaw = super.camera.rotationYaw;
      super.camera.prevRotationPitch = super.camera.rotationPitch;
      if(pitch > 89.0F) {
         pitch = 89.0F;
      }

      if(pitch < -89.0F) {
         pitch = -89.0F;
      }

      super.camera.rotationYaw = yaw;
      super.camera.rotationPitch = pitch;
   }

   public boolean isCameraView(Entity entity) {
      return true;
   }

   public boolean useCurrentWeapon(MCH_WeaponParam prm) {
      if(prm.user != null) {
         MCH_WeaponSet breforeUseWeaponPitch = this.getCurrentWeapon(prm.user);
         if(breforeUseWeaponPitch != null) {
            MCH_AircraftInfo.Weapon breforeUseWeaponYaw = this.getAcInfo().getWeaponByName(breforeUseWeaponPitch.getInfo().name);
            if(breforeUseWeaponYaw != null && breforeUseWeaponYaw.maxYaw != 0.0F && breforeUseWeaponYaw.minYaw != 0.0F) {
               return super.useCurrentWeapon(prm);
            }
         }
      }

      float breforeUseWeaponPitch1 = super.rotationPitch;
      float breforeUseWeaponYaw1 = super.rotationYaw;
      super.rotationPitch = prm.user.rotationPitch;
      super.rotationYaw = prm.user.rotationYaw;
      boolean result = super.useCurrentWeapon(prm);
      super.rotationPitch = breforeUseWeaponPitch1;
      super.rotationYaw = breforeUseWeaponYaw1;
      return result;
   }

   public void handleShip(){
       if(!this.getAcInfo().isNaval){return;} //Only naval turrets work on ships - duh
       for(EntityShip ship : ShipLocator.getShips(worldObj)) {
           List<Entity> riders = ship.getCollider().getRiders();
           if(riders.contains(this)) { //The stolen plans are on board this vessel
               for(Entity e : riders) {
                   if(e instanceof EntityPlayer) {
                       EntityPlayer player = (EntityPlayer)e;
                       if(player.getHeldItem() != null) {
                           if(player.getHeldItem().getItem() == Items.wooden_sword) {
                               lastRiderYaw = player.rotationYaw;
                               lastRiderPitch = player.rotationPitch;
                               this.lastRiddenByEntity = player;
                               //this.rotationYaw = player.rotationYaw;
                           }
                       }
                   }
               }
           }else {
           }
       }
   }

   public void onUpdateAircraft() {
      if(this.vehicleInfo == null) {
         this.changeType(this.getTypeName());
         super.prevPosX = super.posX;
         super.prevPosY = super.posY;
         super.prevPosZ = super.posZ;
      } else {
         if(!super.isRequestedSyncStatus) {
            super.isRequestedSyncStatus = true;
            if(super.worldObj.isRemote) {
               MCH_PacketStatusRequest.requestStatus(this);
            }
         }

         if(super.lastRiddenByEntity == null && this.getRiddenByEntity() != null) {
            this.getRiddenByEntity().rotationPitch = 0.0F;
            this.getRiddenByEntity().prevRotationPitch = 0.0F;
            this.initCurrentWeapon(this.getRiddenByEntity());
         }
         
         if(this.getFirstMountPlayer() == null) {
             handleShip();
             if(this.ridingEntity != null){
                this.lastRiderPitch = 0;
                this.lastRiderYaw = this.rotationYaw;
             }else if(rotationPitch != 0){
                 rotationPitch = 0;
             }
         }
         
         //lastRiderPitch = this.worldObj.getWorldTime() % 360;

         this.updateWeapons();
         this.onUpdate_Seats();
         this.onUpdate_Control();
         super.prevPosX = super.posX;
         super.prevPosY = super.posY;
         super.prevPosZ = super.posZ;
         if(this.isInWater()) {
            super.rotationPitch *= 0.9F;
         }

         if(super.worldObj.isRemote) {
            this.onUpdate_Client();
         } else {
            this.onUpdate_Server();
         }

      }
   }

   protected void onUpdate_Control() {
      double max_y = 1.0D;
      if(super.riddenByEntity != null && !super.riddenByEntity.isDead) {
         if(this.getVehicleInfo().isEnableMove || this.getVehicleInfo().isEnableRot) {
            this.onUpdate_ControlOnGround();
         }
      } else if(this.getCurrentThrottle() > 0.0D) {
         this.addCurrentThrottle(-0.00125D);
      } else {
         this.setCurrentThrottle(0.0D);
      }

      if(this.getCurrentThrottle() < 0.0D) {
         this.setCurrentThrottle(0.0D);
      }

      if(super.worldObj.isRemote) {
         if(!W_Lib.isClientPlayer(this.getRiddenByEntity())) {
            double ct = this.getThrottle();
            if(this.getCurrentThrottle() > ct) {
               this.addCurrentThrottle(-0.005D);
            }

            if(this.getCurrentThrottle() < ct) {
               this.addCurrentThrottle(0.005D);
            }
         }
      } else {
         this.setThrottle(this.getCurrentThrottle());
      }

   }

   protected void onUpdate_ControlOnGround() {
      if(!super.worldObj.isRemote) {
         boolean move = false;
         float yaw = super.rotationYaw;
         double x = 0.0D;
         double z = 0.0D;
         if(this.getVehicleInfo().isEnableMove) {
            if(super.throttleUp) {
               yaw = super.rotationYaw;
               x += Math.sin((double)yaw * 3.141592653589793D / 180.0D);
               z += Math.cos((double)yaw * 3.141592653589793D / 180.0D);
               move = true;
            }

            if(super.throttleDown) {
               yaw = super.rotationYaw - 180.0F;
               x += Math.sin((double)yaw * 3.141592653589793D / 180.0D);
               z += Math.cos((double)yaw * 3.141592653589793D / 180.0D);
               move = true;
            }
         }

         if(this.getVehicleInfo().isEnableMove) {
            if(super.moveLeft && !super.moveRight) {
               super.rotationYaw = (float)((double)super.rotationYaw - 0.5D);
            }

            if(super.moveRight && !super.moveLeft) {
               super.rotationYaw = (float)((double)super.rotationYaw + 0.5D);
            }
         }

         if(move) {
            double d = Math.sqrt(x * x + z * z);
            super.motionX -= x / d * 0.029999999329447746D;
            super.motionZ += z / d * 0.029999999329447746D;
         }
      }

   }

   protected void onUpdate_Particle() {
      double particlePosY = super.posY;
      boolean b = false;

      int y;
      int k;
      for(y = 0; y < 5 && !b; ++y) {
         int pn;
         int z;
         for(pn = -1; pn <= 1; ++pn) {
            for(z = -1; z <= 1; ++z) {
               k = W_WorldFunc.getBlockId(super.worldObj, (int)(super.posX + 0.5D) + pn, (int)(super.posY + 0.5D) - y, (int)(super.posZ + 0.5D) + z);
               if(k != 0 && !b) {
                  particlePosY = (double)((int)(super.posY + 1.0D) - y);
                  b = true;
               }
            }
         }

         for(pn = -3; b && pn <= 3; ++pn) {
            for(z = -3; z <= 3; ++z) {
               if(W_WorldFunc.isBlockWater(super.worldObj, (int)(super.posX + 0.5D) + pn, (int)(super.posY + 0.5D) - y, (int)(super.posZ + 0.5D) + z)) {
                  for(k = 0; (double)k < 7.0D * this.getCurrentThrottle(); ++k) {
                     super.worldObj.spawnParticle("splash", super.posX + 0.5D + (double)pn + (super.rand.nextDouble() - 0.5D) * 2.0D, particlePosY + super.rand.nextDouble(), super.posZ + 0.5D + (double)z + (super.rand.nextDouble() - 0.5D) * 2.0D, (double)pn + (super.rand.nextDouble() - 0.5D) * 2.0D, -0.3D, (double)z + (super.rand.nextDouble() - 0.5D) * 2.0D);
                  }
               }
            }
         }
      }

      double var9 = (double)(5 - y + 1) / 5.0D;
      if(b) {
         for(k = 0; k < (int)(this.getCurrentThrottle() * 6.0D * var9); ++k) {
            float f3 = 0.25F;
            super.worldObj.spawnParticle("explode", super.posX + (super.rand.nextDouble() - 0.5D), particlePosY + (super.rand.nextDouble() - 0.5D), super.posZ + (super.rand.nextDouble() - 0.5D), (super.rand.nextDouble() - 0.5D) * 2.0D, -0.4D, (super.rand.nextDouble() - 0.5D) * 2.0D);
         }
      }

   }

   protected void onUpdate_Client() {
	   
      this.updateCameraViewers();
      if(super.riddenByEntity != null && W_Lib.isClientPlayer(this.getRiddenByEntity())) {
         this.getRiddenByEntity().rotationPitch = this.getRiddenByEntity().prevRotationPitch;
      }

      if(super.aircraftPosRotInc > 0) {
         double rpinc = (double)super.aircraftPosRotInc;
         double yaw = MathHelper.wrapAngleTo180_double(super.aircraftYaw - (double)super.rotationYaw);
         super.rotationYaw = (float)((double)super.rotationYaw + yaw / rpinc);
         super.rotationPitch = (float)((double)super.rotationPitch + (super.aircraftPitch - (double)super.rotationPitch) / rpinc);
         this.setPosition(super.posX + (super.aircraftX - super.posX) / rpinc, super.posY + (super.aircraftY - super.posY) / rpinc, super.posZ + (super.aircraftZ - super.posZ) / rpinc);
         this.setRotation(super.rotationYaw, super.rotationPitch);
         --super.aircraftPosRotInc;
      } else {
         this.setPosition(super.posX + super.motionX, super.posY + super.motionY, super.posZ + super.motionZ);
         if(super.onGround) {
            super.motionX *= 0.95D;
            super.motionZ *= 0.95D;
         }

         if(this.isInWater()) {
            super.motionX *= 0.99D;
            super.motionZ *= 0.99D;
         }
      }

      if(super.riddenByEntity != null) {
         ;
      }

      this.updateCamera(super.posX, super.posY, super.posZ);
   }

   private void updateMissiles() {
	   ArrayList<MCH_EntityAAMissile> newMissiles = new ArrayList<MCH_EntityAAMissile>();
	   for(MCH_EntityAAMissile missile : missiles) {
		   if(missile != null && !missile.isDead) {
			   newMissiles.add(missile);
		   }
	   }
	   missiles = newMissiles;
   }
   
   public boolean getShotMissile(MCH_RadarContact target) { //returns whether we have already fired a missile at a given target
	   for(MCH_EntityAAMissile missile : missiles) {
		   if((missile.getRadarTarget() != null && missile.getRadarTarget().entityID == target.entityID) || missile.targetEntity.getEntityId() == target.entityID) {
			   
			   return true;
		   }else {
			   //return true;
		   }
	   }
	   return false;
   }
   
   public boolean getShotMissile(Entity target) { //returns whether we have already fired a missile at a given target
	   for(MCH_EntityAAMissile missile : missiles) {
		   if((missile.getRadarTarget() != null && missile.getRadarTarget().entityID == target.getEntityId()) || missile.targetEntity.getEntityId() == target.getEntityId()) {
			   
			   return true;
		   }else {
			   //return true;
		   }
	   }
	   return false;
   }
   
   
   @Override
	public MCH_RadarContact getClosestContact() {
		MCH_RadarContact closest = null;
		double d = Double.MAX_VALUE;

		for(MCH_RadarContact c : contacts) {
			double t = this.getDistance(c.x, c.y, c.z);
			if(t < d) {
				Entity e = this.worldObj.getEntityByID(c.entityID);
				if(!e.isDead && e instanceof MCH_EntityAircraft && !this.getShotMissile(e)) {
					MCH_EntityAircraft ac = (MCH_EntityAircraft)e;
					if(ac.getFirstMountPlayer() != null) {
						//ac.print("Ping");
						if(ac.getFirstMountPlayer().getTeam().getRegisteredName() != this.team){
							//ac.print("YEET");
							d = t;
							closest = c;
						}
					}else if(ac.isTargetDrone()) {
						d = t;
						closest = c;
					}
				}
			}
		}
		return closest;
	}

   
   public Entity getTarget() {
	   Entity e = this.worldObj.getEntityByID(this.getClosestContact().entityID);

	   return e;

	   //double range = 100; 
	   //List<Entity> list =worldObj.getEntitiesWithinAABBExcludingEntity(this,this.boundingBox.expand(range, range, range));
	   //for(Entity e : list) {
		 //  if(e instanceof MCH_EntityASMissile || e instanceof MCH_EntitySARHMissile || e instanceof MCH_EntityAShM) {
			//   if(!getShotMissile(e)) {
				//   return e;
			  // }
		  // }
	  // }
	 //  return null;
   }
   
   private void updateAutoTurret() {
	   if(this.isDestroyed()) {return;}
	   this.radarMode = 0;
	   super.updateRadar();
	   
	   updateMissiles();
	   if(this.getFirstSeatWeapon().getAmmoNum() == 0) {
		   return;
		}
	   	   
	   try {
		   Entity e = getTarget();
		   
		   if(e != null) {
			   //System.out.println("Target: " + e.getCommandSenderName());

			   notifyLock(e);
			  // if(!getShotMissile(radarTarget)) {
				   fireMissile(e, new MCH_WeaponInfo("sa-2"));
				   MCH_WeaponSet ws = this.getFirstSeatWeapon();
				   ws.setAmmoNum(ws.getAmmoNum() - 1);
			   //}
		   }else {
			  // System.out.println("E is null");
		   }
	   }catch(Exception e) {}
   }
   
   public void fireMissile(Entity target, MCH_WeaponInfo info) {
		
		W_WorldFunc.MOD_playSoundAtEntity(this, info.soundFileName, 1.0F, 1.0F);

		float yaw = rotationYaw;
		float pitch = rotationPitch;

		double tX = (double)(-MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
		double tZ = (double)(MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
		double tY = (double)(-MathHelper.sin(pitch-90 / 180.0F * 3.1415927F));
		MCH_EntityAAMissile e = new MCH_EntityAAMissile(super.worldObj, posX, posY, posZ, tX, tY, tZ, yaw, pitch, 3.5);
		e.setName(info.name);
		e.explosionPower = 15;
		
		e.type = "ir";
		e.explosionPowerInWater = info.explosionInWater;
		e.setPower(info.power);
		e.piercing = info.piercing;
		e.shootingAircraft = this;
		e.shootingEntity = this;
		
		e.setTargetEntity(target);
		this.missiles.add(e);
		super.worldObj.spawnEntityInWorld(e);
	}


   
	/*
	 * //private Vec3 getTarget() { double range = 10; List<Entity> list =
	 * worldObj.getEntitiesWithinAABBExcludingEntity(this,this.boundingBox.expand(
	 * range, range, range)); for(Entity e : list) { if(e instanceof EntityPlayer) {
	 * return Vec3.createVectorHelper(e.posX, e.posY, e.posZ); } } return null; }
	 */
   
   public float getYawToVector(Vec3 target) {
		double delta_x = target.xCoord - posX;
		double delta_z = posZ - target.zCoord;
		float angle = (float) Math.atan2(delta_x, delta_z);
		//angle = Math.toDegrees(angle);

		//if(angle < 0) { angle += 360;}

		return angle;
	}

private void onUpdate_Server() {
	   firingTimer--;
		if(this.getFirstMountPlayer() == null && this.team!= null){
			if(this.worldObj.getWorldTime() % 10 == 0) {
		updateAutoTurret();}
		}
		else {
		 //  System.out.println("Yeet " + team);
		//   updateAutoTurret();
	   }
	   
      double prevMotion = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      this.updateCameraViewers();
      double dp = 0.0D;
      if(this.canFloatWater()) {
         dp = this.getWaterDepth();
      }

      if(dp == 0.0D) {
         super.motionY += (double)(!this.isInWater()?this.getAcInfo().gravity:this.getAcInfo().gravityInWater);
      } else if(dp < 1.0D) {
         super.motionY -= 1.0E-4D;
         super.motionY += 0.007D * this.getCurrentThrottle();
      } else {
         if(super.motionY < 0.0D) {
            super.motionY /= 2.0D;
         }

         super.motionY += 0.007D;
      }

      double motion = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      float speedLimit = this.getAcInfo().speed;
      if(motion > (double)speedLimit) {
         super.motionX *= (double)speedLimit / motion;
         super.motionZ *= (double)speedLimit / motion;
         motion = (double)speedLimit;
      }

      if(motion > prevMotion && super.currentSpeed < (double)speedLimit) {
         super.currentSpeed += ((double)speedLimit - super.currentSpeed) / 35.0D;
         if(super.currentSpeed > (double)speedLimit) {
            super.currentSpeed = (double)speedLimit;
         }
      } else {
         super.currentSpeed -= (super.currentSpeed - 0.07D) / 35.0D;
         if(super.currentSpeed < 0.07D) {
            super.currentSpeed = 0.07D;
         }
      }

      if(super.onGround) {
         super.motionX *= 0.5D;
         super.motionZ *= 0.5D;
      }

      this.moveEntity(super.motionX, super.motionY, super.motionZ);
      super.motionY *= 0.95D;
      super.motionX *= 0.99D;
      super.motionZ *= 0.99D;
      this.onUpdate_updateBlock();
      if(super.riddenByEntity != null && super.riddenByEntity.isDead) {
         this.unmountEntity();
         super.riddenByEntity = null;
      }

   }

   public void onUpdateAngles(float partialTicks) {}

   public void _updateRiderPosition() {
      float yaw = super.rotationYaw;
      if(super.riddenByEntity != null) {
         super.rotationYaw = super.riddenByEntity.rotationYaw;
      }

      super.updateRiderPosition();
      super.rotationYaw = yaw;
   }

   public boolean canSwitchFreeLook() {
      return false;
   }
}
