package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

import static mcheli.weapon.MCH_WeaponASMissile.rayTraceAllBlocks;

public class MCH_LaserGuidanceSystem implements MCH_IGuidanceSystem {

    public World worldObj;
    protected Entity user;
    public double targetPosX;
    public double targetPosY;
    public double targetPosZ;
    public boolean targeting = false;
    @SideOnly(Side.CLIENT)
    public MCH_EntityLockBox lockBox;
    public boolean hasLaserGuidancePod = true;

    @Override
    public double getLockPosX() {
        return targetPosX;
    }

    @Override
    public double getLockPosY() {
        return targetPosY;
    }

    @Override
    public double getLockPosZ() {
        return targetPosZ;
    }

    @Override
    public void update() {

        if(worldObj.isRemote) {

            if(!targeting) return;

            float yaw;
            float pitch;

            if (hasLaserGuidancePod) {
                yaw = user.rotationYaw;  // Gets player yaw angle
                pitch = user.rotationPitch;  // Gets player pitch angle
            } else {
                MCH_EntityBaseVehicle ac = null; //Entity ridden by the player
                if(user.ridingEntity instanceof MCH_EntityBaseVehicle) {
                    ac = (MCH_EntityBaseVehicle)user.ridingEntity;
                } else if(user.ridingEntity instanceof MCH_EntitySeat) {
                    ac = ((MCH_EntitySeat)user.ridingEntity).getParent();
                } else if(user.ridingEntity instanceof MCH_EntityUavStation) {
                    ac = ((MCH_EntityUavStation)user.ridingEntity).getControlAircract();
                }
                if(ac == null) return;
                yaw = ac.rotationYaw;
                pitch = ac.rotationPitch;
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

//            double posX = user.posX;
//            double posY = user.posY + user.getEyeHeight();
//            double posZ = user.posZ;

            double posX = RenderManager.renderPosX;
            double posY = RenderManager.renderPosY;
            double posZ = RenderManager.renderPosZ;

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

            // Sets missile target position
            targetPosX = hitResult.hitVec.xCoord;
            targetPosY = hitResult.hitVec.yCoord;
            targetPosZ = hitResult.hitVec.zCoord;

            if(lockBox != null) {
                lockBox.setPosition(targetPosX, targetPosY, targetPosZ);
            } else {
                lockBox = new MCH_EntityLockBox(worldObj);
                worldObj.spawnEntityInWorld(lockBox);
            }
        }
    }
}
