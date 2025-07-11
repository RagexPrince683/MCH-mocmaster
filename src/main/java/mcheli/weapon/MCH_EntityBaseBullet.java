package mcheli.weapon;


import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
//import cuchaz.ships.EntityShip;
import mcheli.*;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntityHitBox;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_PacketNotifyHitBullet;
import mcheli.chain.MCH_EntityChain;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.mob.MCH_EntityGunner;
import mcheli.particles.MCH_ParticleParam;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.plane.MCP_EntityPlane;
import mcheli.ship.MCH_EntityShip;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_EntityPlayer;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityCloudFX;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import mcheli.vector.Vector3f;
import mcheli.network.packets.PacketLockTarget;
import cpw.mods.fml.client.FMLClientHandler;
import org.lwjgl.opengl.GL11;

import java.util.*;

import static mcheli.MCH_Config.delayrangeloader;
import static mcheli.MCH_Config.bombletloader;

public abstract class MCH_EntityBaseBullet extends W_Entity implements MCH_IChunkLoader {

    public long idleStartTime = -1;

    public static final int DATAWT_RESERVE1 = 26;
    public static final int DATAWT_TARGET_ENTITY = 27;
    public static final int DATAWT_MARKER_STAT = 28;
    public static final int DATAWT_NAME = 29;
    public static final int DATAWT_BULLET_MODEL = 30;
    public static final int DATAWT_BOMBLET_FLAG = 31;
    public Entity shootingEntity;
    public Entity shootingAircraft;
    private int countOnUpdate;
    public int explosionPower;
    public int explosionPowerInWater;
    private int power;
    public double acceleration;
    public double accelerationFactor;
    public Entity targetEntity;
    public int piercing;
    public int delayFuse;
    public int sprinkleTime;
    public byte isBomblet;
    private MCH_WeaponInfo weaponInfo;
    private MCH_BulletModel model;
    public double prevPosX2;
    public double prevPosY2;
    public double prevPosZ2;
    public double prevMotionX;
    public double prevMotionY;
    public double prevMotionZ;
    private Ticket loaderTicket;
    public boolean bomblet;
    public boolean gravitydown;
    public boolean bigdelay;
    private boolean bigcheck = false;
    public int chemYield = 0;

    //public int delayrangeloaderint = delayrangeloader;

    //public MCH_ConfigPrm delayrangeloaderconfigsetting = delayrangeloader;

    //private final MCH_Fuze fuze = new MCH_Fuze(this);;

    public int antiFlareTick;
    boolean doingTopAttack = false;
    boolean speedAddedFromAircraft = false;
    private ForgeChunkManager.Ticket chunkLoaderTicket;
    //private List<ChunkCoordIntPair> loadedChunks = new ArrayList<>();

    boolean isInRangeToRenderDist2;

    List<ChunkCoordIntPair> bulletLoadedChunks = new ArrayList<>();


    public MCH_EntityBaseBullet(World par1World) {
        super(par1World);
        this.countOnUpdate = 0;
        this.setSize(1.0F, 1.0F);
        super.prevRotationYaw = super.rotationYaw;
        super.prevRotationPitch = super.rotationPitch;
        this.targetEntity = null;
        this.setPower(1);
        this.acceleration = 1.0D;
        this.accelerationFactor = 1.0D;
        this.piercing = 0;
        this.explosionPower = 0;
        this.explosionPowerInWater = 0;
        this.delayFuse = 0;
        this.sprinkleTime = 0;
        this.isBomblet = -1;
        this.weaponInfo = null;
        super.ignoreFrustumCheck = true;
        if (par1World.isRemote) {
            this.model = null;
        }

    }

    //@Override
    //@SideOnly(Side.CLIENT)
    //public boolean isInRangeToRenderDist(double distance){
    //    return true;
    //}
    //already defined ln487 I dont want to mess with it given particles are already laggy as shit

    public MCH_EntityBaseBullet(World par1World, double px, double py, double pz, double mx, double my, double mz, float yaw, float pitch, double acceleration) {
        this(par1World);
        this.setSize(1.0F, 1.0F);
        this.setLocationAndAngles(px, py, pz, yaw, pitch);
        this.setPosition(px, py, pz);
        super.prevRotationYaw = yaw;
        super.prevRotationPitch = pitch;
        super.yOffset = 0.0F;
        if (acceleration > 3.9D) {
            acceleration = 3.9D;
        }

        double d = MathHelper.sqrt_double(mx * mx + my * my + mz * mz);
        super.motionX = mx * acceleration / d;
        super.motionY = my * acceleration / d;
        super.motionZ = mz * acceleration / d;
        this.prevMotionX = super.motionX;
        this.prevMotionY = super.motionY;
        this.prevMotionZ = super.motionZ;
        this.acceleration = acceleration;
    }

    // Chunk loading code courtesy of HBM's nuclear tech mod https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT/

    public void init(Ticket ticket) {
        if (!worldObj.isRemote) {
            if (ticket != null) {
                if (loaderTicket == null) {
                    loaderTicket = ticket;
                    loaderTicket.bindEntity(this);
                    loaderTicket.getModData();
                }
                // Force load the initial chunk where the bullet is spawned
                ForgeChunkManager.forceChunk(loaderTicket, new ChunkCoordIntPair(chunkCoordX, chunkCoordZ));
            }
        }
    }



    List<ChunkCoordIntPair> loadedChunks = new ArrayList<ChunkCoordIntPair>();

    // Dynamically load chunks based on bullet's movement
    public void checkAndLoadChunks() {
        System.out.println("check and load chunks fired");
        // Get the current chunk coordinates for the bullet
        int currentChunkX = MathHelper.floor_double(posX) >> 4;
        int currentChunkZ = MathHelper.floor_double(posZ) >> 4;

        // Determine the direction of bullet movement and load chunks in front
        loadChunksInBulletPath(currentChunkX, currentChunkZ, motionX, motionZ);
    }


