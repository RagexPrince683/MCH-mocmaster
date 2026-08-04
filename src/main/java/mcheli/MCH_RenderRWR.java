package mcheli;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcheli.aircraft.EnumRWRType;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.wrapper.W_MOD;
import mcheli.compat.MCH_ReplayModCompat;
import mcheli.hud.layout.MCH_GuiHudLayoutEditor;
import mcheli.hud.layout.MCH_HudLayoutManager;
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
    private boolean blendEnabled;
    private int sourceBlend;
    private int destinationBlend;
    private int boundTexture;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (Minecraft.getMinecraft().currentScreen instanceof MCH_GuiHudLayoutEditor) return;
        renderRwr(event.partialTicks);
    }

    /** Draws the gameplay RWR through the same path used by the layout editor. */
    private boolean renderRwr(final float partialTicks) {
        if (MCH_ReplayModCompat.isReplayPlaybackActive()) return false;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        World world = mc.theWorld;
        final ScaledResolution sc = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        if (player == null || world == null) return false;

        final MCH_EntityBaseVehicle ac = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(player);
        if(ac == null) return false;
        if(!(ac instanceof MCP_EntityPlane || ac instanceof MCH_EntityHeli)) return false;
        if(ac.getAcInfo().rwrType == null || ac.getAcInfo().rwrType == EnumRWRType.NONE) return false;
        final EntityPlayer renderPlayer = player;
        final double sx = sc.getScaledHeight() * (RWR_CENTER_X / SCREEN_HEIGHT_ADAPT_CONSTANT);
        final double sy = sc.getScaledHeight() * (RWR_CENTER_Y / SCREEN_HEIGHT_ADAPT_CONSTANT);
        MCH_HudLayoutManager.renderBuiltin("rwr", "rwr.display", "RWR", sx, sy, new Runnable() {
            public void run() { drawRwrContents(renderPlayer, ac, sc, sx, sy, partialTicks); }
        });
        return true;
    }

    /** Called while the HUD layout manager's editor frame is already active. */
    public boolean renderHudLayoutEditorPreview(float partialTicks) {
        return renderRwr(partialTicks);
    }

    private void drawRwrContents(EntityPlayer player, MCH_EntityBaseVehicle ac, ScaledResolution sc,
                                 double sx, double sy, float partialTicks) {
            drawRWRCircle(sx, sy, sc);
            double halfSize = sc.getScaledHeight() * (RWR_SIZE / SCREEN_HEIGHT_ADAPT_CONSTANT) / 2.0D;
            MCH_HudLayoutManager.capture(sx - halfSize, sy - halfSize, sx + halfSize, sy + halfSize);

            // New entity rendering logic
            double circleRadius = halfSize;
            for(MCH_EntityInfo entity : getServerLoadedEntity()) {
                if(!isValidEntity(entity, player, ac)) continue;
                if(!isActiveRadarEmitter(entity) && !isMissileThreat(entity)) continue;

                // Calculates interpolated position
                double xPos = interpolate(entity.posX, entity.lastTickPosX, partialTicks);
                double yPos = interpolate(entity.posY, entity.lastTickPosY, partialTicks);
                double zPos = interpolate(entity.posZ, entity.lastTickPosZ, partialTicks);

                // Calculates relative vector
                Vec3 delta = Vec3.createVectorHelper(
                        xPos - (player.posX + (player.posX - player.lastTickPosX) * partialTicks),
                        yPos - (player.posY + (player.posY - player.lastTickPosY) * partialTicks),
                        zPos - (player.posZ + (player.posZ - player.lastTickPosZ) * partialTicks)
                );

                Vec3 lookVec = getDirection(ac, partialTicks);
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
                MCH_HudLayoutManager.capture(markerX - textWidth / 2.0D, markerY - 4.0D,
                        markerX + textWidth / 2.0D + 1.0D,
                        markerY - 4.0D + Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 1.0D);
            }
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
                if(isActiveRadarEmitter(entity)) {
                    return new MCH_RWRResult(ac.getNameOnMyRadar(entity), 0x00FF00);
                } else {
                    return new MCH_RWRResult("MSL", 0xFF0000);
                }
            }
            case ALL_WAY_EARLY: {
                return new MCH_RWRResult(isActiveRadarEmitter(entity) ? "RDR" : "MSL", isActiveRadarEmitter(entity) ? 0x00FF00 : 0xFF0000);
            }
            case FOUR_WAY: {
                return new MCH_RWRResult(isActiveRadarEmitter(entity) ? "R" : "M", isActiveRadarEmitter(entity) ? 0x00FF00 : 0xFF0000);
            }
        }
        return new MCH_RWRResult("?", 0x00FF00);
    }

    private boolean isVehicleEntity(MCH_EntityInfo entity) {
        String type = entity.entityClassName;
        return type.equals("mcheli.helicopter.MCH_EntityHeli")
                || type.equals("mcheli.plane.MCP_EntityPlane")
                || type.equals("mcheli.ship.MCH_EntityShip")
                || type.equals("mcheli.tank.MCH_EntityTank")
                || type.equals("mcheli.vehicle.MCH_EntityTurret");
    }

    private boolean isActiveRadarEmitter(MCH_EntityInfo entity) {
        return isVehicleEntity(entity) && entity.hasRadar && entity.radarActive;
    }

    private boolean isMissileThreat(MCH_EntityInfo entity) {
        String type = entity.entityClassName;
        return type.startsWith("mcheli.weapon.MCH_Entity") && type.endsWith("Missile");
    }

    private void drawRWRCircle(double x, double y, ScaledResolution sc) {
        prepareRenderState();
        try {
            Minecraft.getMinecraft().renderEngine.bindTexture(RWR);
            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            double halfSize = sc.getScaledHeight() * (RWR_SIZE / SCREEN_HEIGHT_ADAPT_CONSTANT) / 2.0;
            tess.addVertexWithUV(x - halfSize, y + halfSize, 0, 0, 1);
            tess.addVertexWithUV(x + halfSize, y + halfSize, 0, 1, 1);
            tess.addVertexWithUV(x + halfSize, y - halfSize, 0, 1, 0);
            tess.addVertexWithUV(x - halfSize, y - halfSize, 0, 0, 0);
            tess.draw();
        } finally {
            restoreRenderState();
        }
    }

    private void prepareRenderState() {
        blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        sourceBlend = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        destinationBlend = GL11.glGetInteger(GL11.GL_BLEND_DST);
        boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glEnable(3042);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glBlendFunc(770, 771);
    }

    private void restoreRenderState() {
        GL11.glBlendFunc(sourceBlend, destinationBlend);
        if(blendEnabled) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTexture);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private double interpolate(double now, double old, float partialTicks) {
        return old + (now - old) * partialTicks;
    }

    public List<MCH_EntityInfo> getServerLoadedEntity() {
        return new ArrayList<>(MCH_EntityInfoClientTracker.getAllTrackedEntities());
    }
}
