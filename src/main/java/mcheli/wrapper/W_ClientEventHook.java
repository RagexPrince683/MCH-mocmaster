/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.client.event.MouseEvent
 *  net.minecraftforge.client.event.RenderLivingEvent
 *  net.minecraftforge.client.event.RenderLivingEvent$Post
 *  net.minecraftforge.client.event.RenderLivingEvent$Pre
 *  net.minecraftforge.client.event.RenderLivingEvent$Specials
 *  net.minecraftforge.client.event.RenderLivingEvent$Specials$Post
 *  net.minecraftforge.client.event.RenderLivingEvent$Specials$Pre
 *  net.minecraftforge.client.event.RenderPlayerEvent
 *  net.minecraftforge.client.event.RenderPlayerEvent$Post
 *  net.minecraftforge.client.event.RenderPlayerEvent$Pre
 *  net.minecraftforge.event.entity.EntityJoinWorldEvent
 *  net.minecraftforge.event.world.WorldEvent
 *  net.minecraftforge.event.world.WorldEvent$Unload
 */
package mcheli.wrapper;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;

public class W_ClientEventHook {
    @SubscribeEvent
    public void onEvent_MouseEvent(MouseEvent event) {
        this.mouseEvent(event);
    }

    public void mouseEvent(MouseEvent event) {
    }

    @SubscribeEvent
    public void onEvent_renderLivingEventSpecialsPre(RenderLivingEvent.Specials.Pre event) {
        this.renderLivingEventSpecialsPre(event);
    }

    public void renderLivingEventSpecialsPre(RenderLivingEvent.Specials.Pre event) {
    }

    @SubscribeEvent
    public void onEvent_renderLivingEventSpecialsPost(RenderLivingEvent.Specials.Post event) {
        this.renderLivingEventSpecialsPost(event);
    }

    public void renderLivingEventSpecialsPost(RenderLivingEvent.Specials.Post event) {
    }

    @SubscribeEvent
    public void onEvent_renderLivingEventPre(RenderLivingEvent.Pre event) {
        this.renderLivingEventPre(event);
    }

    public void renderLivingEventPre(RenderLivingEvent.Pre event) {
    }

    @SubscribeEvent
    public void onEvent_renderLivingEventPost(RenderLivingEvent.Post event) {
        this.renderLivingEventPost(event);
    }

    public void renderLivingEventPost(RenderLivingEvent.Post event) {
    }

    @SubscribeEvent
    public void onEvent_renderPlayerPre(RenderPlayerEvent.Pre event) {
        this.renderPlayerPre(event);
    }

    public void renderPlayerPre(RenderPlayerEvent.Pre event) {
    }

    @SubscribeEvent
    public void Event_renderPlayerPost(RenderPlayerEvent.Post event) {
        this.renderPlayerPost(event);
    }

    public void renderPlayerPost(RenderPlayerEvent.Post event) {
    }

    @SubscribeEvent
    public void onEvent_WorldEventUnload(WorldEvent.Unload event) {
        this.worldEventUnload(event);
    }

    public void worldEventUnload(WorldEvent.Unload event) {
    }

    @SubscribeEvent
    public void onEvent_EntityJoinWorldEvent(EntityJoinWorldEvent event) {
        this.entityJoinWorldEvent(event);
    }

    public void entityJoinWorldEvent(EntityJoinWorldEvent event) {
    }
}

