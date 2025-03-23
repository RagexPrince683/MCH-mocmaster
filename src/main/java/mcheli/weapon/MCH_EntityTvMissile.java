package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vector.Vector3f;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

import static mcheli.weapon.MCH_WeaponASMissile.rayTraceAllBlocks;

public class MCH_EntityTvMissile extends MCH_EntityBaseBullet {

    public boolean isSpawnParticle = true;

    //public static boolean isTVMissile = true;

    public double targetPosX;
    public double targetPosY;
    public double targetPosZ;

    public MCH_EntityTvMissile(World par1World) {
        super(par1World);
    }

    public MCH_EntityTvMissile(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
        super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
    }

    public void setMotion(double targetX, double targetY, double targetZ) {
        double d6 = (double)MathHelper.sqrt_double(targetX * targetX + targetY * targetY + targetZ * targetZ);
        super.motionX = targetX * this.acceleration / d6;
        super.motionY = targetY * this.acceleration / d6;
        super.motionZ = targetZ * this.acceleration / d6;
    }

    public void onUpdate() {
        super.onUpdate();
        this.onUpdateBomblet();
        if (this.isSpawnParticle && this.getInfo() != null && !this.getInfo().disableSmoke) {
            this.spawnExplosionParticle(this.getInfo().trajectoryParticleName, 3, 5.0F * this.getInfo().smokeSize * 0.5F);
        }

        if (super.shootingEntity != null) {
            double x = super.posX - super.shootingEntity.posX;
            double y = super.posY - super.shootingEntity.posY;
            double z = super.posZ - super.shootingEntity.posZ;
            if (x * x + y * y + z * z > 1440000.0D) {
                this.setDead();
            }

            if (!super.worldObj.isRemote && !super.isDead) {
                this.onUpdateMotion();
            }
        } else if (!super.worldObj.isRemote) {
            this.setDead();
        }

    }

