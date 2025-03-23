package mcheli.flare;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.wrapper.W_McClient;
import net.minecraft.world.World;

import java.util.Random;

public class MCH_Chaff {

    //冷却时长 0代表冷却结束
    public int tick;
    //生效时长 0代表使用结束
    public int useTick;
    //箔条使用时间
    public int chaffUseTime;
    //箔条等待时间
    public int chaffWaitTime;
    public World worldObj;
    public MCH_EntityAircraft aircraft;
    //箔条使用时分批间隔
    private int spawnChaffEntityIntervalTick;
    public final Random rand = new Random();

    public MCH_Chaff(World w, MCH_EntityAircraft ac) {
        this.worldObj = w;
        this.aircraft = ac;
    }

    public boolean onUse() {
        boolean result = false;
        System.out.println("MCH_Chaff.onUse");
        if (worldObj.isRemote) {
            if (tick == 0) {
                tick = chaffWaitTime;
                useTick = chaffUseTime;
                spawnChaffEntityIntervalTick = 0;
                result = true;
                //W_McClient.DEF_playSoundFX("flare_deploy", 10.0F, 1.0F);
            }
        } else {
            result = true;
            tick = chaffWaitTime;
            useTick = chaffUseTime;
            spawnChaffEntityIntervalTick = 0;
            aircraft.getEntityData().setBoolean("ChaffUsing", true);
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
            }
            if(this.useTick > 0) {
                this.onUsing();
            }
            if (!this.isUsing() && this.aircraft.getEntityData().getBoolean("ChaffUsing")) {
                this.aircraft.getEntityData().setBoolean("ChaffUsing", false);
            }
        }
    }

    private void onUsing() {
        if(spawnChaffEntityIntervalTick == 0) {
            spawnChaffEntityIntervalTick = chaffUseTime / 10;
            if(!worldObj.isRemote) {
                spawnChaffEntity();
            }
            if(worldObj.isRemote) {
                W_McClient.MOD_playSoundFX("chaff", 10.0F, 1.0F);
            }
        }
        if(spawnChaffEntityIntervalTick > 0) {
            spawnChaffEntityIntervalTick--;
        }
    }

    private void spawnChaffEntity() {
        // 获取飞机的最后位置
        double x = this.aircraft.lastTickPosX;
        double y = this.aircraft.lastTickPosY;
        double z = this.aircraft.lastTickPosZ;

        // 获取飞机的运动速度
        double motionX = this.aircraft.motionX;
        double motionY = this.aircraft.motionY;
        double motionZ = this.aircraft.motionZ;

        // 创建干扰箔条实体
        double offsetX = -motionX * 20D;
        double offsetY = -motionY * 20D;
        double offsetZ = -motionZ * 20D;

        MCH_EntityChaff e = new MCH_EntityChaff(worldObj,
                x + offsetX, y + offsetY, z + offsetZ,
                motionX * 0.5, motionY * 0.5, motionZ * 0.5);

        // 将干扰箔条实体加入到世界中
        this.worldObj.spawnEntityInWorld(e);
    }


    public boolean isInPreparation() {
        return this.tick != 0;
    }

    public boolean isUsing() {
        return this.useTick > 0;
    }
}
