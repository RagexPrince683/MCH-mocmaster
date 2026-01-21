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

      // --- store previous wheel positions & compute wheel motion ---
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

      // --- move wheels ---
      for (MCH_EntityWheel w : this.wheels) {
         if (w == null) continue;
         w.motionY *= 0.15D; // damp vertical motion
         w.moveEntity(w.motionX, w.motionY, w.motionZ);
         // remove fixed downward move, let physics naturally keep wheels on ground
      }

      // --- detect wheel contact consistency ---
      int half = this.wheels.length / 2;
      for (int i = 0; i < half; i++) {
         MCH_EntityWheel w1 = this.wheels[i * 2];
         MCH_EntityWheel w2 = this.wheels[i * 2 + 1];
         if (!w1.isPlus && (w1.onGround || w2.onGround)) {
            w1.onGround = true;
            w2.onGround = true;
         }
         if (w1.isPlus && (w1.onGround || w2.onGround)) {
            w1.onGround = true;
            w2.onGround = true;
         }
      }

      // --- bump detection ---
      boolean bumpDetected = false;
      double horizSpeed = Math.sqrt(ac.motionX*ac.motionX + ac.motionZ*ac.motionZ);
      if (this.wheels.length > 0) {
         double minY = Double.POSITIVE_INFINITY;
         double maxY = Double.NEGATIVE_INFINITY;
         double threshold = Math.max(0.12D, horizSpeed*0.02D); // scale with speed
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

      // --- weighted center / rotation ---
      if ((!ac.onGround && MCH_Lib.getBlockIdY(ac, 1, -2) <= 0) || bumpDetected) {
         Vec3 var29 = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
         Vec3 center = ac.getTransformedPosition(this.weightedCenter);
         center.xCoord -= ac.posX;
         center.yCoord = this.weightedCenter.yCoord;
         center.zCoord -= ac.posZ;

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
            double influence = Math.abs(w1.pos.zCoord / this.avgZ);
            if (!w1.onGround && !w2.onGround) influence = 0.0D;
            var29.xCoord += cross.xCoord * influence;
            var29.yCoord += cross.yCoord * influence;
            var29.zCoord += cross.zCoord * influence;
         }

         var29 = var29.normalize();
         var29.rotateAroundY((float)((double)ac.getRotYaw()*Math.PI/180.0D));
         float candidatePitch = (float)(90.0D - Math.atan2(var29.yCoord, var29.zCoord)*180.0D/Math.PI);
         float candidateRoll  = -((float)(90.0D - Math.atan2(var29.yCoord, var29.xCoord)*180.0D/Math.PI));

         // clamp per-tick change
         float maxPitchDelta = ac.getAcInfo().onGroundPitchFactor;
         float maxRollDelta  = ac.getAcInfo().onGroundRollFactor;
         candidatePitch = MathHelper.clamp_float(candidatePitch, ac.getRotPitch()-maxPitchDelta, ac.getRotPitch()+maxPitchDelta);
         candidateRoll  = MathHelper.clamp_float(candidateRoll, ac.getRotRoll()-maxRollDelta, ac.getRotRoll()+maxRollDelta);

         // smooth rotation application
         float smoothing = 0.6F;
         this.targetPitch = ac.getRotPitch() + (candidatePitch - ac.getRotPitch())*smoothing;
         this.targetRoll  = ac.getRotRoll()  + (candidateRoll  - ac.getRotRoll())*smoothing;

         if (!W_Lib.isClientPlayer(ac.getRiddenByEntity())) {
            ac.setRotPitch(this.targetPitch);
            ac.setRotRoll(this.targetRoll);
         }
      } else {
         // no bump & on ground: smooth level
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

      // --- final wheel positioning ---
      for (MCH_EntityWheel w : this.wheels) {
         if (w == null) continue;
         Vec3 v = this.getTransformedPosition(w.pos.xCoord, w.pos.yCoord, w.pos.zCoord, ac, ac.getRotYaw(), this.targetPitch, this.targetRoll);
         double offset = w.onGround ? 0.01D : 0.0D;
         double rangeH = 2.0D;
         double poy = (double)(w.stepHeight / 2.0F);
         if (w.posX > v.xCoord + rangeH) { w.posX = v.xCoord + rangeH; w.posY = v.yCoord + poy; }
         if (w.posX < v.xCoord - rangeH) { w.posX = v.xCoord - rangeH; w.posY = v.yCoord + poy; }
         if (w.posZ > v.zCoord + rangeH) { w.posZ = v.zCoord + rangeH; w.posY = v.yCoord + poy; }
         if (w.posZ < v.zCoord - rangeH) { w.posZ = v.zCoord - rangeH; w.posY = v.yCoord + poy; }
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