    public void loadNeighboringChunks(int chunkX, int chunkZ) {
        if (!worldObj.isRemote && loaderTicket != null) {
            // Unload previously loaded chunks to avoid memory bloat
            for (ChunkCoordIntPair chunk : loadedChunks) {
                ForgeChunkManager.unforceChunk(loaderTicket, chunk);
            }

            loadedChunks.clear();

            // Define the neighboring chunks (including diagonals)
            ChunkCoordIntPair[] neighboringChunks = {
                    new ChunkCoordIntPair(chunkX, chunkZ),           // Current chunk
                    new ChunkCoordIntPair(chunkX + 1, chunkZ),       // +X
                    new ChunkCoordIntPair(chunkX - 1, chunkZ),       // -X
                    new ChunkCoordIntPair(chunkX, chunkZ + 1),       // +Z
                    new ChunkCoordIntPair(chunkX, chunkZ - 1),       // -Z
                    new ChunkCoordIntPair(chunkX + 1, chunkZ + 1),   // +X, +Z
                    new ChunkCoordIntPair(chunkX - 1, chunkZ - 1),   // -X, -Z
                    new ChunkCoordIntPair(chunkX + 1, chunkZ - 1),   // +X, -Z
                    new ChunkCoordIntPair(chunkX - 1, chunkZ + 1)    // -X, +Z
            };

            // Load surrounding chunks if not already loaded
            for (ChunkCoordIntPair chunk : neighboringChunks) {
                loadedChunks.add(chunk);  // add chunk directly without checking
                ForgeChunkManager.forceChunk(this.loaderTicket, chunk);
            }

            System.out.println("Loaded surrounding chunks at: " + chunkX + ", " + chunkZ);
        }
    }
    // Dynamically load chunks ahead of the bullet based on its current position and motion
    public void loadChunksInBulletPath(int currentChunkX, int currentChunkZ, double motionX, double motionZ) {
        if (!worldObj.isRemote && loaderTicket != null) {
            // Unload only chunks previously loaded by the bullet
            for (ChunkCoordIntPair chunk : bulletLoadedChunks) {
                ForgeChunkManager.unforceChunk(loaderTicket, chunk);
            }
            bulletLoadedChunks.clear();

            // Calculate the next chunk in the direction of the bullet's motion
            int nextChunkX = currentChunkX + (motionX > 0 ? 1 : (motionX < 0 ? -1 : 0));
            int nextChunkZ = currentChunkZ + (motionZ > 0 ? 1 : (motionZ < 0 ? -1 : 0));

            // Define the chunks to load (current, next in X, next in Z, and diagonal)
            ChunkCoordIntPair[] chunksToLoad = {
                    new ChunkCoordIntPair(currentChunkX, currentChunkZ),
                    new ChunkCoordIntPair(nextChunkX, currentChunkZ),
                    new ChunkCoordIntPair(currentChunkX, nextChunkZ),
                    new ChunkCoordIntPair(nextChunkX, nextChunkZ)
            };

            for (ChunkCoordIntPair chunk : chunksToLoad) {
                if (!bulletLoadedChunks.contains(chunk)) {
                    bulletLoadedChunks.add(chunk);
                    ForgeChunkManager.forceChunk(loaderTicket, chunk);
                }
            }
        }
    }

    // Clear chunk loader after bullet impact or despawn to free memory
    //public void clearChunkLoader() {
    //    if (!worldObj.isRemote && loaderTicket != null) {
    //        for (ChunkCoordIntPair chunk : loadedChunks) {
    //            ForgeChunkManager.unforceChunk(loaderTicket, chunk);
    //        }
    //        loadedChunks.clear();
    //    }
    //}
    //unused



    public void setLocationAndAngles(double par1, double par3, double par5, float par7, float par8) {
        super.setLocationAndAngles(par1, par3, par5, par7, par8);
        this.prevPosX2 = par1;
        this.prevPosY2 = par3;
        this.prevPosZ2 = par5;
    }

    protected void entityInit() {
        super.entityInit();
        init(ForgeChunkManager.requestTicket(MCH_MOD.instance, worldObj, Type.ENTITY));
        this.getDataWatcher().addObject(27, Integer.valueOf(0));
        this.getDataWatcher().addObject(29, String.valueOf(""));
        this.getDataWatcher().addObject(30, String.valueOf(""));
        this.getDataWatcher().addObject(31, Byte.valueOf((byte) 0));
    }

    public void setName(String s) {
        if (s != null && !s.isEmpty()) {
            this.weaponInfo = MCH_WeaponInfoManager.get(s);
            if (this.weaponInfo != null) {
                if (!super.worldObj.isRemote) {
                    this.getDataWatcher().updateObject(29, String.valueOf(s));
                }

                this.onSetWeasponInfo();
            }
        }

    }

    public String getName() {
        return this.getDataWatcher().getWatchableObjectString(29);
    }

    public MCH_WeaponInfo getInfo() {
        return this.weaponInfo;
    }

    public void onSetWeasponInfo() {

        if (this.getInfo().gravity < 0.0) {
            this.gravitydown = true;
        } else {
            this.gravitydown = false;
            System.out.println("hey this weapon has no gravity defined, that's probably not a good thing");
        }

        if(this.getInfo().bomblet >= (float)MCH_Config.bombletloader.prmInt) {
            this.bomblet = true;
        } else {
            this.bomblet = false;
        }

        if(this.getInfo().delay < (float)MCH_Config.delayrangeloader.prmInt) { //todone implement config setting here
            //< delayrangeloaderconfigsetting
            this.bigdelay = false;
        } else {
            this.bigdelay = true;
        }

        if (!super.worldObj.isRemote) {
            this.isBomblet = 0;
        }

        if (this.getInfo().bomblet > 0) {
            this.sprinkleTime = this.getInfo().bombletSTime;
        }

        this.piercing = this.getInfo().piercing;
        if (this instanceof MCH_EntityBullet) {
            if (this.getInfo().acceleration > 4.0F) {
                this.accelerationFactor = (double) (this.getInfo().acceleration / 4.0F);
            }
        } else if (this instanceof MCH_EntityRocket && this.isBomblet == 0 && this.getInfo().acceleration > 4.0F) {
            this.accelerationFactor = (double) (this.getInfo().acceleration / 4.0F);
        }

    }
    //todone add a gravity check here

    public void setDead() {
        if (shouldLoadChunks()) {
            System.out.println("should load chunks2");
            //todo checkAndLoadChunks() instead
            checkAndLoadChunks();
            loadNeighboringChunks(getChunkX(), getChunkZ());
            clearBulletChunks();
        }
        super.setDead();
        //this.clearChunkLoader();
        //if (this.piercing <= 0) {
        //   super.setDead();
        //   System.out.println("Setting dead " + this.isDead);
        //}

    }

    public void setBomblet() {
        this.isBomblet = 1;
        this.sprinkleTime = 0;
        super.dataWatcher.updateObject(31, Byte.valueOf((byte) 1));
    }

    public byte getBomblet() {
        return super.dataWatcher.getWatchableObjectByte(31);
    }

    public void setTargetEntity(Entity entity) {
        this.targetEntity = entity;
        if (!super.worldObj.isRemote) {
            if (entity != null) {
                this.getDataWatcher().updateObject(27, Integer.valueOf(W_Entity.getEntityId(entity)));
            } else {
                this.getDataWatcher().updateObject(27, Integer.valueOf(0));
            }
        }

    }

    public void clientSetTargetEntity(Entity entity) {
        if(super.worldObj.isRemote) {
            this.targetEntity = entity;
            if(entity != null) {
                MCH_MOD.getPacketHandler().sendToServer(new PacketLockTarget(entity.getEntityId(), this.getEntityId()));
            } else {
                MCH_MOD.getPacketHandler().sendToServer(new PacketLockTarget(0, this.getEntityId()));
            }
        }

    }

    public int getTargetEntityID() {
        return this.targetEntity != null ? W_Entity.getEntityId(this.targetEntity) : this.getDataWatcher().getWatchableObjectInt(27);
    }

    public MCH_BulletModel getBulletModel() {
        if (this.getInfo() == null) {
            return null;
        } else if (this.isBomblet < 0) {
            return null;
        } else {
            if (this.model == null) {
                if (this.isBomblet == 1) {
                    this.model = this.getInfo().bombletModel;
                } else {
                    this.model = this.getInfo().bulletModel;
                }

                if (this.model == null) {
                    this.model = this.getDefaultBulletModel();
                }
            }

            return this.model;
        }
    }

    public abstract MCH_BulletModel getDefaultBulletModel();

    public void sprinkleBomblet() {
    }

