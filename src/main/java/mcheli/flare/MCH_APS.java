package mcheli.flare;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import cpw.mods.fml.common.network.NetworkRegistry;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.network.packets.PacketAPSEffect;
import mcheli.network.packets.PacketAPSState;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/** Server-owned hard-kill active protection for one vehicle. */
public class MCH_APS {
    public enum State { DISARMED, ARMING, READY, RELOADING, EMPTY }

    private static final int TOGGLE_COOLDOWN = 4;
    private static final double MIN_SPEED_SQ = 1.0E-6D;
    private static final double MAX_PREDICTION_TICKS = 80.0D;
    private final World worldObj;
    private final MCH_EntityBaseVehicle aircraft;
    private State state = State.DISARMED;
    private boolean armed;
    private int armingTimer;
    private int reloadTimer;
    private int ammoRemaining = -1;
    private int lastToggleTick = Integer.MIN_VALUE;
    private int reloadTicks;
    private int armingTicks;
    private int range;
    private int ammoCapacity = -1;

    public MCH_APS(World world, MCH_EntityBaseVehicle aircraft) {
        this.worldObj = world;
        this.aircraft = aircraft;
    }

    public void configure(int reload, int wait, int detectionRange, int ammo) {
        reloadTicks = clamp(reload, 0, 12000);
        armingTicks = clamp(wait, 0, 12000);
        range = clamp(detectionRange, 1, 128);
        int newCapacity = ammo < 0 ? -1 : clamp(ammo, 0, 1024);
        if (ammoCapacity != newCapacity) {
            ammoCapacity = newCapacity;
            if (ammoRemaining < 0 || ammoRemaining > ammoCapacity) ammoRemaining = ammoCapacity;
        }
    }

    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    public boolean requestToggle(Entity requester) {
        if (worldObj.isRemote || !canToggle(requester)) return false;
        int now = aircraft.ticksExisted;
        if (lastToggleTick != Integer.MIN_VALUE && now - lastToggleTick < TOGGLE_COOLDOWN) return false;
        lastToggleTick = now;
        return armed ? disarm() : arm();
    }

    private boolean canToggle(Entity requester) {
        if (requester == null || aircraft == null || aircraft.isDead || aircraft.isDestroyed()
                || aircraft.getAcInfo() == null || !aircraft.getAcInfo().haveAPS()) return false;
        return requester == aircraft.getRiddenByEntity()
                || MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(requester) == aircraft
                && requester == aircraft.getRiddenByEntity();
    }

    public boolean arm() {
        if (worldObj.isRemote || armed || ammoCapacity == 0) return false;
        armed = true;
        armingTimer = armingTicks;
        updateState();
        W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_activate", 10.0F, 1.0F);
        syncState();
        return true;
    }

    public boolean disarm() {
        if (worldObj.isRemote || !armed) return false;
        armed = false;
        armingTimer = 0;
        updateState();
        W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_deactivate", 10.0F, 1.0F);
        syncState();
        return true;
    }

    public void onUpdate() {
        if (aircraft == null || aircraft.isDead || aircraft.isDestroyed() || aircraft.getAcInfo() == null
                || !aircraft.getAcInfo().haveAPS()) { reset(); return; }
        if (worldObj.isRemote) {
            if (armingTimer > 0) --armingTimer;
            if (reloadTimer > 0) --reloadTimer;
            return;
        }
        State old = state;
        int oldArming = armingTimer, oldReload = reloadTimer;
        if (armingTimer > 0) --armingTimer;
        if (reloadTimer > 0) --reloadTimer;
        updateState();
        if (isReady()) {
            MCH_EntityBaseBullet threat = findBestThreat();
            if (threat != null) interceptThreat(threat);
        }
        if (old != state || oldArming > 0 && armingTimer == 0 || oldReload > 0 && reloadTimer == 0) syncState();
    }

    private void updateState() {
        if (!armed) state = State.DISARMED;
        else if (!hasAmmo()) state = State.EMPTY;
        else if (armingTimer > 0) state = State.ARMING;
        else if (reloadTimer > 0) state = State.RELOADING;
        else state = State.READY;
    }

    public MCH_EntityBaseBullet findBestThreat() {
        if (!isReady()) return null;
        List threats = worldObj.getEntitiesWithinAABB(MCH_EntityBaseBullet.class,
                aircraft.boundingBox.expand(range, range, range));
        for (int i = threats.size() - 1; i >= 0; --i) {
            if (!isValidThreat((MCH_EntityBaseBullet)threats.get(i))) threats.remove(i);
        }
        Collections.sort(threats, new Comparator() {
            public int compare(Object a, Object b) {
                MCH_EntityBaseBullet x = (MCH_EntityBaseBullet)a, y = (MCH_EntityBaseBullet)b;
                double tx = predictThreat(x), ty = predictThreat(y);
                if (tx < ty) return -1; if (tx > ty) return 1;
                double dx = x.getDistanceSqToEntity(aircraft), dy = y.getDistanceSqToEntity(aircraft);
                if (dx < dy) return -1; if (dx > dy) return 1;
                return x.getEntityId() < y.getEntityId() ? -1 : x.getEntityId() == y.getEntityId() ? 0 : 1;
            }
        });
        return threats.isEmpty() ? null : (MCH_EntityBaseBullet)threats.get(0);
    }

    public boolean isValidThreat(MCH_EntityBaseBullet bullet) {
        if (bullet == null || bullet.isDead || bullet.isInterceptedByAPS() || !bullet.canBeInterceptedByAPS()
                || bullet.getDistanceSqToEntity(aircraft) > range * range || !isHostileThreat(bullet)) return false;
        double time = predictThreat(bullet);
        if (time <= 0.0D) return false;
        Vec3 start = Vec3.createVectorHelper(bullet.posX, bullet.posY, bullet.posZ);
        Vec3 end = getVehicleCenter();
        MovingObjectPosition obstruction = worldObj.rayTraceBlocks(start, end, false);
        return obstruction == null;
    }

