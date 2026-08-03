package mcheli.lod;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mcheli.MCH_Camera;
import mcheli.MCH_ClientCommonTickHandler;
import mcheli.MCH_Lib;
import mcheli.MCH_Config;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_RenderBaseVehicle;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.helicopter.MCH_RenderHeli;
import mcheli.network.packets.PacketVehicleLODSnapshot;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_RenderPlane;
import mcheli.ship.MCH_ShipInfoManager;
import mcheli.ship.MCH_ShipInfo;
import mcheli.ship.MCH_RenderShip;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.vehicle.MCH_TurretInfoManager;
import mcheli.vehicle.MCH_TurretInfo;
import mcheli.vehicle.MCH_RenderTurret;
import mcheli.wrapper.W_MOD;
import mcheli.wrapper.W_Render;
import mcheli.wrapper.modelloader.W_ModelCustom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraft.world.World;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Client-only render cache for distant vehicles.  Entries are plain data and are
 * never added to a World, so they cannot collide, tick, mount, save, or interact.
 */
@SideOnly(Side.CLIENT)
public final class MCH_VehicleLODManager {
    public static final MCH_VehicleLODManager INSTANCE = new MCH_VehicleLODManager();
    private static final long STALE_AFTER_MS = 5000L;
    private static final LODRenderState RENDER_STATE = new LODRenderState();
    private static final Map<Object, Double> TARGET_DIMENSIONS = new IdentityHashMap<Object, Double>();
    private static long nextDiagnosticMs;
    private final Map<UUID, Display> displays = new HashMap<UUID, Display>();
    private int dimension = Integer.MIN_VALUE;
    private World world;

    private MCH_VehicleLODManager() {
    }

