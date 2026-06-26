package mcheli.weapon;

import java.util.List;
import mcheli.MCH_Config;
import mcheli.plane.MCP_PlaneCCIPHelper;
import mcheli.weapon.MCH_BulletModel;
import mcheli.weapon.MCH_DefaultBulletModels;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_Lib;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_EntityBomb extends MCH_EntityBaseBullet {

   private MCP_PlaneCCIPHelper.Result ccipCalibration;
   private Vec3 ccipAircraftMotion;
   private Vec3 ccipWeaponReleaseParam;
   private Vec3 ccipRealInitialPos;
   private Vec3 ccipRealInitialMotion;

   public MCH_EntityBomb(World par1World) {
      super(par1World);
   }

   public MCH_EntityBomb(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
   }

   public void onUpdate() {
      super.onUpdate();
      if(!super.worldObj.isRemote && this.getInfo() != null) {
         super.motionX *= 0.999D;
         super.motionZ *= 0.999D;
         if(this.isInWater()) {
            super.motionX *= (double)this.getInfo().velocityInWater;
            super.motionY *= (double)this.getInfo().velocityInWater;
            super.motionZ *= (double)this.getInfo().velocityInWater;
         }

         float dist = this.getInfo().proximityFuseDist;
         if(dist > 0.1F && this.getCountOnUpdate() % 10 == 0) {
            List list = super.worldObj.getEntitiesWithinAABBExcludingEntity(this, super.boundingBox.expand((double)dist, (double)dist, (double)dist));
            if(list != null) {
               for(int i = 0; i < list.size(); ++i) {
                  Entity entity = (Entity)list.get(i);
                  if(W_Lib.isEntityLivingBase(entity) && this.canBeCollidedEntity(entity)) {
                     MovingObjectPosition m = new MovingObjectPosition((int)(super.posX + 0.5D), (int)(super.posY + 0.5D), (int)(super.posZ + 0.5D), 0, Vec3.createVectorHelper(super.posX, super.posY, super.posZ));
                     this.onImpact(m, 1.0F);
                     break;
                  }
               }
            }
         }
      }

      this.onUpdateBomblet();
   }

   public void setCCIPCalibration(MCP_PlaneCCIPHelper.Result prediction, Vec3 aircraftMotion, Vec3 weaponReleaseParam) {
      this.ccipCalibration = prediction;
      this.ccipAircraftMotion = copyVec(aircraftMotion);
      this.ccipWeaponReleaseParam = copyVec(weaponReleaseParam);
      this.ccipRealInitialPos = Vec3.createVectorHelper(super.posX, super.posY, super.posZ);
      this.ccipRealInitialMotion = Vec3.createVectorHelper(super.motionX, super.motionY, super.motionZ);
   }

   protected void onCCIPCalibrationImpact(MovingObjectPosition hit) {
      if((MCH_Config.PlaneMouseAimReticleDebug.prmBool || MCH_Config.DebugFlightControl.prmBool) && this.ccipCalibration != null) {
         Vec3 real = hit != null && hit.hitVec != null ? hit.hitVec : Vec3.createVectorHelper(super.posX, super.posY, super.posZ);
         Vec3 predicted = this.ccipCalibration.impact;
         double dx = predicted != null ? real.xCoord - predicted.xCoord : 0.0D;
         double dy = predicted != null ? real.yCoord - predicted.yCoord : 0.0D;
         double dz = predicted != null ? real.zCoord - predicted.zCoord : 0.0D;
         double horizontal = Math.sqrt(dx * dx + dz * dz);
         double total = Math.sqrt(dx * dx + dy * dy + dz * dz);
         double vx = this.ccipAircraftMotion != null ? this.ccipAircraftMotion.xCoord : 0.0D;
         double vz = this.ccipAircraftMotion != null ? this.ccipAircraftMotion.zCoord : 0.0D;
         double vh = Math.sqrt(vx * vx + vz * vz);
         double ahead = vh > 1.0E-7D ? (dx * vx + dz * vz) / vh : 0.0D;
         double right = vh > 1.0E-7D ? (dx * vz - dz * vx) / vh : 0.0D;
         Vec3 predVel = this.ccipCalibration.initialVelocity;
         double ivErr = predVel != null && this.ccipRealInitialMotion != null ? this.ccipRealInitialMotion.distanceTo(predVel) : 0.0D;
         double relErr = this.ccipCalibration.releasePos != null && this.ccipRealInitialPos != null ? this.ccipRealInitialPos.distanceTo(this.ccipCalibration.releasePos) : 0.0D;
         System.out.println(String.format("[CCIP_BOMB_CALIBRATION] weapon=%s predictedImpact=%s realImpact=%.6f,%.6f,%.6f horizontalError=%.6f verticalError=%.6f totalError=%.6f aheadBehindError=%.6f leftRightError=%.6f ticksPredicted=%d ticksReal=%d initialVelocityError=%.6f releasePositionError=%.6f realInitialPos=%s weaponReleaseParam=%s",
               this.getName(), formatVec(predicted), real.xCoord, real.yCoord, real.zCoord, horizontal, dy, total, ahead, right, Integer.valueOf(this.ccipCalibration.ticksSimulated), Integer.valueOf(this.getCountOnUpdate()), ivErr, relErr, formatVec(this.ccipRealInitialPos), formatVec(this.ccipWeaponReleaseParam)));
      }
   }

   private static Vec3 copyVec(Vec3 v) {
      return v != null ? Vec3.createVectorHelper(v.xCoord, v.yCoord, v.zCoord) : null;
   }

   private static String formatVec(Vec3 v) {
      return v != null ? String.format("%.6f,%.6f,%.6f", v.xCoord, v.yCoord, v.zCoord) : "-";
   }

   public void sprinkleBomblet() {
      if(!super.worldObj.isRemote) {
         MCH_EntityBomb e = new MCH_EntityBomb(super.worldObj, super.posX, super.posY, super.posZ, super.motionX, super.motionY, super.motionZ, (float)super.rand.nextInt(360), 0.0F, super.acceleration);
         e.setParameterFromWeapon(this, super.shootingAircraft, super.shootingEntity);
         e.setName(this.getName());
         float MOTION = 1.0F;
         float RANDOM = this.getInfo().bombletDiff;
         e.motionX = super.motionX * 1.0D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM);
         e.motionY = super.motionY * 1.0D / 2.0D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM / 2.0F);
         e.motionZ = super.motionZ * 1.0D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM);
         e.setBomblet();
         super.worldObj.spawnEntityInWorld(e);
      }

   }

   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.Bomb;
   }
}
