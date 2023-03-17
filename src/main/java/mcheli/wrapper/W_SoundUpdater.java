/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.audio.ISound
 *  net.minecraft.client.audio.SoundHandler
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.ResourceLocation
 */
package mcheli.wrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class W_SoundUpdater {
    protected final SoundHandler theSoundHnadler;
    protected W_Sound es;

    public W_SoundUpdater(Minecraft minecraft, Entity entity) {
        this.theSoundHnadler = minecraft.getSoundHandler();
    }

    public void initEntitySound(String name) {
        this.es = new W_Sound(new ResourceLocation("mcheli", name), 1.0f, 1.0f);
        this.es.setRepeat(true);
    }

    public boolean isValidSound() {
        return this.es != null;
    }

    public void playEntitySound(String name, Entity entity, float volume, float pitch, boolean par5) {
        if (this.isValidSound()) {
            this.es.setSoundParam(entity, volume, pitch);
            this.theSoundHnadler.playSound((ISound)this.es);
        }
    }

    public void stopEntitySound(Entity entity) {
        if (this.isValidSound()) {
            this.theSoundHnadler.stopSound((ISound)this.es);
        }
    }

    public boolean isEntitySoundPlaying(Entity entity) {
        return this.isValidSound() ? this.theSoundHnadler.isSoundPlaying((ISound)this.es) : false;
    }

    public void setEntitySoundPitch(Entity entity, float pitch) {
        if (this.isValidSound()) {
            this.es.setPitch(pitch);
        }
    }

    public void setEntitySoundVolume(Entity entity, float volume) {
        if (this.isValidSound()) {
            this.es.setVolume(volume);
        }
    }

    public void updateSoundLocation(Entity entity) {
        if (this.isValidSound()) {
            this.es.setPosition(entity);
        }
    }

    public void updateSoundLocation(double x, double y, double z) {
        if (this.isValidSound()) {
            this.es.setPosition(x, y, z);
        }
    }

    public void _updateSoundLocation(Entity entityListener, Entity entity) {
        if (this.isValidSound()) {
            this.es.setPosition(entity);
        }
    }
}