    public synchronized void update(int packetDimension, List<PacketVehicleLODSnapshot.Entry> entries) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null || mc.thePlayer.dimension != packetDimension) {
            return;
        }
        if (this.world != mc.theWorld) {
            this.displays.clear();
            this.world = mc.theWorld;
        }

        long now = System.currentTimeMillis();
        Set<UUID> received = new HashSet<UUID>();
        for (PacketVehicleLODSnapshot.Entry entry : entries) {
            if (entry.uuid == null || entry.typeName == null || entry.typeName.length() == 0) {
                continue;
            }
            received.add(entry.uuid);
            Display display = this.displays.get(entry.uuid);
            if (display == null) {
                display = new Display(entry);
                this.displays.put(entry.uuid, display);
            } else {
                display.update(entry);
            }
            display.lastUpdateMs = now;
        }

        this.displays.keySet().retainAll(received);
        this.dimension = packetDimension;
    }

    public synchronized void clear() {
        this.displays.clear();
        this.dimension = Integer.MIN_VALUE;
        this.world = null;
    }

    public synchronized void invalidate(UUID vehicleId) {
        if (vehicleId != null) this.displays.remove(vehicleId);
    }

    /** Diagnostic ownership query; a live tracked entity still remains the sole renderer. */
    public synchronized boolean hasSnapshotFor(MCH_EntityBaseVehicle vehicle) {
        if (vehicle == null) return false;
        for (Display display : this.displays.values()) {
            if (vehicle.getEntityId() == display.entityId) return true;
            String commonId = vehicle.getCommonUniqueId();
            if (commonId != null && commonId.length() > 0 && commonId.equals(display.commonUniqueId)
                && vehicle.getAcInfo() != null && display.typeName.equals(vehicle.getAcInfo().name)) return true;
        }
        return false;
    }

    @SubscribeEvent
    public synchronized void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != this.world) {
            clear();
            return;
        }
        if (mc.theWorld == null || mc.thePlayer == null || this.dimension != mc.thePlayer.dimension
            || MCH_Config.EnableAircraftLODRender == null || !MCH_Config.EnableAircraftLODRender.prmBool) {
            return;
        }

        long now = System.currentTimeMillis();
        List<MCH_EntityBaseVehicle> trackedAircraft = new java.util.ArrayList<MCH_EntityBaseVehicle>();
        for (Object object : mc.theWorld.loadedEntityList) {
            if (object instanceof MCH_EntityBaseVehicle && !((Entity)object).isDead) {
                trackedAircraft.add((MCH_EntityBaseVehicle)object);
            }
        }

        Entity camera = mc.renderViewEntity;
        if (camera == null) {
            return;
        }
        double cameraX = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * event.partialTicks;
        double cameraY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * event.partialTicks;
        double cameraZ = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * event.partialTicks;
        double far = MCH_VehicleLODVisibility.hardDistance(config(MCH_Config.AircraftLODFarDistance,
            MCH_VehicleLODVisibility.MAX_LOD_DISTANCE));
        double farSq = far * far;
        RenderContext context = captureRenderContext(mc, event.partialTicks);

        for (Display display : this.displays.values()) {
            if (now - display.lastUpdateMs > STALE_AFTER_MS || hasRealEntity(display, trackedAircraft)) {
                continue;
            }
            float interpolation = Math.min(1.0F, (float)(now - display.previousUpdateMs) / 1000.0F);
            double worldX = display.previousX + (display.x - display.previousX) * interpolation;
            double worldY = display.previousY + (display.y - display.previousY) * interpolation;
            double worldZ = display.previousZ + (display.z - display.previousZ) * interpolation;
            double x = worldX - cameraX;
            double y = worldY - cameraY;
            double z = worldZ - cameraZ;
            double distanceSq = x * x + y * y + z * z;
            double realDistance = Math.sqrt(distanceSq);
            if (distanceSq >= farSq) {
                diagnose(display, context, realDistance, far, 1.0D, display.scale, 0.0D, 0.0D, "hard_range", now);
                continue;
            }
            MCH_BaseVehicleInfo info = getInfo(display.category, display.typeName);
            if (info == null || info.model == null) {
                diagnose(display, context, realDistance, far, 1.0D, display.scale, 0.0D, 0.0D, "missing_model", now);
                continue;
            }
            double projectedPixels = MCH_VehicleLODVisibility.projectedPixels(
                targetDimension(info) * display.scale, realDistance, context.projection[5], context.viewportHeight);
            double minimumPixels = context.thermal ? nonNegativeConfig(MCH_Config.AircraftLODThermalMinPixels, 0.35D)
                : nonNegativeConfig(MCH_Config.AircraftLODOpticalMinPixels, 0.75D);
            double footprintScale = MCH_VehicleLODVisibility.minimumFootprintScale(projectedPixels, minimumPixels);
            double effectiveVisibility = config(MCH_Config.AircraftLODVisibilityDistance,
                MCH_VehicleLODVisibility.MAX_LOD_DISTANCE) * context.weatherMultiplier;
            double transmission = MCH_VehicleLODVisibility.transmission(realDistance, effectiveVisibility);
            double alpha = context.thermal ? MCH_VehicleLODVisibility.thermalAlpha(transmission,
                config(MCH_Config.AircraftLODThermalContrastExponent, 0.35D)) : transmission;
            double depthScale = MCH_VehicleLODVisibility.depthScale(realDistance, context.safeProxyDepth);
            if (alpha < 1.0D / 255.0D) {
                diagnose(display, context, realDistance, far, depthScale, display.scale * depthScale,
                    transmission, projectedPixels, "negligible_alpha", now);
                continue;
            }
            double renderScale = display.scale * depthScale * footprintScale;
            render(display, info, x * depthScale, y * depthScale, z * depthScale,
                (float)renderScale, (float)alpha, interpolation, now);
            diagnose(display, context, realDistance, far, depthScale, renderScale,
                transmission, projectedPixels, "render", now);
        }
    }

    private static RenderContext captureRenderContext(Minecraft mc, float partialTicks) {
        RenderContext context = new RenderContext();
        MCH_VehicleLODProjection.Context projection = MCH_VehicleLODProjection.capture(mc, 0.0D);
        System.arraycopy(projection.projection, 0, context.projection, 0, 16);
        context.farPlane = projection.farPlane;
        context.safeProxyDepth = projection.safeProxyDepth;
        context.viewportHeight = projection.viewportHeight;
        context.cameraMode = MCH_ClientCommonTickHandler.cameraMode;
        context.thermal = context.cameraMode == MCH_Camera.MODE_THERMALVISION;
        if (mc.theWorld.getWeightedThunderStrength(partialTicks) > 0.0F) {
            context.weather = "thunder";
            context.weatherMultiplier = config(MCH_Config.AircraftLODThunderVisibilityMultiplier, 0.45D);
        } else if (mc.theWorld.getRainStrength(partialTicks) > 0.0F) {
            context.weather = "rain";
            context.weatherMultiplier = config(MCH_Config.AircraftLODRainVisibilityMultiplier, 0.70D);
        } else {
            context.weather = "clear";
            context.weatherMultiplier = 1.0D;
        }
        return context;
    }

    private static double targetDimension(MCH_BaseVehicleInfo info) {
        Double cached = TARGET_DIMENSIONS.get(info);
        if (cached != null) return cached.doubleValue();
        double dimension = 1.0D;
        if (info.model instanceof W_ModelCustom) {
            W_ModelCustom model = (W_ModelCustom)info.model;
            dimension = Math.max((double)model.sizeX, Math.max((double)model.sizeY, (double)model.sizeZ));
        }
        dimension = MCH_VehicleLODVisibility.positive(dimension, 1.0D);
        TARGET_DIMENSIONS.put(info, Double.valueOf(dimension));
        return dimension;
    }

    private static double config(mcheli.MCH_ConfigPrm parameter, double fallback) {
        return parameter == null ? fallback : MCH_VehicleLODVisibility.positive(parameter.prmDouble, fallback);
    }

    private static double nonNegativeConfig(mcheli.MCH_ConfigPrm parameter, double fallback) {
        if (parameter == null || Double.isNaN(parameter.prmDouble) || Double.isInfinite(parameter.prmDouble)) return fallback;
        return Math.max(0.0D, parameter.prmDouble);
    }

    private static void diagnose(Display display, RenderContext context, double distance, double hardRange,
        double depthScale, double renderScale, double transmission, double pixels, String reason, long now) {
        if (MCH_Config.DebugVehicleLODVisibility == null || !MCH_Config.DebugVehicleLODVisibility.prmBool
            || now < nextDiagnosticMs) return;
        nextDiagnosticMs = now + 1000L;
        double effectiveVisibility = config(MCH_Config.AircraftLODVisibilityDistance,
            MCH_VehicleLODVisibility.MAX_LOD_DISTANCE) * context.weatherMultiplier;
        double alpha = context.thermal ? MCH_VehicleLODVisibility.thermalAlpha(transmission,
            config(MCH_Config.AircraftLODThermalContrastExponent, 0.35D)) : transmission;
        MCH_Lib.DbgLog(true,
            "VehicleLODVisibility type=%s distance3d=%.1f normalRange=%.1f hardRange=%.1f qualifies=true farPlane=%.1f safeDepth=%.1f proxyDepthScale=%.5f renderScale=%.5f visibility=%.1f transmission=%.5f alpha=%.5f projectedPixels=%.3f cameraMode=%d weather=%s result=%s",
            new Object[]{display.typeName, Double.valueOf(distance),
                Double.valueOf(MCH_VehicleLODVisibility.NORMAL_TRACKING_RANGE), Double.valueOf(hardRange),
                Double.valueOf(context.farPlane), Double.valueOf(context.safeProxyDepth), Double.valueOf(depthScale),
                Double.valueOf(renderScale), Double.valueOf(effectiveVisibility), Double.valueOf(transmission),
                Double.valueOf(alpha), Double.valueOf(pixels), Integer.valueOf(context.cameraMode), context.weather, reason});
    }

    private static final class RenderContext {
        private final float[] projection = new float[16];
        private int viewportHeight;
        private double farPlane;
        private double safeProxyDepth;
        private int cameraMode;
        private boolean thermal;
        private String weather;
        private double weatherMultiplier;
    }

    private static boolean hasRealEntity(Display display, List<MCH_EntityBaseVehicle> vehicles) {
        for(MCH_EntityBaseVehicle vehicle : vehicles) {
            if(vehicle.getEntityId() == display.entityId) return true;
            String commonId = vehicle.getCommonUniqueId();
            if(commonId != null && commonId.length() > 0 && commonId.equals(display.commonUniqueId)
                && vehicle.getAcInfo() != null && display.typeName.equals(vehicle.getAcInfo().name)) return true;
        }
        return false;
    }

    private static void render(Display display, MCH_BaseVehicleInfo info, double x, double y, double z,
        float renderScale, float alpha, float interpolation, long now) {
        String textureFolder = info instanceof MCH_TurretInfo ? ((MCH_TurretInfo)info).getDirectoryName() : getTextureFolder(display.category);
        if (info == null || info.model == null || textureFolder == null) {
            return;
        }

        float previousLightX = OpenGlHelper.lastBrightnessX;
        float previousLightY = OpenGlHelper.lastBrightnessY;
        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        RENDER_STATE.begin(info.smoothShading, display.packedLight);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_FOG);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(false);
            GL11.glColor4f(0.75F, 0.75F, 0.75F, alpha);
            GL11.glTranslated(x, y, z);
            GL11.glRotatef(interpolateAngle(display.previousYaw, display.yaw, interpolation), 0.0F, -1.0F, 0.0F);
            GL11.glRotatef(interpolateAngle(display.previousPitch, display.pitch, interpolation), 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(interpolateAngle(display.previousRoll, display.roll, interpolation), 0.0F, 0.0F, 1.0F);
            GL11.glScalef(renderScale, renderScale, renderScale);
            MCH_RenderBaseVehicle.beginSkinOverlayRender(textureFolder, display.textureName);
            try {
                Minecraft.getMinecraft().renderEngine.bindTexture(
                    new ResourceLocation(W_MOD.DOMAIN, "textures/" + textureFolder + "/"
                        + MCH_RenderBaseVehicle.getBaseTextureName(display.textureName) + ".png"));
                if (MCH_RenderBaseVehicle.hasSeparableBody(info.model)) {
                    MCH_RenderBaseVehicle.renderBody(info.model);
                    if (display.category == 3) {
                        MCH_RenderBaseVehicle.renderTrackRoller(info, display.trackRollerRotation,
                            display.previousTrackRollerRotation, interpolation);
                        MCH_RenderBaseVehicle.renderCrawlerTrack(info, display.crawlerTrackPhase,
                            display.previousCrawlerTrackPhase, display.crawlerTrackDirection, interpolation);
                        MCH_RenderBaseVehicle.renderWheel(info, display.wheelRotation, display.previousWheelRotation,
                            display.wheelYaw, display.previousWheelYaw, interpolation);
                        int weaponCount = Math.min(info.partWeapon.size(), display.weaponPoses.length);
                        for (int i = 0; i < weaponCount; ++i) {
                            MCH_RenderBaseVehicle.renderSnapshotWeapon(info,
                                (MCH_BaseVehicleInfo.PartWeapon)info.partWeapon.get(i), display.weaponPoses[i], interpolation);
                        }
                    } else if (display.category == 0 && info instanceof MCH_HeliInfo) {
                        MCH_RenderHeli.drawSnapshotBlades((MCH_HeliInfo)info, display.getRotorPhase(now), display.rotorFolded);
                    } else if (display.category == 1 && info instanceof MCP_PlaneInfo) {
                        MCP_PlaneInfo plane = (MCP_PlaneInfo)info;
                        MCP_RenderPlane.renderNozzle(plane, display.nozzleRotation, display.previousNozzleRotation, interpolation);
                        MCP_RenderPlane.renderWing(plane, display.wingRotation, display.previousWingRotation, interpolation);
                        float phase = display.getRotorPhase(now);
                        MCP_RenderPlane.renderRotor(plane, display.nozzleRotation, display.previousNozzleRotation, phase, phase, interpolation);
                        MCH_RenderBaseVehicle.renderLandingGear(info, display.landingGearRotation, display.previousLandingGearRotation, interpolation);
                    } else if (display.category == 2 && info instanceof MCH_ShipInfo) {
                        MCH_ShipInfo ship = (MCH_ShipInfo)info;
                        MCH_RenderShip.renderNozzle(ship, display.nozzleRotation, display.previousNozzleRotation, interpolation);
                        MCH_RenderShip.renderWing(ship, display.wingRotation, display.previousWingRotation, interpolation);
                        float phase = display.getRotorPhase(now);
                        MCH_RenderShip.renderRotor(ship, display.nozzleRotation, display.previousNozzleRotation, phase, phase, interpolation);
                        MCH_RenderBaseVehicle.renderLandingGear(info, display.landingGearRotation, display.previousLandingGearRotation, interpolation);
                    } else if (display.category == 4 && info instanceof MCH_TurretInfo) {
                        MCH_RenderTurret.drawSnapshotParts((MCH_TurretInfo)info,
                            interpolateAngle(display.previousYaw, display.yaw, interpolation),
                            interpolateAngle(display.previousPitch, display.pitch, interpolation),
                            interpolateAngle(display.previousAimYaw, display.aimYaw, interpolation),
                            display.previousAimPitch + (display.aimPitch - display.previousAimPitch) * interpolation,
                            interpolateAngle(display.previousTurretBarrelRotation, display.turretBarrelRotation, interpolation),
                            display.turretParts, interpolation);
                    }
                    if (display.category != 3 && display.category != 4) {
                        int weaponCount = Math.min(info.partWeapon.size(), display.weaponPoses.length);
                        for (int i = 0; i < weaponCount; ++i) MCH_RenderBaseVehicle.renderSnapshotWeapon(info,
                            (MCH_BaseVehicleInfo.PartWeapon)info.partWeapon.get(i), display.weaponPoses[i], interpolation);
                    }
                } else {
                    // Legacy monolithic models cannot exclude bind-pose groups safely.
                    MCH_RenderBaseVehicle.renderAllModel(info.model);
                }
            } finally {
                MCH_RenderBaseVehicle.endSkinOverlayRender();
            }
        } finally {
            GL11.glPopMatrix();
            RENDER_STATE.end();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousLightX, previousLightY);
            GL11.glPopAttrib();
            GL11.glMatrixMode(previousMatrixMode);
        }
    }

    private static float interpolateAngle(float previous, float current, float partial) {
        float delta = current - previous;
        while (delta < -180.0F) delta += 360.0F;
        while (delta >= 180.0F) delta -= 360.0F;
        return previous + delta * partial;
    }

    private static MCH_BaseVehicleInfo getInfo(byte category, String typeName) {
        switch (category) {
            case 0: return MCH_HeliInfoManager.get(typeName);
            case 1: return MCP_PlaneInfoManager.get(typeName);
            case 2: return MCH_ShipInfoManager.get(typeName);
            case 3: return MCH_TankInfoManager.get(typeName);
            case 4: return MCH_TurretInfoManager.get(typeName);
            default: return null;
        }
    }

    private static String getTextureFolder(byte category) {
        switch (category) {
            case 0: return "helicopters";
            case 1: return "planes";
            case 2: return "ships";
            case 3: return "tanks";
            case 4: return "vehicles";
            default: return null;
        }
    }

    /** Uses the same fixed-function model render setup as normal MCHeli renderers. */
    private static final class LODRenderState extends W_Render {
        private void begin(boolean smoothShading, int packedLight) {
            this.setCommonRenderParam(smoothShading, packedLight);
        }

        private void end() {
            this.restoreCommonRenderParam();
        }

        @Override
        public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
            // This adapter only exposes W_Render's shared render-state helpers.
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            return TEX_DEFAULT;
        }
    }

    private static final class Display {
        private final UUID uuid;
        private int entityId;
        private String commonUniqueId;
        private int dimension;
        private byte category;
        private String typeName;
        private String textureName;
        private double previousX;
        private double previousY;
        private double previousZ;
        private double x;
        private double y;
        private double z;
        private float previousYaw;
        private float previousPitch;
        private float previousRoll;
        private float yaw;
        private float pitch;
        private float roll;
        private float scale;
        private int packedLight;
        private final float[] trackRollerRotation = new float[2];
        private final float[] previousTrackRollerRotation = new float[2];
        private final float[] crawlerTrackPhase = new float[2];
        private final float[] previousCrawlerTrackPhase = new float[2];
        private final float[] crawlerTrackDirection = new float[2];
        private float wheelRotation;
        private float previousWheelRotation;
        private float wheelYaw;
        private float previousWheelYaw;
        private boolean runningGearInitialized;
        private PacketVehicleLODSnapshot.WeaponPose[] weaponPoses = new PacketVehicleLODSnapshot.WeaponPose[0];
        private float rotorPhase;
        private float rotorAngularChange;
        private boolean rotorFolded;
        private float landingGearRotation, previousLandingGearRotation;
        private float nozzleRotation, previousNozzleRotation, wingRotation, previousWingRotation;
        private float aimYaw, previousAimYaw, aimPitch, previousAimPitch;
        private float turretBarrelRotation, previousTurretBarrelRotation;
        private PacketVehicleLODSnapshot.TurretPartPose[] turretParts = new PacketVehicleLODSnapshot.TurretPartPose[0];
        private long rotorPhaseTimeMs;
        private long previousUpdateMs;
        private long lastUpdateMs;

        private Display(PacketVehicleLODSnapshot.Entry entry) {
            this.uuid = entry.uuid;
            this.previousX = this.x = entry.x;
            this.previousY = this.y = entry.y;
            this.previousZ = this.z = entry.z;
            this.previousYaw = this.yaw = entry.yaw;
            this.previousPitch = this.pitch = entry.pitch;
            this.previousRoll = this.roll = entry.roll;
            update(entry);
        }

        private void update(PacketVehicleLODSnapshot.Entry entry) {
            long now = System.currentTimeMillis();
            byte oldCategory = this.category;
            String oldTypeName = this.typeName;
            float partial = this.previousUpdateMs == 0L ? 1.0F : Math.min(1.0F, (float)(now - this.previousUpdateMs) / 1000.0F);
            this.previousX = this.previousX + (this.x - this.previousX) * partial;
            this.previousY = this.previousY + (this.y - this.previousY) * partial;
            this.previousZ = this.previousZ + (this.z - this.previousZ) * partial;
            this.previousYaw = interpolateAngle(this.previousYaw, this.yaw, partial);
            this.previousPitch = interpolateAngle(this.previousPitch, this.pitch, partial);
            this.previousRoll = interpolateAngle(this.previousRoll, this.roll, partial);
            this.previousUpdateMs = now;
            this.entityId = entry.entityId;
            this.commonUniqueId = entry.commonUniqueId;
            this.dimension = entry.dimension;
            this.category = entry.category;
            this.typeName = entry.typeName;
            this.textureName = entry.textureName;
            this.x = entry.x;
            this.y = entry.y;
            this.z = entry.z;
            this.yaw = entry.yaw;
            this.pitch = entry.pitch;
            this.roll = entry.roll;
            this.scale = entry.scale > 0.0F ? entry.scale : 1.0F;
            boolean categoryChanged = oldTypeName != null && (oldCategory != entry.category || !oldTypeName.equals(entry.typeName));
            if (categoryChanged) {
                this.weaponPoses = new PacketVehicleLODSnapshot.WeaponPose[0];
                this.rotorPhaseTimeMs = 0L;
                this.rotorAngularChange = 0.0F;
                this.runningGearInitialized = false;
                this.turretParts = new PacketVehicleLODSnapshot.TurretPartPose[0];
            }
            if (entry.category == 3) {
                for (int side = 0; side < 2; ++side) {
                    if (this.runningGearInitialized) {
                        this.previousTrackRollerRotation[side] = interpolateAngle(this.previousTrackRollerRotation[side],
                            this.trackRollerRotation[side], partial);
                        this.previousCrawlerTrackPhase[side] = interpolatePhase(this.previousCrawlerTrackPhase[side],
                            this.crawlerTrackPhase[side], this.crawlerTrackDirection[side], partial);
                    } else {
                        this.previousTrackRollerRotation[side] = entry.trackRollerRotation[side];
                        this.previousCrawlerTrackPhase[side] = entry.crawlerTrackPhase[side];
                    }
                    this.trackRollerRotation[side] = entry.trackRollerRotation[side];
                    this.crawlerTrackPhase[side] = entry.crawlerTrackPhase[side];
                    this.crawlerTrackDirection[side] = phaseDelta(entry.previousCrawlerTrackPhase[side],
                        entry.crawlerTrackPhase[side], 0.0F);
                }
                if (this.runningGearInitialized) {
                    this.previousWheelRotation = interpolateAngle(this.previousWheelRotation, this.wheelRotation, partial);
                    this.previousWheelYaw = interpolateAngle(this.previousWheelYaw, this.wheelYaw, partial);
                } else {
                    this.previousWheelRotation = entry.wheelRotation;
                    this.previousWheelYaw = entry.wheelYaw;
                }
                this.wheelRotation = entry.wheelRotation;
                this.wheelYaw = entry.wheelYaw;
                this.runningGearInitialized = true;
            } else {
                this.runningGearInitialized = false;
            }
            PacketVehicleLODSnapshot.WeaponPose[] incoming = entry.weaponPoses == null
                ? new PacketVehicleLODSnapshot.WeaponPose[0] : entry.weaponPoses;
            for (int i = 0; i < incoming.length; ++i) {
                PacketVehicleLODSnapshot.WeaponPose current = incoming[i];
                if (!categoryChanged && i < this.weaponPoses.length) {
                    PacketVehicleLODSnapshot.WeaponPose previous = this.weaponPoses[i];
                    current.prevYaw = previous.yaw;
                    current.prevPitch = previous.pitch;
                    current.prevTurretYaw = previous.turretYaw;
                    current.prevBarrelRotation = previous.barrelRotation;
                    current.prevRecoil = previous.recoil;
                } else {
                    current.prevYaw = current.yaw;
                    current.prevPitch = current.pitch;
                    current.prevTurretYaw = current.turretYaw;
                    current.prevBarrelRotation = current.barrelRotation;
                    current.prevRecoil = current.recoil;
                }
            }
            this.weaponPoses = incoming;
            this.previousLandingGearRotation = (categoryChanged || oldTypeName == null) ? entry.prevLandingGearRotation : this.landingGearRotation;
            this.landingGearRotation = entry.landingGearRotation;
            this.previousNozzleRotation = (categoryChanged || oldTypeName == null) ? entry.prevNozzleRotation : this.nozzleRotation;
            this.nozzleRotation = entry.nozzleRotation;
            this.previousWingRotation = (categoryChanged || oldTypeName == null) ? entry.prevWingRotation : this.wingRotation;
            this.wingRotation = entry.wingRotation;
            this.previousAimYaw = (categoryChanged || oldTypeName == null) ? entry.prevAimYaw : this.aimYaw;
            this.aimYaw = entry.aimYaw;
            this.previousAimPitch = (categoryChanged || oldTypeName == null) ? entry.prevAimPitch : this.aimPitch;
            this.aimPitch = entry.aimPitch;
            this.previousTurretBarrelRotation = (categoryChanged || oldTypeName == null) ? entry.prevTurretBarrelRotation : this.turretBarrelRotation;
            this.turretBarrelRotation = entry.turretBarrelRotation;
            PacketVehicleLODSnapshot.TurretPartPose[] incomingTurretParts = entry.turretParts == null
                ? new PacketVehicleLODSnapshot.TurretPartPose[0] : entry.turretParts;
            if (!categoryChanged && oldTypeName != null) {
                for (int i = 0; i < incomingTurretParts.length && i < this.turretParts.length; ++i) {
                    incomingTurretParts[i].prevRecoil = this.turretParts[i].recoil;
                }
            }
            this.turretParts = incomingTurretParts;
            if (entry.category == 0 || entry.category == 1 || entry.category == 2) {
                if (this.rotorPhaseTimeMs == 0L) {
                    this.rotorPhase = entry.rotorRotation;
                } else {
                    float predicted = advanceRotorPhase(now);
                    float error = interpolateAngle(predicted, entry.rotorRotation, 1.0F) - predicted;
                    this.rotorPhase = predicted + error * 0.25F;
                }
                this.rotorPhaseTimeMs = now;
                this.rotorAngularChange = entry.rotorAngularChange;
                this.rotorFolded = entry.rotorFolded;
            } else {
                this.rotorPhaseTimeMs = 0L;
                this.rotorAngularChange = 0.0F;
                this.rotorFolded = false;
            }
            this.packedLight = entry.packedLight;
        }

        private float getRotorPhase(long now) {
            this.rotorPhase = advanceRotorPhase(now);
            this.rotorPhaseTimeMs = now;
            return this.rotorPhase;
        }

        private float advanceRotorPhase(long now) {
            if (this.rotorPhaseTimeMs == 0L) return this.rotorPhase;
            long elapsed = now - this.rotorPhaseTimeMs;
            long snapshotAge = now - this.lastUpdateMs;
            if (snapshotAge > 1250L) elapsed = Math.max(0L, 1250L - (this.rotorPhaseTimeMs - this.lastUpdateMs));
            if (elapsed < 0L) elapsed = 0L;
            return this.rotorPhase + this.rotorAngularChange * (float)elapsed / 50.0F;
        }

        private static float interpolatePhase(float previous, float current, float direction, float partial) {
            float value = previous + phaseDelta(previous, current, direction) * partial;
            value -= (float)Math.floor(value);
            return value;
        }

        private static float phaseDelta(float previous, float current, float direction) {
            float delta = current - previous;
            while (delta > 0.5F) delta -= 1.0F;
            while (delta < -0.5F) delta += 1.0F;
            if (Math.abs(Math.abs(delta) - 0.5F) < 0.0001F && direction != 0.0F) {
                delta = direction > 0.0F ? 0.5F : -0.5F;
            }
            return delta;
        }
    }
}
