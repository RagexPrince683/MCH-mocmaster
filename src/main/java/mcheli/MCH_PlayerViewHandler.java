package mcheli;

import net.minecraft.client.Minecraft;

import java.util.Random;

public class MCH_PlayerViewHandler {

    /**
     * 通过射击作用于玩家视野的后坐力
     */
    public static float playerRecoilPitch;
    public static float playerRecoilYaw;
    /**
     * 为使后坐力恢复正常，对后坐力施加的补偿量
     */
    public static float antiRecoilPitch;
    public static float antiRecoilYaw;

    public static Minecraft minecraft = Minecraft.getMinecraft();

    public static float recoilControl = 0.8f;

    public static void applyRecoil(float pitch, float yaw, float control) {
        playerRecoilPitch += pitch;
        playerRecoilYaw += yaw;
        recoilControl = control;
    }

    /**
     * 每帧更新视角抖动效果
     */
    public static void onUpdate() {

        if(minecraft.thePlayer == null) {
            return;
        }

        if (playerRecoilPitch > 0) {
            playerRecoilPitch *= recoilControl;
        }

        minecraft.thePlayer.rotationPitch -= playerRecoilPitch;
        minecraft.thePlayer.rotationYaw -= playerRecoilYaw;
        antiRecoilPitch += playerRecoilPitch;
        antiRecoilYaw += playerRecoilYaw;

        minecraft.thePlayer.rotationPitch += antiRecoilPitch * 0.2F;
        minecraft.thePlayer.rotationYaw += antiRecoilYaw * 0.2F;

        antiRecoilPitch *= 0.8F;
        antiRecoilYaw *= 0.8F;

        playerRecoilYaw *= 0.8F;
    }
}
