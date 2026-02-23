package mcheli.multithread;

import mcheli.MCH_ClientProxy;
import mcheli.MCH_ModelManager;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.vehicle.MCH_VehicleInfoManager;
import mcheli.weapon.MCH_DefaultBulletModels;
import mcheli.ship.MCH_ShipInfoManager;

import java.util.Map;

public class MultiThreadModelManager {

    /**
     * Synchronous model loading.
     * MUST run on main client thread.
     */
    public static void start(MCH_ClientProxy proxy) {

        // Wait for data maps to populate
        waitForData("helicopter", MCH_HeliInfoManager.map);
        waitForData("plane", MCP_PlaneInfoManager.map);
        waitForData("ship", MCH_ShipInfoManager.map);
        waitForData("tank", MCH_TankInfoManager.map);
        waitForData("vehicle", MCH_VehicleInfoManager.map);

        // Load block models first
        MCH_ModelManager.load("blocks", "drafting_table");

        System.out.println("=== Starting Model Registration ===");

        // Helicopters
        for (Object key : MCH_HeliInfoManager.map.keySet()) {
            proxy.registerModelsHeli((String) key, false);
        }
        completion("helicopter");

        // Planes
        for (Object key : MCP_PlaneInfoManager.map.keySet()) {
            proxy.registerModelsPlane((String) key, false);
        }
        completion("plane");

        // Ships
        for (Object key : MCH_ShipInfoManager.map.keySet()) {
            proxy.registerModelsShip((String) key, false);
        }
        completion("ship");

        // Tanks
        for (Object key : MCH_TankInfoManager.map.keySet()) {
            proxy.registerModelsTank((String) key, false);
        }
        completion("tank");

        // Vehicles
        for (Object key : MCH_VehicleInfoManager.map.keySet()) {
            proxy.registerModelsVehicle((String) key, false);
        }
        completion("vehicle");

        // Bullets (register once only)
        proxy.registerModels_Bullet();
        loadDefaultBulletModels(proxy);
        completion("bullet");

        // Throwables
        MCH_ClientProxy.registerModels_Throwable();
        completion("throwable");

        System.out.println("=== All model rendering tasks completed successfully ===");
    }

    private static void loadDefaultBulletModels(MCH_ClientProxy proxy) {
        MCH_DefaultBulletModels.Bullet = proxy.loadBulletModel("bullet");
        MCH_DefaultBulletModels.AAMissile = proxy.loadBulletModel("aamissile");
        MCH_DefaultBulletModels.ATMissile = proxy.loadBulletModel("asmissile");
        MCH_DefaultBulletModels.ASMissile = proxy.loadBulletModel("asmissile");
        MCH_DefaultBulletModels.Bomb = proxy.loadBulletModel("bomb");
        MCH_DefaultBulletModels.Rocket = proxy.loadBulletModel("rocket");
        MCH_DefaultBulletModels.Torpedo = proxy.loadBulletModel("torpedo");
    }

    private static void waitForData(String name, Map<?, ?> map) {
        int retries = 0;
        while (map.isEmpty() && retries < 500) { // 5 seconds max
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {}
            retries++;
        }

        if (map.isEmpty()) {
            System.err.println("Warning: " + name + " map is still empty after waiting.");
        } else {
            System.out.println(name + " data ready with " + map.size() + " entries.");
        }
    }

    public static void completion(String type) {
        System.out.println("Successfully registered " + type + " models");
    }
}