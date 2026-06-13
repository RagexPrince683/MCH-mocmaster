
package mcheli.wrapper;


import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

public class W_McClient {


    public static void DEF_playSoundFX(String name, float volume, float pitch) {
        Minecraft.getMinecraft().getSoundHandler().playSound(new W_Sound(new ResourceLocation(name), volume, pitch));
    }

    public static void MOD_playSoundFX(String name, float volume, float pitch) {
        W_McClient.DEF_playSoundFX(W_MOD.DOMAIN + ":" + name, volume, pitch);
    }

    public static void addSound(String name) {
        Minecraft mc = Minecraft.getMinecraft();
    }

    public static void DEF_bindTexture(String tex) {
        Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(tex));
    }

    public static void MOD_bindTexture(String tex) {
        try {
        Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(W_MOD.DOMAIN, tex));
        }
        catch (Exception e) {
            System.out.println("Texture not found : " + tex);
            Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(W_MOD.DOMAIN, "textures/default.png"));
        }
    }

    public static boolean isGamePaused() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.isGamePaused();
    }

    public static Entity getRenderEntity() {
        return Minecraft.getMinecraft().renderViewEntity;
    }

    public static void setRenderEntity(EntityLivingBase entity) {
        Minecraft.getMinecraft().renderViewEntity = entity;
    }
}

