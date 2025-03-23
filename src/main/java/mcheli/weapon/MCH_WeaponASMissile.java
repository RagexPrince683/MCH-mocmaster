package mcheli.weapon;

import mcheli.MCH_Lib;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class MCH_WeaponASMissile extends MCH_WeaponBase {

   // 构造函数，初始化武器属性
   public MCH_WeaponASMissile(World world, Vec3 position, float yaw, float pitch, String name, MCH_WeaponInfo weaponInfo) {
      super(world, position, yaw, pitch, name, weaponInfo);
      this.acceleration = 3.0F;  // 加速度
      this.explosionPower = 9;   // 爆炸威力
      this.power = 40;           // 武器威力
      this.interval = -350;      // 射击间隔
      if (world.isRemote) {
         this.interval -= 10;     // 如果是客户端，减少射击间隔
      }
   }

   // 是否计入重新加载时间
   public boolean isCooldownCountReloadTime() {
      return true;
   }

   // 更新函数
   public void update(int countWait) {
      super.update(countWait);
   }

   // 射击函数
   public boolean shot(MCH_WeaponParam params) {

      float yaw = params.user.rotationYaw;  // 获取玩家的偏航角度
      float pitch = params.user.rotationPitch;  // 获取玩家的俯仰角度

      // 计算目标方向的三维坐标变化量
      double targetX = -MathHelper.sin(yaw / 180.0F * (float)Math.PI) * MathHelper.cos(pitch / 180.0F * (float)Math.PI);
      double targetZ = MathHelper.cos(yaw / 180.0F * (float)Math.PI) * MathHelper.cos(pitch / 180.0F * (float)Math.PI);
      double targetY = -MathHelper.sin(pitch / 180.0F * (float)Math.PI);

      // 计算方向的距离
      double dist = MathHelper.sqrt_double(targetX * targetX + targetY * targetY + targetZ * targetZ);
      double maxDist = 1500.0;
      double segmentLength = 100.0;  // 每段的长度
      int numSegments = (int) (maxDist / segmentLength);  // 计算需要的段数

      // 在客户端和服务器端都将目标方向进行归一化处理
      targetX = targetX * maxDist / dist;
      targetY = targetY * maxDist / dist;
      targetZ = targetZ * maxDist / dist;

      // 计算发射源
      Vec3 src = W_WorldFunc.getWorldVec3(this.worldObj, params.entity.posX, params.entity.posY + params.entity.getEyeHeight(), params.entity.posZ);

      // 射线检测
      MovingObjectPosition hitResult = null;

      for (int i = 1; i <= numSegments; i++) {
         // 计算当前分段的目标点，确保每段都从上一个段的终点开始
         Vec3 currentDst = W_WorldFunc.getWorldVec3(this.worldObj,
                 params.entity.posX + targetX * i / numSegments,
                 params.entity.posY + params.entity.getEyeHeight() + targetY * i / numSegments,
                 params.entity.posZ + targetZ * i / numSegments);

         // 执行射线检测
         List<MovingObjectPosition> hitResults = rayTraceAllBlocks(this.worldObj, src, currentDst, false, true, true);

         if (hitResults != null && !hitResults.isEmpty()) {
            hitResult = hitResults.get(0);
            break;  // 找到碰撞结果后，退出循环
         }

         // 更新src为当前检测的dst
         src = currentDst;  // 当前段的dst成为下一段的src
      }

      // 如果没有检测到碰撞，则返回默认的目标位置
      if (hitResult == null) {
         hitResult = new MovingObjectPosition(null, src.addVector(targetX, targetY, targetZ));  // 使用目标点作为默认值
      }

      // 如果射线击中有效方块并且不是水中方块
      if (!this.worldObj.isRemote) {
         // 创建导弹实体并设置参数
         MCH_EntityASMissile missile = new MCH_EntityASMissile(this.worldObj, params.posX, params.posY, params.posZ, targetX, targetY, targetZ, yaw, pitch, this.acceleration);
         missile.setName(this.name);
         missile.setParameterFromWeapon(this, params.entity, params.user);

         // 设置导弹的目标位置
         missile.targetPosX = hitResult.hitVec.xCoord;
         missile.targetPosY = hitResult.hitVec.yCoord;
         missile.targetPosZ = hitResult.hitVec.zCoord;

         // 将导弹添加到世界中
         this.worldObj.spawnEntityInWorld(missile);

         // 播放武器发射声音
         playSound(params.entity);
      }
      return true;  // 命中并成功发射导弹
   }


   public static ArrayList<MovingObjectPosition> rayTraceAllBlocks(World world, Vec3 p_147447_1_, Vec3 p_147447_2_, boolean p_147447_3_, boolean p_147447_4_, boolean p_147447_5_) {
      if (!Double.isNaN(p_147447_1_.xCoord) && !Double.isNaN(p_147447_1_.yCoord) && !Double.isNaN(p_147447_1_.zCoord)) {
         if (!Double.isNaN(p_147447_2_.xCoord) && !Double.isNaN(p_147447_2_.yCoord) && !Double.isNaN(p_147447_2_.zCoord)) {
            int i = MathHelper.floor_double(p_147447_2_.xCoord);
            int j = MathHelper.floor_double(p_147447_2_.yCoord);
            int k = MathHelper.floor_double(p_147447_2_.zCoord);
            int l = MathHelper.floor_double(p_147447_1_.xCoord);
            int i1 = MathHelper.floor_double(p_147447_1_.yCoord);
            int j1 = MathHelper.floor_double(p_147447_1_.zCoord);
            Block block = world.getBlock(l, i1, j1);
            int k1 = world.getBlockMetadata(l, i1, j1);

            ArrayList<MovingObjectPosition> rayTraceHits = new ArrayList<>();
            k1 = 200;

            outerLoop:
            while (k1-- >= 0) {
               if (Double.isNaN(p_147447_1_.xCoord) || Double.isNaN(p_147447_1_.yCoord) || Double.isNaN(p_147447_1_.zCoord)) {
                  return null;
               }

               if (l == i && i1 == j && j1 == k) {
                  return rayTraceHits;
               }

               boolean flag6 = true;
               boolean flag3 = true;
               boolean flag4 = true;
               double d0 = 999.0D;
               double d1 = 999.0D;
               double d2 = 999.0D;

               if (i > l) {
                  d0 = (double) l + 1.0D;
               } else if (i < l) {
                  d0 = (double) l + 0.0D;
               } else {
                  flag6 = false;
               }

               if (j > i1) {
                  d1 = (double) i1 + 1.0D;
               } else if (j < i1) {
                  d1 = (double) i1 + 0.0D;
               } else {
                  flag3 = false;
               }

               if (k > j1) {
                  d2 = (double) j1 + 1.0D;
               } else if (k < j1) {
                  d2 = (double) j1 + 0.0D;
               } else {
                  flag4 = false;
               }

               double d3 = 999.0D;
               double d4 = 999.0D;
               double d5 = 999.0D;
               double d6 = p_147447_2_.xCoord - p_147447_1_.xCoord;
               double d7 = p_147447_2_.yCoord - p_147447_1_.yCoord;
               double d8 = p_147447_2_.zCoord - p_147447_1_.zCoord;

               if (flag6) {
                  d3 = (d0 - p_147447_1_.xCoord) / d6;
               }

               if (flag3) {
                  d4 = (d1 - p_147447_1_.yCoord) / d7;
               }

               if (flag4) {
                  d5 = (d2 - p_147447_1_.zCoord) / d8;
               }

               boolean flag5 = false;
               byte b0;

               if (d3 < d4 && d3 < d5) {
                  if (i > l) {
                     b0 = 4;
                  } else {
                     b0 = 5;
                  }

                  p_147447_1_.xCoord = d0;
                  p_147447_1_.yCoord += d7 * d3;
                  p_147447_1_.zCoord += d8 * d3;
               } else if (d4 < d5) {
                  if (j > i1) {
                     b0 = 0;
                  } else {
                     b0 = 1;
                  }

                  p_147447_1_.xCoord += d6 * d4;
                  p_147447_1_.yCoord = d1;
                  p_147447_1_.zCoord += d8 * d4;
               } else {
                  if (k > j1) {
                     b0 = 2;
                  } else {
                     b0 = 3;
                  }

                  p_147447_1_.xCoord += d6 * d5;
                  p_147447_1_.yCoord += d7 * d5;
                  p_147447_1_.zCoord = d2;
               }

               Vec3 vec32 = Vec3.createVectorHelper(p_147447_1_.xCoord, p_147447_1_.yCoord, p_147447_1_.zCoord);
               l = (int) (vec32.xCoord = (double) MathHelper.floor_double(p_147447_1_.xCoord));

               if (b0 == 5) {
                  --l;
                  ++vec32.xCoord;
               }

               i1 = (int) (vec32.yCoord = (double) MathHelper.floor_double(p_147447_1_.yCoord));

               if (b0 == 1) {
                  --i1;
                  ++vec32.yCoord;
               }

               j1 = (int) (vec32.zCoord = (double) MathHelper.floor_double(p_147447_1_.zCoord));

               if (b0 == 3) {
                  --j1;
                  ++vec32.zCoord;
               }

               for (MovingObjectPosition mOP : rayTraceHits) {
                  if (mOP.blockX == l && mOP.blockY == i1 && mOP.blockZ == j1) continue outerLoop;
               }

               Block block1 = world.getBlock(l, i1, j1);
               int l1 = world.getBlockMetadata(l, i1, j1);

               if (!p_147447_4_ || block1.getCollisionBoundingBoxFromPool(world, l, i1, j1) != null) {
                  if (block1.canCollideCheck(l1, p_147447_3_)) {
                     MovingObjectPosition movingobjectposition1 = block1.collisionRayTrace(world, l, i1, j1, p_147447_1_, p_147447_2_);

                     if (movingobjectposition1 != null) {
                        rayTraceHits.add(movingobjectposition1);
                     }
                  }
               }
            }
            return rayTraceHits;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }
}