    public void spawnExplosionParticle(String name, int num, float size) {
        if (super.worldObj.isRemote) {
            if (name.isEmpty() || num < 1 || num > 50) {
                return;
            }

            double x = (super.posX - super.prevPosX) / (double) num;
            double y = (super.posY - super.prevPosY) / (double) num;
            double z = (super.posZ - super.prevPosZ) / (double) num;
            double x2 = (super.prevPosX - this.prevPosX2) / (double) num;
            double y2 = (super.prevPosY - this.prevPosY2) / (double) num;
            double z2 = (super.prevPosZ - this.prevPosZ2) / (double) num;

            //if (this.shouldRenderRocketTrail() && name != "rocket") {
            //   double var7 = 40;
            //   //x -= super.motionX * (double)var7;
            //   //y -= super.motionY * (double)var7;
            //   //z -= super.motionZ * (double)var7;
            //}

            int i;
            if (name.equals("explode")) {
                for (i = 0; i < num; ++i) {
                    MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "smoke", (super.prevPosX + x * (double) i + this.prevPosX2 + x2 * (double) i) / 2.0D, (super.prevPosY + y * (double) i + this.prevPosY2 + y2 * (double) i) / 2.0D, (super.prevPosZ + z * (double) i + this.prevPosZ2 + z2 * (double) i) / 2.0D);
                    prm.size = size + super.rand.nextFloat();
                    MCH_ParticlesUtil.spawnParticle(prm);
                }
            } else {
                for (i = 0; i < num; ++i) {
                    MCH_ParticlesUtil.DEF_spawnParticle(name, (super.prevPosX + x * (double) i + this.prevPosX2 + x2 * (double) i) / 2.0D, (super.prevPosY + y * (double) i + this.prevPosY2 + y2 * (double) i) / 2.0D, (super.prevPosZ + z * (double) i + this.prevPosZ2 + z2 * (double) i) / 2.0D, 0.0D, 0.0D, 0.0D, 50.0F);
                }
            }
        }

    }

    public void DEF_spawnParticle(String name, int num, float size) {
        if (super.worldObj.isRemote) {
            if (name.isEmpty() || num < 1 || num > 50) {
                return;
            }

            double x = (super.posX - super.prevPosX) / (double) num;
            double y = (super.posY - super.prevPosY) / (double) num;
            double z = (super.posZ - super.prevPosZ) / (double) num;
            double x2 = (super.prevPosX - this.prevPosX2) / (double) num;
            double y2 = (super.prevPosY - this.prevPosY2) / (double) num;
            double z2 = (super.prevPosZ - this.prevPosZ2) / (double) num;

            for (int i = 0; i < num; ++i) {
                MCH_ParticlesUtil.DEF_spawnParticle(name, (super.prevPosX + x * (double) i + this.prevPosX2 + x2 * (double) i) / 2.0D, (super.prevPosY + y * (double) i + this.prevPosY2 + y2 * (double) i) / 2.0D, (super.prevPosZ + z * (double) i + this.prevPosZ2 + z2 * (double) i) / 2.0D, 0.0D, 0.0D, 0.0D, 150.0F);
            }
        }

    }





    public int getCountOnUpdate() {
        return this.countOnUpdate;
    }

    public void clearCountOnUpdate() {
        this.countOnUpdate = 0;
    }

    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double par1) {
        double d1 = super.boundingBox.getAverageEdgeLength() * 4.0D;
        d1 *= 64.0D;
        return par1 < d1 * d1;
    }

    public void setParameterFromWeapon(MCH_WeaponBase w, Entity entity, Entity user) {
        this.explosionPower = w.explosionPower;
        this.explosionPowerInWater = w.explosionPowerInWater;
        this.setPower(w.power);
        this.chemYield = w.chemYield;
        this.piercing = w.piercing;
        this.shootingAircraft = entity;
        this.shootingEntity = user;
    }

    public void setParameterFromWeapon(MCH_EntityBaseBullet b, Entity entity, Entity user) {
        this.explosionPower = b.explosionPower;
        this.chemYield = b.chemYield;
        this.explosionPowerInWater = b.explosionPowerInWater;
        this.setPower(b.getPower());
        this.piercing = b.piercing;
        this.shootingAircraft = entity;
        this.shootingEntity = user;
    }

    public void setMotion(double targetX, double targetY, double targetZ) {
        double d6 = (double) MathHelper.sqrt_double(targetX * targetX + targetY * targetY + targetZ * targetZ);
        super.motionX = targetX * this.acceleration / d6;
        super.motionY = targetY * this.acceleration / d6;
        super.motionZ = targetZ * this.acceleration / d6;
    }

    public void guidanceToTarget(double targetPosX, double targetPosY, double targetPosZ) {
        this.guidanceToTarget(targetPosX, targetPosY, targetPosZ, 1.0F);
    }

    public void guidanceToTarget(double targetPosX, double targetPosY, double targetPosZ, float accelerationFactor) {
        if(getInfo().tickEndHoming > 0 && ticksExisted > getInfo().tickEndHoming) {
            return;
        }

        // 计算目标位置与当前实体位置之间的差值
        double tx = targetPosX - super.posX;  // 目标X坐标与当前实体X坐标的差值
        double ty = targetPosY - super.posY;  // 目标Y坐标与当前实体Y坐标的差值
        double tz = targetPosZ - super.posZ;  // 目标Z坐标与当前实体Z坐标的差值

        // 计算目标与当前实体之间的距离
        double d = MathHelper.sqrt_double(tx * tx + ty * ty + tz * tz);

        // 计算单位加速度在每个坐标轴上的分量
        double mx = tx * this.acceleration / d;  // X轴加速度分量
        double my = ty * this.acceleration / d;  // Y轴加速度分量
        double mz = tz * this.acceleration / d;  // Z轴加速度分量

        // 计算导弹的运动方向
        Vector3f missileDirection = new Vector3f((float)super.motionX, (float)super.motionY, (float)super.motionZ);

        // 计算目标方向（从导弹指向目标）
        Vector3f targetDirection = new Vector3f((float)tx, (float)ty, (float)tz);

        // 计算导弹运动方向与目标方向之间的夹角（单位：弧度）
        double angle = Math.abs(Vector3f.angle(missileDirection, targetDirection));

        // 设置最大允许角度阈值（单位：弧度）
        double maxAllowedAngle = Math.toRadians(getInfo().maxDegreeOfMissile);  // 可以根据需要调整这个值，10度是一个例子

        // 如果角度超过最大允许值，解除锁定
        if (angle > maxAllowedAngle && !doingTopAttack) {
            targetEntity = null;
            return;
        }

        //计算目标的速度向量
        Vector3f targetVelocity = new Vector3f(targetEntity.motionX, targetEntity.motionY, targetEntity.motionZ);
        double velocityAngle = Math.abs(Vector3f.angle(missileDirection, targetVelocity));
        if (velocityAngle > getInfo().pdHDNMaxDegree) {
            targetEntity = null;
            return;
        }

        if(this instanceof MCH_EntityAAMissile
                && MCH_WeaponGuidanceSystem.isEntityOnGround(targetEntity, weaponInfo.lockMinHeight)) {
            targetEntity = null;
            return;
        }

        // 使用平滑加权平均值来更新当前实体的运动速度
        super.motionX = super.motionX + (mx - super.motionX) * getInfo().turningFactor;  // 平滑过渡X轴速度
        super.motionY = super.motionY + (my - super.motionY) * getInfo().turningFactor;  // 平滑过渡Y轴速度
        super.motionZ = super.motionZ + (mz - super.motionZ) * getInfo().turningFactor;  // 平滑过渡Z轴速度

        // 计算实体朝向目标的旋转角度（Yaw方向）
        double a = (float)Math.atan2(super.motionZ, super.motionX);  // 计算水平方向的角度（Yaw）
        super.rotationYaw = (float)(a * 180.0D / 3.141592653589793D) - 90.0F;  // 转换为角度并设置实体的旋转Yaw

        // 计算实体的俯仰角度（Pitch方向）
        double r = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);  // 计算水平速度的大小
        super.rotationPitch = -((float)(Math.atan2(super.motionY, r) * 180.0D / 3.141592653589793D));  // 计算并设置Pitch角度
    }

    public boolean checkValid() {
        if (this.shootingEntity == null && this.shootingAircraft == null) {
            return false;
        } else if (this.shootingEntity != null && this.shootingEntity.isDead) {
            return false;
        } else {
            if (this.shootingAircraft != null && this.shootingAircraft.isDead) {
                ;
            }

            Entity shooter = this.shootingEntity != null ? this.shootingEntity : this.shootingAircraft;
            double x = super.posX - shooter.posX;
            double z = super.posZ - shooter.posZ;
            return x * x + z * z < 3.38724E7D;
        }
    }

    public float getGravity() {
        return this.getInfo() != null ? this.getInfo().gravity : 0.0F;
    }

    public float getGravityInWater() {
        //oh naw
        return this.getInfo() != null ? this.getInfo().gravityInWater : -1F;
    }

    public void onUpdate() {

        

        if(!worldObj.isRemote) {
            if (shootingAircraft instanceof MCH_EntityAircraft && !speedAddedFromAircraft && getInfo().speedDependsAircraft) {
                MCH_EntityAircraft ac = (MCH_EntityAircraft) shootingAircraft;
                double s = Math.sqrt(ac.motionX * ac.motionX + ac.motionY * ac.motionY + ac.motionZ * ac.motionZ);
                acceleration += s;
                double d = MathHelper.sqrt_double(motionX * motionX + motionY * motionY + motionZ * motionZ);
                super.motionX = motionX * acceleration / d;
                super.motionY = motionY * acceleration / d;
                super.motionZ = motionZ * acceleration / d;
                speedAddedFromAircraft = true;
            }
        }


        //if (!isInRangeToRenderDist2) {
            if (!bomblet && gravitydown && bigdelay) {
                checkAndLoadChunks();  // If this method handles any critical chunk loading, it stays here
            }
        //}

        if (super.worldObj.isRemote && this.countOnUpdate == 0) {
            int f3 = this.getTargetEntityID();
            if (f3 > 0) {
                this.setTargetEntity(super.worldObj.getEntityByID(f3));
            }
        }

        if (this.prevMotionX != super.motionX || this.prevMotionY != super.motionY || this.prevMotionZ != super.motionZ) {
            double var5 = (double) ((float) Math.atan2(super.motionZ, super.motionX));
            super.rotationYaw = (float) (var5 * 180.0D / 3.141592653589793D) - 90.0F;
            double r = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
            super.rotationPitch = -((float) (Math.atan2(super.motionY, r) * 180.0D / 3.141592653589793D));
        }

        this.prevMotionX = super.motionX;
        this.prevMotionY = super.motionY;
        this.prevMotionZ = super.motionZ;
        ++this.countOnUpdate;
        if (this.countOnUpdate > 10000000) {
            this.clearCountOnUpdate();
        }

        this.prevPosX2 = super.prevPosX;
        this.prevPosY2 = super.prevPosY;
        this.prevPosZ2 = super.prevPosZ;
        super.onUpdate();

        if (this.getInfo() == null) {
            if (this.countOnUpdate >= 2) {
                MCH_Lib.Log((Entity) this, "##### MCH_EntityBaseBullet onUpdate() Weapon info null %d, %s, Name=%s",
                        new Object[]{Integer.valueOf(W_Entity.getEntityId(this)), this.getEntityName(), this.getName()});
                System.out.println("ENTITY IS NULL!!!");
                this.setDead();
                return;
            }

            this.setName(this.getName());
            if (this.getInfo() == null) {
                return;
            }
        }

        if (super.worldObj.isRemote && this.isBomblet < 0) {
            this.isBomblet = this.getBomblet();
        }

        if (!super.worldObj.isRemote) {
            if ((int) super.posY <= 255 && !super.worldObj.blockExists((int) super.posX, (int) super.posY, (int) super.posZ)) {
                if (this.getInfo().delayFuse <= 0) {
                    this.setDead();
                    return;
                }

                if (this.delayFuse == 0) {
                    this.delayFuse = this.getInfo().delayFuse;
                }
            }

            if (this.delayFuse > 0) {
                --this.delayFuse;
                if (this.delayFuse == 0) {
                    this.onUpdateTimeout();
                    this.setDead();
                    return;
                }
            }

            if (!this.checkValid()) {
                this.setDead();
                System.out.println("entity is not valid");
                return;
            }

            if(this.getInfo().timeFuse > 0 && this.getCountOnUpdate() > this.getInfo().timeFuse) {
                this.onUpdateTimeout();
                this.setDead();
                return;
            }

            if(this.getInfo().explosionAltitude > 0 && MCH_Lib.getBlockIdY(this, 3, -this.getInfo().explosionAltitude) != 0) {
                MovingObjectPosition var6 = new MovingObjectPosition((int)super.posX, (int)super.posY, (int)super.posZ, 0,
                        Vec3.createVectorHelper(super.posX, super.posY, super.posZ));
                this.onImpact(var6, 1.0F);
            }
        }

        // Apply gravity effects based on whether it's in water
        if(!this.isInWater()) {
            if(ticksExisted > getInfo().speedFactorStartTick && ticksExisted < getInfo().speedFactorEndTick) {
                // 计算当前总速度
                double currentSpeed = Math.sqrt(
                        motionX * motionX +
                                motionY * motionY +
                                motionZ * motionZ
                );

                if(currentSpeed > 0) { // 避免除以零
                    // 获取速度方向单位向量
                    double dirX = motionX / currentSpeed;
                    double dirY = motionY / currentSpeed;
                    double dirZ = motionZ / currentSpeed;

                    // 沿速度方向叠加固定增量
                    motionX += dirX * getInfo().speedFactor;
                    motionY += dirY * getInfo().speedFactor;
                    motionZ += dirZ * getInfo().speedFactor;
                    acceleration += getInfo().speedFactor;
                }
            }
            super.motionY += this.getGravity();
        } else {
            super.motionY += (double) this.getGravityInWater();
        }

        // Chunk loading logic based on bullet motion
        if (!super.isDead) {
            // Get current chunk coordinates based on the bullet's position
            int chunkX = (int) Math.floor(this.posX / 16D);
            int chunkZ = (int) Math.floor(this.posZ / 16D);

            // Check if the bullet still exists before proceeding
            if (!super.isDead) {
                // Ensure the bullet has the specific conditions (e.g., no bomblet, gravityDown, and bigDelay)
                if (!bomblet && gravitydown && bigdelay) {

                    // Load the necessary chunks in front of the bullet
                    loadChunksInBulletPath(chunkX, chunkZ, this.motionX, this.motionZ);

                    // Log that chunks are being loaded
                    //System.out.println("Bullet is loading chunks at: " + chunkX + ", " + chunkZ);
                    //hush, you are working you can sleep now

                    bigcheck = true;  // Mark that the chunks have been checked and loaded
                }
            }

            // Handle the collision and update the bullet state
            this.onUpdateCollided();
        }

        // Apply bullet's motion
        super.posX += super.motionX * this.accelerationFactor;
        super.posY += super.motionY * this.accelerationFactor;
        super.posZ += super.motionZ * this.accelerationFactor;

        if (super.worldObj.isRemote) {
            this.updateSplash();
        }

        // Handle water particles if in water
        if (this.isInWater()) {
            float var7 = 0.25F;
            super.worldObj.spawnParticle("bubble", super.posX - super.motionX * (double) var7,
                    super.posY - super.motionY * (double) var7,
                    super.posZ - super.motionZ * (double) var7,
                    super.motionX, super.motionY, super.motionZ);
        }

        this.setPosition(super.posX, super.posY, super.posZ);
    }




    public void updateSplash() {
        if (this.getInfo() != null) {
            if (this.getInfo().power > 0) {
                if (!W_WorldFunc.isBlockWater(super.worldObj, (int) (super.prevPosX + 0.5D), (int) (super.prevPosY + 0.5D), (int) (super.prevPosZ + 0.5D)) && W_WorldFunc.isBlockWater(super.worldObj, (int) (super.posX + 0.5D), (int) (super.posY + 0.5D), (int) (super.posZ + 0.5D))) {
                    double x = super.posX - super.prevPosX;
                    double y = super.posY - super.prevPosY;
                    double z = super.posZ - super.prevPosZ;
                    double d = Math.sqrt(x * x + y * y + z * z);
                    if (d <= 0.15D) {
                        return;
                    }

                    x /= d;
                    y /= d;
                    z /= d;
                    double px = super.prevPosX;
                    double py = super.prevPosY;
                    double pz = super.prevPosZ;

                    for (int i = 0; (double) i <= d; ++i) {
                        px += x;
                        py += y;
                        pz += z;
                        if (W_WorldFunc.isBlockWater(super.worldObj, (int) (px + 0.5D), (int) (py + 0.5D), (int) (pz + 0.5D))) {
                            float pwr = this.getInfo().power < 20 ? (float) this.getInfo().power : 20.0F;
                            int n = super.rand.nextInt(1 + (int) pwr / 3) + (int) pwr / 2 + 1;
                            pwr *= 0.03F;

                            for (int j = 0; j < n; ++j) {
                                MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "splash", px, py + 0.5D, pz, (double) pwr * (super.rand.nextDouble() - 0.5D) * 0.3D, (double) pwr * (super.rand.nextDouble() * 0.5D + 0.5D) * 1.8D, (double) pwr * (super.rand.nextDouble() - 0.5D) * 0.3D, pwr * 5.0F);
                                MCH_ParticlesUtil.spawnParticle(prm);
                            }

                            return;
                        }
                    }
                }

            }
        }
    }

    public void onUpdateTimeout() {
        if (this.isInWater()) {
            if (this.explosionPowerInWater > 0) {
                this.newExplosion(super.posX, super.posY, super.posZ, (float) this.explosionPowerInWater, (float) this.explosionPowerInWater, true);
            }
        } else if (this.explosionPower > 0) {
            this.newExplosion(super.posX, super.posY, super.posZ, (float) this.explosionPower, (float) this.getInfo().explosionBlock, false);
        } else if (this.explosionPower < 0) {
            this.playExplosionSound();
        }

    }

    public void onUpdateBomblet() {
        if (!super.worldObj.isRemote && this.sprinkleTime > 0 && !super.isDead) {
            --this.sprinkleTime;
            if (this.sprinkleTime == 0) {
                //  if (this.isTVGuided) {
                //
                //  }
                for (int i = 0; i < this.getInfo().bomblet; ++i) {
                    this.sprinkleBomblet();
                }
                //todo if the z alignment doesn't work make this logic more sound by ensuring that the bomblet variable is even defined
                //System.out.println("set dead BOMBLET EDITION");
                this.setDead();
            }
        }

    }

    public void boundBullet(int sideHit) {
        switch (sideHit) {
            case 0:
                if (super.motionY > 0.0D) {
                    super.motionY = -super.motionY * (double) this.getInfo().bound;
                }
                break;
            case 1:
                if (super.motionY < 0.0D) {
                    super.motionY = -super.motionY * (double) this.getInfo().bound;
                }
                break;
            case 2:
                if (super.motionZ > 0.0D) {
                    super.motionZ = -super.motionZ * (double) this.getInfo().bound;
                } else {
                    super.posZ += super.motionZ;
                }
                break;
            case 3:
                if (super.motionZ < 0.0D) {
                    super.motionZ = -super.motionZ * (double) this.getInfo().bound;
                } else {
                    super.posZ += super.motionZ;
                }
                break;
            case 4:
                if (super.motionX > 0.0D) {
                    super.motionX = -super.motionX * (double) this.getInfo().bound;
                } else {
                    super.posX += super.motionX;
                }
                break;
            case 5:
                if (super.motionX < 0.0D) {
                    super.motionX = -super.motionX * (double) this.getInfo().bound;
                } else {
                    super.posX += super.motionX;
                }
        }

    }

    protected void onUpdateCollided() {
        //todo: unforce chunk here too just to prevent the biggest destroyer of computers from activating
        float damageFator = 1.0F;
        double mx = super.motionX * this.accelerationFactor;
        double my = super.motionY * this.accelerationFactor;
        double mz = super.motionZ * this.accelerationFactor;
        MovingObjectPosition m = null;

        Vec3 vec3;
        Vec3 vec31;
        for (int entity = 0; entity < 5; ++entity) {
            vec3 = W_WorldFunc.getWorldVec3(super.worldObj, super.posX, super.posY, super.posZ);
            vec31 = W_WorldFunc.getWorldVec3(super.worldObj, super.posX + mx, super.posY + my, super.posZ + mz);
            m = W_WorldFunc.clip(super.worldObj, vec3, vec31);
            boolean list = false;
            if (this.shootingEntity != null && W_MovingObjectPosition.isHitTypeTile(m)) {
                Block d0 = W_WorldFunc.getBlock(super.worldObj, m.blockX, m.blockY, m.blockZ);
                MCH_Config var10000 = MCH_MOD.config;
                if (MCH_Config.bulletBreakableBlocks.contains(d0)) {
                    W_WorldFunc.destroyBlock(super.worldObj, m.blockX, m.blockY, m.blockZ, true);
                    list = true;
                }
            }

            if (!list) {
                break;
            }
        }

        vec3 = W_WorldFunc.getWorldVec3(super.worldObj, super.posX, super.posY, super.posZ);
        vec31 = W_WorldFunc.getWorldVec3(super.worldObj, super.posX + mx, super.posY + my, super.posZ + mz);
        if (this.getInfo().delayFuse > 0) {
            if (m != null) {
                this.boundBullet(m.sideHit);
                if(this.delayFuse == 0) {
                    this.delayFuse = this.getInfo().delayFuse;
                }
            }

        } else {
            if (m != null) {
                vec31 = W_WorldFunc.getWorldVec3(super.worldObj, m.hitVec.xCoord, m.hitVec.yCoord, m.hitVec.zCoord);
            }

            Entity var22 = null;
            List var23 = super.worldObj.getEntitiesWithinAABBExcludingEntity(this, super.boundingBox.addCoord(mx, my, mz).expand(21.0D, 21.0D, 21.0D));
            double var24 = 0.0D;

            for (int j = 0; j < var23.size(); ++j) {
                Entity entity1 = (Entity) var23.get(j);
                if (this.canBeCollidedEntity(entity1)) {
                    float f = 0.3F;
                    AxisAlignedBB axisalignedbb = entity1.boundingBox.expand((double) f, (double) f, (double) f);
                    MovingObjectPosition m1 = axisalignedbb.calculateIntercept(vec3, vec31);
                    if (m1 != null) {
                        double d1 = vec3.distanceTo(m1.hitVec);
                        if (d1 < var24 || var24 == 0.0D) {
                            var22 = entity1;
                            var24 = d1;
                        }
                    }
                }
            }

            if (var22 != null) {
                m = new MovingObjectPosition(var22);
            }

            if (m != null) {
                this.onImpact(m, damageFator);
            }

        }
    }


    public boolean canBeCollidedEntity(Entity entity) {
        if (entity instanceof MCH_EntityChain) {
            return false;
        } else if (!entity.canBeCollidedWith()) {
            return false;
        } else {
            if (entity instanceof MCH_EntityBaseBullet) {
                if (super.worldObj.isRemote) {
                    return false;
                }

                MCH_EntityBaseBullet i$ = (MCH_EntityBaseBullet) entity;
                if (W_Entity.isEqual(i$.shootingAircraft, this.shootingAircraft)) {
                    return false;
                }

                if (W_Entity.isEqual(i$.shootingEntity, this.shootingEntity)) {
                    return false;
                }
            }
            //if(entity instanceof EntityShip) {
            //EntityShip ship = (EntityShip)entity;
            //if(W_Entity.isEqual(entity, this.shootingEntity)){return false;}
            //if(W_Entity.isEqual(entity, this.shootingAircraft)){return false;}

            //if(ship.getCollider().getAllColliding(super.boundingBox).size() == 0){
				/*if(ship.getCollider().getCollidingBoxes(this).size() == 0) {

					//System.out.println("Yote");
					return false;
				}else {
					//System.out.println("Yeet");
					return true;
				}*/
            //		if(!ship.getCollider().isColliding(this.boundingBox)) {
            //		return false;
            //	}
            //	System.out.println("colliding " + this.shootingEntity + " " + entity);
            //}
            if (entity instanceof MCH_EntitySeat) {
                return false;
            } else if (entity instanceof MCH_EntityHitBox) {
                return false;
            } else if (W_Entity.isEqual(entity, this.shootingEntity)) {
                return false;
            } else {
                if (this.shootingAircraft instanceof MCH_EntityAircraft) {
                    if (W_Entity.isEqual(entity, this.shootingAircraft)) {
                        return false;
                    }

                    if (((MCH_EntityAircraft) this.shootingAircraft).isMountedEntity(entity)) {
                        return false;
                    }
                }

                MCH_Config var10000 = MCH_MOD.config;
                Iterator i$1 = MCH_Config.IgnoreBulletHitList.iterator();

                String s;
                do {
                    if (!i$1.hasNext()) {
                        return true;
                    }

                    s = (String) i$1.next();
                } while (entity.getClass().getName().toLowerCase().indexOf(s.toLowerCase()) < 0);

                return false;
            }
        }
    }

    public void notifyHitBullet() {
        if (this.shootingAircraft instanceof MCH_EntityAircraft && W_EntityPlayer.isPlayer(this.shootingEntity)) {
            MCH_PacketNotifyHitBullet.send((MCH_EntityAircraft) this.shootingAircraft, (EntityPlayer) this.shootingEntity);
        }

        if (W_EntityPlayer.isPlayer(this.shootingEntity)) {
            MCH_PacketNotifyHitBullet.send((MCH_EntityAircraft) null, (EntityPlayer) this.shootingEntity);
        }

    }


    public void onImpact(MovingObjectPosition hit, float damageFactor) {

        //todo shouldLoadChunks() check here
        if (shouldLoadChunks()) {
            System.out.println("should load chunks3");
            checkAndLoadChunks();
            loadNeighboringChunks(getChunkX(), getChunkZ());
            System.out.println("Extra chunk loader activated.");
        }

        if (!worldObj.isRemote) { // Server-side logic
            try {
                handleServerSideImpact(hit, damageFactor);
            } catch (Exception e) {
                MCH_Lib.Log(this, "Error in onImpact: %s", e.getMessage());
                e.printStackTrace();
            }
        } else if (shouldHandleTileHit(hit)) {
            handleTileHit(hit);
        }
    }

    private void handleServerSideImpact(MovingObjectPosition hit, float damageFactor) {
        if (hit.entityHit != null) {
            processEntityImpact(hit, damageFactor);
        }

        if(hit.entityHit instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft ac = (MCH_EntityAircraft) hit.entityHit;
            if(ac.ironCurtainRunningTick > 0) {
                spawnIronCurtainParticle(hit, hit.blockX, hit.blockY, hit.blockZ);
            }
        }

        float explosionPower = this.explosionPower * damageFactor;
        float waterExplosionPower = this.explosionPowerInWater * damageFactor;

        if (piercing > 0) {
            handlePiercingHit(hit, explosionPower, waterExplosionPower, damageFactor);
        } else {
            handleRegularHit(hit, explosionPower, waterExplosionPower);
            finalizeImpact();
        }
    }

    @SideOnly(Side.CLIENT)
    public void spawnIronCurtainParticle(MovingObjectPosition raytraceResult, int xTile, int yTile, int zTile) {
        // 定义暗红色参数（RGB：0.5, 0.1, 0.1）
        final float DARK_RED_R = 0.5f;
        final float DARK_RED_G = 0.1f;
        final float DARK_RED_B = 0.1f;

        int num = getInfo().flakParticlesCrack + rand.nextInt(3);
        float scale = 1.0F;
        for (int i = 0; i < num; i++) {
            EntityDiggingFX fx = new EntityDiggingFX(
                    this.worldObj,
                    raytraceResult.hitVec.xCoord + (rand.nextFloat() - 0.5D) * width,
                    raytraceResult.hitVec.yCoord + 0.1D,
                    raytraceResult.hitVec.zCoord + (rand.nextFloat() - 0.5D) * width,
                    0, 0, 0,
                    worldObj.getBlock(xTile, yTile, zTile),
                    this.worldObj.getBlockMetadata(xTile, yTile, zTile)
            );

            // 覆盖原有颜色设置
            fx.setRBGColorF(DARK_RED_R, DARK_RED_G, DARK_RED_B); // 强制设置为暗红色
            fx.multipleParticleScaleBy(scale * 0.8f); // 适当缩小粒子尺寸

            // 调整运动参数
            fx.motionX += getInfo().flakParticlesDiff * (rand.nextGaussian() * 0.5);
            fx.motionZ += getInfo().flakParticlesDiff * (rand.nextGaussian() * 0.5);
            fx.motionY += getInfo().flakParticlesDiff * Math.abs(rand.nextGaussian());

            Minecraft.getMinecraft().effectRenderer.addEffect(fx);



        }
    }

    @SideOnly(Side.CLIENT)
    public void spawnBlockPar(MovingObjectPosition raytraceResult, int xTile, int yTile, int zTile) {
        int num = getInfo().flakParticlesCrack + rand.nextInt(3);
        float scale = 1.0F;
        for (int i = 0; i < num; i++) {
            EntityDiggingFX fx = (new EntityDiggingFX(this.worldObj,
                    raytraceResult.hitVec.xCoord + (rand.nextFloat() - 0.5D) * width,
                    raytraceResult.hitVec.yCoord + 0.1D,
                    raytraceResult.hitVec.zCoord + (rand.nextFloat() - 0.5D) * width,
                    0, 0, 0, worldObj.getBlock(xTile, yTile, zTile),
                    this.worldObj.getBlockMetadata(xTile, yTile, zTile))).applyRenderColor(this.worldObj.getBlockMetadata(xTile, yTile, zTile));
            fx.motionX += getInfo().flakParticlesDiff / 2 * rand.nextGaussian();
            fx.motionZ += getInfo().flakParticlesDiff / 2 * rand.nextGaussian();
            fx.motionY += getInfo().flakParticlesDiff * Math.abs(rand.nextGaussian());
            fx.multipleParticleScaleBy(scale);
            Minecraft.getMinecraft().effectRenderer.addEffect(fx);
        }
    }

    private void processEntityImpact(MovingObjectPosition hit, float damageFactor) {
        if (shouldLoadChunks()) {
            System.out.println("should load chunks4");
            checkAndLoadChunks();
            loadNeighboringChunks(getChunkX(), getChunkZ());
            System.out.println("Extra chunk loader activated.");
        }
        onImpactEntity(hit.entityHit, damageFactor);

        //resetEntityMotion(hit.entityHit);
        MCH_EntityUtils.resetMotionIfNotMCHVehicle(hit.entityHit);
    }

    private void handlePiercingHit(MovingObjectPosition hit, float explosionPower, float waterExplosionPower, float damageFactor) {
        piercing--;
        if (piercing <= 0) {
            handleRegularHit(hit, explosionPower, waterExplosionPower);
        }
        processBlockDestruction(hit, damageFactor);
    }

    private void processBlockDestruction(MovingObjectPosition hit, float damageFactor) {
        int x = (int) hit.hitVec.xCoord;
        int y = (int) hit.hitVec.yCoord;
        int z = (int) hit.hitVec.zCoord;
        Block block = worldObj.getBlock(x, y, z);
        float waterExplosionPower = this.explosionPowerInWater * damageFactor;

        if (block == Blocks.bedrock) {
            newExplosion(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord, 1.0F, 1.0F, false);
        } else {
            if (explosionPower > 0.0F) {
                //worldObj.setBlockToAir(x, y, z);
                newExplosion(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord, 1.0F, 1.0F, false);
            } else {
                if (piercing > 0) {
                    handlePiercingHit(hit, explosionPower, waterExplosionPower, damageFactor);
                }
                //hopefully stops bullshit where things that should NOT explode are exploding YAY THIS IS SO COOL I LOVE THIS FUCKNIG MOD YAY YAY YAY FUCK
                newExplosion(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord, 0.0F, 0.0F, false);
            }
        }
    }

    private void handleRegularHit(MovingObjectPosition hit, float explosionPower, float waterExplosionPower) {
        try {
            if (shouldCreateFAExplosion()) {
                newFAExplosion(posX, posY, posZ, explosionPower, getInfo().explosionBlock);
            } else if (explosionPower > 0.0F) {
                newExplosion(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord, explosionPower, getInfo().explosionBlock, false);
            } else if (explosionPower < 0.0F) {
                playExplosionSound();
            }
        } catch (Throwable t) {
            MCH_Lib.Log(this, "Error creating explosion: %s", t.getMessage());
            t.printStackTrace();
        }
    }

    private void finalizeImpact() {
        if (!super.isDead) {
            setDead();
            System.out.println("Impact detected, entity set to dead.");
            //todone do another chunk load then clear and add checks
            if (shouldLoadChunks()) {
                System.out.println("should load chunks5");
                checkAndLoadChunks();
                loadNeighboringChunks(getChunkX(), getChunkZ());
                clearBulletChunks();
            }
        }
    }

    //private void clearChunkLoaders() {
    //    if (shouldClearChunkLoaders()) {
    //        for (ChunkCoordIntPair chunk : loadedChunks) {
    //            System.out.println("Clearing chunk loader due to impact.");
    //            ForgeChunkManager.unforceChunk(loaderTicket, chunk);
    //        }
    //    }
    //}
    //unused

    public void clearBulletChunks() {
        if (!worldObj.isRemote && loaderTicket != null) {
            for (ChunkCoordIntPair chunk : bulletLoadedChunks) {
                ForgeChunkManager.unforceChunk(loaderTicket, chunk);
            }
            bulletLoadedChunks.clear();
        }
    }

    // Utility methods
    private int getChunkX() { return (int) Math.floor(posX / 16.0); }
    private int getChunkZ() { return (int) Math.floor(posZ / 16.0); }
    public boolean shouldLoadChunks() { return !bomblet && gravitydown && bigdelay && bigcheck; }
    private boolean shouldClearChunkLoaders() { return !bomblet && gravitydown && bigdelay; }
    private boolean shouldCreateFAExplosion() { return getInfo().isFAE; }
    private boolean shouldHandleTileHit(MovingObjectPosition hit) {
        return getInfo() != null && (getInfo().explosion == 0 || getInfo().modeNum >= 2) && W_MovingObjectPosition.isHitTypeTile(hit);
    }

    //private void resetEntityMotion(Entity entity) {
    //    if (entity instanceof MCH_EntityAircraft || entity instanceof MCH_EntitySeat || entity instanceof MCH_EntityVehicle || entity instanceof MCH_EntityShip || entity instanceof MCH_EntityHitBox || entity instanceof MCH_EntityBaseBullet || entity instanceof MCH_EntityGunner || entity instanceof MCH_DummyEntityPlayer || entity instanceof MCH_EntityHeli || entity instanceof MCP_EntityPlane || entity instanceof MCH_EntityUavStation) {
    //        //hopefully fixes the issue of vehicles (mainly flying 'aircraft') dropping like flies after being impacted
    //        //covers literally everything except tanks I can't think of a better way to do this.
    //    } else {
    //        entity.motionX = 0;
    //        entity.motionY = 0;
    //        entity.motionZ = 0;
    //    }
    //}


    private void handleTileHit(MovingObjectPosition hit) {
        float power = getInfo().power;

        for (int i = 0; i < power / 3.0F; i++) {
            MCH_ParticlesUtil.spawnParticleTileCrack(
                    worldObj,
                    hit.blockX,
                    hit.blockY,
                    hit.blockZ,
                    hit.hitVec.xCoord + (rand.nextFloat() - 0.5) * power / 10.0,
                    hit.hitVec.yCoord + 0.1,
                    hit.hitVec.zCoord + (rand.nextFloat() - 0.5) * power / 10.0,
                    -motionX * power / 2.0,
                    power / 2.0F,
                    -motionZ * power / 2.0
            );
        }
    }

    private final Set<Entity> hitEntities = new HashSet<>();

    public void onImpactEntity(Entity entity, float damageFactor) {
        if (!hitEntities.contains(entity)) {
            hitEntities.add(entity);
            if (this.piercing > 0 || !entity.isDead) { //entity.isDead check to not decrease piercing for dead targets
                MCH_Lib.DbgLog(super.worldObj, "MCH_EntityBaseBullet.onImpactEntity:Damage=%d:" + entity.getClass(), new Object[]{Integer.valueOf(this.getPower())});
                MCH_Lib.applyEntityHurtResistantTimeConfig(entity);
                DamageSource ds = DamageSource.causeThrownDamage(this, this.shootingEntity);
                if (this.power == 1) {
                    ds = new MCH_DamageSource("bullet", this);

                    this.power *= this.weaponInfo.damageFactor.getDamageFactor(EntityPlayer.class);
                }
                MCH_Config var10000 = MCH_MOD.config;
                float damage = MCH_Config.applyDamageVsEntity(entity, ds, (float) this.getPower() * damageFactor);
                damage *= this.getInfo() != null ? this.getInfo().getDamageFactor(entity) : 1.0F;
                entity.attackEntityFrom(ds, damage);
                --this.piercing;
            } else {
                //this.piercing = 0;

                //if (!entity.isDead) {
                MCH_Lib.DbgLog(super.worldObj, "MCH_EntityBaseBullet.onImpactEntity:Damage=%d:" + entity.getClass(), new Object[]{Integer.valueOf(this.getPower())});
                MCH_Lib.applyEntityHurtResistantTimeConfig(entity);
                DamageSource ds = DamageSource.causeThrownDamage(this, this.shootingEntity);
                if (this.power == 1) {
                    ds = new MCH_DamageSource("bullet", this);

                    this.power *= this.weaponInfo.damageFactor.getDamageFactor(EntityPlayer.class);
                }
                //todone: add piercing compat here


                MCH_Config var10000 = MCH_MOD.config;
                float damage = MCH_Config.applyDamageVsEntity(entity, ds, (float) this.getPower() * damageFactor);
                damage *= this.getInfo() != null ? this.getInfo().getDamageFactor(entity) : 1.0F;
                entity.attackEntityFrom(ds, damage);
                if (this instanceof MCH_EntityBullet && entity instanceof EntityVillager && this.shootingEntity != null && this.shootingEntity.ridingEntity instanceof MCH_EntitySeat) {
                    MCH_Achievement.addStat(this.shootingEntity, MCH_Achievement.aintWarHell, 1);
                }

                if (entity.isDead) {
                    System.out.println("isdead");
                    ;
                }
                //}

                this.notifyHitBullet();
                //}
            }
        }
    }

    public void newFAExplosion(double x, double y, double z, float exp, float expBlock) {
        MCH_Explosion.ExplosionResult result = MCH_Explosion.newExplosion(super.worldObj, this, this.shootingEntity, x, y, z, exp, expBlock, true, true, this.getInfo().flaming, false, 15);
        if (result != null && result.hitEntity) {
            this.notifyHitBullet();
        }

    }

    public void newExplosion(double x, double y, double z, float exp, float expBlock, boolean inWater) {
        MCH_Explosion.ExplosionResult result;
        if(!inWater) {
            if (this.getInfo().explosionType.equals("hbmNT_Bomb")) {
                int hbmExplosionPower = this.explosionPower;

                Object explosionNTInstance = MCH_HBMUtil.ExplosionNT_instance_init(super.worldObj, null, x, y, z, expBlock);
                if (explosionNTInstance != null) {
                    MCH_HBMUtil.ExplosionNT_instance_addAttrib(explosionNTInstance, "NOHURT");
                    MCH_HBMUtil.ExplosionNT_instance_overrideResolutionAndExplode(explosionNTInstance, 64);
                }
                MCH_HBMUtil.ExplosionCreator_composeEffectStandard(worldObj, x + 0.5, y + 1, z + 0.5, (int) expBlock);
                result = MCH_Explosion.newExplosion(super.worldObj, this, this.shootingEntity, x, y, z, exp, expBlock, this.isBomblet == 1 ? super.rand.nextInt(3) == 0 : true, false, this.getInfo().flaming, false, 0, this.getInfo() != null ? this.getInfo().damageFactor : null);
            } else {
                result = MCH_Explosion.newExplosion(super.worldObj, this, this.shootingEntity, x, y, z, exp, expBlock, this.isBomblet == 1 ? super.rand.nextInt(3) == 0 : true, true, this.getInfo().flaming, true, 0, this.getInfo() != null ? this.getInfo().damageFactor : null);
            }
        } else {
            result = MCH_Explosion.newExplosionInWater(super.worldObj, this, this.shootingEntity, x, y, z, exp, expBlock, this.isBomblet == 1 ? super.rand.nextInt(3) == 0 : true, true, this.getInfo().flaming, true, 0, this.getInfo() != null ? this.getInfo().damageFactor : null);
        }

        if(this.getInfo().nukeYield > 0) {
            if(!this.getInfo().nukeEffectOnly) {
                worldObj.spawnEntityInWorld((Entity) MCH_HBMUtil.EntityNukeExplosionMK5_statFac(super.worldObj, this.getInfo().nukeYield, this.posX + 0.5, this.posY + 0.5, this.posZ + 0.5));
            }
            MCH_HBMUtil.EntityNukeTorex_statFac(super.worldObj, this.posX + 0.5, this.posY + 0.5, this.posZ + 0.5, (float) this.getInfo().nukeYield);
        }

        if(this.getInfo().chemYield > 0) {
            System.out.println("chem yield detected");
            MCH_HBMUtil.ExplosionChaos_spawnChlorine(super.worldObj, posX, posY + 0.5, posZ, this.getInfo().chemYield);
        }

        if (result != null && result.hitEntity) {
            this.notifyHitBullet();
        }

    }

    public void playExplosionSound() {
        MCH_Explosion.playExplosionSound(super.worldObj, super.posX, super.posY, super.posZ);
    }

    public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
        par1NBTTagCompound.setTag("direction", this.newDoubleNBTList(new double[]{super.motionX, super.motionY, super.motionZ}));
        par1NBTTagCompound.setString("WeaponName", this.getName());
    }

    public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
        //("read entity from nbt, would set dead but commented out");
        this.setDead();
        System.out.println("setting dead due to readentityfromnbt");
    }

    public boolean canBeCollidedWith() {
        return true;
    }

    public float getCollisionBorderSize() {
        return 1.0F;
    }

    public boolean attackEntityFrom(DamageSource ds, float par2) {
        if (this.isEntityInvulnerable()) {
            return false;
        } else if (!super.worldObj.isRemote && par2 > 0.0F && ds.getDamageType().equalsIgnoreCase("thrown")) {
            this.setBeenAttacked();
            MovingObjectPosition m = new MovingObjectPosition((int) (super.posX + 0.5D), (int) (super.posY + 0.5D), (int) (super.posZ + 0.5D), 0, Vec3.createVectorHelper(super.posX + 0.5D, super.posY + 0.5D, super.posZ + 0.5D));
            this.onImpact(m, 1.0F);
            return true;
        } else {
            return false;
        }
    }

    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 0.0F;
    }

    public float getBrightness(float par1) {
        return 1.0F;
    }

    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender(float par1) {
        return 15728880;
    }

    public int getPower() {
        return this.power;
    }

    public void setPower(int power) {
        this.power = power;
    }
}
// public boolean shouldRenderRocketTrail() {
//    return true;
// }
//}
