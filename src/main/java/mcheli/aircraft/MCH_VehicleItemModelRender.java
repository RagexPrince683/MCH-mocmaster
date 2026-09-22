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
import org.lwjgl.opengl.ARBSync;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GLSync;
import mcheli.MCH_Config;
import mcheli.MCH_ConfigPrm;

/** Model capture produces persistent pixels; ready inventory icons never draw geometry. */
public class MCH_VehicleItemModelRender implements IItemRenderer, IResourceManagerReloadListener {

    public static final int ICON_CACHE_VERSION = 4;
    private static final int SIZE = 128;
    private static final int MAX_RESOLVE_BACKLOG = 1024;
    private static final int MAX_CAPTURE_BACKLOG = 1024;
    private static final int PIXEL_BYTES = SIZE * SIZE * 4;
    private static final int VBO_VERTICES_PER_FRAME = 2048;
    private static final int VBO_VERTICES_PER_CHUNK = 512;
    private static final long VBO_TIME_BUDGET_NS = 750000L;
    private static final long BAKE_INTERVAL_MS = 350L;
    private static final long CAPTURE_INTERVAL_MS = 350L;
    private static final Map<Object, Entry> ENTRIES = new IdentityHashMap<Object, Entry>();
    private static final LinkedHashMap<Object, Entry> RESOLVE_BACKLOG = new LinkedHashMap<Object, Entry>();
    private static final LinkedHashMap<Object, Entry> MODEL_BACKLOG = new LinkedHashMap<Object, Entry>();
    private static final LinkedHashMap<Object, Entry> CAPTURE_BACKLOG = new LinkedHashMap<Object, Entry>();
    private static final LinkedHashMap<Object, Entry> READY_UPLOADS = new LinkedHashMap<Object, Entry>();
    private static final java.util.concurrent.ArrayBlockingQueue<Runnable> COMPLETIONS =
            new java.util.concurrent.ArrayBlockingQueue<Runnable>(16);
    private static final java.util.concurrent.atomic.AtomicLong writes = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong writeFailures = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong writeNanos = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong corruptFiles = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.ThreadPoolExecutor WORKER = executor("mcheli icon resolver", 8, true);
    private static final java.util.concurrent.ThreadPoolExecutor WRITER = executor("mcheli icon PNG writer", 32, false);
    private static Framebuffer framebuffer;
    private static final ByteBuffer PIXELS = BufferUtils.createByteBuffer(PIXEL_BYTES);
    private static final int[] PIXEL_PACK_BUFFERS = new int[2];
    private static int nextPixelPackBuffer;
    private static GLSync pixelPackFence;
    private static boolean pixelPackDisabled;
    private static Entry active;
    private static long epoch;
    private static long nextBakeWork, nextCaptureWork, lastReport, requests, duplicates, deferred, prebaked, hits, misses,
            captures, failures, loads, resolverSubmissions, resolverDeferred, resolverFailures, processorDeferred,
            inventoryRequests, handleRequests, renderItemRequests, modelSubmissions, modelDeferred, modelFailures,
            modelWaits, pboIssues, pboCompletions, pboFallbacks;
    private static long resolveNanos, modelLookupNanos, textureNanos, vboNanos, framebufferNanos, renderNanos,
            readPixelsNanos, pixelCopyNanos, processNanos, uploadNanos, readyNanos, modelLoadNanos,
            textureWarmNanos, framebufferCreateNanos, pboIssueNanos, pboLatencyNanos,
            framebufferBindNanos, clearNanos, stateSetupNanos, textureBindNanos, modelDrawNanos,
            overlayDrawNanos, captureRestoreNanos, textureAllocationNanos, textureDataCopyNanos,
            textureGpuUploadNanos;
    private static long resolveWorst, modelLookupWorst, textureWorst, vboWorst, framebufferWorst, renderWorst,
            readPixelsWorst, pixelCopyWorst, processWorst, uploadWorst, readyWorst, modelLoadWorst,
            textureWarmWorst, framebufferCreateWorst, pboIssueWorst, pboLatencyWorst;
    private static long framebufferBindWorst, clearWorst, stateSetupWorst, textureBindWorst, modelDrawWorst,
            overlayDrawWorst, captureRestoreWorst, textureAllocationWorst, textureDataCopyWorst,
            textureGpuUploadWorst;
    private static long vboGroupsPrepared;
    private static int resolveBacklogHighWater, captureBacklogHighWater, workerHighWater, writerHighWater;
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

