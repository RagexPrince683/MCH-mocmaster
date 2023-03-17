/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.audio.ISound
 *  net.minecraft.client.audio.SoundManager
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.ResourceLocation
 */
package mcheli.wrapper;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class W_SoundManagerFunc {
    public static void DEF_playEntitySound(SoundManager sm, String name, Entity entity, float volume, float pitch, boolean par5) {
        sm.playSound((ISound)new W_Sound(new ResourceLocation(name), volume, pitch, entity.posX, entity.posY, entity.posZ));
    }

    public static void MOD_playEntitySound(SoundManager sm, String name, Entity entity, float volume, float pitch, boolean par5) {
        W_SoundManagerFunc.DEF_playEntitySound(sm, W_MOD.DOMAIN + ":" + name, entity, volume, pitch, par5);
    }
}

