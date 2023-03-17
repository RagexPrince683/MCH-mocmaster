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
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

public class W_EventHook {
    @SubscribeEvent
    public void onEvent_entitySpawn(EntityJoinWorldEvent event) {
        this.entitySpawn(event);
    }

    public void entitySpawn(EntityJoinWorldEvent event) {
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

