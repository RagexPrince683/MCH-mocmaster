package mcheli.tank;

import java.util.List;
import java.util.Random;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.tank.MCH_EntityWheel;
import mcheli.wrapper.W_Block;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WheelManager {

   public final MCH_EntityAircraft parent;
   public MCH_EntityWheel[] wheels;
   private double minZ;
   private double maxZ;
   private double avgZ;
   public Vec3 weightedCenter;
   public float targetPitch;
   public float targetRoll;
   public float prevYaw;
   private static Random rand = new Random();


   public MCH_WheelManager(MCH_EntityAircraft ac) {
      this.parent = ac;
      this.wheels = new MCH_EntityWheel[0];
      this.weightedCenter = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
   }

   public void createWheels(World w, List list, Vec3 weightedCenter) {
      this.wheels = new MCH_EntityWheel[list.size() * 2];
      this.minZ = 999999.0D;
      this.maxZ = -999999.0D;
      this.weightedCenter = weightedCenter;

      for(int i = 0; i < this.wheels.length; ++i) {
         MCH_EntityWheel wheel = new MCH_EntityWheel(w);
         wheel.setParents(this.parent);
         Vec3 wp = ((MCH_AircraftInfo.Wheel)list.get(i / 2)).pos;
         wheel.setWheelPos(Vec3.createVectorHelper(i % 2 == 0?wp.xCoord:-wp.xCoord, wp.yCoord, wp.zCoord), this.weightedCenter);
         Vec3 v = this.parent.getTransformedPosition(wheel.pos.xCoord, wheel.pos.yCoord, wheel.pos.zCoord);
         wheel.setLocationAndAngles(v.xCoord, v.yCoord + 1.0D, v.zCoord, 0.0F, 0.0F);
         this.wheels[i] = wheel;
         if(wheel.pos.zCoord <= this.minZ) {
            this.minZ = wheel.pos.zCoord;
         }

         if(wheel.pos.zCoord >= this.maxZ) {
            this.maxZ = wheel.pos.zCoord;
         }
      }

      this.avgZ = this.maxZ - this.minZ;
   }

   public void move(double x, double y, double z) {
      MCH_EntityAircraft ac = this.parent;
      if (ac.getAcInfo() == null) return;

      // --- store previous wheel positions & compute motion ---
      for (MCH_EntityWheel w : this.wheels) {
         if (w == null) continue;
         w.prevPosX = w.posX;
         w.prevPosY = w.posY;
         w.prevPosZ = w.posZ;

         Vec3 worldPos = ac.getTransformedPosition(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord);
         w.motionX = worldPos.xCoord - w.posX + x;
         w.motionY = worldPos.yCoord - w.posY;
         w.motionZ = worldPos.zCoord - w.posZ + z;
      }

      // --- move wheels with damped vertical motion ---
      for (MCH_EntityWheel w : this.wheels) {
         if (w == null) continue;
         w.motionY *= 0.15D; // soft vertical damping
         w.moveEntity(w.motionX, w.motionY, w.motionZ);
         // removed fixed downward movement
      }

      // --- detect wheel contact consistency ---
      int half = this.wheels.length / 2;
      for (int i = 0; i < half; i++) {
         MCH_EntityWheel w1 = this.wheels[i*2];
         MCH_EntityWheel w2 = this.wheels[i*2 +1];
         if ((w1.onGround || w2.onGround)) {
            w1.onGround = true;
            w2.onGround = true;
         }
      }

      // --- bump detection (ignore at very low speed to prevent jitter) ---
      boolean bumpDetected = false;
      double horizSpeed = Math.sqrt(ac.motionX*ac.motionX + ac.motionZ*ac.motionZ);
      if (horizSpeed > 0.01D && this.wheels.length > 0) {
         double minY = Double.POSITIVE_INFINITY;
         double maxY = Double.NEGATIVE_INFINITY;
         double threshold = Math.max(0.12D, horizSpeed*0.02D);
         for (MCH_EntityWheel w : this.wheels) {
            if (w == null) continue;
            if (Math.abs(w.posY - w.prevPosY) > threshold || Math.abs(w.motionY) > threshold) {
               bumpDetected = true;
               break;
            }
            minY = Math.min(minY, w.posY);
            maxY = Math.max(maxY, w.posY);
         }
         if (!bumpDetected && maxY - minY > Math.max(0.18D, horizSpeed*0.03D)) bumpDetected = true;
      }

      // --- weighted center / rotation (soft, speed-limited) ---
      if (bumpDetected || (!ac.onGround && MCH_Lib.getBlockIdY(ac, 1, -2) <= 0)) {
         Vec3 center = ac.getTransformedPosition(this.weightedCenter);
         center.xCoord -= ac.posX;
         center.yCoord = this.weightedCenter.yCoord;
         center.zCoord -= ac.posZ;

         Vec3 influenceVec = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);

         for (int i = 0; i < half; i++) {
            MCH_EntityWheel w1 = this.wheels[i*2];
            MCH_EntityWheel w2 = this.wheels[i*2 +1];
            Vec3 diff1 = Vec3.createVectorHelper(w1.posX-(ac.posX+center.xCoord),
                    w1.posY-(ac.posY+center.yCoord),
                    w1.posZ-(ac.posZ+center.zCoord));
            Vec3 diff2 = Vec3.createVectorHelper(w2.posX-(ac.posX+center.xCoord),
                    w2.posY-(ac.posY+center.yCoord),
                    w2.posZ-(ac.posZ+center.zCoord));
            Vec3 cross = w1.pos.zCoord>=0.0D ? diff2.crossProduct(diff1) : diff1.crossProduct(diff2);
            cross = cross.normalize();
            double factor = (!w1.onGround && !w2.onGround) ? 0.0D : Math.abs(w1.pos.zCoord / this.avgZ);
            influenceVec.xCoord += cross.xCoord * factor;
            influenceVec.yCoord += cross.yCoord * factor;
            influenceVec.zCoord += cross.zCoord * factor;
         }

         influenceVec = influenceVec.normalize();
         influenceVec.rotateAroundY((float)(ac.getRotYaw()*Math.PI/180.0D));

         // candidate rotation
         float candPitch = (float)(90.0D - Math.atan2(influenceVec.yCoord, influenceVec.zCoord)*180.0D/Math.PI);
         float candRoll  = -((float)(90.0D - Math.atan2(influenceVec.yCoord, influenceVec.xCoord)*180.0D/Math.PI));

         // clamp rotation delta and absolute angle to prevent flips
         float maxDelta = 2.5F; // max change per tick
         candPitch = MathHelper.clamp_float(candPitch, ac.getRotPitch()-maxDelta, ac.getRotPitch()+maxDelta);
         candRoll  = MathHelper.clamp_float(candRoll,  ac.getRotRoll()-maxDelta,  ac.getRotRoll()+maxDelta);

         float maxAngle = 15.0F; // absolute max pitch/roll
         candPitch = MathHelper.clamp_float(candPitch, -maxAngle, maxAngle);
         candRoll  = MathHelper.clamp_float(candRoll,  -maxAngle, maxAngle);

         // smooth apply
         float smooth = (float) (0.4F + 0.6F * Math.min(1.0F, 0.5F/horizSpeed)); // less effect at high speed
         this.targetPitch = ac.getRotPitch() + (candPitch - ac.getRotPitch())*smooth;
         this.targetRoll  = ac.getRotRoll()  + (candRoll  - ac.getRotRoll())*smooth;

         if (!W_Lib.isClientPlayer(ac.getRiddenByEntity())) {
            ac.setRotPitch(this.targetPitch);
            ac.setRotRoll(this.targetRoll);
         }
      } else {
         // smooth leveling when no bump
         float smoothFactor = 0.85F;
         this.targetPitch *= smoothFactor;
         this.targetRoll  *= smoothFactor;
         if (Math.abs(this.targetPitch) < 0.2F) this.targetPitch = 0.0F;
         if (Math.abs(this.targetRoll)  < 0.2F) this.targetRoll  = 0.0F;
         if (!W_Lib.isClientPlayer(ac.getRiddenByEntity())) {
            ac.setRotPitch(this.targetPitch);
            ac.setRotRoll(this.targetRoll);
         }
      }

      // --- final wheel positioning (clamped) ---
      for (MCH_EntityWheel w : this.wheels) {
         if (w == null) continue;
         Vec3 v = this.getTransformedPosition(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord,
                 ac, ac.getRotYaw(), this.targetPitch, this.targetRoll);
         double rangeH = 2.0D;
         double poy = w.stepHeight/2.0;
         w.posX = MathHelper.clamp_double(w.posX, v.xCoord-rangeH, v.xCoord+rangeH);
         w.posZ = MathHelper.clamp_double(w.posZ, v.zCoord-rangeH, v.zCoord+rangeH);
         w.posY = v.yCoord + poy;
         w.setPositionAndRotation(w.posX, w.posY, w.posZ, 0.0F, 0.0F);
      }
   }



   public Vec3 getTransformedPosition(double x, double y, double z, MCH_EntityAircraft ac, float yaw, float pitch, float roll) {
      Vec3 v = MCH_Lib.RotVec3(x, y, z, -yaw, -pitch, -roll);
      return v.addVector(ac.posX, ac.posY, ac.posZ);
   }

   public void updateBlock() {
      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.Collision_DestroyBlock.prmBool) {
         MCH_EntityAircraft ac = this.parent;
         MCH_EntityWheel[] arr$ = this.wheels;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_EntityWheel w = arr$[i$];
            Vec3 v = ac.getTransformedPosition(w.pos);
            int x = (int)(v.xCoord + 0.5D);
            int y = (int)(v.yCoord + 0.5D);
            int z = (int)(v.zCoord + 0.5D);
            Block block = ac.worldObj.getBlock(x, y, z);
            if(block == W_Block.getSnowLayer()) {
               ac.worldObj.setBlockToAir(x, y, z);
            }

            if(block == Blocks.waterlily || block == Blocks.cake) {
               W_WorldFunc.destroyBlock(ac.worldObj, x, y, z, false);
            }
         }

      }
   }

   public void particleLandingGear() {
      if(this.wheels.length > 0) {
         MCH_EntityAircraft ac = this.parent;
         double d = ac.motionX * ac.motionX + ac.motionZ * ac.motionZ + (double)Math.abs(this.prevYaw - ac.getRotYaw());
         this.prevYaw = ac.getRotYaw();
         if(d > 0.001D) {
            for(int i = 0; i < 2; ++i) {
               MCH_EntityWheel w = this.wheels[rand.nextInt(this.wheels.length)];
               Vec3 v = ac.getTransformedPosition(w.pos);
               int x = MathHelper.floor_double(v.xCoord + 0.5D);
               int y = MathHelper.floor_double(v.yCoord - 0.5D);
               int z = MathHelper.floor_double(v.zCoord + 0.5D);
               Block block = ac.worldObj.getBlock(x, y, z);
               if(Block.isEqualTo(block, Blocks.air)) {
                  y = MathHelper.floor_double(v.yCoord + 0.5D);
                  block = ac.worldObj.getBlock(x, y, z);
               }

               if(!Block.isEqualTo(block, Blocks.air)) {
                  MCH_ParticlesUtil.spawnParticleTileCrack(ac.worldObj, x, y, z, v.xCoord + ((double)rand.nextFloat() - 0.5D), v.yCoord + 0.1D, v.zCoord + ((double)rand.nextFloat() - 0.5D), -ac.motionX * 4.0D + ((double)rand.nextFloat() - 0.5D) * 0.1D, (double)rand.nextFloat() * 0.5D, -ac.motionZ * 4.0D + ((double)rand.nextFloat() - 0.5D) * 0.1D);
               }
            }
         }

      }
   }

}
