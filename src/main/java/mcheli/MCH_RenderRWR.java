package mcheli;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_MOD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class MCH_RenderRWR {

    private static final ResourceLocation RWR = new ResourceLocation(W_MOD.DOMAIN, "textures/RWR.png");
    private static final int RWR_SIZE = 180;
    private static final int RWR_CENTER_X = 100;
    private static final int RWR_CENTER_Y = 280;
    private static final double SCREEN_HEIGHT_ADAPT_CONSTANT = 520;

    private static final double MIN_DISTANCE = 50.0;  // 最小显示距离（米）
    private static final double MAX_DISTANCE = 1000.0; // 最大显示距离（米）
    private static final int MIN_RADIUS = 30;          // 最小显示半径（像素

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        //获取基本信息
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        World world = mc.theWorld;
        ScaledResolution sc = new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        if (player == null || world == null) return;

        //获取玩家机载武器
        MCH_EntityAircraft ac = null;
        if(player.ridingEntity instanceof MCH_EntityAircraft) {
            ac = (MCH_EntityAircraft)player.ridingEntity;
        } else if(player.ridingEntity instanceof MCH_EntitySeat) {
            ac = ((MCH_EntitySeat)player.ridingEntity).getParent();
        } else if(player.ridingEntity instanceof MCH_EntityUavStation) {
            ac = ((MCH_EntityUavStation)player.ridingEntity).getControlAircract();
        }
        if(!(ac instanceof MCP_EntityPlane || ac instanceof MCH_EntityHeli)) return;

        //开始渲染
        GL11.glPushMatrix();
        {
            double sx = sc.getScaledHeight() * (RWR_CENTER_X / SCREEN_HEIGHT_ADAPT_CONSTANT);
            double sy = sc.getScaledHeight() * (RWR_CENTER_Y / SCREEN_HEIGHT_ADAPT_CONSTANT);
            drawRWRCircle(sx, sy, sc);

            // 新增实体渲染逻辑
            double circleRadius = sc.getScaledHeight() * (RWR_SIZE / SCREEN_HEIGHT_ADAPT_CONSTANT) / 2.0;
            for(MCH_EntityInfo entity : getServerLoadedEntity()) {
                if(!isValidEntity(entity, player)) continue;

                // 计算插值位置
                double xPos = interpolate(entity.posX, entity.lastTickPosX, event.partialTicks);
                double yPos = interpolate(entity.posY, entity.lastTickPosY, event.partialTicks);
                double zPos = interpolate(entity.posZ, entity.lastTickPosZ, event.partialTicks);

                // 计算相对向量
                Vec3 delta = Vec3.createVectorHelper(
                        xPos - (player.posX + (player.posX - player.lastTickPosX) * event.partialTicks),
                        yPos - (player.posY + (player.posY - player.lastTickPosY) * event.partialTicks),
                        zPos - (player.posZ + (player.posZ - player.lastTickPosZ) * event.partialTicks)
                );

                Vec3 lookVec = getDirection(ac, event.partialTicks);
                Vec3 deltaHorizontal = Vec3.createVectorHelper(delta.xCoord, 0, delta.zCoord).normalize();
                Vec3 lookHorizontal = Vec3.createVectorHelper(lookVec.xCoord, 0, lookVec.zCoord).normalize();

                double dot = lookHorizontal.dotProduct(deltaHorizontal);
                double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
                if(lookHorizontal.crossProduct(deltaHorizontal).yCoord < 0) angle = -angle;

                // 计算距离相关参数
                double distance = Math.sqrt(delta.xCoord*delta.xCoord + delta.yCoord*delta.yCoord + delta.zCoord*delta.zCoord);
                double radiusRatio = Math.min(Math.max((distance - MIN_DISTANCE) / (MAX_DISTANCE - MIN_DISTANCE), 0), 1); // 100-1000米映射到0-1
                double renderRadius = MIN_RADIUS + (circleRadius - MIN_RADIUS) * radiusRatio; // 20像素到最大半径

                // 计算屏幕坐标
                double radian = Math.toRadians(angle);
                double markerX = sx + renderRadius * Math.sin(-radian);
                double markerY = sy - renderRadius * Math.cos(radian);

                // 绘制文字
                MCH_RWRResult rwrResult = getTargetTypeOnRadar(entity, ac);
                String text = rwrResult.name;
                int color = rwrResult.color;
                int textWidth = Minecraft.getMinecraft().fontRenderer.getStringWidth(text);
                Minecraft.getMinecraft().fontRenderer.drawString(
                        text,
                        (int)(markerX - textWidth/2),
                        (int)(markerY - 4),
                        color, true
                );
            }
        }
        GL11.glPopMatrix();
    }

    public Vec3 getDirection(Entity e, float factor) {
        float f1;
        float f2;
        float f3;
        float f4;

        if (factor == 1.0F) {
            f1 = MathHelper.cos(-e.rotationYaw * 0.017453292F - (float)Math.PI);
            f2 = MathHelper.sin(-e.rotationYaw * 0.017453292F - (float)Math.PI);
            f3 = -MathHelper.cos(-e.rotationPitch * 0.017453292F);
            f4 = MathHelper.sin(-e.rotationPitch * 0.017453292F);
            return Vec3.createVectorHelper(f2 * f3, f4, f1 * f3);
        }
        else {
            f1 = e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * factor;
            f2 = e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * factor;
            f3 = MathHelper.cos(-f2 * 0.017453292F - (float)Math.PI);
            f4 = MathHelper.sin(-f2 * 0.017453292F - (float)Math.PI);
            float f5 = -MathHelper.cos(-f1 * 0.017453292F);
            float f6 = MathHelper.sin(-f1 * 0.017453292F);
            return Vec3.createVectorHelper(f4 * f5, f6, f3 * f5);
        }
    }


    // 新增实体校验方法
    private boolean isValidEntity(MCH_EntityInfo entity, EntityPlayer player) {
        if (entity.entityClassName.contains("MCH_EntityChaff") || entity.entityClassName.contains("MCH_EntityFlare")
                || entity.entityClassName.contains("EntityPlayer")) {
            return false;
        }
        if(entity.getDistanceSqToEntity(player) < MIN_DISTANCE * MIN_DISTANCE) {
            return false;
        }
        return true;
    }

    private MCH_RWRResult getTargetTypeOnRadar(MCH_EntityInfo entity, MCH_EntityAircraft ac) {
        switch (ac.getAcInfo().rwrType) {
            case DIGITAL: {
                if(entity.entityClassName.contains("MCH_EntityHeli")
                || entity.entityClassName.contains("MCP_EntityPlane")
                || entity.entityClassName.contains("MCH_EntityShip")
                || entity.entityClassName.contains("MCH_EntityTank")
                || entity.entityClassName.contains("MCH_EntityVehicle")) {
                    return new MCH_RWRResult(ac.getNameOnMyRadar(entity), 0x00FF00);
                } else {
                    return new MCH_RWRResult("MSL", 0xFF0000);
                }
            }
        }
        return new MCH_RWRResult("?", 0x00FF00);
    }


    private void drawRWRCircle(double x, double y, ScaledResolution sc) {
        prepareRenderState();
        Minecraft.getMinecraft().renderEngine.bindTexture(RWR);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        double halfSize = sc.getScaledHeight() * (RWR_SIZE / SCREEN_HEIGHT_ADAPT_CONSTANT) / 2.0;
        tess.addVertexWithUV(x - halfSize, y + halfSize, 0, 0, 1);
        tess.addVertexWithUV(x + halfSize, y + halfSize, 0, 1, 1);
        tess.addVertexWithUV(x + halfSize, y - halfSize, 0, 1, 0);
        tess.addVertexWithUV(x - halfSize, y - halfSize, 0, 0, 0);
        tess.draw();
        restoreRenderState();
    }

    private void prepareRenderState() {
        GL11.glEnable(3042);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glBlendFunc(770, 771);
    }

    private void restoreRenderState() {
        int srcBlend = GL11.glGetInteger(3041);
        int dstBlend = GL11.glGetInteger(3040);
        GL11.glBlendFunc(srcBlend, dstBlend);
        GL11.glDisable(3042);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private double interpolate(double now, double old, float partialTicks) {
        return old + (now - old) * partialTicks;
    }

    public List<MCH_EntityInfo> getServerLoadedEntity() {
        return new ArrayList<>(MCH_EntityInfoClientTracker.getAllTrackedEntities());
    }
}