    private static java.util.concurrent.ThreadPoolExecutor executor(final String name, int capacity, final boolean daemon) {
        return new java.util.concurrent.ThreadPoolExecutor(0, 1, 5L, java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.ArrayBlockingQueue<Runnable>(capacity), new java.util.concurrent.ThreadFactory() {
            public Thread newThread(Runnable job) {
                Thread thread = new Thread(job, name);
                thread.setDaemon(daemon);
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

    private static Entry request(Object identity, ItemStack stack, IItemRenderer renderer, RequestOrigin origin) {
        ++inventoryRequests;
        if(origin == RequestOrigin.HANDLE_RENDER_TYPE) ++handleRequests; else ++renderItemRequests;
        Entry entry = ENTRIES.get(identity);
        if(entry != null && entry.appearance != appearance(identity)) {
            discard(entry);
            entry = null;
        }
        if(entry != null) {
            if(entry.state != State.READY && entry.state != State.FAILED) ++duplicates;
            if(entry.state == State.DEFERRED) enqueueResolve(entry);
            return entry;
        }
        entry = new Entry(identity, new ItemStack(stack.getItem(), 1, 0), renderer, epoch);
        ENTRIES.put(identity, entry);
        enqueueResolve(entry);
        ++requests;
        return entry;
    }

    private static void enqueueResolve(Entry entry) {
        if(entry.cancelled || RESOLVE_BACKLOG.containsKey(entry.identity)) return;
        if(RESOLVE_BACKLOG.size() >= MAX_RESOLVE_BACKLOG) {
            entry.state = State.DEFERRED;
            ++deferred;
            return;
        }
        entry.state = State.WAIT_RESOLVE;
        RESOLVE_BACKLOG.put(entry.identity, entry);
        resolveBacklogHighWater = Math.max(resolveBacklogHighWater, RESOLVE_BACKLOG.size());
    }

    /** Advances cheap states immediately while each expensive render-thread operation remains bounded. */
    public static void onRenderFrame() {
        long now = Minecraft.getSystemTime();
        report(now);
        Runnable completion = COMPLETIONS.poll();
        if(completion != null) completion.run();
        if(!READY_UPLOADS.isEmpty()) {
            Entry ready = READY_UPLOADS.values().iterator().next();
            READY_UPLOADS.remove(ready.identity);
            try { timedUpload(ready); }
            catch(Exception failure) { fail(ready, failure); }
            return;
        }
        submitQueuedLookup();
        if(Boolean.getBoolean("mcheli.bakeIcons") && now >= nextBakeWork) {
            nextBakeWork = now + BAKE_INTERVAL_MS;
            if(!bakeStarted) { bakeItems = Item.itemRegistry.iterator(); bakeStarted = true; }
            for(int i = 0; i < 16 && bakeItems.hasNext(); ++i) bake((Item)bakeItems.next());
        }
        if(active == null && !CAPTURE_BACKLOG.isEmpty()) {
            active = CAPTURE_BACKLOG.values().iterator().next();
            CAPTURE_BACKLOG.remove(active.identity);
        }
        if(active == null) return;
        try {
            switch(active.state) {
                case PREPARE_CAPTURE: timedPrepare(active); break;
                case PREPARE_TEXTURE: timedPrepareTexture(active); break;
                case PREPARE_MODEL_BUFFERS: timedVbo(active); break;
                case PREPARE_FRAMEBUFFER: timedPrepareFramebuffer(active); break;
                case RENDER_MODEL:
                    if(now >= nextCaptureWork) timedRender(active);
                    break;
                case READBACK: timedReadback(active); break;
                case WAIT_PBO: timedPboCompletion(active); break;
                case WAIT_PROCESS: trySubmitPixelProcessing(active); break;
                case UPLOAD_TEXTURE: timedUpload(active); break;
                default: break;
            }
        } catch(Exception failure) {
            fail(active, failure);
        }
    }

    private static boolean submitResolve(final Entry entry) {
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
                                if(!current(entry) || entry.state != State.RESOLVING) return;
                                entry.key = key;
                                entry.cacheFile = new File(cacheDirectory, key + ".png");
                                resolveNanos += elapsed;
                                resolveWorst = Math.max(resolveWorst, elapsed);
                                corruptFiles.addAndGet(result.corrupt);
                                if(result.image != null) {
                                    entry.image = result.image;
                                    if(result.source == LoadSource.PREBAKED) ++prebaked; else ++hits;
                                    ++loads;
                                    transition(entry, State.UPLOAD_TEXTURE,
                                            result.source == LoadSource.PREBAKED ? "prebaked-hit" : "disk-hit");
                                    READY_UPLOADS.put(entry.identity, entry);
                                } else {
                                    ++misses;
                                    entry.persist = true;
                                    MCH_BaseVehicleInfo info = (MCH_BaseVehicleInfo)entry.identity;
                                    if(info.model == null) enqueueModel(entry, result.reason);
                                    else {
                                        transition(entry, State.PREPARE_CAPTURE, result.reason);
                                        enqueueCapture(entry);
                                    }
                                }
                            }
                        });
                    } catch(final Exception failure) {
                        complete(new Runnable() {
                            public void run() {
                                if(current(entry)) { ++resolverFailures; fail(entry, failure); }
                            }
                        });
                    }
                }
            });
            transition(entry, State.RESOLVING, "lookup-queued");
            ++resolverSubmissions;
            workerHighWater = Math.max(workerHighWater, WORKER.getQueue().size());
            return true;
        } catch(java.util.concurrent.RejectedExecutionException full) {
            entry.state = State.WAIT_RESOLVE;
            ++resolverDeferred;
            return false;
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
        if(image != null) return new ResolveResult(image, LoadSource.PREBAKED, corrupt, "prebaked-hit");

        File file = new File(cacheDirectory, key + ".png");
        if(file.isFile()) {
            try { image = valid(ImageIO.read(file)); }
            catch(IOException invalid) { image = null; }
            if(image == null) {
                ++corrupt;
                try { Files.deleteIfExists(file.toPath()); } catch(IOException ignored) {}
            }
        }
        return new ResolveResult(image, image != null ? LoadSource.DISK : LoadSource.NONE, corrupt,
                image != null ? "disk-hit" : corrupt > 0 ? "invalid-cache" : "disk-miss");
    }

    private static void timedPrepare(Entry entry) throws IOException {
        MCH_BaseVehicleInfo info = (MCH_BaseVehicleInfo)entry.identity;
        long started = System.nanoTime();
        entry.captureModel = info.model;
        long elapsed = System.nanoTime() - started;
        modelLookupNanos += elapsed; modelLookupWorst = Math.max(modelLookupWorst, elapsed);
        if(entry.captureModel == null) {
            enqueueModel(entry, "body-model-not-yet-ready");
            if(active == entry) active = null;
            return;
        }

        started = System.nanoTime();
        entry.captureTexture = resolvedTextureIfPresent(info, entry.captureModel);
        elapsed = System.nanoTime() - started;
        textureNanos += elapsed; textureWorst = Math.max(textureWorst, elapsed);
        if(!modelSupportsPreparedBuffers(entry.captureModel))
            throw new IOException("Vehicle model has no safe prepared-VBO capture path");
        entry.captureOverlay = resolveOverlayTexture(info);
        entry.preset = CompositionPreset.forInfo(info);
        entry.capturePass = CapturePass.PREVIEW;
        entry.finalAttempts = 0;
        initializePreviewComposition(entry);
        transition(entry, State.PREPARE_TEXTURE, "capture-resources-resolved");
    }

    private static void timedPrepareTexture(Entry entry) {
        long started = System.nanoTime();
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            net.minecraft.client.renderer.texture.TextureManager manager = Minecraft.getMinecraft().getTextureManager();
            manager.bindTexture(entry.captureTexture);
            if(entry.captureOverlay != null) manager.bindTexture(entry.captureOverlay);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            long elapsed = System.nanoTime() - started;
            textureWarmNanos += elapsed; textureWarmWorst = Math.max(textureWarmWorst, elapsed);
        }
        entry.modelGroups = groups(entry.captureModel);
        transition(entry, State.PREPARE_MODEL_BUFFERS, "textures-resident");
    }

    private static void timedVbo(Entry entry) {
        long started = System.nanoTime();
        try {
            int budget = VBO_VERTICES_PER_FRAME;
            long deadline = started + VBO_TIME_BUDGET_NS;
            while(budget > 0) {
                if(entry.activeGroup == null) {
                    if(entry.modelGroups == null || !entry.modelGroups.hasNext()) {
                        transition(entry, State.PREPARE_FRAMEBUFFER, "model-buffers-ready");
                        return;
                    }
                    entry.activeGroup = (mcheli.wrapper.modelloader.W_GroupObject)entry.modelGroups.next();
                }
                int pending = entry.activeGroup.getPendingVboVertices();
                if(pending <= 0) {
                    entry.activeGroup = null;
                    continue;
                }
                int allowed = Math.min(Math.min(budget, pending), VBO_VERTICES_PER_CHUNK);
                boolean ready = entry.activeGroup.prepareVboChunk(allowed);
                budget -= allowed;
                if(ready) {
                    ++vboGroupsPrepared;
                    entry.activeGroup = null;
                }
                if(System.nanoTime() >= deadline) break;
            }
        } finally {
            long elapsed = System.nanoTime() - started;
            vboNanos += elapsed; vboWorst = Math.max(vboWorst, elapsed);
        }
    }

    private static void timedPrepareFramebuffer(Entry entry) throws IOException {
        if(!OpenGlHelper.isFramebufferEnabled()) throw new IOException("Framebuffer capture unavailable/disabled");
        long started = System.nanoTime();
        if(framebuffer == null) {
            int previousFramebuffer = GL11.glGetInteger(0x8CA6);
            framebuffer = new Framebuffer(SIZE, SIZE, true);
            OpenGlHelper.func_153171_g(0x8D40, previousFramebuffer);
        }
        ensurePixelPackBuffers();
        long elapsed = System.nanoTime() - started;
        framebufferCreateNanos += elapsed; framebufferCreateWorst = Math.max(framebufferCreateWorst, elapsed);
        transition(entry, State.RENDER_MODEL, "capture-target-ready");
    }

    private static void timedRender(Entry entry) throws IOException {
        if(renderCapture(entry)) {
            ++captures;
            nextCaptureWork = Minecraft.getSystemTime() + CAPTURE_INTERVAL_MS;
            transition(entry, State.READBACK, "capture-complete");
        }
    }

    private static void timedReadback(final Entry entry) throws IOException {
        if(supportsAsyncReadback()) {
            try {
                issuePboReadback(entry);
                return;
            } catch(Exception unsupportedAtRuntime) {
                deletePixelPackFence();
                pixelPackDisabled = true;
                ++pboFallbacks;
            }
        } else {
            ++pboFallbacks;
        }
        entry.pixels = readbackSynchronously();
        transition(entry, State.WAIT_PROCESS, "readback-complete");
        trySubmitPixelProcessing(entry);
    }

    private static void timedPboCompletion(Entry entry) throws IOException {
        if(pixelPackFence == null) throw new IOException("Missing pixel readback fence");
        int result = clientWait(pixelPackFence);
        if(result == ARBSync.GL_TIMEOUT_EXPIRED) return;
        if(result == ARBSync.GL_WAIT_FAILED) {
            fallbackFromPbo(entry, "pbo-fence-wait-failed");
            return;
        }
        long started = System.nanoTime();
        int previousBuffer = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, PIXEL_PACK_BUFFERS[entry.pixelPackBuffer]);
        boolean valid = false;
        try {
            ByteBuffer mapped = GL15.glMapBuffer(GL21.GL_PIXEL_PACK_BUFFER, GL15.GL_READ_ONLY,
                    PIXEL_BYTES, null);
            if(mapped != null) {
                entry.pixels = new byte[PIXEL_BYTES];
                mapped.position(0);
                mapped.get(entry.pixels);
                valid = GL15.glUnmapBuffer(GL21.GL_PIXEL_PACK_BUFFER);
            }
        } finally {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, previousBuffer);
            deletePixelPackFence();
        }
        if(!valid) {
            entry.pixels = null;
            fallbackFromPbo(entry, "pbo-map-failed");
            return;
        }
        long copyElapsed = System.nanoTime() - started;
        pixelCopyNanos += copyElapsed; pixelCopyWorst = Math.max(pixelCopyWorst, copyElapsed);
        long latency = System.nanoTime() - entry.pixelPackIssuedNanos;
        pboLatencyNanos += latency; pboLatencyWorst = Math.max(pboLatencyWorst, latency);
        ++pboCompletions;
        transition(entry, State.WAIT_PROCESS, "pbo-readback-complete");
        trySubmitPixelProcessing(entry);
    }

    private static void trySubmitPixelProcessing(final Entry entry) {
        final byte[] pixels = entry.pixels;
        final CapturePass pass = entry.capturePass;
        try {
            WORKER.execute(new Runnable() {
                public void run() {
                    long processStarted = System.nanoTime();
                    try {
                        final PixelResult result = processPixels(pixels, pass == CapturePass.FINAL);
                        final long elapsed = System.nanoTime() - processStarted;
                        complete(new Runnable() {
                            public void run() {
                                if(!current(entry)) return;
                                processNanos += elapsed; processWorst = Math.max(processWorst, elapsed);
                                entry.pixels = null;
                                if(pass == CapturePass.PREVIEW) {
                                    applyPreviewComposition(entry, result.bounds);
                                    entry.capturePass = CapturePass.FINAL;
                                    transition(entry, State.RENDER_MODEL, "preview-silhouette-measured");
                                } else if(needsSafetyCorrection(entry, result.bounds)) {
                                    if(entry.finalAttempts++ < 1) {
                                        applySafetyCorrection(entry, result.bounds);
                                        transition(entry, State.RENDER_MODEL, "final-edge-safety-correction");
                                    } else {
                                        fail(entry, new IOException("Final icon silhouette exceeds safety margin"));
                                    }
                                } else {
                                    entry.image = result.image;
                                    clearCaptureResources(entry);
                                    transition(entry, State.UPLOAD_TEXTURE, "pixels-ready");
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
            transition(entry, State.PROCESSING, "pixel-processing-queued");
            workerHighWater = Math.max(workerHighWater, WORKER.getQueue().size());
        } catch(java.util.concurrent.RejectedExecutionException full) {
            entry.state = State.WAIT_PROCESS;
            ++processorDeferred;
        }
    }

    private static void timedUpload(Entry entry) {
        long started = System.nanoTime();
        try {
            long stageStarted = System.nanoTime();
            DynamicTexture dynamic = new DynamicTexture(entry.image.getWidth(), entry.image.getHeight());
            long elapsed = System.nanoTime() - stageStarted;
            textureAllocationNanos += elapsed; textureAllocationWorst = Math.max(textureAllocationWorst, elapsed);
            stageStarted = System.nanoTime();
            entry.image.getRGB(0, 0, entry.image.getWidth(), entry.image.getHeight(),
                    dynamic.getTextureData(), 0, entry.image.getWidth());
            elapsed = System.nanoTime() - stageStarted;
            textureDataCopyNanos += elapsed; textureDataCopyWorst = Math.max(textureDataCopyWorst, elapsed);
            stageStarted = System.nanoTime();
            dynamic.updateDynamicTexture();
            elapsed = System.nanoTime() - stageStarted;
            textureGpuUploadNanos += elapsed; textureGpuUploadWorst = Math.max(textureGpuUploadWorst, elapsed);
            entry.texture = Minecraft.getMinecraft().getTextureManager()
                    .getDynamicTextureLocation("mcheli_inventory_icon", dynamic);
            if(entry.persist) save(entry, entry.image, entry.cacheFile);
            if(Boolean.getBoolean("mcheli.bakeIcons")) save(entry, entry.image, exportFile(entry.key));
            entry.image = null;
            long readyElapsed = System.nanoTime() - entry.requestedNanos;
            readyNanos += readyElapsed; readyWorst = Math.max(readyWorst, readyElapsed);
            transition(entry, State.READY, entry.persist ? "generated" : "cache-loaded");
            if(active == entry) active = null;
        } finally { long elapsed = System.nanoTime() - started; uploadNanos += elapsed; uploadWorst = Math.max(uploadWorst, elapsed); }
    }

    private static void complete(Runnable completion) {
        if(!COMPLETIONS.offer(completion)) writeFailures.incrementAndGet();
    }

    private static void submitQueuedLookup() {
        if(WORKER.getQueue().remainingCapacity() <= 0) {
            if(!RESOLVE_BACKLOG.isEmpty()) ++resolverDeferred;
            else if(!MODEL_BACKLOG.isEmpty()) ++modelDeferred;
            return;
        }
        if(!RESOLVE_BACKLOG.isEmpty()) {
            Entry entry = RESOLVE_BACKLOG.values().iterator().next();
            if(submitResolve(entry)) RESOLVE_BACKLOG.remove(entry.identity);
        } else if(!MODEL_BACKLOG.isEmpty()) {
            Entry entry = MODEL_BACKLOG.values().iterator().next();
            if(submitModelLoad(entry)) MODEL_BACKLOG.remove(entry.identity);
        }
    }

    private static void enqueueModel(Entry entry, String reason) {
        entry.state = State.WAIT_MODEL;
        MODEL_BACKLOG.put(entry.identity, entry);
        ++modelWaits;
        cacheEvent(entry, "state=WAIT_MODEL reason=" + reason);
    }

    private static boolean submitModelLoad(final Entry entry) {
        final MCH_BaseVehicleInfo info = (MCH_BaseVehicleInfo)entry.identity;
        try {
            WORKER.execute(new Runnable() {
                public void run() {
                    long started = System.nanoTime();
                    try {
                        final net.minecraftforge.client.model.IModelCustom loaded =
                                mcheli.MCH_ModelManager.load(info.getDirectoryName(), info.name);
                        final long elapsed = System.nanoTime() - started;
                        complete(new Runnable() {
                            public void run() {
                                if(!current(entry) || entry.state != State.LOADING_MODEL) return;
                                modelLoadNanos += elapsed; modelLoadWorst = Math.max(modelLoadWorst, elapsed);
                                if(info.model == null) info.model = loaded;
                                if(info.model == null) {
                                    ++modelFailures;
                                    fail(entry, new IOException("Missing vehicle body model"));
                                    return;
                                }
                                transition(entry, State.PREPARE_CAPTURE, "body-model-ready");
                                enqueueCapture(entry);
                            }
                        });
                    } catch(final Exception failure) {
                        complete(new Runnable() {
                            public void run() {
                                if(current(entry) && entry.state == State.LOADING_MODEL) {
                                    ++modelFailures; fail(entry, failure);
                                }
                            }
                        });
                    }
                }
            });
            transition(entry, State.LOADING_MODEL, "body-model-queued");
            ++modelSubmissions;
            workerHighWater = Math.max(workerHighWater, WORKER.getQueue().size());
            return true;
        } catch(java.util.concurrent.RejectedExecutionException full) {
            entry.state = State.WAIT_MODEL;
            ++modelDeferred;
            return false;
        }
    }

    private static void enqueueCapture(Entry entry) {
        if(entry.cancelled || CAPTURE_BACKLOG.containsKey(entry.identity)) return;
        if(CAPTURE_BACKLOG.size() >= MAX_CAPTURE_BACKLOG) {
            entry.state = State.DEFERRED;
            ++deferred;
            cacheEvent(entry, "capture-backlog-full");
            return;
        }
        CAPTURE_BACKLOG.put(entry.identity, entry);
        captureBacklogHighWater = Math.max(captureBacklogHighWater, CAPTURE_BACKLOG.size());
    }

    private static void deferActive(Entry entry, String reason) {
        deferCapture(entry, reason);
        if(active == entry) active = null;
    }

    private static void deferCapture(Entry entry, String reason) {
        CAPTURE_BACKLOG.put(entry.identity, entry);
        captureBacklogHighWater = Math.max(captureBacklogHighWater, CAPTURE_BACKLOG.size());
        cacheEvent(entry, reason);
    }

    private static boolean current(Entry entry) {
        return !entry.cancelled && entry.epoch == epoch && ENTRIES.get(entry.identity) == entry;
    }

    private static void fail(Entry entry, Exception failure) {
        if(entry == null) return;
        String stage = entry.state != null ? entry.state.name() : "UNKNOWN";
        if(entry.state == State.WAIT_PBO) deletePixelPackFence();
        clearCaptureResources(entry);
        entry.pixels = null;
        entry.image = null;
        transition(entry, State.FAILED, failure.getClass().getSimpleName() + ":" + failure.getMessage());
        ++failures;
        if(active == entry) active = null;
        System.err.println("[mcheli icons] Failed at " + stage + " for "
                + entry.stack.getUnlocalizedName() + ": " + failure);
    }

    private static BufferedImage valid(BufferedImage image) {
        return image != null && image.getWidth() == SIZE && image.getHeight() == SIZE ? image : null;
    }

    private static void save(final Entry entry, final BufferedImage image, final File destination) {
        try {
            WRITER.execute(new Runnable() {
                public void run() {
                    long started = System.nanoTime();
                    File temporary = null;
                    try {
                        File parent = destination.getParentFile();
                        if(!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create " + parent);
                        temporary = new File(destination.getPath() + ".tmp");
                        if(!ImageIO.write(image, "png", temporary)) throw new IOException("PNG encoder unavailable");
                        if(!temporary.isFile() || temporary.length() <= 0L)
                            throw new IOException("PNG encoder produced no data");
                        try {
                            Files.move(temporary.toPath(), destination.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                        } catch(IOException atomicFailure) {
                            try {
                                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            } catch(IOException fallbackFailure) {
                                fallbackFailure.addSuppressed(atomicFailure);
                                throw fallbackFailure;
                            }
                        }
                        if(!destination.isFile() || destination.length() <= 0L)
                            throw new IOException("Final PNG is missing or empty");
                        writes.incrementAndGet();
                        cacheEvent(entry, "write=OK path=" + destination.getAbsolutePath());
                    } catch(Exception failure) {
                        writeFailures.incrementAndGet();
                        cacheEvent(entry, "write=FAILED path=" + destination.getAbsolutePath()
                                + " reason=" + failure.getClass().getSimpleName() + ":" + failure.getMessage());
                    } finally {
                        writeNanos.addAndGet(System.nanoTime() - started);
                        if(temporary != null) temporary.delete();
                    }
                }
            });
            writerHighWater = Math.max(writerHighWater, WRITER.getQueue().size());
        } catch(java.util.concurrent.RejectedExecutionException full) {
            writeFailures.incrementAndGet();
            cacheEvent(entry, "write=FAILED path=" + destination.getAbsolutePath() + " reason=writer-queue-full");
        }
    }

    public static void resetForReload() {
        ++epoch;
        if(active != null) active.cancelled = true;
        active = null;
        for(Entry entry : ENTRIES.values()) { entry.cancelled = true; release(entry); }
        ENTRIES.clear();
        RESOLVE_BACKLOG.clear();
        MODEL_BACKLOG.clear();
        CAPTURE_BACKLOG.clear();
        READY_UPLOADS.clear();
        WORKER.getQueue().clear();
        COMPLETIONS.clear();
        // Completed images are immutable and fingerprinted, so queued disk writes remain valid.
        if(framebuffer != null) {
            int previous = GL11.glGetInteger(0x8CA6);
            framebuffer.deleteFramebuffer();
            OpenGlHelper.func_153171_g(0x8D40, previous);
            framebuffer = null;
        }
        deletePixelPackBuffers();
        nextBakeWork = 0L;
        nextCaptureWork = 0L;
        bakeStarted = false;
        capturing = false;
    }

    private static void discard(Entry entry) {
        entry.cancelled = true;
        release(entry);
        ENTRIES.remove(entry.identity);
        RESOLVE_BACKLOG.remove(entry.identity);
        MODEL_BACKLOG.remove(entry.identity);
        CAPTURE_BACKLOG.remove(entry.identity);
        READY_UPLOADS.remove(entry.identity);
        if(active == entry) active = null;
    }

    private static void release(Entry entry) {
        if(entry != null && entry.texture != null)
            Minecraft.getMinecraft().getTextureManager().deleteTexture(entry.texture);
        clearCaptureResources(entry);
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

    private static boolean renderCapture(Entry entry) throws IOException {
        if(!OpenGlHelper.isFramebufferEnabled()) throw new IOException("Framebuffer capture unavailable/disabled");
        if(framebuffer == null) {
            transition(entry, State.PREPARE_FRAMEBUFFER, "capture-target-lost");
            return false;
        }
        net.minecraft.client.renderer.texture.TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
        if(textureManager.getTexture(entry.captureTexture) == null
                || (entry.captureOverlay != null && textureManager.getTexture(entry.captureOverlay) == null)) {
            transition(entry, State.PREPARE_TEXTURE, "capture-texture-not-resident");
            return false;
        }
        if(!modelBuffersReady(entry.captureModel)) {
            entry.modelGroups = groups(entry.captureModel);
            entry.activeGroup = null;
            transition(entry, State.PREPARE_MODEL_BUFFERS, "buffer-readiness-changed");
            return false;
        }
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
            long stageStarted = System.nanoTime();
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            framebuffer.bindFramebuffer(true);
            framebuffer.checkFramebufferComplete();
            GL11.glViewport(0, 0, SIZE, SIZE);
            long elapsed = System.nanoTime() - stageStarted;
            framebufferBindNanos += elapsed; framebufferBindWorst = Math.max(framebufferBindWorst, elapsed);
            stageStarted = System.nanoTime();
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            GL11.glClearColor(0, 0, 0, 0);
            GL11.glClearDepth(1);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            long framebufferElapsed = System.nanoTime() - stageStarted;
            framebufferNanos += framebufferElapsed;
            framebufferWorst = Math.max(framebufferWorst, framebufferElapsed);
            clearNanos += framebufferElapsed; clearWorst = Math.max(clearWorst, framebufferElapsed);
            stageStarted = System.nanoTime();
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
            elapsed = System.nanoTime() - stageStarted;
            stateSetupNanos += elapsed; stateSetupWorst = Math.max(stateSetupWorst, elapsed);
            capturing = true;
            long drawStarted = System.nanoTime();
            renderCanonical(entry);
            long drawElapsed = System.nanoTime() - drawStarted;
            renderNanos += drawElapsed; renderWorst = Math.max(renderWorst, drawElapsed);
            return true;
        } finally {
            long restoreStarted = System.nanoTime();
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
            long restoreElapsed = System.nanoTime() - restoreStarted;
            captureRestoreNanos += restoreElapsed;
            captureRestoreWorst = Math.max(captureRestoreWorst, restoreElapsed);
        }
    }

    private static byte[] readbackSynchronously() throws IOException {
        if(framebuffer == null) throw new IOException("Missing icon framebuffer");
        int previousFramebuffer = GL11.glGetInteger(0x8CA6);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(-1);
        try {
            framebuffer.bindFramebuffer(true);
            PIXELS.clear();
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            long readStarted = System.nanoTime();
            GL11.glReadPixels(0, 0, SIZE, SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, PIXELS);
            long readElapsed = System.nanoTime() - readStarted;
            readPixelsNanos += readElapsed; readPixelsWorst = Math.max(readPixelsWorst, readElapsed);
            long copyStarted = System.nanoTime();
            byte[] result = new byte[SIZE * SIZE * 4];
            PIXELS.position(0);
            PIXELS.get(result);
            long copyElapsed = System.nanoTime() - copyStarted;
            pixelCopyNanos += copyElapsed; pixelCopyWorst = Math.max(pixelCopyWorst, copyElapsed);
            return result;
        } finally {
            GL11.glPopClientAttrib();
            GL11.glPopAttrib();
            OpenGlHelper.func_153171_g(0x8D40, previousFramebuffer);
        }
    }

    private static boolean supportsAsyncReadback() {
        org.lwjgl.opengl.ContextCapabilities capabilities = GLContext.getCapabilities();
        boolean pbo = capabilities.OpenGL21 || capabilities.GL_ARB_pixel_buffer_object;
        boolean sync = capabilities.OpenGL32 || capabilities.GL_ARB_sync;
        return !pixelPackDisabled && capabilities.OpenGL15 && pbo && sync;
    }

    private static void ensurePixelPackBuffers() {
        if(!supportsAsyncReadback() || PIXEL_PACK_BUFFERS[0] != 0) return;
        int previousBuffer = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        try {
            for(int i = 0; i < PIXEL_PACK_BUFFERS.length; ++i) {
                PIXEL_PACK_BUFFERS[i] = GL15.glGenBuffers();
                GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, PIXEL_PACK_BUFFERS[i]);
                GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER, PIXEL_BYTES, GL15.GL_STREAM_READ);
            }
        } finally {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, previousBuffer);
        }
    }

    private static void issuePboReadback(Entry entry) throws IOException {
        if(framebuffer == null || PIXEL_PACK_BUFFERS[0] == 0)
            throw new IOException("Missing asynchronous icon readback buffers");
        int previousFramebuffer = GL11.glGetInteger(0x8CA6);
        int previousBuffer = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int index = nextPixelPackBuffer++ % PIXEL_PACK_BUFFERS.length;
        long started = System.nanoTime();
        GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
        try {
            framebuffer.bindFramebuffer(true);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, PIXEL_PACK_BUFFERS[index]);
            GL11.glReadPixels(0, 0, SIZE, SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);
            pixelPackFence = createPixelPackFence();
            if(pixelPackFence == null) throw new IOException("Unable to create icon readback fence");
        } finally {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, previousBuffer);
            GL11.glPopClientAttrib();
            GL11.glPopAttrib();
            OpenGlHelper.func_153171_g(0x8D40, previousFramebuffer);
        }
        long elapsed = System.nanoTime() - started;
        pboIssueNanos += elapsed; pboIssueWorst = Math.max(pboIssueWorst, elapsed);
        ++pboIssues;
        entry.pixelPackBuffer = index;
        entry.pixelPackIssuedNanos = System.nanoTime();
        transition(entry, State.WAIT_PBO, "pbo-readback-issued");
    }

    private static GLSync createPixelPackFence() {
        return GLContext.getCapabilities().OpenGL32
                ? GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0)
                : ARBSync.glFenceSync(ARBSync.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }

    private static int clientWait(GLSync fence) {
        return GLContext.getCapabilities().OpenGL32
                ? GL32.glClientWaitSync(fence, 0, 0L)
                : ARBSync.glClientWaitSync(fence, 0, 0L);
    }

    private static void deletePixelPackFence() {
        if(pixelPackFence == null) return;
        if(GLContext.getCapabilities().OpenGL32) GL32.glDeleteSync(pixelPackFence);
        else ARBSync.glDeleteSync(pixelPackFence);
        pixelPackFence = null;
    }

    private static void deletePixelPackBuffers() {
        deletePixelPackFence();
        for(int i = 0; i < PIXEL_PACK_BUFFERS.length; ++i) {
            if(PIXEL_PACK_BUFFERS[i] != 0) GL15.glDeleteBuffers(PIXEL_PACK_BUFFERS[i]);
            PIXEL_PACK_BUFFERS[i] = 0;
        }
        nextPixelPackBuffer = 0;
        pixelPackDisabled = false;
    }

    private static void fallbackFromPbo(Entry entry, String reason) {
        deletePixelPackFence();
        pixelPackDisabled = true;
        ++pboFallbacks;
        transition(entry, State.READBACK, reason);
    }

    private static void initializePreviewComposition(Entry entry) {
        entry.compositionX = 0.0F;
        entry.compositionY = 0.0F;
        entry.modelCenterX = entry.modelCenterY = entry.modelCenterZ = 0.0F;
        float largest = Math.max(Math.max(((MCH_BaseVehicleInfo)entry.identity).bodyWidth,
                ((MCH_BaseVehicleInfo)entry.identity).bodyHeight), 1.0F);
        entry.compositionScale = 0.85F / largest;
        if(entry.captureModel instanceof mcheli.wrapper.modelloader.W_ModelCustom) {
            mcheli.wrapper.modelloader.W_ModelCustom model =
                    (mcheli.wrapper.modelloader.W_ModelCustom)entry.captureModel;
            float sizeX = model.maxX - model.minX;
            float sizeY = model.maxY - model.minY;
            float sizeZ = model.maxZ - model.minZ;
            largest = Math.max(sizeX, Math.max(sizeY, sizeZ));
            if(largest > 0.0001F) entry.compositionScale = 1.0F / largest;
            entry.modelCenterX = (model.minX + model.maxX) * 0.5F;
            entry.modelCenterY = (model.minY + model.maxY) * 0.5F;
            entry.modelCenterZ = (model.minZ + model.maxZ) * 0.5F;
        }
    }

    private static void applyPreviewComposition(Entry entry, SilhouetteBounds bounds) {
        MCH_BaseVehicleInfo info = (MCH_BaseVehicleInfo)entry.identity;
        float configuredScale = getCompositionScaleMultiplier(info, entry.preset) * info.itemIconScaleFactor;
        float desired = Math.min(SIZE * entry.preset.occupancy * configuredScale,
                SIZE - entry.preset.margin * 2.0F);
        desired = Math.max(16.0F, desired);
        float correction = desired / Math.max(bounds.width(), bounds.height());
        entry.compositionScale *= correction;
        entry.compositionX = -correction * bounds.centerNdcX();
        entry.compositionY = -correction * bounds.centerNdcY();
    }

    private static boolean needsSafetyCorrection(Entry entry, SilhouetteBounds bounds) {
        int margin = entry.preset.margin;
        return bounds.minX < margin || bounds.minY < margin
                || bounds.maxX >= SIZE - margin || bounds.maxY >= SIZE - margin;
    }

    private static void applySafetyCorrection(Entry entry, SilhouetteBounds bounds) {
        float available = SIZE - (entry.preset.margin + 1.0F) * 2.0F;
        float correction = Math.min(1.0F, Math.min(available / bounds.width(), available / bounds.height()));
        entry.compositionX = -correction * (bounds.centerNdcX() - entry.compositionX);
        entry.compositionY = -correction * (bounds.centerNdcY() - entry.compositionY);
        entry.compositionScale *= correction;
    }

    private static float getCompositionScaleMultiplier(MCH_BaseVehicleInfo info, CompositionPreset preset) {
        float legacyDefault = preset == CompositionPreset.PLANE || preset == CompositionPreset.SHIP ? 0.1F
                : preset == CompositionPreset.FALLBACK ? 1.0F : 0.3F;
        return getTypeScale(info) / legacyDefault;
    }

    private static void clearCaptureResources(Entry entry) {
        if(entry == null) return;
        entry.captureModel = null;
        entry.captureTexture = null;
        entry.captureOverlay = null;
        entry.modelGroups = null;
        entry.activeGroup = null;
    }

    private static PixelResult processPixels(byte[] pixels, boolean createImage) throws IOException {
        int[] argb = createImage ? new int[SIZE * SIZE] : null;
        SilhouetteBounds bounds = new SilhouetteBounds();
        for(int y = 0; y < SIZE; ++y) for(int x = 0; x < SIZE; ++x) {
            int p = (x + y * SIZE) * 4;
            int imageY = SIZE - y - 1;
            int alpha = pixels[p + 3] & 255;
            if(createImage) argb[x + imageY * SIZE] = alpha << 24
                    | (pixels[p] & 255) << 16 | (pixels[p + 1] & 255) << 8 | pixels[p + 2] & 255;
            if(alpha != 0) bounds.include(x, imageY);
        }
        if(bounds.empty()) throw new IOException("Empty model capture");
        BufferedImage image = null;
        if(createImage) {
            image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, SIZE, SIZE, argb, 0, SIZE);
        }
        return new PixelResult(image, bounds);
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

    private static File directory() { return new File(Minecraft.getMinecraft().mcDataDir, "cache/mcheli/icons"); }
    private static File exportFile(String key) {
        return new File(directory(), "export/assets/mcheli/textures/icons/" + key + ".png");
    }

    private static void report(long now) {
        if(!diagnostics() || now - lastReport < 10000L) return;
        lastReport = now;
        System.out.println(String.format(java.util.Locale.ROOT,
                "[MCH IconCache] stage=%s prebaked=%d diskHits=%d diskMisses=%d corrupt=%d requests=%d deduplicated=%d deferred=%d resolverSubmissions=%d resolverDeferred=%d resolverFailures=%d processorDeferred=%d vboGroups=%d captures=%d failures=%d loads=%d writes=%d writeFailures=%d resolveBacklog=%d/%d captureBacklog=%d/%d workerQueue=%d/%d writerQueue=%d/%d avg/worstMs resolve=%.2f/%.2f modelLookup=%.3f/%.3f textureLookup=%.3f/%.3f vboFrame=%.3f/%.3f clear=%.3f/%.3f draw=%.3f/%.3f glReadPixels=%.3f/%.3f pixelCopy=%.3f/%.3f process=%.3f/%.3f upload=%.3f/%.3f ready=%.1f/%.1f write=%.2f",
                active != null ? active.state : "IDLE", prebaked, hits, misses, corruptFiles.get(), requests, duplicates,
                deferred, resolverSubmissions, resolverDeferred, resolverFailures, processorDeferred,
                vboGroupsPrepared, captures, failures,
                loads, writes.get(), writeFailures.get(), RESOLVE_BACKLOG.size(), resolveBacklogHighWater,
                CAPTURE_BACKLOG.size(), captureBacklogHighWater,
                WORKER.getQueue().size(), workerHighWater, WRITER.getQueue().size(), writerHighWater,
                resolveNanos / 1e6 / Math.max(1, hits + misses + prebaked), resolveWorst / 1e6,
                modelLookupNanos / 1e6 / Math.max(1, captures + failures), modelLookupWorst / 1e6,
                textureNanos / 1e6 / Math.max(1, captures + failures), textureWorst / 1e6,
                vboNanos / 1e6 / Math.max(1, vboGroupsPrepared), vboWorst / 1e6,
                framebufferNanos / 1e6 / Math.max(1, captures), framebufferWorst / 1e6,
                renderNanos / 1e6 / Math.max(1, captures), renderWorst / 1e6,
                readPixelsNanos / 1e6 / Math.max(1, captures), readPixelsWorst / 1e6,
                pixelCopyNanos / 1e6 / Math.max(1, captures), pixelCopyWorst / 1e6,
                processNanos / 1e6 / Math.max(1, captures), processWorst / 1e6,
                uploadNanos / 1e6 / Math.max(1, loads + captures), uploadWorst / 1e6,
                readyNanos / 1e6 / Math.max(1, loads + captures), readyWorst / 1e6,
                writeNanos.get() / 1e6 / Math.max(1, writes.get() + writeFailures.get())));
        System.out.println(String.format(java.util.Locale.ROOT,
                "[MCH IconCache prerequisites] inventoryRequests=%d handleRenderType=%d renderItem=%d waitResolve=%d waitModel=%d modelWaitEvents=%d modelSubmissions=%d modelDeferred=%d modelFailures=%d pboSupported=%s pboIssues=%d pboCompletions=%d pboFallbacks=%d avg/worstMs modelLoad=%.2f/%.2f textureWarm=%.2f/%.2f framebufferCreate=%.2f/%.2f pboIssue=%.3f/%.3f pboLatency=%.2f/%.2f",
                inventoryRequests, handleRequests, renderItemRequests, RESOLVE_BACKLOG.size(), MODEL_BACKLOG.size(),
                modelWaits, modelSubmissions, modelDeferred, modelFailures, Boolean.valueOf(supportsAsyncReadback()),
                pboIssues, pboCompletions, pboFallbacks,
                modelLoadNanos / 1e6 / Math.max(1, modelSubmissions), modelLoadWorst / 1e6,
                textureWarmNanos / 1e6 / Math.max(1, captures + failures), textureWarmWorst / 1e6,
                framebufferCreateNanos / 1e6 / Math.max(1, captures + failures), framebufferCreateWorst / 1e6,
                pboIssueNanos / 1e6 / Math.max(1, pboIssues), pboIssueWorst / 1e6,
                pboLatencyNanos / 1e6 / Math.max(1, pboCompletions), pboLatencyWorst / 1e6));
        System.out.println(String.format(java.util.Locale.ROOT,
                "[MCH IconCache capture] avg/worstMs framebufferBind=%.3f/%.3f clear=%.3f/%.3f stateSetup=%.3f/%.3f textureBind=%.3f/%.3f modelDraw=%.3f/%.3f overlayDraw=%.3f/%.3f restore=%.3f/%.3f textureAllocate=%.3f/%.3f textureCopy=%.3f/%.3f textureGpuUpload=%.3f/%.3f",
                framebufferBindNanos / 1e6 / Math.max(1, captures), framebufferBindWorst / 1e6,
                clearNanos / 1e6 / Math.max(1, captures), clearWorst / 1e6,
                stateSetupNanos / 1e6 / Math.max(1, captures), stateSetupWorst / 1e6,
                textureBindNanos / 1e6 / Math.max(1, captures), textureBindWorst / 1e6,
                modelDrawNanos / 1e6 / Math.max(1, captures), modelDrawWorst / 1e6,
                overlayDrawNanos / 1e6 / Math.max(1, captures), overlayDrawWorst / 1e6,
                captureRestoreNanos / 1e6 / Math.max(1, captures), captureRestoreWorst / 1e6,
                textureAllocationNanos / 1e6 / Math.max(1, loads + captures), textureAllocationWorst / 1e6,
                textureDataCopyNanos / 1e6 / Math.max(1, loads + captures), textureDataCopyWorst / 1e6,
                textureGpuUploadNanos / 1e6 / Math.max(1, loads + captures), textureGpuUploadWorst / 1e6));
    }


    private static long appearance(Object identity) {
        MCH_BaseVehicleInfo info = (MCH_BaseVehicleInfo)identity;
        long value = Float.floatToIntBits(info.bodyWidth);
        value = value * 31 + Float.floatToIntBits(info.bodyHeight);
        value = value * 31 + Float.floatToIntBits(info.itemIconScaleFactor);
        return value * 31 + Float.floatToIntBits(getTypeScale(info));
    }

    private enum State {
        DEFERRED, WAIT_RESOLVE, RESOLVING, WAIT_MODEL, LOADING_MODEL, PREPARE_CAPTURE, PREPARE_TEXTURE,
        PREPARE_MODEL_BUFFERS, PREPARE_FRAMEBUFFER, RENDER_MODEL, READBACK, WAIT_PBO, WAIT_PROCESS,
        PROCESSING, UPLOAD_TEXTURE, READY, FAILED
    }
    private enum CapturePass { PREVIEW, FINAL }
    private enum CompositionPreset {
        PLANE(22.0F, 42.0F, 0.86F, 9),
        HELICOPTER(28.0F, 45.0F, 0.83F, 10),
        GROUND(30.0F, 45.0F, 0.80F, 11),
        SHIP(24.0F, 40.0F, 0.85F, 10),
        FALLBACK(30.0F, 45.0F, 0.82F, 10);

        final float pitch, yaw, occupancy;
        final int margin;
        CompositionPreset(float pitch, float yaw, float occupancy, int margin) {
            this.pitch = pitch; this.yaw = yaw; this.occupancy = occupancy; this.margin = margin;
        }
        static CompositionPreset forInfo(MCH_BaseVehicleInfo info) {
            if(info instanceof mcheli.plane.MCP_PlaneInfo) return PLANE;
            if(info instanceof mcheli.helicopter.MCH_HeliInfo) return HELICOPTER;
            if(info instanceof mcheli.ship.MCH_ShipInfo) return SHIP;
            if(info instanceof mcheli.tank.MCH_TankInfo || info instanceof mcheli.vehicle.MCH_TurretInfo) return GROUND;
            return FALLBACK;
        }
    }
    private static final class SilhouetteBounds {
        int minX = SIZE, minY = SIZE, maxX = -1, maxY = -1;
        void include(int x, int y) {
            minX = Math.min(minX, x); minY = Math.min(minY, y);
            maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
        }
        boolean empty() { return maxX < minX; }
        int width() { return maxX - minX + 1; }
        int height() { return maxY - minY + 1; }
        float centerNdcX() { return (minX + maxX + 1.0F) / SIZE - 1.0F; }
        float centerNdcY() { return 1.0F - (minY + maxY + 1.0F) / SIZE; }
    }
    private static final class PixelResult {
        final BufferedImage image;
        final SilhouetteBounds bounds;
        PixelResult(BufferedImage image, SilhouetteBounds bounds) { this.image = image; this.bounds = bounds; }
    }
    private enum LoadSource { NONE, PREBAKED, DISK }
    private enum RequestOrigin { HANDLE_RENDER_TYPE, RENDER_ITEM }
    private static final class ResolveResult {
        final BufferedImage image;
        final LoadSource source;
        final int corrupt;
        final String reason;
        ResolveResult(BufferedImage image, LoadSource source, int corrupt, String reason) {
            this.image = image; this.source = source; this.corrupt = corrupt; this.reason = reason;
        }
    }
    private static final class Entry {
        final Object identity;
        final ItemStack stack;
        final long appearance;
        final IItemRenderer renderer;
        final long epoch;
        final String contentId;
        String key;
        File cacheFile;
        ResourceLocation texture;
        ResourceLocation captureTexture;
        ResourceLocation captureOverlay;
        net.minecraftforge.client.model.IModelCustom captureModel;
        java.util.Iterator modelGroups;
        mcheli.wrapper.modelloader.W_GroupObject activeGroup;
        byte[] pixels;
        int pixelPackBuffer;
        long pixelPackIssuedNanos;
        BufferedImage image;
        CompositionPreset preset = CompositionPreset.FALLBACK;
        CapturePass capturePass = CapturePass.PREVIEW;
        float compositionScale, compositionX, compositionY;
        float modelCenterX, modelCenterY, modelCenterZ;
        int finalAttempts;
        State state = State.WAIT_RESOLVE;
        boolean persist;
        boolean cancelled;
        final long requestedNanos = System.nanoTime();
        Entry(Object identity, ItemStack stack, IItemRenderer renderer, long epoch) {
            this.identity = identity; this.stack = stack; this.renderer = renderer; this.epoch = epoch;
            this.appearance = appearance(identity);
            MCH_BaseVehicleInfo info = (MCH_BaseVehicleInfo)identity;
            this.contentId = info.getDirectoryName() + "/" + info.name;
        }
    }

    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        MCH_BaseVehicleInfo info = getInfo(item);
        if(info == null || !is3DIconEnabled(info)) return false;
        if(type == ItemRenderType.INVENTORY)
            return request(info, item, this, RequestOrigin.HANDLE_RENDER_TYPE).state == State.READY;
        mcheli.MCH_ClientProxy.ensureVehicleModel(info);
        return info.model != null;
    }
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) { return type != ItemRenderType.INVENTORY; }
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        MCH_BaseVehicleInfo info = getInfo(item);
        if(info == null || !is3DIconEnabled(info)) return;
        if(type == ItemRenderType.INVENTORY) {
            draw(request(info, item, this, RequestOrigin.RENDER_ITEM));
            return;
        }
        mcheli.MCH_ClientProxy.ensureVehicleModel(info);
        if(info.model != null) renderLiveModel(type, info);
    }
    public void onResourceManagerReload(IResourceManager manager) { resetForReload(); }
    public static void onVehicleModelAvailable(MCH_BaseVehicleInfo info) {
        Entry entry = ENTRIES.get(info);
        if(entry != null && (entry.state == State.WAIT_MODEL || entry.state == State.LOADING_MODEL)) {
            MODEL_BACKLOG.remove(info);
            transition(entry, State.PREPARE_CAPTURE, "normal-model-lifecycle-ready");
            enqueueCapture(entry);
        }
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
            if(info != null && is3DIconEnabled(info)) request(info, stack, null, RequestOrigin.HANDLE_RENDER_TYPE);
        }
    }
    private static boolean diagnostics() {
        return MCH_Config.DebugVehicleIconCache != null && MCH_Config.DebugVehicleIconCache.prmBool;
    }
    private static void transition(Entry entry, State state, String reason) {
        entry.state = state;
        cacheEvent(entry, "state=" + state + (reason != null ? " reason=" + reason : ""));
    }
    private static void cacheEvent(Entry entry, String message) {
        if(!diagnostics()) return;
        String key = entry != null && entry.key != null ? entry.key : "pending";
        String id = entry != null ? entry.contentId : "unknown";
        System.out.println("[MCH IconCache] " + id + " key=" + key + " " + message);
    }
    private static void projection() { GL11.glOrtho(-1, 1, -1, 1, -1000, 1000); }
    private static java.util.Iterator groups(net.minecraftforge.client.model.IModelCustom model) {
        if(model instanceof mcheli.wrapper.modelloader.W_WavefrontObject)
            return ((mcheli.wrapper.modelloader.W_WavefrontObject)model).groupObjects.iterator();
        if(model instanceof mcheli.wrapper.modelloader.W_MetasequoiaObject)
            return ((mcheli.wrapper.modelloader.W_MetasequoiaObject)model).groupObjects.iterator();
        return null;
    }
    private static boolean modelBuffersReady(net.minecraftforge.client.model.IModelCustom model) {
        java.util.Iterator iterator = groups(model);
        if(iterator == null) return true;
        while(iterator.hasNext()) {
            if(!((mcheli.wrapper.modelloader.W_GroupObject)iterator.next()).isVboReady()) return false;
        }
        return true;
    }
    private static boolean modelSupportsPreparedBuffers(net.minecraftforge.client.model.IModelCustom model) {
        java.util.Iterator iterator = groups(model);
        if(iterator == null) return false;
        while(iterator.hasNext()) {
            if(!((mcheli.wrapper.modelloader.W_GroupObject)iterator.next()).canUseVbo()) return false;
        }
        return true;
    }
    private static void renderCanonical(Entry entry) {
        renderModel(ItemRenderType.INVENTORY, (MCH_BaseVehicleInfo)entry.identity,
                entry.captureModel, entry.captureTexture, entry);
    }
    private static void renderLiveModel(ItemRenderType type, MCH_BaseVehicleInfo info) {
        renderModel(type, info, info.model, resolveTexture(info, info.model), null);
    }
    private static ResourceLocation resolveTexture(MCH_BaseVehicleInfo info, net.minecraftforge.client.model.IModelCustom model) {
        ResourceLocation original = new ResourceLocation("mcheli", "textures/" + info.getDirectoryName()
                + "/" + MCH_RenderBaseVehicle.getBaseTextureName(info.name) + ".png");
        return mcheli.texture.MCH_ModelTextureRepairManager.resolve(
                original, model, info.getDirectoryName() + "/" + info.name);
    }
    private static ResourceLocation resolvedTextureIfPresent(MCH_BaseVehicleInfo info, net.minecraftforge.client.model.IModelCustom model) {
        ResourceLocation original = new ResourceLocation("mcheli", "textures/" + info.getDirectoryName()
                + "/" + MCH_RenderBaseVehicle.getBaseTextureName(info.name) + ".png");
        return mcheli.texture.MCH_ModelTextureRepairManager.resolveForBackgroundCapture(original, model);
    }
    private static ResourceLocation resolveOverlayTexture(MCH_BaseVehicleInfo info) {
        int separator = info.name.indexOf("|skinoverlays/");
        return separator >= 0 ? new ResourceLocation("mcheli", MCH_EntityBaseVehicle.getTexturePath(
                info.getDirectoryName(), info.name.substring(separator + 1))) : null;
    }
    private static void renderModel(ItemRenderType type, MCH_BaseVehicleInfo info,
            net.minecraftforge.client.model.IModelCustom model, ResourceLocation texture, Entry entry) {
        GL11.glPushMatrix();
        GL11.glEnable(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1, 1, 1, 1);
        try {
            transform(type, info, entry);
            long stageStarted = System.nanoTime();
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            long elapsed = System.nanoTime() - stageStarted;
            if(capturing) { textureBindNanos += elapsed; textureBindWorst = Math.max(textureBindWorst, elapsed); }
            MCH_RenderBaseVehicle.beginSkinOverlayRender(info.getDirectoryName(), info.name);
            try {
                stageStarted = System.nanoTime();
                model.renderAll();
                elapsed = System.nanoTime() - stageStarted;
                if(capturing) { modelDrawNanos += elapsed; modelDrawWorst = Math.max(modelDrawWorst, elapsed); }
                stageStarted = System.nanoTime();
                MCH_RenderBaseVehicle.renderPreparedSkinOverlay(model);
                elapsed = System.nanoTime() - stageStarted;
                if(capturing) { overlayDrawNanos += elapsed; overlayDrawWorst = Math.max(overlayDrawWorst, elapsed); }
            } finally { MCH_RenderBaseVehicle.endSkinOverlayRender(); }
        } finally { GL11.glPopMatrix(); GL11.glColor4f(1, 1, 1, 1); GL11.glEnable(GL11.GL_BLEND); }
    }
    private static String cacheKey(Entry entry) throws Exception {
        MCH_BaseVehicleInfo info = (MCH_BaseVehicleInfo)entry.identity;
        String name = info.getDirectoryName() + "/" + info.name;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        bytes(digest, "mcheli-canonical-renderer-3|" + ICON_CACHE_VERSION + "|" + name
                + "|two-pass-silhouette-composition|" + CompositionPreset.forInfo(info).name()
                + "|" + info.bodyWidth + "|" + info.bodyHeight
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
        // Keep the final component short enough for legacy Windows instance paths. The full
        // stable content identity and all source fingerprints are already inside the digest.
        return "v" + ICON_CACHE_VERSION + "-" + hex(digest.digest());
    }
    private static void sourceHash(MessageDigest digest, String path) throws IOException {
        bytes(digest, path);
        try(java.io.InputStream in = mcheli.MCH_ResourceHelper.openResourceStream(path)) {
            if(in == null) throw new IOException("Missing source " + path);
            streamHash(digest, in);
        }
    }
   private static void transform(ItemRenderType type, MCH_BaseVehicleInfo info, Entry entry) {
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
            if(entry != null) {
               GL11.glTranslatef(entry.compositionX, entry.compositionY, 0.0F);
               GL11.glRotatef(entry.preset.pitch, 1.0F, 0.0F, 0.0F);
               GL11.glRotatef(entry.preset.yaw, 0.0F, 1.0F, 0.0F);
               GL11.glScalef(entry.compositionScale, entry.compositionScale, entry.compositionScale);
               GL11.glTranslatef(-entry.modelCenterX, -entry.modelCenterY, -entry.modelCenterZ);
               return;
            }
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
