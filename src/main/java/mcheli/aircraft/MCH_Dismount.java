package mcheli.aircraft;

import java.util.Map;
import java.util.WeakHashMap;
import mcheli.MCH_Lib;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;

/** Server-owned, one-shot exit placement. All candidate coordinates are physical feet. */
public final class MCH_Dismount {
    private static final Map<Entity, Operation> OPERATIONS = new WeakHashMap<Entity, Operation>();

    private MCH_Dismount() { }

    private static final class Operation {
        final int vehicleId;
        final int mountId;
        final int seat;
        int id;
        boolean completed;
        boolean rejectionLogged;

        Operation(MCH_EntityBaseVehicle vehicle, Entity mount, int seat) {
            this.vehicleId = vehicle.getEntityId();
            this.mountId = mount.getEntityId();
            this.seat = seat;
            this.id = MCH_PacketNotifyOnMountEntity.nextSequence();
        }
    }

    public static void observeMount(MCH_EntityBaseVehicle vehicle, Entity entity, Entity mount, int seat) {
        if(entity == null || entity.ridingEntity != mount) return;
        Operation old = OPERATIONS.get(entity);
        if(old == null || old.completed || old.mountId != mount.getEntityId() || old.vehicleId != vehicle.getEntityId()) {
            OPERATIONS.put(entity, new Operation(vehicle, mount, seat));
        }
    }

    public static boolean canComplete(MCH_EntityBaseVehicle vehicle, Entity entity, Entity mount) {
        if(entity == null) return false;
        Operation operation = OPERATIONS.get(entity);
        boolean allowed = entity.ridingEntity == null && (operation == null
                || !operation.completed && operation.vehicleId == vehicle.getEntityId()
                && operation.mountId == mount.getEntityId());
        if(!allowed && operation != null && !operation.rejectionLogged) {
            operation.rejectionLogged = true;
            log(vehicle, entity, operation, "reject-stale-callback", null);
        }
        return allowed;
    }

