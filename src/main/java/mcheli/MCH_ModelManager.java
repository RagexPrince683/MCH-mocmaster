package mcheli;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import mcheli.wrapper.W_ModelBase;
import mcheli.wrapper.W_ResourcePath;
import mcheli.wrapper.modelloader.W_ModelCustom;
import mcheli.wrapper.modelloader.W_GroupObject;
import mcheli.wrapper.modelloader.W_MetasequoiaObject;
import mcheli.wrapper.modelloader.W_WavefrontObject;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.client.model.IModelCustom;

@SideOnly(Side.CLIENT)
public class MCH_ModelManager extends W_ModelBase {

   private static final ConcurrentHashMap<String, IModelCustom> MAP = new ConcurrentHashMap<>();
   private static final Random RAND = new Random();

   private static final ModelRenderer DEFAULT_MODEL;

   private static final AtomicLong REQUESTS = new AtomicLong();
   private static final AtomicLong PARSED = new AtomicLong();
   private static final AtomicLong CACHE_HITS = new AtomicLong();
   private static final AtomicLong FAILURES = new AtomicLong();

   static {
      DEFAULT_MODEL = new ModelRenderer(new MCH_ModelManager(), 0, 0);
      DEFAULT_MODEL.addBox(-5.0F, -5.0F, -5.0F, 10, 10, 10, 0.0F);
   }

   private MCH_ModelManager() {}

   public static void setForceReloadMode(boolean b) {
      // Compatibility shim. Reload intent is now carried by each load request.
   }

   /** Drops all model objects before a complete client-thread registration pass. */
   public static void clearForReload() {
      MAP.clear();
      mcheli.tank.MCH_TurretPopModelCache.clear();
   }

   public static IModelCustom load(String path, String name) {
      return (name != null && !name.isEmpty()) ? load(path + "/" + name) : null;
   }

   public static IModelCustom load(String path, String name, boolean reload) {
      return (name != null && !name.isEmpty()) ? load(path + "/" + name, reload) : null;
   }

   public static IModelCustom load(String name) {
      return load(name, false);
   }

   public static IModelCustom load(String name, final boolean reload) {
      return load(name, reload, true);
   }

   /**
    * Loads a model while leaving failure reporting to a caller that can provide
    * more useful context (for example, the owning vehicle definition).
    */
   static IModelCustom loadWithoutFailureLog(String path, String name, boolean reload) {
      return name != null && !name.isEmpty() ? load(path + "/" + name, reload, false) : null;
   }

   private static IModelCustom load(String name, final boolean reload, final boolean logFailure) {
      if (name == null || name.isEmpty()) return null;
      final String resourceName = normalizeResourceName(name);
      final String cacheKey = cacheKey(resourceName);
      REQUESTS.incrementAndGet();

      if (!reload) {
         IModelCustom existing = MAP.get(cacheKey);
         if (existing != null) {
            CACHE_HITS.incrementAndGet();
            return existing;
         }
      }

      return MAP.compute(cacheKey, (key, existing) -> {
         if (existing != null && !reload) {
            CACHE_HITS.incrementAndGet();
            return existing;
         }

         try {
            String mqoPath = "assets/mcheli/models/" + resourceName + ".mqo";
            String objPath = "assets/mcheli/models/" + resourceName + ".obj";
            String tcnPath = "assets/mcheli/models/" + resourceName + ".tcn";

            String modelPath = null;

            if (MCH_ResourceHelper.resourceExists(mqoPath)) {
               modelPath = W_ResourcePath.getModelPath() + "models/" + resourceName + ".mqo";
            } else if (MCH_ResourceHelper.resourceExists(objPath)) {
               modelPath = W_ResourcePath.getModelPath() + "models/" + resourceName + ".obj";
            } else if (MCH_ResourceHelper.resourceExists(tcnPath)) {
               modelPath = W_ResourcePath.getModelPath() + "models/" + resourceName + ".tcn";
            }

            IModelCustom loaded = (modelPath != null) ? W_ModelBase.loadModel(modelPath) : null;
            if(loaded != null) {
               PARSED.incrementAndGet();
               return loaded;
            }
            FAILURES.incrementAndGet();
            if(logFailure) {
               MCH_Lib.Log("Model load failed: name=%s resource=assets/mcheli/models/%s.[mqo|obj|tcn] (resource not found or loader returned null)",
                     name, resourceName);
            }
            return existing;

         } catch (Exception e) {
            FAILURES.incrementAndGet();
            if(logFailure) {
               MCH_Lib.Log("Model load failed: name=%s resource=assets/mcheli/models/%s: %s",
                     name, resourceName, e.toString());
               e.printStackTrace();
            }
            return existing;
         }
      });
   }