    public void onUpdateMotion() {
        Entity e = super.shootingEntity;

        //拖线制导
        if (!getInfo().laserGuidance) {
            if (e != null && !e.isDead) {
                MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(e);
                if (ac != null && ac.getTVMissile() == this) {
                    float yaw = e.rotationYaw;
                    float pitch = e.rotationPitch;
                    double tX = -MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                    double tZ = MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
                    double tY = -MathHelper.sin(pitch / 180.0F * 3.1415927F);
                    this.setMotion(tX, tY, tZ);
                    this.setRotation(yaw, pitch);
                }
            }
        }

        //激光制导
        else {

            MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(e);
            if(ac != null && ac.getCurrentWeapon(e).getCurrentWeapon() instanceof MCH_WeaponTvMissile) {
                MCH_WeaponTvMissile weaponTvMissile = (MCH_WeaponTvMissile) ac.getCurrentWeapon(e).getCurrentWeapon();
                if(weaponTvMissile.guidanceSystem != null && !weaponTvMissile.guidanceSystem.targeting) {
                    return;
                }
            }

            float yaw;
            float pitch;

            if (getInfo().hasLaserGuidancePod) {
                yaw = e.rotationYaw;  // 获取玩家的偏航角度
                pitch = e.rotationPitch;  // 获取玩家的俯仰角度
            } else {
//                MCH_EntityAircraft ac = null; //玩家乘坐的实体
//                if(e.ridingEntity instanceof MCH_EntityAircraft) {
//                    ac = (MCH_EntityAircraft)e.ridingEntity;
//                } else if(e.ridingEntity instanceof MCH_EntitySeat) {
//                    ac = ((MCH_EntitySeat)e.ridingEntity).getParent();
//                } else if(e.ridingEntity instanceof MCH_EntityUavStation) {
//                    ac = ((MCH_EntityUavStation)e.ridingEntity).getControlAircract();
//                }
//                if(ac == null) return;
                yaw = shootingAircraft.rotationYaw;
                pitch = shootingAircraft.rotationPitch;
            }

            // 计算目标方向的三维坐标变化量
            double targetX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double targetZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double targetY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);

            // 计算方向的距离
            double dist = MathHelper.sqrt_double(targetX * targetX + targetY * targetY + targetZ * targetZ);
            double maxDist = 1500.0;
            double segmentLength = 100.0;  // 每段的长度
            int numSegments = (int) (maxDist / segmentLength);  // 计算需要的段数

            // 在客户端和服务器端都将目标方向进行归一化处理
            targetX = targetX * maxDist / dist;
            targetY = targetY * maxDist / dist;
            targetZ = targetZ * maxDist / dist;

            double posX;
            double posY;
            double posZ;

            if (!worldObj.isRemote) {
                posX = e.posX;
                posY = e.posY + e.getEyeHeight();
                posZ = e.posZ;
            } else {
                posX = clientTarget().xCoord;
                posY = clientTarget().yCoord;
                posZ = clientTarget().zCoord;
            }

            // 计算发射源
            Vec3 src = W_WorldFunc.getWorldVec3(this.worldObj, posX, posY, posZ);

            // 射线检测
            MovingObjectPosition hitResult = null;

            for (int i = 1; i <= numSegments; i++) {
                // 计算当前分段的目标点，确保每段都从上一个段的终点开始
                Vec3 currentDst = W_WorldFunc.getWorldVec3(this.worldObj,
                        posX + targetX * i / numSegments,
                        posY + targetY * i / numSegments,
                        posZ + targetZ * i / numSegments);

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
                // 设置导弹的目标位置
                targetPosX = hitResult.hitVec.xCoord;
                targetPosY = hitResult.hitVec.yCoord;
                targetPosZ = hitResult.hitVec.zCoord;
            }

            onLaserGuide();
        }
    }

    @SideOnly(Side.CLIENT)
    private Vec3 clientTarget() {
        return Vec3.createVectorHelper(RenderManager.renderPosX, RenderManager.renderPosY, RenderManager.renderPosZ);
    }

    public void onLaserGuide() {

        // 获取当前导弹目标位置的方块
        Block targetBlock = W_WorldFunc.getBlock(super.worldObj, (int) this.targetPosX, (int) this.targetPosY, (int) this.targetPosZ);

        // 如果目标位置有方块且该方块是可碰撞的
        if (targetBlock != null && targetBlock.isCollidable()) {
            double heightOffset = 0.0D;
            double deltaX, deltaY, deltaZ, distance;

            // 如果导弹的重力为 0，则执行以下逻辑
            if ((double) this.getGravity() == 0.0D) {
                // 在更新次数少于10次时，给定一个高度偏移量
                if (this.getCountOnUpdate() < 10) {
                    //heightOffset = 20.0D;
                    heightOffset = 0.0D;
                }

                // 计算目标与当前导弹位置的差距
                deltaX = this.targetPosX - super.posX;
                deltaY = this.targetPosY + heightOffset - super.posY;
                deltaZ = this.targetPosZ - super.posZ;

                // 计算导弹到目标的距离
                distance = MathHelper.sqrt_double(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                double targetMotionX = deltaX * super.acceleration / distance;
                double targetMotionY = deltaY * super.acceleration / distance;
                double targetMotionZ = deltaZ * super.acceleration / distance;
                // 计算导弹的速度分量
                super.motionX += (targetMotionX - super.motionX) * getInfo().turningFactor;
                super.motionY += (targetMotionY - super.motionY) * getInfo().turningFactor;
                super.motionZ += (targetMotionZ - super.motionZ) * getInfo().turningFactor;

                // 限制速度上限，防止导弹速度过快
                double maxSpeed = getInfo().acceleration; // 最大速度值
                double currentSpeed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
                if (currentSpeed > maxSpeed) {
                    double scale = maxSpeed / currentSpeed;
                    motionX *= scale;
                    motionY *= scale;
                    motionZ *= scale;
                }

            } else {
                // 如果导弹有重力，则按以下逻辑处理
                deltaX = this.targetPosX - super.posX;
                deltaY = this.targetPosY - super.posY;
                deltaY *= 0.3D;  // 对垂直方向进行适当的缩放
                deltaZ = this.targetPosZ - super.posZ;

                // 计算导弹与目标的距离
                distance = MathHelper.sqrt_double(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                // 计算导弹的速度分量，确保不超过加速度的最大值
                super.motionX = deltaX * super.acceleration / distance;
                super.motionZ = deltaZ * super.acceleration / distance;
            }
        }

        // 计算导弹的朝向（水平旋转角度）
        double yawAngle = (float) Math.atan2(super.motionZ, super.motionX);
        super.rotationYaw = (float) (yawAngle * 180.0D / 3.141592653589793D) - 90.0F;

        // 计算导弹的朝向（垂直旋转角度）
        double horizontalSpeed = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
        super.rotationPitch = -((float) (Math.atan2(super.motionY, horizontalSpeed) * 180.0D / 3.141592653589793D));
    }



    public void sprinkleBomblet() {
        if (!super.worldObj.isRemote) {
            MCH_EntityRocket e = new MCH_EntityRocket(super.worldObj, super.posX, super.posY, super.posZ, super.motionX, super.motionY, super.motionZ, super.rotationYaw, super.rotationPitch, super.acceleration);
            e.setName(this.getName());
            e.setParameterFromWeapon(this, super.shootingAircraft, super.shootingEntity);
            float MOTION = this.getInfo().bombletDiff;
            float RANDOM = 1.2F;
            e.motionX += ((double) super.rand.nextFloat() - 0.5D) * (double) MOTION;
            e.motionY += ((double) super.rand.nextFloat() - 0.5D) * (double) MOTION;
            e.motionZ += ((double) super.rand.nextFloat() - 0.5D) * (double) MOTION;
            e.setBomblet();
            super.worldObj.spawnEntityInWorld(e);
        }

    }

    public MCH_BulletModel getDefaultBulletModel() {
        return MCH_DefaultBulletModels.ATMissile;
    }


}
