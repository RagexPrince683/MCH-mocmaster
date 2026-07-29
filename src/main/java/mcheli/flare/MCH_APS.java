package mcheli.flare;

import java.util.List;
import mcheli.MCH_FMURUtil;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_EntityHitBox;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.network.packets.PacketIronCurtainUse;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

/** Server-authoritative active protection state for one vehicle. */
public class MCH_APS {
    private static final int LEGACY_IRON_CURTAIN_RANGE = 100;
    private static final String INTERCEPTED_TAG = "MCH_APSIntercepted";

    public int tick;
    public int useTick;
    public int useTime;
    public int waitTime;
    public World worldObj;
    public MCH_EntityBaseVehicle aircraft;
    public int range;
    private Entity user;

    public MCH_APS(World world, MCH_EntityBaseVehicle aircraft) {
        this.worldObj = world;
        this.aircraft = aircraft;
    }

    public boolean onUse(Entity entity) {
        if (worldObj.isRemote) {
            return canActivate(entity);
        }
        return activate(entity);
    }

    public boolean canActivate(Entity entity) {
        return aircraft != null && aircraft.getAcInfo() != null && aircraft.getAcInfo().haveAPS()
                && !aircraft.isDead && !aircraft.isDestroyed() && tick == 0 && !isActive()
                && isAuthorized(entity);
    }

