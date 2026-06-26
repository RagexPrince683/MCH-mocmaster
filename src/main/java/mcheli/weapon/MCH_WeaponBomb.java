package mcheli.weapon;

import mcheli.MCH_Config;
import mcheli.MCH_Explosion;
import mcheli.plane.MCP_PlaneCCIPHelper;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.weapon.MCH_EntityBomb;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponParam;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponBomb extends MCH_WeaponBase {

   public MCH_WeaponBomb(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.acceleration = 0.5F;
      super.explosionPower = 9;
      super.power = 35;
      super.interval = -90;
      if(w.isRemote) {
         super.interval -= 10;
      }

   }

   public boolean shot(MCH_WeaponParam prm) {
      if(this.getInfo() != null && this.getInfo().destruct) {
         if(prm.entity instanceof MCH_EntityBaseVehicle) {
            MCH_EntityBaseVehicle e1 = (MCH_EntityBaseVehicle)prm.entity;
            if((e1.isUAV() || e1.isNewUAV()) && e1.getSeatNum() == 0) {
               if(!super.worldObj.isRemote) {
                  MCH_Explosion.newExplosion(super.worldObj, (Entity)null, prm.user, e1.posX, e1.posY, e1.posZ, (float)this.getInfo().explosion, (float)this.getInfo().explosionBlock, true, true, this.getInfo().flaming, true, 0);
                  this.playSound(prm.entity);
                  //destroyAircraft
                  //e1.destroyAircraft();

               }

               e1.destroyAircraft();
               //e1.destruct();

            }
         }
      } else if(!super.worldObj.isRemote) {
         this.playSound(prm.entity);
         MCH_EntityBomb e = new MCH_EntityBomb(super.worldObj, prm.posX, prm.posY, prm.posZ, prm.entity.motionX, prm.entity.motionY, prm.entity.motionZ, prm.entity.rotationYaw, 0.0F, (double)super.acceleration);
         e.setName(super.name);
         e.setParameterFromWeapon(this, prm.entity, prm.user);
         e.motionX = prm.entity.motionX;
         e.motionY = prm.entity.motionY;
         e.motionZ = prm.entity.motionZ;
         if(MCH_Config.PlaneMouseAimReticleDebug.prmBool || MCH_Config.DebugFlightControl.prmBool) {
            double hs = Math.sqrt(prm.entity.motionX * prm.entity.motionX + prm.entity.motionZ * prm.entity.motionZ);
            System.out.println(String.format("[CCIP_BOMB_SPAWN] weapon=%s aircraftMotion=%.6f,%.6f,%.6f aircraftHorizontalSpeed=%.6f aircraftYawPitchRoll=%.2f,%.2f,%.2f releaseParam=%.6f,%.6f,%.6f bombPos=%.6f,%.6f,%.6f bombMotion=%.6f,%.6f,%.6f deltaFromAircraft=%.6f,%.6f,%.6f gravity=%.6f dragXZ=0.999000 ejection=0.000000,0.000000,0.000000 speedDependsAircraft=%s accelerationConfig=%.6f",
                  super.name, prm.entity.motionX, prm.entity.motionY, prm.entity.motionZ, hs, prm.entity.rotationYaw, prm.entity.rotationPitch, prm.entity instanceof mcheli.aircraft.MCH_EntityBaseVehicle ? ((mcheli.aircraft.MCH_EntityBaseVehicle)prm.entity).getRotRoll() : 0.0F,
                  prm.posX, prm.posY, prm.posZ, e.posX, e.posY, e.posZ, e.motionX, e.motionY, e.motionZ, e.motionX - prm.entity.motionX, e.motionY - prm.entity.motionY, e.motionZ - prm.entity.motionZ, this.getInfo().gravity, Boolean.valueOf(this.getInfo().speedDependsAircraft), this.getInfo().acceleration));
            MCP_PlaneCCIPHelper.Result ccip = MCP_PlaneCCIPHelper.predict(super.worldObj, this.getInfo(), Vec3.createVectorHelper(e.posX, e.posY, e.posZ), Vec3.createVectorHelper(e.motionX, e.motionY, e.motionZ));
            e.setCCIPCalibration(ccip, Vec3.createVectorHelper(prm.entity.motionX, prm.entity.motionY, prm.entity.motionZ), Vec3.createVectorHelper(prm.posX, prm.posY, prm.posZ));
            System.out.println(String.format("[CCIP_BOMB_PREDICT] weapon=%s predictedRelease=%.6f,%.6f,%.6f predictedInitialVelocity=%.6f,%.6f,%.6f predictedGravity=%.6f predictedDrag=%.6f predictedEjection=0.000000,0.000000,0.000000 predictedTimestep=%.1f predictedImpact=%s ticks=%d valid=%s reason=%s",
                  super.name, e.posX, e.posY, e.posZ, e.motionX, e.motionY, e.motionZ, ccip.gravity, ccip.horizontalDrag, ccip.simulationTimeStep, ccip.impact != null ? String.format("%.6f,%.6f,%.6f", ccip.impact.xCoord, ccip.impact.yCoord, ccip.impact.zCoord) : "-", Integer.valueOf(ccip.ticksSimulated), Boolean.valueOf(ccip.valid), ccip.reasonInvalid));
         }
         super.worldObj.spawnEntityInWorld(e);
      }

      return true;
   }
}
