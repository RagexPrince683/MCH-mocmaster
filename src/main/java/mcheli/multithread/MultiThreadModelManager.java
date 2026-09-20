package mcheli.multithread;

import mcheli.MCH_ClientProxy;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;

/**
 * Compatibility entry point for the old eager parallel loader.
 *
 * Vehicle meshes are now loaded on demand. Parsing on the render thread is the
 * safe Java 8 fallback because Forge 1.7 resource managers are not thread-safe.
 * The configuration option remains accepted for existing installations.
 */
public final class MultiThreadModelManager {

    private static boolean migrationLogged;

    private MultiThreadModelManager() {
    }

    public static synchronized void start(MCH_ClientProxy proxy) {
        if(MCH_Config.MultiThreadedModelLoading.prmBool && !migrationLogged) {
            MCH_Lib.DbgLog(false,
                    "MultiThreadedModelLoading now uses shared lazy model loading; eager worker registration is disabled.");
            migrationLogged = true;
        }
    }
}