    public boolean activate(Entity entity) {
        if (worldObj.isRemote || !canActivate(entity)) {
            return false;
        }
        int duration = Math.max(0, useTime);
        if (duration == 0) {
            return false;
        }
        user = entity;
        tick = Math.max(0, waitTime);
        useTick = duration;
        aircraft.getEntityData().setBoolean("APSUsing", true);
        if (isIronCurtainMode()) {
            aircraft.ironCurtainRunningTick = useTick;
            W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ,
                    "iron_curtain", 10.0F, 1.0F);
            MCH_MOD.getPacketHandler().sendToAll(new PacketIronCurtainUse(aircraft.getEntityId(), useTick));
        } else {
            W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ,
                    "aps_activate", 10.0F, 1.0F);
        }
        return true;
    }

    private boolean isAuthorized(Entity entity) {
        return entity != null && (aircraft.isMountedEntity(entity)
                || MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(entity) == aircraft);
    }

    public void onUpdate() {
        if (aircraft == null || aircraft.isDead || aircraft.isDestroyed() || aircraft.getAcInfo() == null
                || !aircraft.getAcInfo().haveAPS()) {
            reset();
            return;
        }
        if (tick > 0) {
            --tick;
        }
        if (useTick > 0) {
            --useTick;
            if (!worldObj.isRemote && useTick > 0 && !isIronCurtainMode()) {
                onUsing();
            }
            if (useTick == 0) {
                endActiveState();
            }
        }
    }

    private void onUsing() {
        List entities = worldObj.getEntitiesWithinAABBExcludingEntity(aircraft,
                aircraft.boundingBox.expand(range, range, range));
        for (Object object : entities) {
            Entity entity = (Entity)object;
            if (isValidThreat(entity) && isHostileThreat(entity) && isApproaching(entity)) {
                interceptThreat(entity);
            }
        }
    }

    public boolean isValidThreat(Entity entity) {
        if (entity == null || entity.isDead || entity.getEntityData().getBoolean(INTERCEPTED_TAG)
                || entity == aircraft || entity instanceof MCH_EntityBaseVehicle
                || entity instanceof MCH_EntitySeat || entity instanceof MCH_EntityHitBox
                || entity instanceof MCH_EntityFlare || entity instanceof MCH_EntityChaff
                || entity instanceof EntityItem || entity.getClass().getName().startsWith("mcheli.particles.")) {
            return false;
        }
        return entity instanceof MCH_EntityBaseBullet || MCH_FMURUtil.isAPSThreat(entity);
    }

    public boolean isHostileThreat(Entity entity) {
        Entity shooter = getShooter(entity);
        Entity shooterVehicle = getVehicle(shooter);
        if (entity.getEntityData().getBoolean("MCH_APSSpawned") || shooter == aircraft
                || shooterVehicle == aircraft || aircraft.isMountedEntity(shooter)) {
            return false;
        }
        EntityLivingBase shooterLiving = shooter instanceof EntityLivingBase ? (EntityLivingBase)shooter : null;
        if (shooterLiving != null && aircraft.isMountedSameTeamEntity(shooterLiving)) {
            return false;
        }
        if (shooterVehicle instanceof MCH_EntityBaseVehicle) {
            EntityLivingBase occupant = getLivingOccupant((MCH_EntityBaseVehicle)shooterVehicle);
            if (occupant != null && aircraft.isMountedSameTeamEntity(occupant)) {
                return false;
            }
        }
        return true;
    }

    private Entity getShooter(Entity entity) {
        if (entity instanceof MCH_EntityBaseBullet) {
            MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet)entity;
            return bullet.shootingEntity != null ? bullet.shootingEntity : bullet.shootingAircraft;
        }
        return MCH_FMURUtil.getAPSOwner(entity);
    }

    private Entity getVehicle(Entity entity) {
        if (entity instanceof MCH_EntityBaseVehicle) return entity;
        if (entity instanceof MCH_EntitySeat) return ((MCH_EntitySeat)entity).getParent();
        return MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(entity);
    }

    private EntityLivingBase getLivingOccupant(MCH_EntityBaseVehicle vehicle) {
        for (int i = 0; i <= vehicle.getSeatNum(); ++i) {
            Entity occupant = vehicle.getEntityBySeatId(i);
            if (occupant instanceof EntityLivingBase) return (EntityLivingBase)occupant;
        }
        return null;
    }

    public boolean isApproaching(Entity entity) {
        double dx = aircraft.posX - entity.posX;
        double dy = aircraft.posY + aircraft.height * 0.5D - entity.posY;
        double dz = aircraft.posZ - entity.posZ;
        double dot = dx * entity.motionX + dy * entity.motionY + dz * entity.motionZ;
        if (dot <= 0.0D) return false;
        double speedSq = entity.motionX * entity.motionX + entity.motionY * entity.motionY
                + entity.motionZ * entity.motionZ;
        if (speedSq <= 1.0E-6D) return false;
        double time = dot / speedSq;
        double cx = dx - entity.motionX * time;
        double cy = dy - entity.motionY * time;
        double cz = dz - entity.motionZ * time;
        double protectedRadius = Math.max(2.0D, Math.max(aircraft.width, aircraft.height) * 0.75D + 1.0D);
        return cx * cx + cy * cy + cz * cz <= protectedRadius * protectedRadius;
    }

    public boolean interceptThreat(Entity entity) {
        if (worldObj.isRemote || entity == null || entity.isDead
                || entity.getEntityData().getBoolean(INTERCEPTED_TAG)) return false;
        entity.getEntityData().setBoolean(INTERCEPTED_TAG, true);
        Entity shooter = getShooter(entity);
        double x = entity.posX, y = entity.posY, z = entity.posZ;
        boolean externalHandled = !(entity instanceof MCH_EntityBaseBullet)
                && MCH_FMURUtil.destroyAPSThreat(entity, user instanceof EntityLivingBase ? (EntityLivingBase)user : null);
        if (!externalHandled) entity.setDead();
        W_WorldFunc.MOD_playSoundEffect(worldObj, x, y, z, "aps_shoot", 10.0F, 1.0F);
        if (shooter instanceof EntityPlayerMP) MCH_FMURUtil.sendAPSMarker((EntityPlayerMP)shooter);
        return true;
    }

    public boolean isIronCurtainMode() {
        return range == LEGACY_IRON_CURTAIN_RANGE;
    }

    public boolean isActive() { return useTick > 0; }
    public boolean isCoolingDown() { return tick > 0; }
    public boolean isUsing() { return isActive(); }
    public boolean isInPreparation() { return isCoolingDown(); }

    private void endActiveState() {
        aircraft.getEntityData().setBoolean("APSUsing", false);
        if (!worldObj.isRemote) {
            W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ,
                    "aps_deactivate", 10.0F, 1.0F);
        }
        clearIronCurtain();
        user = null;
    }

    private void clearIronCurtain() {
        if (aircraft != null) {
            aircraft.ironCurtainRunningTick = 0;
            aircraft.ironCurtainWaveTimer = 0;
            aircraft.ironCurtainCurrentFactor = 0.5F;
            aircraft.ironCurtainLastFactor = 0.5F;
        }
    }

    public void reset() {
        tick = 0;
        useTick = 0;
        user = null;
        if (aircraft != null) aircraft.getEntityData().setBoolean("APSUsing", false);
        clearIronCurtain();
    }
}
