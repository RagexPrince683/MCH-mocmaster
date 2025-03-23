package mcheli;

import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.entity.Entity;

public class MCH_EntityInfo {
    public int entityId;
    public String worldName;
    public String entityName;
    public String entityClassName;
    public double posX;
    public double posY;
    public double posZ;
    public double lastTickPosX;
    public double lastTickPosY;
    public double lastTickPosZ;
    public long lastUpdateTime;

    public MCH_EntityInfo(int entityId, String worldName, String entityName, String entityClassName, double posX, double posY, double posZ, double lastTickPosX, double lastTickPosY, double lastTickPosZ) {
        this.entityId = entityId;
        this.worldName = worldName;
        this.entityName = entityName;
        this.entityClassName = entityClassName;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.lastTickPosX = lastTickPosX;
        this.lastTickPosY = lastTickPosY;
        this.lastTickPosZ = lastTickPosZ;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public static MCH_EntityInfo createInfo(Entity e) {
        String name = e.getCommandSenderName();
        if(e instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft ac = (MCH_EntityAircraft) e;
            if(ac.getAcInfo() != null) {
                name = ac.getAcInfo().name;
            }
        }
        return new MCH_EntityInfo(e.getEntityId(),
                e.worldObj.getWorldInfo().getWorldName(),
                name,
                e.getClass().getName(),
                e.posX, e.posY, e.posZ,
                e.lastTickPosX, e.lastTickPosY, e.lastTickPosZ
        );
    }

    public double getDistanceToEntity(Entity e) {
        return Math.sqrt((e.posX - posX) * (e.posX - posX) + (e.posY - posY) * (e.posY - posY) + (e.posZ - posZ) * (e.posZ - posZ));
    }

    public double getDistanceSqToEntity(Entity e) {
        return (e.posX - posX) * (e.posX - posX) + (e.posY - posY) * (e.posY - posY) + (e.posZ - posZ) * (e.posZ - posZ);
    }
}