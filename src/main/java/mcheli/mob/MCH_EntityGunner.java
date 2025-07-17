package mcheli.mob;

import java.util.ArrayList;
import java.util.List;

import com.hbm.lib.Library;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_SeatInfo;
import mcheli.weapon.*;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.*;
import net.minecraft.world.World;

public class MCH_EntityGunner extends EntityLivingBase {
    public boolean isCreative = false;

    public String ownerUUID = "";

    public int targetType = 0;

    public int despawnCount = 0;

    public int switchTargetCount = 0;

    public Entity targetEntity = null;

    public double targetPrevPosX = 0.0D;

    public double targetPrevPosY = 0.0D;

    public double targetPrevPosZ = 0.0D;

    public boolean waitCooldown = false;

    public int idleCount = 0;

    public int idleRotation = 0;

    public MCH_EntityGunner(World world) {
        super(world);
    }

    public MCH_EntityGunner(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);
    }

    protected void entityInit() {
        super.entityInit();
        getDataWatcher().addObject(17, "");
    }

    public String getTeamName() {
        return getDataWatcher().getWatchableObjectString(17);
    }

    public void setTeamName(String name) {
        getDataWatcher().updateObject(17, name);
    }

    public Team getTeam() {
        return (Team)this.worldObj.getScoreboard().getTeam(getTeamName());
    }

    public boolean isOnSameTeam(EntityLivingBase p_142014_1_) {
        return super.isOnSameTeam(p_142014_1_);
    }

    public IChatComponent getFormattedCommandSenderName() {
        Team team = getTeam();
        if (team != null)
            return (IChatComponent)new ChatComponentText(ScorePlayerTeam.formatPlayerName(team, team.getRegisteredName() + " Gunner"));
        return (IChatComponent)new ChatComponentText("");
    }

    public boolean isEntityInvulnerable() {
        return this.isCreative;
    }

    public void onDeath(DamageSource source) {
        super.onDeath(source);
    }

    public boolean interactFirst(EntityPlayer player) {
        if (this.worldObj.isRemote)
            return false;
        if (this.ridingEntity == null)
            return false;
        if (player.capabilities.isCreativeMode) {
            removeFromAircraft(player);
            return true;
        }
        if (this.isCreative) {
            player.addChatMessage((IChatComponent)new ChatComponentText("Creative mode only."));
            return false;
        }
        if ( isOnSameTeam((EntityLivingBase)player)) { //removed getTeam() == null ||
            removeFromAircraft(player);
            return true;
        }
        player.addChatMessage((IChatComponent)new ChatComponentText("You are on other team."));
        return false;
    }

    public void removeFromAircraft(EntityPlayer player) {
        if (!this.worldObj.isRemote) {
            W_WorldFunc.MOD_playSoundAtEntity((Entity)player, "wrench", 1.0F, 1.0F);
            setDead();
            MCH_EntityAircraft ac = null;
            if (this.ridingEntity instanceof MCH_EntityAircraft) {
                ac = (MCH_EntityAircraft)this.ridingEntity;
            } else if (this.ridingEntity instanceof MCH_EntitySeat) {
                ac = ((MCH_EntitySeat)this.ridingEntity).getParent();
            }
            String name = "";
            if (ac != null && ac.getAcInfo() != null)
                name = " on " + (ac.getAcInfo()).displayName + " seat " + (ac.getSeatIdByEntity((Entity)this) + 1);
            player.addChatMessage((IChatComponent)new ChatComponentText("Removed gunner" + name + " by " + ScorePlayerTeam.formatPlayerName(player.getTeam(), player.getDisplayName()) + "."));
            mountEntity(null);
        }
    }

    public void onUpdate() {
        super.onUpdate();
        if (!this.worldObj.isRemote && !this.isDead) {
            if (this.ridingEntity != null && this.ridingEntity.isDead)
                this.ridingEntity = null;
            if (this.ridingEntity instanceof MCH_EntityAircraft) {
                shotTarget((MCH_EntityAircraft)this.ridingEntity);
            } else if (this.ridingEntity instanceof MCH_EntitySeat && ((MCH_EntitySeat)this.ridingEntity).getParent() != null) {
                shotTarget(((MCH_EntitySeat)this.ridingEntity).getParent());
            } else if (this.despawnCount < 20) {
                this.despawnCount++;
            } else if (this.ridingEntity == null || this.ticksExisted > 100) {
                setDead();
            }
            //todo remember target here
            if (this.targetEntity == null) {
                if (this.idleCount == 0) {
                    this.idleCount = (3 + this.rand.nextInt(5)) * 20;
                    this.idleRotation = this.rand.nextInt(5) - 2;
                }
                this.rotationYaw += this.idleRotation / 2.0F;
            } else {
                this.idleCount = 60;
            }
        }
        if (this.switchTargetCount > 0)
            this.switchTargetCount--;
        if (this.idleCount > 0)
            this.idleCount--;
    }

    public boolean canAttackEntity(EntityLivingBase entity, MCH_EntityAircraft ac, MCH_WeaponSet ws) {
        boolean ret = false;
        if (this.targetType == 0) {
            ret = (entity != this
                    && !(entity instanceof net.minecraft.entity.monster.EntityEnderman)
                    && !entity.isDead
                    && !isOnSameTeam(entity)
                    && entity.getHealth() > 0.0F
                    && !ac.isMountedEntity(entity)
                    );
            //maybe we don't need this?
            //if we do we'll have to do some fuckery with entitylivingbase because HBM shit is not livingbase obviously
                    //temp
                    //&& MCH_CompatUtil.isRadarDetectableAndVisible(entity, this)
                    //&& MCH_CompatUtil.isMissileFalling(entity)
                    //&& MCH_CompatUtil.isTargetMachine(entity));
        } else {
            ret = (entity != this && !((EntityPlayer)entity).capabilities.isCreativeMode && !entity.isDead && !getTeamName().isEmpty() && !isOnSameTeam(entity) && entity.getHealth() > 0.0F && !ac.isMountedEntity((Entity)entity));
        }
        MCH_IGuidanceSystem guidanceSystem = ws.getCurrentWeapon().getGuidanceSystem();
        if (ret && guidanceSystem != null) {
            MCH_EntityGuidanceSystem gs = (MCH_EntityGuidanceSystem) ws.getCurrentWeapon().getGuidanceSystem();
            ret = gs.canLockEntity((Entity) entity);
        }
        return ret;
    }

    public boolean canEntityBeSeenOrBehindBreakableBlocks(Entity target) {
        Vec3 start = Vec3.createVectorHelper(this.posX, this.posY + this.getEyeHeight(), this.posZ);
        Vec3 end = Vec3.createVectorHelper(target.posX, target.posY + target.getEyeHeight(), target.posZ);

        MovingObjectPosition result = this.worldObj.rayTraceBlocks(start, end);

        if (result == null) {
            // No obstruction, clear line of sight
            return true;
        }

        if (result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            Block hitBlock = this.worldObj.getBlock(result.blockX, result.blockY, result.blockZ);

            // Parse the config string (e.g., "glass_pane, stained_glass_pane, tallgrass, ...")
            String[] blockNames = MCH_Config.BulletBreakableBlock.prmString.split(",");
            for (String rawName : blockNames) {
                String name = rawName.trim();
                Block configBlock = Block.getBlockFromName(name);
                if (configBlock != null && configBlock == hitBlock) {
                    System.out.println("[AI] Block is breakable, allowing target through: " + name);
                    return true;
                }
            }
        }

        return false;
    }

    public void shotTarget(MCH_EntityAircraft ac) {
        if (ac.isDestroyed())
            return;
        if (!ac.getGunnerStatus())
            return;
        MCH_WeaponSet ws = ac.getCurrentWeapon((Entity)this);
        if (ws == null || ws.getInfo() == null || ws.getCurrentWeapon() == null)
            return;
        MCH_WeaponBase cw = ws.getCurrentWeapon();
        if (this.targetEntity != null && (this.targetEntity.isDead || ((EntityLivingBase)this.targetEntity).getHealth() <= 0.0F))
            if (this.switchTargetCount > 20)
                this.switchTargetCount = 20;
        Vec3 pos = getGunnerWeaponPos(ac, ws);


        if ((this.targetEntity == null && this.switchTargetCount <= 0) || this.switchTargetCount <= 0) {
            List<EntityLivingBase> list;
            this.switchTargetCount = 20;
            EntityLivingBase nextTarget = null;
            if (this.targetType == 0) {
                // Get hostile mobs first (as originally defined)
                int rh = MCH_Config.RangeOfGunner_VsMonster_Horizontal.prmInt;
                int rv = MCH_Config.RangeOfGunner_VsMonster_Vertical.prmInt;
                list = this.worldObj.getEntitiesWithinAABB(Entity.class, this.boundingBox.expand(rh, rv, rh));

                for (Entity entity : list) {
                    // Skip self, dead things, invalid health, etc.
                    if (entity == this || entity.isDead || !entity.isEntityAlive()) continue;
                    if (entity instanceof net.minecraft.entity.monster.EntityEnderman) continue;

                    // Skip if on same team
                    if (isOnSameTeam((EntityLivingBase) entity)) continue;

                    // Skip if mounted on this aircraft
                    if (ac.isMountedEntity(entity)) continue;

                    // Skip if it’s not a mob AND not a targetMachine (if custom targeting is enabled)
                    boolean isHostileMob = entity instanceof IMob;
                    //boolean isTargetableMachine = false;

                    //if (this.targetMachines) {
                    //always target machines, we do not care
                        if (!MCH_CompatUtil.isRadarDetectableAndVisible(entity, this)) continue;
                        if (!MCH_CompatUtil.isMissileFalling(entity)) continue;
                        if (!MCH_CompatUtil.isTargetMachine(entity)) continue;

                        //isTargetableMachine = true;
                    //}

                    // Final filter: must be hostile mob or machine
                    if (!isHostileMob ) continue; //&& !isTargetableMachine

                    // Good target
                    //ret = true;
                    //break;
                }
            } else {
                int rh = MCH_Config.RangeOfGunner_VsPlayer_Horizontal.prmInt;
                int rv = MCH_Config.RangeOfGunner_VsPlayer_Vertical.prmInt;
                list = this.worldObj.getEntitiesWithinAABB(EntityPlayer.class, this.boundingBox.expand(rh, rv, rh));
            }
            for (int i = 0; i < list.size(); i++) {
                EntityLivingBase entity = list.get(i);
                if (canAttackEntity(entity, ac, ws))
                    if (checkPitch(entity, ac, pos))
                        if ((nextTarget == null || getDistanceToEntity((Entity)entity) < getDistanceToEntity((Entity)nextTarget)) && (canEntityBeSeenOrBehindBreakableBlocks((Entity)entity) ))
                            //todo here is where we want to check if we are accidentally flagging friendlies?

                            if (isInAttackable(entity, ac, ws, pos)) {
                                nextTarget = entity;
                                this.switchTargetCount = 60;
                            }
            }
            if (nextTarget != null && this.targetEntity != nextTarget) {
                this.targetPrevPosX = nextTarget.posX;
                this.targetPrevPosY = nextTarget.posY;
                this.targetPrevPosZ = nextTarget.posZ;
            }
            this.targetEntity = (Entity)nextTarget;
        }


        if (this.targetEntity != null) {
            float rotSpeed = 10.0F;
            if (ac.isPilot((Entity)this))
                rotSpeed = (ac.getAcInfo()).cameraRotationSpeed / 10.0F;
            this.rotationPitch = MathHelper.wrapAngleTo180_float(this.rotationPitch);
            this.rotationYaw = MathHelper.wrapAngleTo180_float(this.rotationYaw);
            double dist = getDistanceToEntity(this.targetEntity);
            double tick = 1.0D;
            if (dist >= 10.0D && (ws.getInfo()).acceleration > 1.0F)
                tick = dist / (ws.getInfo()).acceleration;
            if (this.targetEntity.ridingEntity instanceof MCH_EntitySeat || this.targetEntity.ridingEntity instanceof MCH_EntityAircraft)
                tick -= MCH_Config.HitBoxDelayTick.prmInt;
            double dx = (this.targetEntity.posX - this.targetPrevPosX) * tick;
            double dy = (this.targetEntity.posY - this.targetPrevPosY) * tick + this.targetEntity.height * this.rand.nextDouble();
            double dz = (this.targetEntity.posZ - this.targetPrevPosZ) * tick;
            double d0 = this.targetEntity.posX + dx - pos.xCoord;
            double d1 = this.targetEntity.posY + dy - pos.yCoord;
            double d2 = this.targetEntity.posZ + dz - pos.zCoord;
            double d3 = MathHelper.sqrt_double(d0 * d0 + d2 * d2);
            float yaw = MathHelper.wrapAngleTo180_float((float)(Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0F);
            float pitch = (float)-(Math.atan2(d1, d3) * 180.0D / Math.PI);
            if (Math.abs(this.rotationPitch - pitch) < rotSpeed && Math.abs(this.rotationYaw - yaw) < rotSpeed) {
                float r = ac.isPilot((Entity)this) ? 0.1F : 0.5F;
                this.rotationPitch = pitch + (this.rand.nextFloat() - 0.5F) * r - cw.fixRotationPitch;
                this.rotationYaw = yaw + (this.rand.nextFloat() - 0.5F) * r;
                if (!this.waitCooldown || ws.currentHeat <= 0 || (ws.getInfo()).maxHeatCount <= 0) {
                    this.waitCooldown = false;
                    MCH_WeaponParam prm = new MCH_WeaponParam();
                    prm.setPosition(ac.posX, ac.posY, ac.posZ);
                    prm.user = (Entity)this;
                    prm.entity = (Entity)ac;
                    prm.option1 = (cw instanceof mcheli.weapon.MCH_WeaponEntitySeeker) ? this.targetEntity.getEntityId() : 0;
                    if (ac.useCurrentWeapon(prm))
                        if ((ws.getInfo()).maxHeatCount > 0 && ws.currentHeat > (ws.getInfo()).maxHeatCount * 4 / 5)
                            this.waitCooldown = true;
                }
            }
            if (Math.abs(pitch - this.rotationPitch) >= rotSpeed)
                this.rotationPitch += (pitch > this.rotationPitch) ? rotSpeed : -rotSpeed;
            if (Math.abs(yaw - this.rotationYaw) >= rotSpeed)
                if (Math.abs(yaw - this.rotationYaw) <= 180.0F) {
                    this.rotationYaw += (yaw > this.rotationYaw) ? rotSpeed : -rotSpeed;
                } else {
                    this.rotationYaw += (yaw > this.rotationYaw) ? -rotSpeed : rotSpeed;
                }
            this.rotationYawHead = this.rotationYaw;
            this.targetPrevPosX = this.targetEntity.posX;
            this.targetPrevPosY = this.targetEntity.posY;
            this.targetPrevPosZ = this.targetEntity.posZ;
        } else {
            this.rotationPitch *= 0.95F;
        }
    }

    private boolean checkPitch(EntityLivingBase entity, MCH_EntityAircraft ac, Vec3 pos) {
        try {
            double d0 = entity.posX - pos.xCoord;
            double d1 = entity.posY - pos.yCoord;
            double d2 = entity.posZ - pos.zCoord;
            double d3 = MathHelper.sqrt_double(d0 * d0 + d2 * d2);
            float pitch = (float)-(Math.atan2(d1, d3) * 180.0D / Math.PI);
            MCH_AircraftInfo ai = ac.getAcInfo();
            if (ac instanceof mcheli.vehicle.MCH_EntityVehicle && ac.isPilot((Entity)this))
                if (Math.abs(ai.minRotationPitch) + Math.abs(ai.maxRotationPitch) > 0.0F) {
                    if (pitch < ai.minRotationPitch)
                        return false;
                    if (pitch > ai.maxRotationPitch)
                        return false;
                }
            MCH_WeaponBase cw = ac.getCurrentWeapon((Entity)this).getCurrentWeapon();
            if (!(cw instanceof mcheli.weapon.MCH_WeaponEntitySeeker)) {
                MCH_AircraftInfo.Weapon wi = ai.getWeaponById(ac.getCurrentWeaponID((Entity)this));
                if (Math.abs(wi.minPitch) + Math.abs(wi.maxPitch) > 0.0F) {
                    if (pitch < wi.minPitch)
                        return false;
                    if (pitch > wi.maxPitch)
                        return false;
                }
            }
        } catch (Exception e) {}
        return true;
    }

    public Vec3 getGunnerWeaponPos(MCH_EntityAircraft ac, MCH_WeaponSet ws) {
        MCH_SeatInfo seatInfo = ac.getSeatInfo((Entity)this);
        if ((seatInfo != null && seatInfo.rotSeat) || ac instanceof mcheli.vehicle.MCH_EntityVehicle)
            return ac.calcOnTurretPos((ws.getCurrentWeapon()).position).addVector(ac.posX, ac.posY, ac.posZ);
        return ac.getTransformedPosition((ws.getCurrentWeapon()).position);
    }

    private boolean isInAttackable(EntityLivingBase entity, MCH_EntityAircraft ac, MCH_WeaponSet ws, Vec3 pos) {
        if (ac instanceof mcheli.vehicle.MCH_EntityVehicle)
            return true;
        try {
            if (ac.getCurrentWeapon((Entity)this).getCurrentWeapon() instanceof mcheli.weapon.MCH_WeaponEntitySeeker)
                return true;
            MCH_AircraftInfo.Weapon wi = ac.getAcInfo().getWeaponById(ac.getCurrentWeaponID((Entity)this));
            Vec3 v1 = Vec3.createVectorHelper(0.0D, 0.0D, 1.0D);
            float yaw = -ac.getRotYaw() + (wi.maxYaw + wi.minYaw) / 2.0F - wi.defaultYaw;
            v1.rotateAroundY(yaw * 3.1415927F / 180.0F);
            Vec3 v2 = Vec3.createVectorHelper(entity.posX - pos.xCoord, 0.0D, entity.posZ - pos.zCoord).normalize();
            double dot = v1.dotProduct(v2);
            double rad = Math.acos(dot);
            double deg = rad * 180.0D / Math.PI;
            return (deg < (Math.abs(wi.maxYaw - wi.minYaw) / 2.0F));
        } catch (Exception e) {
            return false;
        }
    }

    public MCH_EntityAircraft getAc() {
        if (this.ridingEntity == null)
            return null;
        return (this.ridingEntity instanceof MCH_EntitySeat) ? ((MCH_EntitySeat)this.ridingEntity).getParent() : ((this.ridingEntity instanceof MCH_EntityAircraft) ? (MCH_EntityAircraft)this.ridingEntity : null);
    }

    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setBoolean("Creative", this.isCreative);
        nbt.setString("OwnerUUID", this.ownerUUID);
        nbt.setString("TeamName", getTeamName());
        nbt.setInteger("TargetType", this.targetType);
    }

    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.isCreative = nbt.getBoolean("Creative");
        this.ownerUUID = nbt.getString("OwnerUUID");
        setTeamName(nbt.getString("TeamName"));
        this.targetType = nbt.getInteger("TargetType");
    }

    public void travelToDimension(int dim) {}

    public void setDead() {
        if (!this.worldObj.isRemote && !this.isDead && !this.isCreative)
            if (this.targetType == 0) {
                dropItem((Item)MCH_MOD.itemSpawnGunnerVsMonster, 1);
            } else if (this.targetType == 1) {
                dropItem((Item)MCH_MOD.itemSpawnGunnerVsPlayer, 1);
            }
        super.setDead();
        MCH_Lib.DbgLog(this.worldObj, "MCH_EntityGunner.setDead type=%d :" + toString(), new Object[] { Integer.valueOf(this.targetType) });
    }

    public boolean attackEntityFrom(DamageSource ds, float p_70097_2_) {
        if (ds == DamageSource.outOfWorld)
            setDead();
        return super.attackEntityFrom(ds, p_70097_2_);
    }

    public ItemStack getHeldItem() {
        return null;
    }

    public ItemStack getEquipmentInSlot(int p_71124_1_) {
        return null;
    }

    public void setCurrentItemOrArmor(int p_70062_1_, ItemStack p_70062_2_) {}

    public ItemStack[] getLastActiveItems() {
        return new ItemStack[0];
    }

    public ItemStack[] getInventory() {
        return new ItemStack[0];
    }
}