    public boolean isHostileThreat(MCH_EntityBaseBullet bullet) {
        Entity shooterVehicle = resolveVehicle(bullet.shootingAircraft);
        Entity shooter = bullet.shootingEntity;
        if (shooterVehicle == aircraft || shooter == aircraft || aircraft.isMountedEntity(shooter)) return false;
        Entity mountedVehicle = resolveVehicle(shooter);
        if (mountedVehicle == aircraft) return false;
        if (shooter instanceof EntityLivingBase && aircraft.isMountedSameTeamEntity((EntityLivingBase)shooter)) return false;
        if (shooterVehicle instanceof MCH_EntityBaseVehicle) {
            MCH_EntityBaseVehicle vehicle = (MCH_EntityBaseVehicle)shooterVehicle;
            for (int i = 0; i <= vehicle.getSeatNum(); ++i) {
                Entity occupant = vehicle.getEntityBySeatId(i);
                if (occupant instanceof EntityLivingBase && aircraft.isMountedSameTeamEntity((EntityLivingBase)occupant)) return false;
            }
        }
        return true;
    }

    private Entity resolveVehicle(Entity entity) {
        if (entity instanceof MCH_EntityBaseVehicle) return entity;
        if (entity instanceof MCH_EntitySeat) return ((MCH_EntitySeat)entity).getParent();
        return MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(entity);
    }

    /** Returns time in ticks to closest approach, or -1 when the path misses. */
    public double predictThreat(MCH_EntityBaseBullet bullet) {
        Vec3 center = getVehicleCenter();
        double rx = bullet.posX - center.xCoord, ry = bullet.posY - center.yCoord, rz = bullet.posZ - center.zCoord;
        double vx = bullet.motionX - aircraft.motionX, vy = bullet.motionY - aircraft.motionY, vz = bullet.motionZ - aircraft.motionZ;
        double speedSq = vx * vx + vy * vy + vz * vz;
        if (speedSq < MIN_SPEED_SQ) return -1.0D;
        double time = -(rx * vx + ry * vy + rz * vz) / speedSq;
        if (time <= 0.0D || time > MAX_PREDICTION_TICKS) return -1.0D;
        double cx = rx + vx * time, cy = ry + vy * time, cz = rz + vz * time;
        double radius = Math.max(aircraft.boundingBox.maxX - aircraft.boundingBox.minX,
                Math.max(aircraft.boundingBox.maxY - aircraft.boundingBox.minY,
                        aircraft.boundingBox.maxZ - aircraft.boundingBox.minZ)) * 0.5D + 1.0D;
        return cx * cx + cy * cy + cz * cz <= radius * radius ? time : -1.0D;
    }

    private Vec3 getVehicleCenter() {
        return Vec3.createVectorHelper((aircraft.boundingBox.minX + aircraft.boundingBox.maxX) * 0.5D,
                (aircraft.boundingBox.minY + aircraft.boundingBox.maxY) * 0.5D,
                (aircraft.boundingBox.minZ + aircraft.boundingBox.maxZ) * 0.5D);
    }

    public boolean interceptThreat(MCH_EntityBaseBullet bullet) {
        if (!isReady() || !isValidThreat(bullet) || !bullet.interceptByAPS()) return false;
        double x = bullet.posX, y = bullet.posY, z = bullet.posZ;
        if (ammoRemaining > 0) --ammoRemaining;
        reloadTimer = reloadTicks;
        updateState();
        W_WorldFunc.MOD_playSoundEffect(worldObj, x, y, z, "aps_shoot", 10.0F, 1.0F);
        MCH_MOD.getPacketHandler().sendToAllAround(new PacketAPSEffect(x, y, z),
                new NetworkRegistry.TargetPoint(aircraft.dimension, x, y, z, 96.0D));
        syncState();
        return true;
    }

    public void refillAmmo() {
        if (ammoCapacity >= 0 && ammoRemaining < ammoCapacity) { ammoRemaining = ammoCapacity; updateState(); syncState(); }
    }

    public void reset() {
        armed = false; armingTimer = 0; reloadTimer = 0; lastToggleTick = Integer.MIN_VALUE; updateState();
    }

    public void loadAmmo(int ammo) { ammoRemaining = ammoCapacity < 0 ? -1 : clamp(ammo, 0, ammoCapacity); reset(); }
    public void applyClientState(int ordinal, boolean isArmed, int ammo, int arming, int reload) {
        if (!worldObj.isRemote) return;
        state = ordinal >= 0 && ordinal < State.values().length ? State.values()[ordinal] : State.DISARMED;
        armed = isArmed; ammoRemaining = ammo; armingTimer = arming; reloadTimer = reload;
    }
    public void syncState() { if (!worldObj.isRemote) MCH_MOD.getPacketHandler().sendToAll(new PacketAPSState(aircraft, this)); }
    public State getState() { return state; }
    public boolean isArmed() { return armed; }
    public boolean isReady() { return state == State.READY; }
    public boolean isArming() { return state == State.ARMING; }
    public boolean isReloading() { return state == State.RELOADING; }
    public boolean isEmpty() { return state == State.EMPTY; }
    public boolean hasAmmo() { return ammoRemaining < 0 || ammoRemaining > 0; }
    public int getAmmoRemaining() { return ammoRemaining; }
    public int getArmingTimer() { return armingTimer; }
    public int getReloadTimer() { return reloadTimer; }
}
