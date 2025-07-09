package mcheli.flare;

import mcheli.MCH_Explosion;
import mcheli.MCH_FMURUtil;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.packets.PacketIronCurtainUse;
import mcheli.weapon.*;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import java.util.List;

public class MCH_APS {

    // Duration of cooldown — 0 means cooldown is finished
    public int tick;
    // Duration of active use — 0 means the usage has ended
    public int useTick;
    // Total active time the APS (Active Protection System) is effective
    public int useTime;
    // Waiting time before APS can activate again
    public int waitTime;

    public World worldObj;

    public MCH_EntityAircraft aircraft;

    public int range;

    public Entity user;

    public MCH_APS(World w, MCH_EntityAircraft ac) {
        this.worldObj = w;
        this.aircraft = ac;
    }

    public boolean onUse(Entity user) {
        boolean result = false;
        System.out.println("MCH_APS.onUse");
        this.user = user;
        if (worldObj.isRemote) {
            if (tick == 0) {
                tick = waitTime;
                useTick = useTime;
                result = true;
                if(range == 100) {
                    W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "iron_curtain", 10.0F, 1.0F);
                    aircraft.ironCurtainRunningTick = useTick;
                } else {
                    W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_activate", 10.0F, 1.0F);
                }
            }
        } else {
            result = true;
            tick = waitTime;
            useTick = useTime;
            aircraft.getEntityData().setBoolean("APSUsing", true);
            if(range == 100) {
                W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "iron_curtain", 10.0F, 1.0F);
                aircraft.ironCurtainRunningTick = useTick;
                MCH_MOD.getPacketHandler().sendToAll(new PacketIronCurtainUse(aircraft.getEntityId(), useTick));
            } else {
                W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_activate", 10.0F, 1.0F);
            }
        }
        return result;
    }

    public void onUpdate() {
        if (this.aircraft != null && !this.aircraft.isDead) {
            if (this.tick > 0) {
                --this.tick;
            }
            if (this.useTick > 0) {
                --this.useTick;
                if(useTick == 0) {
                    W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_deactivate", 10.0F, 1.0F);
                    onEnd();
                }
            }
            if(this.useTick > 0) {
                this.onUsing();
            }
            if (!this.isUsing() && this.aircraft.getEntityData().getBoolean("APSUsing")) {
                this.aircraft.getEntityData().setBoolean("APSUsing", false);
            }
        }
    }

    private void onUsing() {
        if(worldObj.isRemote) {
        } else {
            if(range == 100) {
                return;
            }
            List list = worldObj.getEntitiesWithinAABBExcludingEntity(aircraft, aircraft.boundingBox.expand(range, range, range));
            for (Object obj : list) {
                Entity entity = (Entity) obj;

                if(entity.getClass().getName().contains("EntityBullet")) {
                    if(MCH_FMURUtil.bulletDestructedByAPS(entity, (EntityLivingBase) user)) {
                        W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_shoot", 10.0F, 1.0F);
                    }
                }

                if(entity.getClass().getName().contains("EntityGrenade")) {
                    if(MCH_FMURUtil.grenadeDestructedByAPS(entity, (EntityLivingBase) user)) {
                        W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_shoot", 10.0F, 1.0F);
                        MCH_Explosion.newExplosion(worldObj, user, user, entity.posX, entity.posY, entity.posZ,
                                2, 0, true, true, false, true, 0, null);
                    }
                }

                if(entity instanceof MCH_EntityAAMissile
                        || entity instanceof MCH_EntityRocket
                        || entity instanceof MCH_EntityATMissile 
                        || entity instanceof MCH_EntityASMissile
                        || entity instanceof MCH_EntityTvMissile
                ) {
                    MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet) entity;
                    if(bullet.shootingEntity instanceof EntityPlayer && !((EntityPlayer) user).isOnSameTeam((EntityLivingBase) bullet.shootingEntity)) {
                        bullet.setDead();
                        W_WorldFunc.MOD_playSoundEffect(worldObj, aircraft.posX, aircraft.posY, aircraft.posZ, "aps_shoot", 10.0F, 1.0F);
                        MCH_FMURUtil.sendAPSMarker((EntityPlayerMP) bullet.shootingEntity);
                        MCH_Explosion.newExplosion(worldObj, user, user, entity.posX, entity.posY, entity.posZ,
                                3, 0, true, true, false, true, 0, null);
                    }
                }

            }
        }
    }

    private void onEnd() {
        if(range == 100) {
            aircraft.ironCurtainRunningTick = 0;
            aircraft.ironCurtainWaveTimer = 0;
            aircraft.ironCurtainCurrentFactor = 0.5f;
            aircraft.ironCurtainLastFactor = 0.5f;
        }
    }

    public boolean isInPreparation() {
        return this.tick != 0;
    }

    public boolean isUsing() {
        return this.useTick > 0;
    }
}
