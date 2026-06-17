package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityBaseVehicle;
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

        //Wire guidance
        if (!getInfo().laserGuidance) {
            if (e != null && !e.isDead) {
                MCH_EntityBaseVehicle ac = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(e);
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

        //Laser guidance
        else {

            MCH_EntityBaseVehicle ac = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(e);
            if(ac != null && ac.getCurrentWeapon(e).getCurrentWeapon() instanceof MCH_WeaponTvMissile) {
                MCH_WeaponTvMissile weaponTvMissile = (MCH_WeaponTvMissile) ac.getCurrentWeapon(e).getCurrentWeapon();
                if(weaponTvMissile.guidanceSystem != null && !weaponTvMissile.guidanceSystem.targeting) {
                    return;
                }
            }

            float yaw;
            float pitch;

            if (getInfo().hasLaserGuidancePod) {
                yaw = e.rotationYaw;  // Gets player yaw angle
                pitch = e.rotationPitch;  // Gets player pitch angle
            } else {
//                MCH_EntityBaseVehicle ac = null; //Entity ridden by the player
//                if(e.ridingEntity instanceof MCH_EntityBaseVehicle) {
//                    ac = (MCH_EntityBaseVehicle)e.ridingEntity;
//                } else if(e.ridingEntity instanceof MCH_EntitySeat) {
//                    ac = ((MCH_EntitySeat)e.ridingEntity).getParent();
//                } else if(e.ridingEntity instanceof MCH_EntityUavStation) {
//                    ac = ((MCH_EntityUavStation)e.ridingEntity).getControlAircract();
//                }
//                if(ac == null) return;
                yaw = shootingAircraft.rotationYaw;
                pitch = shootingAircraft.rotationPitch;
            }

            // Calculates 3D coordinate deltas toward target direction
            double targetX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double targetZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(pitch / 180.0F * (float) Math.PI);
            double targetY = -MathHelper.sin(pitch / 180.0F * (float) Math.PI);

            // Calculates direction distance
            double dist = MathHelper.sqrt_double(targetX * targetX + targetY * targetY + targetZ * targetZ);
            double maxDist = 1500.0;
            double segmentLength = 100.0;  // Length of each segment
            int numSegments = (int) (maxDist / segmentLength);  // Calculates required number of segments

            // Normalizes target direction on both client and server
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

            // Calculates launch source
            Vec3 src = W_WorldFunc.getWorldVec3(this.worldObj, posX, posY, posZ);

            // Raycast
            MovingObjectPosition hitResult = null;

            for (int i = 1; i <= numSegments; i++) {
                // Calculates target point of current segment, ensuring each segment starts from previous endpoint
                Vec3 currentDst = W_WorldFunc.getWorldVec3(this.worldObj,
                        posX + targetX * i / numSegments,
                        posY + targetY * i / numSegments,
                        posZ + targetZ * i / numSegments);

                // Performs raycast
                List<MovingObjectPosition> hitResults = rayTraceAllBlocks(this.worldObj, src, currentDst, false, true, true);

                if (hitResults != null && !hitResults.isEmpty()) {
                    hitResult = hitResults.get(0);
                    break;  // Exits loop after finding collision result
                }

                // Updates src to the current checked dst
                src = currentDst;  // Current segment dst becomes next segment src
            }

            // If no collision is detected, returns default target position
            if (hitResult == null) {
                hitResult = new MovingObjectPosition(null, src.addVector(targetX, targetY, targetZ));  // Uses target point as default value
            }

            // If raycast hits a valid block and it is not underwater
            if (!this.worldObj.isRemote) {
                // Sets missile target position
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

        // Gets block at current missile target position
        Block targetBlock = W_WorldFunc.getBlock(super.worldObj, (int) this.targetPosX, (int) this.targetPosY, (int) this.targetPosZ);

        // If target position has a block and the block is collidable
        if (targetBlock != null && targetBlock.isCollidable()) {
            double heightOffset = 0.0D;
            double deltaX, deltaY, deltaZ, distance;

            // If missile gravity is 0, executes the following logic
            if ((double) this.getGravity() == 0.0D) {
                // Applies a height offset while update count is less than 10
                if (this.getCountOnUpdate() < 10) {
                    //heightOffset = 20.0D;
                    heightOffset = 0.0D;
                }

                // Calculates difference between target and current missile position
                deltaX = this.targetPosX - super.posX;
                deltaY = this.targetPosY + heightOffset - super.posY;
                deltaZ = this.targetPosZ - super.posZ;

                // Calculates distance from missile to target
                distance = MathHelper.sqrt_double(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                double targetMotionX = deltaX * super.acceleration / distance;
                double targetMotionY = deltaY * super.acceleration / distance;
                double targetMotionZ = deltaZ * super.acceleration / distance;
                // Calculates missile velocity components
                super.motionX += (targetMotionX - super.motionX) * getInfo().turningFactor;
                super.motionY += (targetMotionY - super.motionY) * getInfo().turningFactor;
                super.motionZ += (targetMotionZ - super.motionZ) * getInfo().turningFactor;

                // Limits max speed to prevent missile from moving too fast
                double maxSpeed = getInfo().acceleration; // Maximum speed value
                double currentSpeed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
                if (currentSpeed > maxSpeed) {
                    double scale = maxSpeed / currentSpeed;
                    motionX *= scale;
                    motionY *= scale;
                    motionZ *= scale;
                }

            } else {
                // If missile has gravity, handles with the following logic
                deltaX = this.targetPosX - super.posX;
                deltaY = this.targetPosY - super.posY;
                deltaY *= 0.3D;  // Appropriately scales vertical direction
                deltaZ = this.targetPosZ - super.posZ;

                // Calculates distance between missile and target
                distance = MathHelper.sqrt_double(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                // Calculates missile velocity components, ensuring they do not exceed max acceleration
                super.motionX = deltaX * super.acceleration / distance;
                super.motionZ = deltaZ * super.acceleration / distance;
            }
        }

        // Calculates missile orientation (horizontal rotation angle)
        double yawAngle = (float) Math.atan2(super.motionZ, super.motionX);
        super.rotationYaw = (float) (yawAngle * 180.0D / 3.141592653589793D) - 90.0F;

        // Calculates missile orientation (vertical rotation angle)
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
