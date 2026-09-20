package mcheli.aircraft;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import mcheli.MCH_Config;
import mcheli.MCH_ConfigPrm;

/** Model capture produces persistent pixels; ready inventory icons never draw geometry. */
public class MCH_VehicleItemModelRender implements IItemRenderer, IResourceManagerReloadListener {

    public static final int ICON_CACHE_VERSION = 2;
    private static final int SIZE = 128;
    private static final long INTERVAL_MS = 350L;
    private static final Map<Object, Entry> ENTRIES = new IdentityHashMap<Object, Entry>();
    private static final LinkedHashMap<Object, Entry> QUEUE = new LinkedHashMap<Object, Entry>();
    private static final java.util.concurrent.atomic.AtomicLong writes = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong writeFailures = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.ThreadPoolExecutor WRITER = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0L, java.util.concurrent.TimeUnit.MILLISECONDS,
            new java.util.concurrent.ArrayBlockingQueue<Runnable>(32), new java.util.concurrent.ThreadFactory() {
        public Thread newThread(Runnable job) {
            Thread thread = new Thread(job, "mcheli icon PNG writer");
            thread.setDaemon(true);
            return thread;
        }
    }, new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
    private static Framebuffer framebuffer;
    private static final ByteBuffer PIXELS = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
    private static long nextWork, lastReport, requests, duplicates, prebaked, hits, misses, captures, failures, loads;
    private static long captureNanos, loadNanos;
    private static java.util.Iterator bakeItems;
    private static boolean bakeStarted;
    private static boolean capturing;

    public static boolean isCapturing() { return capturing; }

    private static Entry request(Object identity, ItemStack stack, IItemRenderer renderer) {
        Entry entry = ENTRIES.get(identity);
        if(entry != null && entry.appearance != appearance(identity)) {
            release(entry);
            ENTRIES.remove(identity);
            QUEUE.remove(identity);
            entry = null;
        }
        if(entry != null) {
            if(entry.state == State.QUEUED || entry.state == State.GENERATING) ++duplicates;
            return entry;
        }
        entry = new Entry(identity, new ItemStack(stack.getItem(), 1, 0), renderer);
        ENTRIES.put(identity, entry);
        QUEUE.put(identity, entry);
        ++requests;
        return entry;
    }

    /** One queued resolution/capture per interval; never called from an item callback. */
    public static void onRenderFrame() {
        long now = Minecraft.getSystemTime();
        report(now);
        if(now < nextWork) return;
        nextWork = now + INTERVAL_MS;
        if(Boolean.getBoolean("mcheli.bakeIcons")) {
            if(!bakeStarted) { bakeItems = Item.itemRegistry.iterator(); bakeStarted = true; }
            // Bounded registry walk, using precisely the normal request/generation path.
            for(int i = 0; i < 16 && bakeItems.hasNext(); ++i) bake((Item)bakeItems.next());
        }
        if(QUEUE.isEmpty()) return;
        Entry entry = QUEUE.values().iterator().next();
        QUEUE.remove(entry.identity);
        entry.state = State.GENERATING;
        try {
            entry.key = cacheKey(entry);
            long started = System.nanoTime();
            BufferedImage image = loadPrebaked(entry.key);
            boolean shipped = image != null;
            if(shipped) ++prebaked;
            File file = new File(directory(), entry.key + ".png");
            if(image == null) {
                image = read(file);
                if(image != null) ++hits;
                else ++misses;
            }
            loadNanos += System.nanoTime() - started;
            if(image != null) {
                ++loads;
                upload(entry, image);
                if(Boolean.getBoolean("mcheli.bakeIcons")) save(image, exportFile(entry.key));
                return;
            }
            started = System.nanoTime();
            try {
                prepare(entry);
                image = capture(entry);
                ++captures;
            } finally {
                captureNanos += System.nanoTime() - started;
            }
            // Upload before submitting disk work: persistence never gates this session's icon.
            upload(entry, image);
            save(image, file);
            if(Boolean.getBoolean("mcheli.bakeIcons")) save(image, exportFile(entry.key));
        } catch(Exception failure) {
            entry.state = State.FAILED;
            ++failures;
            System.err.println("[mcheli icons] Failed " + entry.stack.getUnlocalizedName() + ": " + failure);
        }
    }

