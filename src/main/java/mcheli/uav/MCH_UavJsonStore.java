package mcheli.uav;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

/** Server-side JSON persistence for NewUAV/NewSmallUAV shift-exit locations. */
public final class MCH_UavJsonStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "mcheli_new_uavs.json";

    private MCH_UavJsonStore() {}

    public static final class StoredUav {
        public int stationDimension;
        public int stationX;
        public int stationY;
        public int stationZ;
        public int uavDimension;
        public double exitX;
        public double exitY;
        public double exitZ;
        public float exitYaw;
        public float exitPitch;
        public String droneType;
        public String uavCategory;
        public boolean smallUav;
        public String itemId;
        public int itemDamage;

        public ItemStack createItemStack() {
            if(itemId == null || itemId.isEmpty()) {
                return null;
            }
            Object registered = Item.itemRegistry.getObject(itemId);
            return registered instanceof Item ? new ItemStack((Item)registered, 1, itemDamage) : null;
        }
    }

    private static final class StationKey {
        int dimension;
        int x;
        int y;
        int z;
    }

    private static final class StoreFile {
        int version = 2;
        List<StoredUav> drones = new ArrayList<StoredUav>();
        List<StationKey> destroyedStations = new ArrayList<StationKey>();
    }

    public static synchronized boolean save(World world, MCH_EntityUavStation station, MCH_EntityBaseVehicle aircraft, ItemStack itemStack,
                                            double exitX, double exitY, double exitZ) {
        if(world == null || world.isRemote || station == null || aircraft == null || itemStack == null || itemStack.getItem() == null) {
            return false;
        }
        File file = getFile();
        if(file == null) {
            return false;
        }
        StoreFile data = read(file);
        if(data == null) {
            return false;
        }
        removeMatching(data, station.dimension, station.posX, station.posY, station.posZ);
        removeDestroyedMatching(data, station.dimension, station.posX, station.posY, station.posZ);

        StoredUav stored = new StoredUav();
        stored.stationDimension = station.dimension;
        stored.stationX = MathHelper.floor_double(station.posX);
        stored.stationY = MathHelper.floor_double(station.posY);
        stored.stationZ = MathHelper.floor_double(station.posZ);
        stored.uavDimension = aircraft.dimension;
        stored.exitX = exitX;
        stored.exitY = exitY;
        stored.exitZ = exitZ;
        stored.exitYaw = aircraft.rotationYaw;
        stored.exitPitch = aircraft.rotationPitch;
        stored.droneType = aircraft.getAcInfo() == null ? "" : aircraft.getAcInfo().name;
        stored.smallUav = aircraft.isSmallUAV();
        stored.uavCategory = stored.smallUav ? "newsmallUAVS" : "newUAVs";
        Object itemName = Item.itemRegistry.getNameForObject(itemStack.getItem());
        stored.itemId = itemName == null ? "" : itemName.toString();
        stored.itemDamage = itemStack.getItemDamage();
        data.drones.add(stored);
        return write(file, data);
    }

    public static synchronized StoredUav load(World world, MCH_EntityUavStation station) {
        if(world == null || world.isRemote || station == null) {
            return null;
        }
        File file = getFile();
        if(file == null || !file.isFile()) {
            return null;
        }
        StoreFile data = read(file);
        if(data == null) {
            return null;
        }
        int x = MathHelper.floor_double(station.posX);
        int y = MathHelper.floor_double(station.posY);
        int z = MathHelper.floor_double(station.posZ);
        for(StoredUav stored : data.drones) {
            if(stored != null && stored.stationDimension == station.dimension && stored.stationX == x && stored.stationY == y && stored.stationZ == z) {
                return stored;
            }
        }
        return null;
    }

    public static synchronized void signalDestroyed(World world, int stationDimension, double stationX, double stationY, double stationZ) {
        if(world == null || world.isRemote) {
            return;
        }
        File file = getFile();
        if(file == null) {
            return;
        }
        StoreFile data = read(file);
        if(data == null) {
            return;
        }
        removeMatching(data, stationDimension, stationX, stationY, stationZ);
        removeDestroyedMatching(data, stationDimension, stationX, stationY, stationZ);
        StationKey destroyed = new StationKey();
        destroyed.dimension = stationDimension;
        destroyed.x = MathHelper.floor_double(stationX);
        destroyed.y = MathHelper.floor_double(stationY);
        destroyed.z = MathHelper.floor_double(stationZ);
        data.destroyedStations.add(destroyed);
        write(file, data);
    }

    public static synchronized boolean consumeDestroyed(World world, MCH_EntityUavStation station) {
        if(world == null || world.isRemote || station == null) {
            return false;
        }
        File file = getFile();
        if(file == null || !file.isFile()) {
            return false;
        }
        StoreFile data = read(file);
        if(data == null || !removeDestroyedMatching(data, station.dimension, station.posX, station.posY, station.posZ)) {
            return false;
        }
        write(file, data);
        return true;
    }

    public static synchronized void remove(World world, MCH_EntityUavStation station) {
        if(station != null) {
            remove(world, station.dimension, station.posX, station.posY, station.posZ);
        }
    }

    public static synchronized void remove(World world, int stationDimension, double stationX, double stationY, double stationZ) {
        if(world == null || world.isRemote) {
            return;
        }
        File file = getFile();
        if(file == null || !file.isFile()) {
            return;
        }
        StoreFile data = read(file);
        if(data != null && removeMatching(data, stationDimension, stationX, stationY, stationZ)) {
            write(file, data);
        }
    }

    private static boolean removeMatching(StoreFile data, int dimension, double x, double y, double z) {
        int blockX = MathHelper.floor_double(x);
        int blockY = MathHelper.floor_double(y);
        int blockZ = MathHelper.floor_double(z);
        boolean removed = false;
        Iterator<StoredUav> iterator = data.drones.iterator();
        while(iterator.hasNext()) {
            StoredUav stored = iterator.next();
            if(stored != null && stored.stationDimension == dimension && stored.stationX == blockX && stored.stationY == blockY && stored.stationZ == blockZ) {
                iterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    private static boolean removeDestroyedMatching(StoreFile data, int dimension, double x, double y, double z) {
        int blockX = MathHelper.floor_double(x);
        int blockY = MathHelper.floor_double(y);
        int blockZ = MathHelper.floor_double(z);
        boolean removed = false;
        Iterator<StationKey> iterator = data.destroyedStations.iterator();
        while(iterator.hasNext()) {
            StationKey stored = iterator.next();
            if(stored != null && stored.dimension == dimension && stored.x == blockX && stored.y == blockY && stored.z == blockZ) {
                iterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    private static File getFile() {
        File saveRoot = DimensionManager.getCurrentSaveRootDirectory();
        if(saveRoot == null) {
            MCH_Lib.Log("Unable to access the current world directory for New UAV JSON persistence.", new Object[0]);
            return null;
        }
        File dataDirectory = new File(saveRoot, "data");
        if(!dataDirectory.isDirectory() && !dataDirectory.mkdirs()) {
            MCH_Lib.Log("Unable to create New UAV JSON data directory: %s", new Object[] { dataDirectory.getAbsolutePath() });
            return null;
        }
        return new File(dataDirectory, FILE_NAME);
    }

    private static StoreFile read(File file) {
        if(!file.isFile()) {
            return new StoreFile();
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            try {
                StoreFile data = GSON.fromJson(reader, StoreFile.class);
                if(data == null) {
                    data = new StoreFile();
                }
                if(data.drones == null) {
                    data.drones = new ArrayList<StoredUav>();
                }
                if(data.destroyedStations == null) {
                    data.destroyedStations = new ArrayList<StationKey>();
                }
                return data;
            } finally {
                reader.close();
            }
        } catch(Exception e) {
            MCH_Lib.Log("Failed to read New UAV JSON store %s: %s", new Object[] { file.getAbsolutePath(), e.toString() });
            return null;
        }
    }

    private static boolean write(File file, StoreFile data) {
        File temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(temporary));
            try {
                GSON.toJson(data, writer);
            } finally {
                writer.close();
            }
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch(IOException atomicMoveFailure) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch(IOException e) {
            MCH_Lib.Log("Failed to write New UAV JSON store %s: %s", new Object[] { file.getAbsolutePath(), e.toString() });
            if(temporary.exists()) {
                temporary.delete();
            }
            return false;
        }
    }
}