   private static String normalizeResourceName(String name) {
      return name.replace('\\', '/');
   }

   private static String cacheKey(String name) {
      return normalizeResourceName(name).toLowerCase(java.util.Locale.ROOT);
   }

   private static IModelCustom getOrLoad(String name) {
      IModelCustom model = MAP.get(cacheKey(name));
      return model != null ? model : load(name);
   }

   public static void logDiagnostics() {
      if(Boolean.getBoolean("mcheli.debugModelLoading")) {
         long groups = 0L;
         long faces = 0L;
         long bytes = 0L;
         for(IModelCustom model : MAP.values()) {
            java.util.List groupObjects = model instanceof W_MetasequoiaObject
                  ? ((W_MetasequoiaObject)model).groupObjects
                  : model instanceof W_WavefrontObject ? ((W_WavefrontObject)model).groupObjects : null;
            if(groupObjects == null) {
               continue;
            }
            for(Object object : groupObjects) {
               W_GroupObject group = (W_GroupObject)object;
               ++groups;
               faces += group.getFaceCount();
               bytes += group.getRetainedGeometryBytes();
            }
         }
         MCH_Lib.Log("Model geometry: uniqueRequests=%d parsed=%d cacheHits=%d joined=%d failures=%d groups=%d faces=%d bytes=%d pending=0",
               Long.valueOf(REQUESTS.get()), Long.valueOf(PARSED.get()), Long.valueOf(CACHE_HITS.get()),
               Long.valueOf(0L), Long.valueOf(FAILURES.get()), Long.valueOf(groups), Long.valueOf(faces), Long.valueOf(bytes));
      }
   }

   public static void render(String path, String name) {
      render(path + "/" + name);
   }

   public static void render(String name) {
      IModelCustom model = getOrLoad(name);

      if (model != null) {
         model.renderAll();
      }
   }

   public static void renderPart(String name, String partName) {
      IModelCustom model = getOrLoad(name);
      if (model != null) {
         model.renderPart(partName);
      }
   }

   public static void renderLine(String path, String name, int startLine, int maxLine) {
      IModelCustom model = getOrLoad(path + "/" + name);
      if (model instanceof W_ModelCustom) {
         ((W_ModelCustom) model).renderAllLine(startLine, maxLine);
      }
   }



   public static void render(String path, String name, int startFace, int maxFace) {
      IModelCustom model = getOrLoad(path + "/" + name);
      if (model instanceof W_ModelCustom) {
         ((W_ModelCustom) model).renderAll(startFace, maxFace);
      }
   }

   public static int getVertexNum(String path, String name) {
      IModelCustom model = getOrLoad(path + "/" + name);
      return (model instanceof W_ModelCustom)
              ? ((W_ModelCustom) model).getVertexNum()
              : 0;
   }

   public static W_ModelCustom get(String path, String name) {
      IModelCustom model = getOrLoad(path + "/" + name);
      return (model instanceof W_ModelCustom)
              ? (W_ModelCustom) model
              : null;
   }

   public static W_ModelCustom getRandom() {
      Object[] values = MAP.values().toArray();
      if (values.length == 0) return null;

      for (int i = 0; i < 10; i++) {
         Object obj = values[RAND.nextInt(values.length)];
         if (obj instanceof W_ModelCustom) {
            return (W_ModelCustom) obj;
         }
      }

      return null;
   }

   public static boolean containsModel(String path, String name) {
      return containsModel(path + "/" + name);
   }

   public static boolean containsModel(String name) {
      return MAP.containsKey(cacheKey(name));
   }
}