    private static BufferedImage loadPrebaked(String key) {
        try (java.io.InputStream stream = Minecraft.getMinecraft().getResourceManager()
                .getResource(new ResourceLocation("mcheli", "textures/icons/" + key + ".png")).getInputStream()) {
            return valid(ImageIO.read(stream));
        } catch(IOException unavailable) { return null; }
    }

    private static BufferedImage read(File file) {
        if(!file.isFile()) return null;
        try { return valid(ImageIO.read(file)); }
        catch(IOException corrupt) { return null; }
    }

    private static BufferedImage valid(BufferedImage image) {
        return image != null && image.getWidth() == SIZE && image.getHeight() == SIZE ? image : null;
    }

    private static void upload(Entry entry, BufferedImage image) {
        entry.texture = Minecraft.getMinecraft().getTextureManager()
                .getDynamicTextureLocation("mcheli_inventory_icon", new DynamicTexture(image));
        entry.state = State.READY;
    }

    private static void save(final BufferedImage image, final File destination) {
        try {
            WRITER.execute(new Runnable() {
                public void run() {
                    File temporary = null;
                    try {
                        File parent = destination.getParentFile();
                        if(!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create " + parent);
                        temporary = File.createTempFile("icon-", ".tmp", parent);
                        if(!ImageIO.write(image, "png", temporary)) throw new IOException("PNG encoder unavailable");
                        try {
                            Files.move(temporary.toPath(), destination.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                        } catch(java.nio.file.AtomicMoveNotSupportedException unsupported) {
                            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                        writes.incrementAndGet();
                    } catch(IOException failure) {
                        writeFailures.incrementAndGet();
                    } finally {
                        if(temporary != null) temporary.delete();
                    }
                }
            });
        } catch(java.util.concurrent.RejectedExecutionException full) {
            // Keep READY. A full writer must never block the render thread.
            writeFailures.incrementAndGet();
        }
    }

    public static void resetForReload() {
        for(Entry entry : ENTRIES.values()) release(entry);
        ENTRIES.clear();
        QUEUE.clear();
        // Already running immutable writes remain safe: their filenames include the old fingerprint.
        WRITER.getQueue().clear();
        if(framebuffer != null) {
            int previous = GL11.glGetInteger(0x8CA6);
            framebuffer.deleteFramebuffer();
            OpenGlHelper.func_153171_g(0x8D40, previous);
            framebuffer = null;
        }
        nextWork = 0L;
        bakeStarted = false;
    }

    private static void release(Entry entry) {
        if(entry != null && entry.texture != null)
            Minecraft.getMinecraft().getTextureManager().deleteTexture(entry.texture);
    }

    private static void draw(Entry entry) {
        if(entry == null || entry.state != State.READY) return;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            Minecraft.getMinecraft().getTextureManager().bindTexture(entry.texture);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.01F);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1, 1, 1, 1);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.addVertexWithUV(0, 16, 0, 0, 1);
            t.addVertexWithUV(16, 16, 0, 1, 1);
            t.addVertexWithUV(16, 0, 0, 1, 0);
            t.addVertexWithUV(0, 0, 0, 0, 0);
            t.draw();
        } finally { GL11.glPopAttrib(); }
    }

