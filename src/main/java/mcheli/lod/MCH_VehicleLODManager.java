package mcheli.lod;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mcheli.MCH_Config;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_RenderAircraft;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.network.packets.PacketVehicleLODSnapshot;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.ship.MCH_ShipInfoManager;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.vehicle.MCH_VehicleInfoManager;
import mcheli.wrapper.W_MOD;
import mcheli.wrapper.W_Render;
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
        Set<UUID> trackedAircraft = new HashSet<UUID>();
        Set<Integer> trackedAircraftIds = new HashSet<Integer>();
        for (Object object : mc.theWorld.loadedEntityList) {
            if (object instanceof MCH_EntityAircraft) {
                trackedAircraft.add(((Entity)object).getUniqueID());
                trackedAircraftIds.add(((Entity)object).getEntityId());
            }
        }

        Entity camera = mc.renderViewEntity;
        if (camera == null) {
            return;
        }
        double cameraX = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * event.partialTicks;
        double cameraY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * event.partialTicks;
        double cameraZ = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * event.partialTicks;
        double far = MCH_Config.AircraftLODFarDistance != null ? MCH_Config.AircraftLODFarDistance.prmDouble : 4096.0D;
        double farSq = far > 0.0D ? far * far : Double.MAX_VALUE;

        for (Display display : this.displays.values()) {
            if (now - display.lastUpdateMs > STALE_AFTER_MS || trackedAircraft.contains(display.uuid)
                || trackedAircraftIds.contains(display.entityId)) {
                continue;
            }
            float interpolation = Math.min(1.0F, (float)(now - display.previousUpdateMs) / 1000.0F);
            double worldX = display.previousX + (display.x - display.previousX) * interpolation;
            double worldY = display.previousY + (display.y - display.previousY) * interpolation;
            double worldZ = display.previousZ + (display.z - display.previousZ) * interpolation;
            double x = worldX - cameraX;
            double y = worldY - cameraY;
            double z = worldZ - cameraZ;
            if (x * x + y * y + z * z > farSq) {
                continue;
            }
            render(display, x, y, z, interpolation);
        }
    }

    private static void render(Display display, double x, double y, double z, float interpolation) {
        MCH_AircraftInfo info = getInfo(display.category, display.typeName);
        String textureFolder = getTextureFolder(display.category);
        if (info == null || info.model == null || textureFolder == null) {
            return;
        }

        float previousLightX = OpenGlHelper.lastBrightnessX;
        float previousLightY = OpenGlHelper.lastBrightnessY;
        RENDER_STATE.begin(info.smoothShading, display.packedLight);
        GL11.glPushMatrix();
        try {
            // Keep the active world shader/fog state and use the normal aircraft model setup.
            GL11.glColor4f(0.75F, 0.75F, 0.75F, 1.0F);
            GL11.glTranslated(x, y, z);
            GL11.glRotatef(interpolateAngle(display.previousYaw, display.yaw, interpolation), 0.0F, -1.0F, 0.0F);
            GL11.glRotatef(interpolateAngle(display.previousPitch, display.pitch, interpolation), 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(interpolateAngle(display.previousRoll, display.roll, interpolation), 0.0F, 0.0F, 1.0F);
            GL11.glScalef(display.scale, display.scale, display.scale);
            Minecraft.getMinecraft().renderEngine.bindTexture(
                new ResourceLocation(W_MOD.DOMAIN, "textures/" + textureFolder + "/" + display.textureName + ".png"));
            MCH_RenderAircraft.renderBody(info.model);
        } finally {
            GL11.glPopMatrix();
            RENDER_STATE.end();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousLightX, previousLightY);
        }
    }

    private static float interpolateAngle(float previous, float current, float partial) {
        float delta = current - previous;
        while (delta < -180.0F) delta += 360.0F;
        while (delta >= 180.0F) delta -= 360.0F;
        return previous + delta * partial;
    }

    private static MCH_AircraftInfo getInfo(byte category, String typeName) {
        switch (category) {
            case 0: return MCH_HeliInfoManager.get(typeName);
            case 1: return MCP_PlaneInfoManager.get(typeName);
            case 2: return MCH_ShipInfoManager.get(typeName);
            case 3: return MCH_TankInfoManager.get(typeName);
            case 4: return MCH_VehicleInfoManager.get(typeName);
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
        protected ResourceLocation getEntityTexture(Entity entity) {
            return TEX_DEFAULT;
        }
    }

    private static final class Display {
        private final UUID uuid;
        private int entityId;
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
            float partial = this.previousUpdateMs == 0L ? 1.0F : Math.min(1.0F, (float)(now - this.previousUpdateMs) / 1000.0F);
            this.previousX = this.previousX + (this.x - this.previousX) * partial;
            this.previousY = this.previousY + (this.y - this.previousY) * partial;
            this.previousZ = this.previousZ + (this.z - this.previousZ) * partial;
            this.previousYaw = interpolateAngle(this.previousYaw, this.yaw, partial);
            this.previousPitch = interpolateAngle(this.previousPitch, this.pitch, partial);
            this.previousRoll = interpolateAngle(this.previousRoll, this.roll, partial);
            this.previousUpdateMs = now;
            this.entityId = entry.entityId;
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
            this.packedLight = entry.packedLight;
        }
    }
}
