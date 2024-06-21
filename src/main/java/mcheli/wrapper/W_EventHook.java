/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.event.CommandEvent
 *  net.minecraftforge.event.entity.EntityEvent
 *  net.minecraftforge.event.entity.EntityEvent$CanUpdate
 *  net.minecraftforge.event.entity.EntityJoinWorldEvent
 *  net.minecraftforge.event.entity.living.LivingAttackEvent
 *  net.minecraftforge.event.entity.living.LivingHurtEvent
 *  net.minecraftforge.event.entity.player.EntityInteractEvent
 */
package mcheli.wrapper;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_PacketAircraftLocation;
import mcheli.plane.MCP_EntityPlane;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

import java.util.ArrayList;
import java.util.List;

public class W_EventHook {
    @SubscribeEvent
    public void onEvent_entitySpawn(EntityJoinWorldEvent event) {
        this.entitySpawn(event);
    }

    public void entitySpawn(EntityJoinWorldEvent event) {

    }

    @SubscribeEvent
    void onWorldTick(TickEvent.WorldTickEvent evt) {
        System.out.println("onworldtick");
        World worldObj = evt.world;
        for (Object obj : worldObj.playerEntities) {
            if (obj instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) obj;
                AxisAlignedBB aabb = player.boundingBox.expand(350, 350, 350);
                List<MCH_EntityAircraft> list = new ArrayList<>();
                for (Object entityObj : worldObj.getEntitiesWithinAABBExcludingEntity(player, aabb)) {
                    if (entityObj instanceof MCH_EntityAircraft) {
                        System.out.println("MCH_EntityAircraft");
                        MCH_EntityAircraft plane = (MCH_EntityAircraft) entityObj;
                        if (!plane.onGround) {
                            list.add(plane);
                            MCH_PacketAircraftLocation.send(plane, player);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onEvent_livingHurtEvent(LivingHurtEvent event) {
        this.livingHurtEvent(event);
    }

    public void livingHurtEvent(LivingHurtEvent event) {
    }

    @SubscribeEvent
    public void onEvent_livingAttackEvent(LivingAttackEvent event) {
        this.livingAttackEvent(event);
    }

    public void livingAttackEvent(LivingAttackEvent event) {
    }

    @SubscribeEvent
    public void onEvent_entityInteractEvent(EntityInteractEvent event) {
        this.entityInteractEvent(event);
    }

    public void entityInteractEvent(EntityInteractEvent event) {
    }

    @SubscribeEvent
    public void onEvent_entityCanUpdate(EntityEvent.CanUpdate event) {
        this.entityCanUpdate(event);
    }

    public void entityCanUpdate(EntityEvent.CanUpdate event) {
    }

    @SubscribeEvent
    public void onEvent_commandEvent(CommandEvent event) {
        this.commandEvent(event);
    }

    public void commandEvent(CommandEvent event) {
    }
}