    private static BufferedImage capture(Entry entry) throws IOException {
        if(!OpenGlHelper.isFramebufferEnabled()) throw new IOException("Framebuffer capture unavailable/disabled");
        int previousFramebuffer = GL11.glGetInteger(0x8CA6);
        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int previousActiveTexture = GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE);
        float previousLightX = OpenGlHelper.lastBrightnessX, previousLightY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(-1 /* GL_CLIENT_ALL_ATTRIB_BITS */);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            if(framebuffer == null) framebuffer = new Framebuffer(SIZE, SIZE, true);
            framebuffer.bindFramebuffer(true);
            framebuffer.checkFramebufferComplete();
            GL11.glViewport(0, 0, SIZE, SIZE);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            GL11.glClearColor(0, 0, 0, 0);
            GL11.glClearDepth(1);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            projection();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.01F);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glShadeModel(GL11.GL_SMOOTH);
            GL11.glColor4f(1, 1, 1, 1);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
            capturing = true;
            renderCanonical(entry);
            capturing = false;
            PIXELS.clear();
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glReadPixels(0, 0, SIZE, SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, PIXELS);
            BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            for(int y = 0; y < SIZE; ++y) for(int x = 0; x < SIZE; ++x) {
                int p = (x + y * SIZE) * 4;
                image.setRGB(x, SIZE - y - 1, (PIXELS.get(p + 3) & 255) << 24
                        | (PIXELS.get(p) & 255) << 16 | (PIXELS.get(p + 1) & 255) << 8 | PIXELS.get(p + 2) & 255);
            }
            return cropAndPad(image);
        } finally {
            capturing = false;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousLightX, previousLightY);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glPopClientAttrib();
            GL11.glPopAttrib();
            OpenGlHelper.func_153171_g(0x8D40, previousFramebuffer);
            OpenGlHelper.setActiveTexture(previousActiveTexture);
            GL11.glMatrixMode(previousMatrixMode);
        }
    }

    private static BufferedImage cropAndPad(BufferedImage source) throws IOException {
        int minX = SIZE, minY = SIZE, maxX = -1, maxY = -1;
        for(int y = 0; y < SIZE; ++y) for(int x = 0; x < SIZE; ++x) {
            if((source.getRGB(x, y) >>> 24) != 0) {
                minX = Math.min(minX, x); minY = Math.min(minY, y);
                maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
            }
        }
        if(maxX < minX) throw new IOException("Empty model capture");
        // Trim transparent bounds without normalizing away author scale/offset controls.
        // Uniform 8px canvas padding preserves relative sizes and displacements.
        BufferedImage output = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = output.createGraphics();
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(source, 8 + minX * 112 / SIZE, 8 + minY * 112 / SIZE,
                    8 + (maxX + 1) * 112 / SIZE, 8 + (maxY + 1) * 112 / SIZE,
                    minX, minY, maxX + 1, maxY + 1, null);
        } finally { g.dispose(); }
        return output;
    }

    private static void resourceHash(MessageDigest digest, ResourceLocation location) throws IOException {
        bytes(digest, location.toString());
        try(java.io.InputStream in = Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream()) {
            streamHash(digest, in);
        }
    }

    private static void streamHash(MessageDigest digest, java.io.InputStream in) throws IOException {
        byte[] buffer = new byte[16384];
        int read;
        while((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
    }

    private static void bytes(MessageDigest digest, String value) throws IOException {
        digest.update(value.getBytes("UTF-8"));
        digest.update((byte)0);
    }

    private static String hex(byte[] value) {
        StringBuilder result = new StringBuilder();
        for(byte b : value) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return result.toString();
    }

    private static String safeName(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }

    private static File directory() { return new File(Minecraft.getMinecraft().mcDataDir, "cache/mcheli/icons"); }
    private static File exportFile(String key) {
        return new File(directory(), "export/assets/mcheli/textures/icons/" + key + ".png");
    }

    private static void report(long now) {
        if(!diagnostics() || now - lastReport < 10000L) return;
        lastReport = now;
        System.out.println(String.format(java.util.Locale.ROOT,
                "[mcheli icons] prebaked=%d hits=%d misses=%d requests=%d deduplicated=%d captures=%d failures=%d loads=%d writes=%d writeFailures=%d queue=%d writerQueue=%d avgCaptureMs=%.2f totalCaptureMs=%.2f avgResolveLoadMs=%.2f",
                prebaked, hits, misses, requests, duplicates, captures, failures, loads, writes.get(), writeFailures.get(),
                QUEUE.size(), WRITER.getQueue().size(), captureNanos / 1e6 / Math.max(1, captures + failures),
                captureNanos / 1e6, loadNanos / 1e6 / Math.max(1, hits + misses + prebaked)));
    }


    private static long appearance(Object identity) {
        MCH_BaseVehicleInfo info = (MCH_BaseVehicleInfo)identity;
        long value = Float.floatToIntBits(info.bodyWidth);
        value = value * 31 + Float.floatToIntBits(info.bodyHeight);
        value = value * 31 + Float.floatToIntBits(info.itemIconScaleFactor);
        return value * 31 + Float.floatToIntBits(getTypeScale(info));
    }

    private enum State { QUEUED, GENERATING, READY, FAILED }
    private static final class Entry {
        final Object identity;
        final ItemStack stack;
        final long appearance;
        final IItemRenderer renderer;
        String key;
        ResourceLocation texture;
        net.minecraftforge.client.model.IModelCustom captureModel;
        State state = State.QUEUED;
        Entry(Object identity, ItemStack stack, IItemRenderer renderer) {
            this.identity = identity; this.stack = stack; this.renderer = renderer;
            this.appearance = appearance(identity);
        }
    }

    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        MCH_BaseVehicleInfo info = getInfo(item);
        if(info == null || !is3DIconEnabled(info)) return false;
        if(type == ItemRenderType.INVENTORY) return request(info, item, this).state == State.READY;
        mcheli.MCH_ClientProxy.ensureVehicleModel(info);
        return info.model != null;
    }
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) { return type != ItemRenderType.INVENTORY; }
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        MCH_BaseVehicleInfo info = getInfo(item);
        if(info == null || !is3DIconEnabled(info)) return;
        if(type == ItemRenderType.INVENTORY) { draw(request(info, item, this)); return; }
        mcheli.MCH_ClientProxy.ensureVehicleModel(info);
        if(info.model != null) renderLiveModel(type, info);
    }
    public void onResourceManagerReload(IResourceManager manager) { resetForReload(); }
    public static void onVehicleModelAvailable(MCH_BaseVehicleInfo info) {
        // Demand driven: loading an entity model does not enqueue an inventory capture.
    }
    public static void invalidate(MCH_BaseVehicleInfo info) { release(ENTRIES.remove(info)); QUEUE.remove(info); }
    private static MCH_BaseVehicleInfo getInfo(ItemStack stack) {
        return stack != null && stack.getItem() instanceof MCH_ItemBaseVehicle ? ((MCH_ItemBaseVehicle)stack.getItem()).getAircraftInfo() : null;
    }
    private static boolean is3DIconEnabled(MCH_BaseVehicleInfo info) {
        return info.enable3DItemIcon && (MCH_Config.Override3DItemIcon == null || !MCH_Config.Override3DItemIcon.prmBool);
    }
    private static void bake(Item item) {
        if(item instanceof MCH_ItemBaseVehicle) {
            ItemStack stack = new ItemStack(item);
            MCH_BaseVehicleInfo info = getInfo(stack);
            if(info != null && is3DIconEnabled(info)) request(info, stack, null);
        }
    }
    private static boolean diagnostics() {
        return MCH_Config.DebugVehicleInventorySnapshots != null && MCH_Config.DebugVehicleInventorySnapshots.prmBool;
    }
    private static void projection() { GL11.glOrtho(-1, 1, -1, 1, -1000, 1000); }
    private static void prepare(Entry entry) throws IOException {
        MCH_BaseVehicleInfo info = (MCH_BaseVehicleInfo)entry.identity;
        // A resource reload may have retained the world's old model object. Parse the
        // current source only on a cache miss, without replacing that world's reference.
        entry.captureModel = mcheli.MCH_ModelManager.load(info.getDirectoryName(), info.name, true);
        if(entry.captureModel == null) throw new IOException("Missing vehicle model");
    }
    private static void renderCanonical(Entry entry) {
        renderModel(ItemRenderType.INVENTORY, (MCH_BaseVehicleInfo)entry.identity, entry.captureModel);
        entry.captureModel = null;
    }
    private static void renderLiveModel(ItemRenderType type, MCH_BaseVehicleInfo info) {
        renderModel(type, info, info.model);
    }
    private static void renderModel(ItemRenderType type, MCH_BaseVehicleInfo info, net.minecraftforge.client.model.IModelCustom model) {
        GL11.glPushMatrix();
        GL11.glEnable(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1, 1, 1, 1);
        try {
            transform(type, info);
            ResourceLocation original = new ResourceLocation("mcheli", "textures/" + info.getDirectoryName()
                    + "/" + MCH_RenderBaseVehicle.getBaseTextureName(info.name) + ".png");
            Minecraft.getMinecraft().getTextureManager().bindTexture(
                    mcheli.texture.MCH_ModelTextureRepairManager.resolve(original, model, info.getDirectoryName() + "/" + info.name));
            MCH_RenderBaseVehicle.beginSkinOverlayRender(info.getDirectoryName(), info.name);
            try {
                model.renderAll();
                MCH_RenderBaseVehicle.renderPreparedSkinOverlay(model);
            } finally { MCH_RenderBaseVehicle.endSkinOverlayRender(); }
        } finally { GL11.glPopMatrix(); GL11.glColor4f(1, 1, 1, 1); GL11.glEnable(GL11.GL_BLEND); }
    }
    private static String cacheKey(Entry entry) throws Exception {
        MCH_BaseVehicleInfo info = (MCH_BaseVehicleInfo)entry.identity;
        String name = info.getDirectoryName() + "/" + info.name;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        bytes(digest, "mcheli-canonical-renderer-2|" + ICON_CACHE_VERSION + "|" + name
                + "|rotation:30,45|translate:0,-.35,0|scale:1.35|" + info.bodyWidth + "|" + info.bodyHeight
                + "|" + getTypeScale(info) + "|" + info.itemIconScaleFactor
                + "|" + MCH_Config.EnableModelTextureRepair.prmBool + "|" + MCH_Config.EnableModelUVCorrection.prmBool
                + "|" + MCH_Config.ModelTextureMaxHoleArea.prmInt + "|" + MCH_Config.ModelTextureMaxHoleThickness.prmInt
                + "|" + MCH_Config.ModelTextureRGBBleedRadius.prmInt + "|" + MCH_Config.ModelTextureAlphaExpansionRadius.prmInt
                + "|" + MCH_Config.ModelTextureUVCorrectionRadius.prmInt);
        sourceHash(digest, info.filePath != null ? info.filePath : "assets/mcheli/" + name + ".txt");
        boolean found = false;
        for(String extension : new String[]{".mqo", ".obj", ".tcn"}) {
            String path = "assets/mcheli/models/" + name + extension;
            if(mcheli.MCH_ResourceHelper.resourceExists(path)) { sourceHash(digest, path); found = true; break; }
        }
        if(!found) throw new IOException("Missing model source " + name);
        resourceHash(digest, new ResourceLocation("mcheli", "textures/" + info.getDirectoryName()
                + "/" + MCH_RenderBaseVehicle.getBaseTextureName(info.name) + ".png"));
        int overlay = info.name.indexOf("|skinoverlays/");
        if(overlay >= 0) resourceHash(digest, new ResourceLocation("mcheli",
                MCH_EntityBaseVehicle.getTexturePath(info.getDirectoryName(), info.name.substring(overlay + 1))));
        return safeName(name) + "-v" + ICON_CACHE_VERSION + "-" + hex(digest.digest());
    }
    private static void sourceHash(MessageDigest digest, String path) throws IOException {
        bytes(digest, path);
        try(java.io.InputStream in = mcheli.MCH_ResourceHelper.openResourceStream(path)) {
            if(in == null) throw new IOException("Missing source " + path);
            streamHash(digest, in);
        }
    }
   private static void transform(ItemRenderType type, MCH_BaseVehicleInfo info) {
      switch(type) {
         case ENTITY:
            GL11.glTranslatef(0.0F, 0.35F, 0.0F);
            GL11.glRotatef(35.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            break;
         case EQUIPPED:
            GL11.glTranslatef(0.25F, 0.45F, 0.55F);
            GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
            break;
         case EQUIPPED_FIRST_PERSON:
            GL11.glTranslatef(0.65F, 0.35F, 0.35F);
            GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(125.0F, 0.0F, 1.0F, 0.0F);
            break;
         case INVENTORY:
            GL11.glTranslatef(0.0F, -0.35F, 0.0F);
            GL11.glRotatef(30.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            break;
         default:
            break;
      }

      float largest = Math.max(Math.max(info.bodyWidth, info.bodyHeight), 1.0F);
      float scale = type == ItemRenderType.INVENTORY ? 1.35F / largest : 0.75F / largest;
      if(type == ItemRenderType.ENTITY) scale = 1.0F / largest;
      scale *= getTypeScale(info) * info.itemIconScaleFactor;
      GL11.glScalef(scale, scale, scale);
   }

   private static float getTypeScale(MCH_BaseVehicleInfo info) {
      String directory = info.getDirectoryName();
      if("helicopters".equalsIgnoreCase(directory)) return getScale(MCH_Config.Heli3DItemIconScale);
      if("planes".equalsIgnoreCase(directory)) return getScale(MCH_Config.Plane3DItemIconScale);
      if("ships".equalsIgnoreCase(directory)) return getScale(MCH_Config.Ship3DItemIconScale);
      if("tanks".equalsIgnoreCase(directory)) return getScale(MCH_Config.Tank3DItemIconScale);
      if("vehicles".equalsIgnoreCase(directory) || "turrets".equalsIgnoreCase(directory)) {
         return getScale(MCH_Config.Turret3DItemIconScale);
      }
      return 1.0F;
   }

   private static float getScale(MCH_ConfigPrm prm) {
      return prm != null ? Math.max((float)prm.prmDouble, 0.01F) : 1.0F;
   }

}
