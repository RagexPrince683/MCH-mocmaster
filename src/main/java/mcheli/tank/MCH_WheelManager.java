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
      if(ac.getAcInfo() != null) {
         boolean showLog = ac.ticksExisted % 1 == 1;
         if(showLog) {
            MCH_Lib.DbgLog(ac.worldObj, "[" + (ac.worldObj.isRemote?"Client":"Server") + "] ==============================", new Object[0]);
         }

         MCH_EntityWheel[] zmog = this.wheels;
         int rv = zmog.length;

         int wc;
         MCH_EntityWheel pitch;
         for(wc = 0; wc < rv; ++wc) {
            pitch = zmog[wc];
            pitch.prevPosX = pitch.posX;
            pitch.prevPosY = pitch.posY;
            pitch.prevPosZ = pitch.posZ;
            Vec3 roll = ac.getTransformedPosition(pitch.pos.xCoord, pitch.pos.yCoord, pitch.pos.zCoord);
            pitch.motionX = roll.xCoord - pitch.posX + x;
            pitch.motionY = roll.yCoord - pitch.posY;
            pitch.motionZ = roll.zCoord - pitch.posZ + z;
         }

         zmog = this.wheels;
         rv = zmog.length;

         for(wc = 0; wc < rv; ++wc) {
            pitch = zmog[wc];
            // make wheel vertical responsiveness scale with vehicle horizontal speed
            double horizSpeed = Math.sqrt(ac.motionX * ac.motionX + ac.motionZ * ac.motionZ);

            // base responsiveness (original 0.15) plus scaling factor
            double wheelVertResp = 0.15D + Math.min(0.85D, horizSpeed * 0.12D); // clamps to [0.15,1.0]
            // If you're using MPH mapping where 1.8 ~ 180MPH, this will raise responsiveness above ~0.5 at very high speed

            pitch.motionY *= wheelVertResp;
            pitch.moveEntity(pitch.motionX, pitch.motionY, pitch.motionZ);
            // move wheel according to computed motion
            pitch.moveEntity(pitch.motionX, pitch.motionY, pitch.motionZ);

            // small soft downward bias only when wheel is not contacting ground AND speed is low
            double softDown = 0.02D; // small bias to settle wheels when slow
            double speedThreshold = 1.80D; // 1.8 => 180MPH in your units
            if (!pitch.onGround) {
               if (horizSpeed < speedThreshold) {
                  // at low speeds we keep a small settle push
                  pitch.moveEntity(0.0D, -softDown, 0.0D);
               } else {
                  // at high speed avoid extra push — it causes digging
                  // but apply a tiny corrective downward nudge if wheel is far above expected pos
                  // (no unconditional downward push)
                  double diff = (ac.posY + this.weightedCenter.yCoord) - pitch.posY;
                  if (diff > 1.0D) {
                     pitch.moveEntity(0.0D, -0.01D, 0.0D);
                  }
               }
            }
         }

         int var28 = -1;

         MCH_EntityWheel var30;
         for(rv = 0; rv < this.wheels.length / 2; ++rv) {
            var28 = rv;
            var30 = this.wheels[rv * 2 + 0];
            pitch = this.wheels[rv * 2 + 1];
            if(!var30.isPlus && (var30.onGround || pitch.onGround)) {
               var28 = -1;
               break;
            }
         }

         if(var28 >= 0) {
            this.wheels[var28 * 2 + 0].onGround = true;
            this.wheels[var28 * 2 + 1].onGround = true;
         }

         var28 = -1;

         for(rv = this.wheels.length / 2 - 1; rv >= 0; --rv) {
            var28 = rv;
            var30 = this.wheels[rv * 2 + 0];
            pitch = this.wheels[rv * 2 + 1];
            if(var30.isPlus && (var30.onGround || pitch.onGround)) {
               var28 = -1;
               break;
            }
         }

         if(var28 >= 0) {
            this.wheels[var28 * 2 + 0].onGround = true;
            this.wheels[var28 * 2 + 1].onGround = true;
         }

         Vec3 var29 = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
         Vec3 var31 = ac.getTransformedPosition(this.weightedCenter);
         var31.xCoord -= ac.posX;
         var31.yCoord = this.weightedCenter.yCoord;
         var31.zCoord -= ac.posZ;

         for(int var33 = 0; var33 < this.wheels.length / 2; ++var33) {
            MCH_EntityWheel var34 = this.wheels[var33 * 2 + 0];
            MCH_EntityWheel ogpf = this.wheels[var33 * 2 + 1];
            Vec3 ogrf = Vec3.createVectorHelper(var34.posX - (ac.posX + var31.xCoord), var34.posY - (ac.posY + var31.yCoord), var34.posZ - (ac.posZ + var31.zCoord));
            Vec3 arr$ = Vec3.createVectorHelper(ogpf.posX - (ac.posX + var31.xCoord), ogpf.posY - (ac.posY + var31.yCoord), ogpf.posZ - (ac.posZ + var31.zCoord));
            Vec3 len$ = var34.pos.zCoord >= 0.0D?arr$.crossProduct(ogrf):ogrf.crossProduct(arr$);
            len$ = len$.normalize();
            double i$ = Math.abs(var34.pos.zCoord / this.avgZ);
            if(!var34.onGround && !ogpf.onGround) {
               i$ = 0.0D;
            }

            var29.xCoord += len$.xCoord * i$;
            var29.yCoord += len$.yCoord * i$;
            var29.zCoord += len$.zCoord * i$;
            if(showLog) {
               len$.rotateAroundY((float)((double)ac.getRotYaw() * 3.141592653589793D / 180.0D));
               MCH_Lib.DbgLog(ac.worldObj, "%2d : %.2f :[%+.1f, %+.1f, %+.1f][%s %d %d][%+.2f(%+.2f), %+.2f(%+.2f)][%+.1f, %+.1f, %+.1f]", new Object[]{Integer.valueOf(var33), Double.valueOf(i$), Double.valueOf(len$.xCoord), Double.valueOf(len$.yCoord), Double.valueOf(len$.zCoord), var34.isPlus?"+":"-", Integer.valueOf(var34.onGround?1:0), Integer.valueOf(ogpf.onGround?1:0), Double.valueOf(var34.posY - var34.prevPosY), Double.valueOf(var34.motionY), Double.valueOf(ogpf.posY - ogpf.prevPosY), Double.valueOf(ogpf.motionY), Double.valueOf(len$.xCoord), Double.valueOf(len$.yCoord), Double.valueOf(len$.zCoord)});
            }
         }

         var29 = var29.normalize();
         if(var29.yCoord > 0.01D && var29.yCoord < 0.7D) {
            ac.motionX += var29.xCoord / 50.0D;
            ac.motionZ += var29.zCoord / 50.0D;
         }

         var29.rotateAroundY((float)((double)ac.getRotYaw() * 3.141592653589793D / 180.0D));
         float var35 = (float)(90.0D - Math.atan2(var29.yCoord, var29.zCoord) * 180.0D / 3.141592653589793D);
         float var36 = -((float)(90.0D - Math.atan2(var29.yCoord, var29.xCoord) * 180.0D / 3.141592653589793D));
         float var37 = ac.getAcInfo().onGroundPitchFactor;
         if(var35 - ac.getRotPitch() > var37) {
            var35 = ac.getRotPitch() + var37;
         }

         if(var35 - ac.getRotPitch() < -var37) {
            var35 = ac.getRotPitch() - var37;
         }

         float var38 = ac.getAcInfo().onGroundRollFactor;
         if(var36 - ac.getRotRoll() > var38) {
            var36 = ac.getRotRoll() + var38;
         }

         if(var36 - ac.getRotRoll() < -var38) {
            var36 = ac.getRotRoll() - var38;
         }

         this.targetPitch = var35;
         this.targetRoll = var36;
         if(!W_Lib.isClientPlayer(ac.getRiddenByEntity())) {
            ac.setRotPitch(var35);
            ac.setRotRoll(var36);
         }

         if(showLog) {
            MCH_Lib.DbgLog(ac.worldObj, "%+03d, %+03d :[%.2f, %.2f, %.2f] yaw=%.2f, pitch=%.2f, roll=%.2f", new Object[]{Integer.valueOf((int)var35), Integer.valueOf((int)var36), Double.valueOf(var29.xCoord), Double.valueOf(var29.yCoord), Double.valueOf(var29.zCoord), Float.valueOf(ac.getRotYaw()), Float.valueOf(this.targetPitch), Float.valueOf(this.targetRoll)});
         }

         MCH_EntityWheel[] var39 = this.wheels;
         int var40 = var39.length;

         for(int var41 = 0; var41 < var40; ++var41) {
            MCH_EntityWheel wheel = var39[var41];
            Vec3 v = this.getTransformedPosition(wheel.pos.xCoord, wheel.pos.yCoord, wheel.pos.zCoord, ac, ac.getRotYaw(), this.targetPitch, this.targetRoll);
            double offset = wheel.onGround?0.01D:-0.0D;
            double rangeH = 2.0D;
            double poy = (double)(wheel.stepHeight / 2.0F);
            int b = 0;
            if(wheel.posX > v.xCoord + rangeH) {
               wheel.posX = v.xCoord + rangeH;
               wheel.posY = v.yCoord + poy;
               b |= 1;
            }

            if(wheel.posX < v.xCoord - rangeH) {
               wheel.posX = v.xCoord - rangeH;
               wheel.posY = v.yCoord + poy;
               b |= 2;
            }

            if(wheel.posZ > v.zCoord + rangeH) {
               wheel.posZ = v.zCoord + rangeH;
               wheel.posY = v.yCoord + poy;
               b |= 4;
            }

            if(wheel.posZ < v.zCoord - rangeH) {
               wheel.posZ = v.zCoord - rangeH;
               wheel.posY = v.yCoord + poy;
               b |= 8;
            }

            wheel.setPositionAndRotation(wheel.posX, wheel.posY, wheel.posZ, 0.0F, 0.0F);

            // ---- clamp wheel Y to terrain to avoid penetration (drop-in) ----
            try {
               // check a few blocks under wheel to find the topmost non-air surface
               int wx = MathHelper.floor_double(wheel.posX + 0.5D);
               int wz = MathHelper.floor_double(wheel.posZ + 0.5D);

               // start search from expected position v.yCoord (we still have 'v' earlier in loop)
               // fallback to wheel.posY if v is not in scope: use wheel.posY
               int startY = MathHelper.floor_double(wheel.posY + 1.0D);
               int groundY = Integer.MIN_VALUE;
               // search downward up to 6 blocks for a block that's not air
               for (int yy = startY; yy >= startY - 6; yy--) {
                  Block bl = ac.worldObj.getBlock(wx, yy, wz);
                  if (bl != Blocks.air && bl != W_Block.getSnowLayer()) { // treat snow layer special if needed
                     groundY = yy;
                     break;
                  }
               }

               if (groundY != Integer.MIN_VALUE) {
                  double minAllowedY = groundY + 0.5D; // lift wheels slightly above block top
                  if (wheel.posY < minAllowedY) {
                     wheel.posY = minAllowedY;
                     wheel.setPositionAndRotation(wheel.posX, wheel.posY, wheel.posZ, 0.0F, 0.0F);
                     // zero vertical motion to avoid oscillations
                     wheel.motionY = 0.0D;
                     wheel.prevPosY = wheel.posY;
                  }
               }
            } catch (Exception e) {
               // protective: don't crash on any unexpected NPE in world queries
            }
         }

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
