package mcheli.weapon;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.gui.MCH_Gui;
import mcheli.plane.MCP_EntityPlane;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_Render;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

public class MCH_RenderLockBox extends W_Render {
    @Override
    public void doRender(Entity entity, double posX, double posY, double posZ, float par8, float tickTime) {
        renderGuidanceHUD();
    }


    public static void renderGuidanceHUD() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if(player == null) return;
        MCH_EntityAircraft ac = null; //玩家乘坐的实体
        if(player.ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft)player.ridingEntity;
        } else if(player.ridingEntity instanceof MCH_EntitySeat) {
            ac = ((MCH_EntitySeat)player.ridingEntity).getParent();
        } else if(player.ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation)player.ridingEntity).getControlAircract();
        }
        if(ac == null) return;
        MCH_IGuidanceSystem guidanceSystem = ac.getCurrentWeapon(player).getCurrentWeapon().getGuidanceSystem();
        if(guidanceSystem == null) {
            return;
        }

        if (guidanceSystem instanceof MCH_LaserGuidanceSystem) {

            if(!((MCH_LaserGuidanceSystem) guidanceSystem).targeting) return;

            double lockPosX = guidanceSystem.getLockPosX();
            double lockPosY = guidanceSystem.getLockPosY();
            double lockPosZ = guidanceSystem.getLockPosZ();

//            double posX = player.posX;
//            double posY = player.posY + player.getEyeHeight();
//            double posZ = player.posZ;

            double posX = RenderManager.renderPosX;
            double posY = RenderManager.renderPosY;
            double posZ = RenderManager.renderPosZ;

            RenderManager rm = RenderManager.instance;
            double distance = Math.sqrt(Math.pow(lockPosX - posX, 2) + Math.pow(lockPosY - posY, 2) + Math.pow(lockPosZ - posZ, 2));

            double x = lockPosX - posX;
            double y = lockPosY - posY;
            double z = lockPosZ - posZ;

            if(distance > 1000) return;

            GL11.glPushMatrix();
            // 进行位置变换，将目标实体渲染到玩家视角中
            GL11.glTranslatef((float)x, (float)y , (float)z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(-0.02666667F, -0.02666667F, 0.02666667F);
            GL11.glDisable(2896); // 禁用深度测试
            GL11.glTranslatef(0.0F, 9.374999F, 0.0F); // 上移一些偏移量
            GL11.glDepthMask(false); // 禁用深度写入
            GL11.glEnable(3042); // 启用混合
            GL11.glBlendFunc(770, 771); // 设置混合模式
            GL11.glDisable(3553); // 禁用纹理
            GL11.glDisable(2929 /* GL_DEPTH_TEST */);

            // 获取绘制前的屏幕宽度
            int prevWidth = GL11.glGetInteger(2849);
            // 设置目标实体大小 50-20, 1000-1000
            float minDistance = 50.0F;
            float size1 = 20.0F;
            float maxDistance = 300.0F;
            float maxSize = 100.0F;
            float size = size1 + (float)((distance - minDistance) / (maxDistance - minDistance)) * (maxSize - size1);
            size = Math.max(size1, Math.min(maxSize, size));


            // 创建Tessellator对象，用于绘制图形
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawing(2); // 开始绘制线条
            tessellator.setBrightness(240); // 设置亮度

            GL11.glLineWidth((float) MCH_Gui.scaleFactor * 1.5F); // 设置线宽
            tessellator.setColorRGBA_F(0.0F, 1.0F, 0.0F, 1.0F); // 绿色

            // 绘制矩形框，表示锁定范围
            tessellator.addVertex(-size - 1.0F, 0.0D, 0.0D);
            tessellator.addVertex(-size - 1.0F, size * 2.0F, 0.0D);
            tessellator.addVertex(size + 1.0F, size * 2.0F, 0.0D);
            tessellator.addVertex(size + 1.0F, 0.0D, 0.0D);
            tessellator.draw(); // 绘制线条


            if(distance > 10) {
                // 获取 FontRenderer 对象并设置颜色为绿色
                FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
                {
                    GL11.glPushMatrix();
                    GL11.glTranslatef(0.0F, size * 2.0F + 1.0F, 0.0F); // 将文本放置在矩形框的下方
                    float fontSize = 5.0F + (float) ((distance - 10.0D) / (300.0D - 10.0D)) * (40.0F - 5.0F);
                    // 确保字体大小在5和40之间
                    fontSize = Math.max(5.0F, Math.min(40.0F, fontSize));
                    GL11.glScalef(fontSize, fontSize, fontSize);
                    String text = String.format("%.1f", distance); // 格式化为一位小数
                    fontRenderer.drawString(text, -fontRenderer.getStringWidth(text) / 2, 0, 0x00ff00);
                    GL11.glPopMatrix();
                }
            }

            GL11.glPopMatrix();
            // 恢复之前的线宽，启用纹理，恢复深度写入和深度测试
            GL11.glLineWidth((float) prevWidth);
            GL11.glEnable(3553);
            GL11.glDepthMask(true);
            GL11.glEnable(2896);
            GL11.glDisable(3042);
            GL11.glEnable(2929 /* GL_DEPTH_TEST */);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F); // 恢复默认颜色
        }
    }
}
