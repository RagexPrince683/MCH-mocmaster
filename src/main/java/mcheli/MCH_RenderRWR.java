package mcheli;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.aircraft.EnumRWRType;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.wrapper.W_MOD;
import mcheli.compat.MCH_ReplayModCompat;
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

    private static final double MIN_DISTANCE = 50.0;  // Minimum display distance (meters)
    private static final double MAX_DISTANCE = 1000.0; // Maximum display distance (meters)
    private static final int MIN_RADIUS = 30;          // Minimum display radius (pixels

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (MCH_ReplayModCompat.isReplayPlaybackActive()) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        //Gets basic information
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        World world = mc.theWorld;
        ScaledResolution sc = new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        if (player == null || world == null) return;

        //Gets the player aircraft weapon
        MCH_EntityBaseVehicle ac = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(player);
        if(ac == null) return;
        if(!(ac instanceof MCP_EntityPlane || ac instanceof MCH_EntityHeli)) return;
        if(ac.getAcInfo().rwrType == null || ac.getAcInfo().rwrType == EnumRWRType.NONE) return;

        //Starts rendering
        GL11.glPushMatrix();
        {
            double sx = sc.getScaledHeight() * (RWR_CENTER_X / SCREEN_HEIGHT_ADAPT_CONSTANT);
            double sy = sc.getScaledHeight() * (RWR_CENTER_Y / SCREEN_HEIGHT_ADAPT_CONSTANT);
            drawRWRCircle(sx, sy, sc);

            // New entity rendering logic
            double circleRadius = sc.getScaledHeight() * (RWR_SIZE / SCREEN_HEIGHT_ADAPT_CONSTANT) / 2.0;
            for(MCH_EntityInfo entity : getServerLoadedEntity()) {
                if(!isValidEntity(entity, player, ac)) continue;

                // Calculates interpolated position
                double xPos = interpolate(entity.posX, entity.lastTickPosX, event.partialTicks);
                double yPos = interpolate(entity.posY, entity.lastTickPosY, event.partialTicks);
                double zPos = interpolate(entity.posZ, entity.lastTickPosZ, event.partialTicks);

                // Calculates relative vector
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

                // Calculates distance-related parameters
                double distance = Math.sqrt(delta.xCoord*delta.xCoord + delta.yCoord*delta.yCoord + delta.zCoord*delta.zCoord);
                double radiusRatio = Math.min(Math.max((distance - MIN_DISTANCE) / (MAX_DISTANCE - MIN_DISTANCE), 0), 1); // 100-1000metersmaps to0-1
                double renderRadius = MIN_RADIUS + (circleRadius - MIN_RADIUS) * radiusRatio; // 20pixelsto maximum radius

                // Calculates screen coordinates
                double radian = Math.toRadians(angle);
                double markerX = sx + renderRadius * Math.sin(-radian);
                double markerY = sy - renderRadius * Math.cos(radian);

                // Draws text
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


    // New entity validation method
    private boolean isValidEntity(MCH_EntityInfo entity, EntityPlayer player, MCH_EntityBaseVehicle ac) {
        if(entity == null || entity.entityId <= 0) {
            return false;
        }
        if(entity.entityId == ac.getEntityId()) {
            return false;
        }
        if (entity.entityClassName.contains("MCH_EntityChaff") || entity.entityClassName.contains("MCH_EntityFlare")
                || entity.entityClassName.contains("EntityPlayer")) {
            return false;
        }
        if(entity.getDistanceSqToEntity(player) < MIN_DISTANCE * MIN_DISTANCE) {
            return false;
        }
        return true;
    }

    private MCH_RWRResult getTargetTypeOnRadar(MCH_EntityInfo entity, MCH_EntityBaseVehicle ac) {
        switch (ac.getAcInfo().rwrType) {
            case DIGITAL: {
                if(isRadarEmitter(entity)) {
                    return new MCH_RWRResult(ac.getNameOnMyRadar(entity), 0x00FF00);
                } else {
                    return new MCH_RWRResult("MSL", 0xFF0000);
                }
            }
            case ALL_WAY_EARLY: {
                return new MCH_RWRResult(isRadarEmitter(entity) ? "RDR" : "MSL", isRadarEmitter(entity) ? 0x00FF00 : 0xFF0000);
            }
            case FOUR_WAY: {
                return new MCH_RWRResult(isRadarEmitter(entity) ? "R" : "M", isRadarEmitter(entity) ? 0x00FF00 : 0xFF0000);
            }
        }
        return new MCH_RWRResult("?", 0x00FF00);
    }

    private boolean isRadarEmitter(MCH_EntityInfo entity) {
        return entity.entityClassName.contains("MCH_EntityHeli")
                || entity.entityClassName.contains("MCP_EntityPlane")
                || entity.entityClassName.contains("MCH_EntityShip")
                || entity.entityClassName.contains("MCH_EntityTank")
                || entity.entityClassName.contains("MCH_EntityTurret");
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
