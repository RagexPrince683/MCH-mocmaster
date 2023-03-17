/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.common.eventhandler.SubscribeEvent
 *  cpw.mods.fml.common.gameevent.TickEvent
 *  cpw.mods.fml.common.gameevent.TickEvent$ClientTickEvent
 *  cpw.mods.fml.common.gameevent.TickEvent$Phase
 *  cpw.mods.fml.common.gameevent.TickEvent$PlayerTickEvent
 *  cpw.mods.fml.common.gameevent.TickEvent$RenderTickEvent
 *  net.minecraft.client.Minecraft
 *  net.minecraft.entity.player.EntityPlayer
 */
package mcheli.wrapper;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public abstract class W_TickHandler
implements ITickHandler {
    protected Minecraft mc;

    public W_TickHandler(Minecraft m) {
        this.mc = m;
    }

    public void onPlayerTickPre(EntityPlayer player) {
    }

    public void onPlayerTickPost(EntityPlayer player) {
    }

    public void onRenderTickPre(float partialTicks) {
    }

    public void onRenderTickPost(float partialTicks) {
    }

    public void onTickPre() {
    }

    public void onTickPost() {
    }

    @SubscribeEvent
    public void onPlayerTickEvent(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            this.onPlayerTickPre(event.player);
        }
        if (event.phase == TickEvent.Phase.END) {
            this.onPlayerTickPost(event.player);
        }
    }

    @SubscribeEvent
    public void onClientTickEvent(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            this.onTickPre();
        }
        if (event.phase == TickEvent.Phase.END) {
            this.onTickPost();
        }
    }

    @SubscribeEvent
    public void onRenderTickEvent(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            this.onRenderTickPre(event.renderTickTime);
        }
        if (event.phase == TickEvent.Phase.END) {
            this.onRenderTickPost(event.renderTickTime);
        }
    }

    static enum TickType {
        RENDER,
        CLIENT;
        

        private TickType() {
        }
    }

}

