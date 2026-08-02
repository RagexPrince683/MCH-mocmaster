package mcheli.multithread;

import mcheli.MCH_ClientProxy;
import mcheli.MCH_Lib;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.ship.MCH_ShipInfoManager;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.vehicle.MCH_TurretInfoManager;
import mcheli.weapon.MCH_DefaultBulletModels;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MultiThreadModelManager {

    private static final int THREADS = Runtime.getRuntime().availableProcessors();
    public static synchronized void start(MCH_ClientProxy proxy) {

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        waitForData("helicopter", MCH_HeliInfoManager.map);
        waitForData("plane", MCP_PlaneInfoManager.map);
        waitForData("ship", MCH_ShipInfoManager.map);
        waitForData("tank", MCH_TankInfoManager.map);
        waitForData("vehicle", MCH_TurretInfoManager.map);

        System.out.println("=== Starting Parallel Model Registration ===");

        List<Future<?>> futures = new ArrayList<>();

        // Helicopters
        for (Object key : MCH_HeliInfoManager.map.keySet()) {
            String name = (String) key;
            futures.add(executor.submit(() ->
                    proxy.registerModelsHeli(name, false)
            ));
        }

        // Planes
        for (Object key : MCP_PlaneInfoManager.map.keySet()) {
            String name = (String) key;
            futures.add(executor.submit(() ->
                    proxy.registerModelsPlane(name, false)
            ));
        }

        // Ships
        for (Object key : MCH_ShipInfoManager.map.keySet()) {
            String name = (String) key;
            futures.add(executor.submit(() ->
                    proxy.registerModelsShip(name, false)
            ));
        }

        // Tanks
        for (Object key : MCH_TankInfoManager.map.keySet()) {
            String name = (String) key;
            futures.add(executor.submit(() ->
                    proxy.registerModelsTank(name, false)
            ));
        }

        // Vehicles
        for (Object key : MCH_TurretInfoManager.map.keySet()) {
            String name = (String) key;
            futures.add(executor.submit(() -> {
                try {
                    proxy.registerModelsVehicle(name, false);
                } catch (Exception e) {
                    System.err.println("Failed loading vehicle model: " + name);
                    e.printStackTrace();
                    throw e;
                }
            }));
        }

        // Wait for all to complete
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }



        executor.shutdown();

        // Bullets (register once only)
        proxy.registerModels_Bullet();
        loadDefaultBulletModels(proxy);
        //completion("bullet");

        MCH_ClientProxy.registerModels_Throwable();
        //completion("throwable");

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

    private static void waitForData(String name, java.util.Map<?, ?> map) {
        int retries = 0;
        while (map.isEmpty() && retries < 500) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {}
            retries++;
        }

        MCH_Lib.DbgLog(false, "%s data ready with %d entries.", name, Integer.valueOf(map.size()));
    }
}
