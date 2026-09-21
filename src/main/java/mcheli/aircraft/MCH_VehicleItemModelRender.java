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
    private static final int MAX_REQUESTS = 256;
    private static final long BAKE_INTERVAL_MS = 350L;
    private static final Map<Object, Entry> ENTRIES = new IdentityHashMap<Object, Entry>();
    private static final LinkedHashMap<Object, Entry> QUEUE = new LinkedHashMap<Object, Entry>();
    private static final java.util.concurrent.ArrayBlockingQueue<Runnable> COMPLETIONS =
            new java.util.concurrent.ArrayBlockingQueue<Runnable>(16);
    private static final java.util.concurrent.atomic.AtomicLong writes = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong writeFailures = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong writeNanos = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong corruptFiles = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.ThreadPoolExecutor WORKER = executor("mcheli icon resolver", 8);
    private static final java.util.concurrent.ThreadPoolExecutor WRITER = executor("mcheli icon PNG writer", 32);
    private static Framebuffer framebuffer;
    private static final ByteBuffer PIXELS = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
    private static Entry active;
    private static long epoch;
    private static long nextBakeWork, lastReport, requests, duplicates, deferred, prebaked, hits, misses, captures, failures, loads;
    private static long resolveNanos, prepareNanos, renderNanos, readbackNanos, processNanos, uploadNanos;
    private static int queueHighWater, workerHighWater, writerHighWater;
    private static java.util.Iterator bakeItems;
    private static boolean bakeStarted;
    private static boolean capturing;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                drain(WORKER);
                drain(WRITER);
            }
        }, "mcheli icon cache shutdown"));
    }

    private static java.util.concurrent.ThreadPoolExecutor executor(final String name, int capacity) {
        return new java.util.concurrent.ThreadPoolExecutor(1, 1, 0L, java.util.concurrent.TimeUnit.MILLISECONDS,
                new java.util.concurrent.ArrayBlockingQueue<Runnable>(capacity), new java.util.concurrent.ThreadFactory() {
            public Thread newThread(Runnable job) {
                Thread thread = new Thread(job, name);
                thread.setDaemon(true);
                return thread;
            }
        }, new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
    }

    private static void drain(java.util.concurrent.ThreadPoolExecutor executor) {
        executor.shutdown();
        try { executor.awaitTermination(10L, java.util.concurrent.TimeUnit.SECONDS); }
        catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }

    public static boolean isCapturing() { return capturing; }

    private static Entry request(Object identity, ItemStack stack, IItemRenderer renderer) {
        Entry entry = ENTRIES.get(identity);
        if(entry != null && entry.appearance != appearance(identity)) {
            discard(entry);
            entry = null;
        }
        if(entry != null) {
            if(entry.state != State.READY && entry.state != State.FAILED) ++duplicates;
            if(entry.state == State.DEFERRED) enqueue(entry);
            return entry;
        }
        entry = new Entry(identity, new ItemStack(stack.getItem(), 1, 0), renderer, epoch);
        ENTRIES.put(identity, entry);
        enqueue(entry);
        ++requests;
        return entry;
    }

    private static void enqueue(Entry entry) {
        if(entry.cancelled || QUEUE.containsKey(entry.identity)) return;
        if(QUEUE.size() >= MAX_REQUESTS) {
            entry.state = State.DEFERRED;
            ++deferred;
            return;
        }
        entry.state = State.REQUESTED;
        QUEUE.put(entry.identity, entry);
        queueHighWater = Math.max(queueHighWater, QUEUE.size());
    }

    /** Advances at most one capture stage per render frame; item callbacks only request or draw icons. */
    public static void onRenderFrame() {
        long now = Minecraft.getSystemTime();
        report(now);
        Runnable completion = COMPLETIONS.poll();
        if(completion != null) completion.run();
        if(Boolean.getBoolean("mcheli.bakeIcons") && now >= nextBakeWork) {
            nextBakeWork = now + BAKE_INTERVAL_MS;
            if(!bakeStarted) { bakeItems = Item.itemRegistry.iterator(); bakeStarted = true; }
            for(int i = 0; i < 16 && bakeItems.hasNext(); ++i) bake((Item)bakeItems.next());
        }
        if(active == null && !QUEUE.isEmpty()) {
            active = QUEUE.values().iterator().next();
            QUEUE.remove(active.identity);
            submitResolve(active);
            return;
        }
        if(active == null) return;
        try {
            switch(active.state) {
                case PREPARE_CAPTURE: timedPrepare(active); break;
                case RENDER_MODEL: timedRender(active); break;
                case READBACK: timedReadback(active); break;
                case UPLOAD_TEXTURE: timedUpload(active); break;
                case QUEUE_DISK_WRITE: finish(active); break;
                default: break;
            }
        } catch(Exception failure) {
            fail(active, failure);
        }
    }

    private static void submitResolve(final Entry entry) {
        entry.state = State.RESOLVE_ASSETS;
        final File cacheDirectory = directory();
        try {
            WORKER.execute(new Runnable() {
                public void run() {
                    long started = System.nanoTime();
                    try {
                        final String key = cacheKey(entry);
                        final ResolveResult result = resolve(key, cacheDirectory);
                        final long elapsed = System.nanoTime() - started;
                        complete(new Runnable() {
                            public void run() {
                                if(!current(entry)) return;
                                entry.key = key;
                                resolveNanos += elapsed;
                                corruptFiles.addAndGet(result.corrupt);
                                if(result.image != null) {
                                    entry.image = result.image;
                                    if(result.source == LoadSource.PREBAKED) ++prebaked; else ++hits;
                                    ++loads;
                                    entry.state = State.UPLOAD_TEXTURE;
                                } else {
                                    ++misses;
                                    entry.persist = true;
                                    entry.state = State.PREPARE_CAPTURE;
                                }
                            }
                        });
                    } catch(final Exception failure) {
                        complete(new Runnable() {
                            public void run() { if(current(entry)) fail(entry, failure); }
                        });
                    }
                }
            });
            workerHighWater = Math.max(workerHighWater, WORKER.getQueue().size());
        } catch(java.util.concurrent.RejectedExecutionException full) {
            fail(entry, new IOException("Icon resolver queue is full"));
        }
    }

    private static ResolveResult resolve(String key, File cacheDirectory) {
        int corrupt = 0;
        BufferedImage image = null;
        java.io.InputStream stream = null;
        try {
            stream = mcheli.MCH_ResourceHelper.openResourceStream(
                    "assets/mcheli/textures/icons/" + key + ".png");
            if(stream != null) {
                image = valid(ImageIO.read(stream));
                if(image == null) ++corrupt;
            }
        } catch(IOException invalid) { ++corrupt; }
        finally { if(stream != null) try { stream.close(); } catch(IOException ignored) {} }
        if(image != null) return new ResolveResult(image, LoadSource.PREBAKED, corrupt);

        File file = new File(cacheDirectory, key + ".png");
        if(file.isFile()) {
            try { image = valid(ImageIO.read(file)); }
            catch(IOException invalid) { image = null; }
            if(image == null) {
                ++corrupt;
                try { Files.deleteIfExists(file.toPath()); } catch(IOException ignored) {}
            }
        }
        return new ResolveResult(image, image != null ? LoadSource.DISK : LoadSource.NONE, corrupt);
    }

    private static void timedPrepare(Entry entry) throws IOException {
        long started = System.nanoTime();
        try { prepare(entry); entry.state = State.RENDER_MODEL; }
        finally { prepareNanos += System.nanoTime() - started; }
    }

    private static void timedRender(Entry entry) throws IOException {
        long started = System.nanoTime();
        try { renderCapture(entry); ++captures; entry.state = State.READBACK; }
        finally { renderNanos += System.nanoTime() - started; }
    }

    private static void timedReadback(final Entry entry) throws IOException {
        long started = System.nanoTime();
        final byte[] pixels;
        try { pixels = readback(); }
        finally { readbackNanos += System.nanoTime() - started; }
        entry.state = State.PROCESS_PIXELS;
        try {
            WORKER.execute(new Runnable() {
                public void run() {
                    long processStarted = System.nanoTime();
                    try {
                        final BufferedImage image = processPixels(pixels);
                        final long elapsed = System.nanoTime() - processStarted;
                        complete(new Runnable() {
                            public void run() {
                                if(!current(entry)) return;
                                processNanos += elapsed;
                                entry.image = image;
                                entry.state = State.UPLOAD_TEXTURE;
                            }
                        });
                    } catch(final Exception failure) {
                        complete(new Runnable() {
                            public void run() { if(current(entry)) fail(entry, failure); }
                        });
                    }
                }
            });
            workerHighWater = Math.max(workerHighWater, WORKER.getQueue().size());
        } catch(java.util.concurrent.RejectedExecutionException full) {
            throw new IOException("Icon pixel processor queue is full", full);
        }
    }

    private static void timedUpload(Entry entry) {
        long started = System.nanoTime();
        try {
            entry.texture = Minecraft.getMinecraft().getTextureManager()
                    .getDynamicTextureLocation("mcheli_inventory_icon", new DynamicTexture(entry.image));
            entry.state = State.QUEUE_DISK_WRITE;
        } finally { uploadNanos += System.nanoTime() - started; }
    }

    private static void finish(Entry entry) {
        if(entry.persist) save(entry.image, new File(directory(), entry.key + ".png"));
        if(Boolean.getBoolean("mcheli.bakeIcons")) save(entry.image, exportFile(entry.key));
        entry.image = null;
        entry.state = State.READY;
        if(active == entry) active = null;
    }

    private static void complete(Runnable completion) {
        if(!COMPLETIONS.offer(completion)) writeFailures.incrementAndGet();
    }

    private static boolean current(Entry entry) {
        return !entry.cancelled && entry.epoch == epoch && ENTRIES.get(entry.identity) == entry;
    }

    private static void fail(Entry entry, Exception failure) {
        if(entry == null) return;
        String stage = entry.state != null ? entry.state.name() : "UNKNOWN";
        entry.captureModel = null;
        entry.captureTexture = null;
        entry.image = null;
        entry.state = State.FAILED;
        ++failures;
        if(active == entry) active = null;
        System.err.println("[mcheli icons] Failed at " + stage + " for "
                + entry.stack.getUnlocalizedName() + ": " + failure);
    }

    private static BufferedImage valid(BufferedImage image) {
        return image != null && image.getWidth() == SIZE && image.getHeight() == SIZE ? image : null;
    }

    private static void save(final BufferedImage image, final File destination) {
        try {
            WRITER.execute(new Runnable() {
                public void run() {
                    long started = System.nanoTime();
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
                    } catch(Exception failure) {
                        writeFailures.incrementAndGet();
                    } finally {
                        writeNanos.addAndGet(System.nanoTime() - started);
                        if(temporary != null) temporary.delete();
                    }
                }
            });
            writerHighWater = Math.max(writerHighWater, WRITER.getQueue().size());
        } catch(java.util.concurrent.RejectedExecutionException full) {
            writeFailures.incrementAndGet();
        }
    }

    public static void resetForReload() {
        ++epoch;
        if(active != null) active.cancelled = true;
        active = null;
        for(Entry entry : ENTRIES.values()) { entry.cancelled = true; release(entry); }
        ENTRIES.clear();
        QUEUE.clear();
        WORKER.getQueue().clear();
        COMPLETIONS.clear();
        // Completed images are immutable and fingerprinted, so queued disk writes remain valid.
        if(framebuffer != null) {
            int previous = GL11.glGetInteger(0x8CA6);
            framebuffer.deleteFramebuffer();
            OpenGlHelper.func_153171_g(0x8D40, previous);
            framebuffer = null;
        }
        nextBakeWork = 0L;
        bakeStarted = false;
        capturing = false;
    }

    private static void discard(Entry entry) {
        entry.cancelled = true;
        release(entry);
        ENTRIES.remove(entry.identity);
        QUEUE.remove(entry.identity);
        if(active == entry) active = null;
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

    private static void renderCapture(Entry entry) throws IOException {
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
        } finally {
            capturing = false;
            entry.captureModel = null;
            entry.captureTexture = null;
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

    private static byte[] readback() throws IOException {
        if(framebuffer == null) throw new IOException("Missing icon framebuffer");
        int previousFramebuffer = GL11.glGetInteger(0x8CA6);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(-1);
        try {
            framebuffer.bindFramebuffer(true);
            PIXELS.clear();
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glReadPixels(0, 0, SIZE, SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, PIXELS);
            byte[] result = new byte[SIZE * SIZE * 4];
            for(int i = 0; i < result.length; ++i) result[i] = PIXELS.get(i);
            return result;
        } finally {
            GL11.glPopClientAttrib();
            GL11.glPopAttrib();
            OpenGlHelper.func_153171_g(0x8D40, previousFramebuffer);
        }
    }

    private static BufferedImage processPixels(byte[] pixels) throws IOException {
        int[] argb = new int[SIZE * SIZE];
        for(int y = 0; y < SIZE; ++y) for(int x = 0; x < SIZE; ++x) {
            int p = (x + y * SIZE) * 4;
            argb[x + (SIZE - y - 1) * SIZE] = (pixels[p + 3] & 255) << 24
                    | (pixels[p] & 255) << 16 | (pixels[p + 1] & 255) << 8 | pixels[p + 2] & 255;
        }
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, SIZE, SIZE, argb, 0, SIZE);
        return cropAndPad(image);
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
                "[mcheli icons] stage=%s prebaked=%d hits=%d misses=%d corrupt=%d requests=%d deduplicated=%d deferred=%d captures=%d failures=%d loads=%d writes=%d writeFailures=%d queue=%d/%d workerQueue=%d/%d writerQueue=%d/%d avgResolveMs=%.2f avgPrepareMs=%.2f avgRenderMs=%.2f avgReadbackMs=%.2f avgProcessMs=%.2f avgUploadMs=%.2f avgWriteMs=%.2f",
                active != null ? active.state : "IDLE", prebaked, hits, misses, corruptFiles.get(), requests, duplicates,
                deferred, captures, failures, loads, writes.get(), writeFailures.get(), QUEUE.size(), queueHighWater,
                WORKER.getQueue().size(), workerHighWater, WRITER.getQueue().size(), writerHighWater,
                resolveNanos / 1e6 / Math.max(1, hits + misses + prebaked), prepareNanos / 1e6 / Math.max(1, captures + failures),
                renderNanos / 1e6 / Math.max(1, captures), readbackNanos / 1e6 / Math.max(1, captures),
                processNanos / 1e6 / Math.max(1, captures), uploadNanos / 1e6 / Math.max(1, loads + captures),
                writeNanos.get() / 1e6 / Math.max(1, writes.get() + writeFailures.get())));
    }


    private static long appearance(Object identity) {
        MCH_BaseVehicleInfo info = (MCH_BaseVehicleInfo)identity;
        long value = Float.floatToIntBits(info.bodyWidth);
        value = value * 31 + Float.floatToIntBits(info.bodyHeight);
        value = value * 31 + Float.floatToIntBits(info.itemIconScaleFactor);
        return value * 31 + Float.floatToIntBits(getTypeScale(info));
    }

    private enum State {
        DEFERRED, REQUESTED, RESOLVE_ASSETS, PREPARE_CAPTURE, RENDER_MODEL, READBACK,
        PROCESS_PIXELS, UPLOAD_TEXTURE, QUEUE_DISK_WRITE, READY, FAILED
    }
    private enum LoadSource { NONE, PREBAKED, DISK }
    private static final class ResolveResult {
        final BufferedImage image;
        final LoadSource source;
        final int corrupt;
        ResolveResult(BufferedImage image, LoadSource source, int corrupt) {
            this.image = image; this.source = source; this.corrupt = corrupt;
        }
    }
    private static final class Entry {
        final Object identity;
        final ItemStack stack;
        final long appearance;
        final IItemRenderer renderer;
        final long epoch;
        String key;
        ResourceLocation texture;
        ResourceLocation captureTexture;
        net.minecraftforge.client.model.IModelCustom captureModel;
        BufferedImage image;
        State state = State.REQUESTED;
        boolean persist;
        boolean cancelled;
        Entry(Object identity, ItemStack stack, IItemRenderer renderer, long epoch) {
            this.identity = identity; this.stack = stack; this.renderer = renderer; this.epoch = epoch;
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
    public static void invalidate(MCH_BaseVehicleInfo info) {
        Entry entry = ENTRIES.get(info);
        if(entry != null) discard(entry);
    }
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
        if(info.model == null) mcheli.MCH_ClientProxy.ensureVehicleModel(info);
        entry.captureModel = info.model;
        if(entry.captureModel == null) throw new IOException("Missing vehicle model");
        entry.captureTexture = resolveTexture(info, entry.captureModel);
    }
    private static void renderCanonical(Entry entry) {
        renderModel(ItemRenderType.INVENTORY, (MCH_BaseVehicleInfo)entry.identity,
                entry.captureModel, entry.captureTexture);
    }
    private static void renderLiveModel(ItemRenderType type, MCH_BaseVehicleInfo info) {
        renderModel(type, info, info.model, resolveTexture(info, info.model));
    }
    private static ResourceLocation resolveTexture(MCH_BaseVehicleInfo info, net.minecraftforge.client.model.IModelCustom model) {
        ResourceLocation original = new ResourceLocation("mcheli", "textures/" + info.getDirectoryName()
                + "/" + MCH_RenderBaseVehicle.getBaseTextureName(info.name) + ".png");
        return mcheli.texture.MCH_ModelTextureRepairManager.resolve(
                original, model, info.getDirectoryName() + "/" + info.name);
    }
    private static void renderModel(ItemRenderType type, MCH_BaseVehicleInfo info,
            net.minecraftforge.client.model.IModelCustom model, ResourceLocation texture) {
        GL11.glPushMatrix();
        GL11.glEnable(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1, 1, 1, 1);
        try {
            transform(type, info);
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
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
        sourceHash(digest, "assets/mcheli/textures/" + info.getDirectoryName()
                + "/" + MCH_RenderBaseVehicle.getBaseTextureName(info.name) + ".png");
        int overlay = info.name.indexOf("|skinoverlays/");
        if(overlay >= 0) sourceHash(digest, "assets/mcheli/"
                + MCH_EntityBaseVehicle.getTexturePath(info.getDirectoryName(), info.name.substring(overlay + 1)));
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
