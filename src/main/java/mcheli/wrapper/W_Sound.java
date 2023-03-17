/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.audio.MovingSound
 *  net.minecraft.client.audio.PositionedSound
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.ResourceLocation
 */
package mcheli.wrapper;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class W_Sound
extends MovingSound {
    protected W_Sound(ResourceLocation r, float volume, float pitch, double x, double y, double z) {
        super(r);
        this.setVolumeAndPitch(volume, pitch);
        this.setPosition(x, y, z);
    }

    protected W_Sound(ResourceLocation r, float volume, float pitch) {
        super(r);
        this.setVolumeAndPitch(volume, pitch);
        Entity entity = W_McClient.getRenderEntity();
        if (entity != null) {
            this.setPosition(entity.posX, entity.posY, entity.posZ);
        }
    }

    public void setRepeat(boolean b) {
        this.repeat = b;
    }

    public void setSoundParam(Entity e, float v, float p) {
        this.setPosition(e);
        this.setVolumeAndPitch(v, p);
    }

    public void setVolumeAndPitch(float v, float p) {
        this.setVolume(v);
        this.setPitch(p);
    }

    public void setVolume(float v) {
        this.volume = v;
    }

    public void setPitch(float p) {
        this.field_147663_c = p;
    }

    public void setPosition(double x, double y, double z) {
        this.xPosF = (float)x;
        this.yPosF = (float)y;
        this.zPosF = (float)z;
    }

    public void setPosition(Entity e) {
        this.setPosition(e.posX, e.posY, e.posZ);
    }

    public void update() {
    }
}