    public static void commit(MCH_EntityBaseVehicle vehicle, Entity entity, double x, double feetY,
                              double z, float yaw, float pitch, String reason) {
        // Client callbacks must never compete with S08 or remote entity tracking.
        if(entity == null || entity.worldObj.isRemote || entity.ridingEntity != null || entity.isDead) return;
        Operation operation = OPERATIONS.get(entity);
        if(operation != null && (operation.completed || operation.vehicleId != vehicle.getEntityId())) {
            if(!operation.rejectionLogged) {
                operation.rejectionLogged = true;
                log(vehicle, entity, operation, "reject-duplicate-exit", null);
            }
            return;
        }
        if(operation == null) {
            operation = new Operation(vehicle, vehicle, 0);
            OPERATIONS.put(entity, operation);
        }
        operation.id = MCH_PacketNotifyOnMountEntity.nextSequence();
        Vec3 exit = selectExit(vehicle, entity, operation, Vec3.createVectorHelper(x, feetY, z));
        operation.completed = true; // Before callbacks from setPosition can reenter.
        log(vehicle, entity, operation, "write-before:" + reason, exit);
        if(entity instanceof EntityPlayerMP) {
            // NetHandlerPlayServer treats lastPosY as feet and adds 1.62 in S08.
            // EntityPlayerMP's vanilla convention is zero, unlike EntityPlayerSP.
            entity.yOffset = 0.0F;
            entity.ySize = 0.0F;
            EntityPlayerMP player = (EntityPlayerMP)entity;
            player.playerNetServerHandler.setPlayerLocation(exit.xCoord, exit.yCoord, exit.zCoord, yaw, pitch);
        } else {
            entity.setPosition(exit.xCoord, exit.yCoord + entity.yOffset - entity.ySize, exit.zCoord);
            entity.rotationYaw = yaw;
            entity.rotationPitch = pitch;
        }
        log(vehicle, entity, operation, "exit-committed:" + reason, exit);
        if(entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)entity;
            MCH_PacketNotifyOnMountEntity.sendExit(vehicle, player, operation.id, operation.seat, exit);
            MinecraftForge.EVENT_BUS.post(new MCH_DismountEvent(player, vehicle.getUniqueID(),
                    vehicle.getEntityId(), operation.seat, operation.id, exit, false));
        }
    }

    private static Vec3 selectExit(MCH_EntityBaseVehicle vehicle, Entity entity, Operation operation, Vec3 preferred) {
        int rejected = 0;
        String blocker = blockingType(vehicle, entity, preferred);
        if(blocker == null) return preferred;
        log(vehicle, entity, operation, "reject-candidate:" + blocker, preferred);
        // Bounded: 5 heights, 8 rings, 8 directions. Prefer nearby horizontal exits.
        for(int height = 0; height <= 4; height++) {
            for(int radius = 1; radius <= 8; radius++) {
                for(int direction = 0; direction < 8; direction++) {
                    double angle = direction * Math.PI / 4.0D;
                    Vec3 candidate = preferred.addVector(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
                    blocker = blockingType(vehicle, entity, candidate);
                    if(blocker == null) return candidate;
                    // Per-operation diagnostic budget, not tick logging.
                    if(rejected++ < 8) log(vehicle, entity, operation, "reject-candidate:" + blocker, candidate);
                }
            }
        }
        // Explicit one-shot fallback: retain vanilla's provisional detached feet.
        // No retry timer, gravity suppression, or repeated teleport is installed.
        Vec3 fallback = Vec3.createVectorHelper(entity.posX, entity.boundingBox.minY, entity.posZ);
        log(vehicle, entity, operation, "search-exhausted:vanilla-fallback:" + blockingType(vehicle, entity, fallback), fallback);
        return fallback;
    }

    private static String blockingType(MCH_EntityBaseVehicle vehicle, Entity entity, Vec3 feet) {
        double halfWidth = entity.width * 0.5D;
        // Keep supported larger geometry; allow room to stand after mounted low poses.
        double height = entity instanceof EntityPlayer ? Math.max(entity.height, 1.8D) : entity.height;
        AxisAlignedBB body = AxisAlignedBB.getBoundingBox(feet.xCoord - halfWidth, feet.yCoord, feet.zCoord - halfWidth,
                feet.xCoord + halfWidth, feet.yCoord + height, feet.zCoord + halfWidth);
        java.util.List collisions = entity.worldObj.getCollidingBoundingBoxes(entity, body);
        for(Object collision : collisions) {
            if(collision instanceof AxisAlignedBB && ((AxisAlignedBB)collision).intersectsWith(body)) {
                return collision instanceof MCH_BaseVehicleBoundingBox ? "vehicle-hull" : "block-or-entity";
            }
        }
        // Extended hulls can lie outside vanilla's two-block entity broad phase.
        for(Object object : entity.worldObj.loadedEntityList) {
            if(object instanceof MCH_EntityBaseVehicle && object != entity && !((Entity)object).isDead
                    && ((MCH_EntityBaseVehicle)object).getBoundingBox().intersectsWith(body)) return "vehicle-hull";
        }
        return null;
    }

    private static void log(MCH_EntityBaseVehicle vehicle, Entity entity, Operation operation, String reason, Vec3 exit) {
        MCH_Lib.DbgLog(entity.worldObj,
                "[MCH-EXIT] side=%s player=%s vehicle=%s seat=%d operation=%d reason=%s exit=%s posY=%.5f minY=%.5f yOffset=%.5f ySize=%.5f",
                entity.worldObj.isRemote ? "CLIENT" : "SERVER", entity.getUniqueID(), vehicle.getUniqueID(),
                operation.seat, operation.id, reason, exit, entity.posY, entity.boundingBox.minY, entity.yOffset, entity.ySize);
    }
}
