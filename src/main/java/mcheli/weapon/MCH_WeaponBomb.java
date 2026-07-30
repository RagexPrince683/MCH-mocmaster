package mcheli.weapon;

import mcheli.MCH_Config;
import mcheli.MCH_Explosion;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.plane.MCP_PlaneCCIPHelper;
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
            MCH_EntityBaseVehicle aircraft = (MCH_EntityBaseVehicle)prm.entity;
            if((aircraft.isUAV() || aircraft.isNewUAV()) && aircraft.getSeatNum() == 0) {
               if(!super.worldObj.isRemote) {
                  MCH_Explosion.newExplosion(super.worldObj, (Entity)null, prm.user,
                        aircraft.posX, aircraft.posY, aircraft.posZ,
                        (float)this.getInfo().explosion,
                        (float)this.getInfo().explosionBlock,
                        true, true, this.getInfo().flaming, true, 0);
                  this.playSound(prm.entity);
               }
               aircraft.destroyAircraft();
            }
         }
      } else if(!super.worldObj.isRemote) {
         this.playSound(prm.entity);

         MCH_EntityBomb bomb = new MCH_EntityBomb(
               super.worldObj,
               prm.posX, prm.posY, prm.posZ,
               prm.entity.motionX, prm.entity.motionY, prm.entity.motionZ,
               prm.entity.rotationYaw, 0.0F,
               super.getEffectiveLaunchAcceleration());

         bomb.setName(super.name);
         bomb.setParameterFromWeapon(this, prm.entity, prm.user);

         // Gravity bombs inherit the aircraft's actual world velocity exactly.
         // Keep this assignment after construction because the bullet constructor
         // normalizes its direction vector to the configured acceleration.
         bomb.motionX = prm.entity.motionX;
         bomb.motionY = prm.entity.motionY;
         bomb.motionZ = prm.entity.motionZ;

         if(MCH_Config.PlaneMouseAimReticleDebug.prmBool
               || MCH_Config.DebugFlightControl.prmBool) {
            Vec3 aircraftMotion = Vec3.createVectorHelper(
                  prm.entity.motionX,
                  prm.entity.motionY,
                  prm.entity.motionZ);
            Vec3 releasePos = Vec3.createVectorHelper(
                  bomb.posX,
                  bomb.posY,
                  bomb.posZ);
            Vec3 initialVelocity = Vec3.createVectorHelper(
                  bomb.motionX,
                  bomb.motionY,
                  bomb.motionZ);

            double horizontalSpeed = Math.sqrt(
                  prm.entity.motionX * prm.entity.motionX
                        + prm.entity.motionZ * prm.entity.motionZ);
            float roll = prm.entity instanceof MCH_EntityBaseVehicle
                  ? ((MCH_EntityBaseVehicle)prm.entity).getRotRoll()
                  : 0.0F;

            System.out.println(String.format(
                  "[CCIP_BOMB_SPAWN] weapon=%s aircraftMotion=%.6f,%.6f,%.6f aircraftHorizontalSpeed=%.6f aircraftYawPitchRoll=%.2f,%.2f,%.2f releaseParam=%.6f,%.6f,%.6f bombPos=%.6f,%.6f,%.6f bombMotion=%.6f,%.6f,%.6f deltaFromAircraft=%.6f,%.6f,%.6f gravity=%.6f dragXZ=0.999000 ejection=0.000000,0.000000,0.000000 speedDependsAircraft=%s accelerationConfig=%.6f",
                  super.name,
                  prm.entity.motionX, prm.entity.motionY, prm.entity.motionZ,
                  horizontalSpeed,
                  prm.entity.rotationYaw, prm.entity.rotationPitch, roll,
                  prm.posX, prm.posY, prm.posZ,
                  bomb.posX, bomb.posY, bomb.posZ,
                  bomb.motionX, bomb.motionY, bomb.motionZ,
                  bomb.motionX - prm.entity.motionX,
                  bomb.motionY - prm.entity.motionY,
                  bomb.motionZ - prm.entity.motionZ,
                  this.getInfo().gravity,
                  Boolean.valueOf(this.getInfo().speedDependsAircraft),
                  this.getInfo().acceleration));

            // Pass aircraftMotion here as well. Without it, debug calibration
            // disagrees with the real entity whenever speedDependsAircraft is set.
            MCP_PlaneCCIPHelper.Result ccip = MCP_PlaneCCIPHelper.predict(
                  super.worldObj,
                  this.getInfo(),
                  releasePos,
                  initialVelocity,
                  aircraftMotion);

            bomb.setCCIPCalibration(
                  ccip,
                  aircraftMotion,
                  Vec3.createVectorHelper(prm.posX, prm.posY, prm.posZ));

            System.out.println(String.format(
                  "[CCIP_BOMB_PREDICT] weapon=%s predictedRelease=%.6f,%.6f,%.6f predictedInitialVelocity=%.6f,%.6f,%.6f predictedGravity=%.6f predictedDrag=%.6f predictedEjection=0.000000,0.000000,0.000000 predictedTimestep=%.1f predictedImpact=%s ticks=%d valid=%s reason=%s",
                  super.name,
                  ccip.releasePos != null ? ccip.releasePos.xCoord : bomb.posX,
                  ccip.releasePos != null ? ccip.releasePos.yCoord : bomb.posY,
                  ccip.releasePos != null ? ccip.releasePos.zCoord : bomb.posZ,
                  ccip.initialVelocity != null ? ccip.initialVelocity.xCoord : bomb.motionX,
                  ccip.initialVelocity != null ? ccip.initialVelocity.yCoord : bomb.motionY,
                  ccip.initialVelocity != null ? ccip.initialVelocity.zCoord : bomb.motionZ,
                  ccip.gravity,
                  ccip.horizontalDrag,
                  ccip.simulationTimeStep,
                  ccip.impact != null
                        ? String.format("%.6f,%.6f,%.6f",
                              ccip.impact.xCoord,
                              ccip.impact.yCoord,
                              ccip.impact.zCoord)
                        : "-",
                  Integer.valueOf(ccip.ticksSimulated),
                  Boolean.valueOf(ccip.valid),
                  ccip.reasonInvalid));
         }

         super.worldObj.spawnEntityInWorld(bomb);
      }

      return true;
   }
}
