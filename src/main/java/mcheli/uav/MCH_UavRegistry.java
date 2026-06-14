package mcheli.uav;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** Runtime lookup cache only; persistent truth lives on aircraft/station/player NBT. */
public final class MCH_UavRegistry {
    private static final Map<String, MCH_EntityBaseVehicle> BY_PERSISTENT_UUID = new HashMap<String, MCH_EntityBaseVehicle>();
    private static final Map<String, MCH_EntityBaseVehicle> BY_ENTITY_UUID = new HashMap<String, MCH_EntityBaseVehicle>();
    private static final Map<String, MCH_EntityBaseVehicle> BY_COMMON_ID = new HashMap<String, MCH_EntityBaseVehicle>();
    private static final Map<String, MCH_EntityBaseVehicle> BY_OWNER = new HashMap<String, MCH_EntityBaseVehicle>();

    private MCH_UavRegistry() {}

    public static void register(MCH_EntityBaseVehicle ac) {
        if (!isValidUav(ac)) {
            return;
        }
        MCH_EntityBaseVehicle canonical = pruneDuplicate(ac);
        if (canonical != ac) {
            return;
        }
        UUID persistentId = ac.getUavPersistentUUID();
        if (persistentId != null) {
            BY_PERSISTENT_UUID.put(persistentId.toString(), ac);
        }
        BY_ENTITY_UUID.put(ac.getUniqueID().toString(), ac);
        if (ac.getCommonUniqueId() != null && !ac.getCommonUniqueId().isEmpty()) {
            BY_COMMON_ID.put(ac.getCommonUniqueId(), ac);
        }
        if (ac.getOwnerUUID() != null) {
            BY_OWNER.put(ac.getOwnerUUID().toString(), ac);
        }
    }

    public static void unregister(MCH_EntityBaseVehicle ac) {
        if (ac == null) {
            return;
        }
        removeIfSame(BY_ENTITY_UUID, ac.getUniqueID().toString(), ac);
        UUID persistentId = ac.getUavPersistentUUID(false);
        if (persistentId != null) {
            removeIfSame(BY_PERSISTENT_UUID, persistentId.toString(), ac);
        }
        if (ac.getCommonUniqueId() != null) {
            removeIfSame(BY_COMMON_ID, ac.getCommonUniqueId(), ac);
        }
        if (ac.getOwnerUUID() != null) {
            removeIfSame(BY_OWNER, ac.getOwnerUUID().toString(), ac);
        }
    }

    public static MCH_EntityBaseVehicle findLinkedUav(World world, UUID persistentUuid, String commonId, UUID owner) {
        boolean hasStableLink = persistentUuid != null || (commonId != null && !commonId.isEmpty());
        MCH_EntityBaseVehicle ac = getLive(BY_PERSISTENT_UUID.get(persistentUuid == null ? "" : persistentUuid.toString()), world);
        if (ac != null) return ac;
        ac = getLive(BY_ENTITY_UUID.get(persistentUuid == null ? "" : persistentUuid.toString()), world);
        if (ac != null) return ac;
        ac = getLive(BY_COMMON_ID.get(commonId == null ? "" : commonId), world);
        if (ac != null) return ac;
        if (!hasStableLink) {
            ac = getLive(BY_OWNER.get(owner == null ? "" : owner.toString()), world);
            if (ac != null) return ac;
        }
        return searchLoaded(world, persistentUuid, commonId, hasStableLink ? null : owner);
    }

    public static MCH_EntityBaseVehicle findByOwner(World world, UUID owner) {
        return findLinkedUav(world, null, null, owner);
    }

    public static MCH_EntityBaseVehicle findLoadedByUuid(World world, UUID uuid) {
        if (world == null || uuid == null) {
            return null;
        }
        List list = world.loadedEntityList;
        for (int i = 0; i < list.size(); ++i) {
            Object obj = list.get(i);
            if (obj instanceof MCH_EntityBaseVehicle) {
                MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)obj;
                UUID persistentId = ac.getUavPersistentUUID(false);
                if (isValidUav(ac) && (uuid.equals(ac.getUniqueID()) || uuid.equals(persistentId))) {
                    register(ac);
                    return ac;
                }
            }
        }
        return null;
    }

    public static boolean isPresentInLoadedEntityList(World world, Entity entity) {
        return world != null && entity != null && world.loadedEntityList.contains(entity);
    }

    public static void rebuildUavRegistry(World world) {
        if (world == null) return;
        searchLoaded(world, null, null, null);
    }

    private static MCH_EntityBaseVehicle searchLoaded(World world, UUID entityUuid, String commonId, UUID owner) {
        if (world == null) return null;
        List list = world.loadedEntityList;
        MCH_EntityBaseVehicle first = null;
        MCH_EntityBaseVehicle match = null;
        for (int i = 0; i < list.size(); ++i) {
            Object obj = list.get(i);
            if (obj instanceof MCH_EntityBaseVehicle) {
                MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)obj;
                if (isValidUav(ac)) {
                    register(ac);
                    if (ac.isDead) {
                        continue;
                    }
                    UUID persistentId = ac.getUavPersistentUUID();
                    boolean entityMatch = entityUuid != null && (entityUuid.equals(persistentId) || entityUuid.equals(ac.getUniqueID()));
                    boolean commonMatch = commonId != null && !commonId.isEmpty() && commonId.equals(ac.getCommonUniqueId());
                    boolean ownerMatch = owner != null && owner.equals(ac.getOwnerUUID());
                    if (entityUuid == null && (commonId == null || commonId.isEmpty()) && owner == null && first == null) {
                        first = ac;
                    } else if (entityMatch || commonMatch || ownerMatch) {
                        match = match == null || match.isDead ? ac : chooseCanonical(match, ac);
                    }
                }
            }
        }
        if (match != null) {
            register(match);
            return getLive(match, world);
        }
        return first == null ? null : getLive(first, world);
    }

    private static MCH_EntityBaseVehicle getLive(MCH_EntityBaseVehicle ac, World world) {
        if (isValidUav(ac) && (world == null || ac.worldObj == world)
                && (world == null || isPresentInLoadedEntityList(world, ac))) {
            return ac;
        }
        unregister(ac);
        return null;
    }

    private static void removeIfSame(Map<String, MCH_EntityBaseVehicle> map, String key, MCH_EntityBaseVehicle ac) {
        if (key != null && map.get(key) == ac) {
            map.remove(key);
        }
    }

    private static MCH_EntityBaseVehicle pruneDuplicate(MCH_EntityBaseVehicle ac) {
        MCH_EntityBaseVehicle duplicate = null;
        UUID persistentId = ac.getUavPersistentUUID();
        if (persistentId != null) {
            duplicate = getLive(BY_PERSISTENT_UUID.get(persistentId.toString()), ac.worldObj);
        }
        if (duplicate == null && ac.getCommonUniqueId() != null && !ac.getCommonUniqueId().isEmpty()) {
            duplicate = getLive(BY_COMMON_ID.get(ac.getCommonUniqueId()), ac.worldObj);
        }
        if (duplicate == null || duplicate == ac) {
            return ac;
        }

        MCH_EntityBaseVehicle keep = chooseCanonical(duplicate, ac);
        MCH_EntityBaseVehicle remove = keep == ac ? duplicate : ac;
        MCH_Lib.Log(remove, "Duplicate UAV identity detected: keeping entity %d and removing duplicate %d (persistent=%s, common=%s)", new Object[] {
                Integer.valueOf(keep.getEntityId()), Integer.valueOf(remove.getEntityId()),
                persistentId == null ? "" : persistentId.toString(), ac.getCommonUniqueId() == null ? "" : ac.getCommonUniqueId() });
        // Do not delete a duplicate while either copy may still be restoring its station or
        // pilot after a restart. Leaving it unregistered is recoverable; setDead is not.
        unregister(remove);
        remove.discardDuplicateUav();
        return keep;
    }

    private static MCH_EntityBaseVehicle chooseCanonical(MCH_EntityBaseVehicle a, MCH_EntityBaseVehicle b) {
        if (b.getRiddenByEntity() != null && a.getRiddenByEntity() == null) return b;
        if (a.getRiddenByEntity() != null && b.getRiddenByEntity() == null) return a;
        UUID aPersistent = a.getUavPersistentUUID(false);
        UUID bPersistent = b.getUavPersistentUUID(false);
        boolean aOriginal = aPersistent != null && aPersistent.equals(a.getUniqueID());
        boolean bOriginal = bPersistent != null && bPersistent.equals(b.getUniqueID());
        if (aOriginal != bOriginal) return aOriginal ? a : b;
        if (b.getUavStation() != null && a.getUavStation() == null) return b;
        if (a.getUavStation() != null && b.getUavStation() == null) return a;
        int uuidOrder = a.getUniqueID().toString().compareTo(b.getUniqueID().toString());
        if (uuidOrder != 0) return uuidOrder < 0 ? a : b;
        return a.getEntityId() <= b.getEntityId() ? a : b;
    }

    private static boolean isValidUav(MCH_EntityBaseVehicle ac) {
        return ac != null && !ac.isDead && (ac.isUAV() || ac.isNewUAV());
    }
}
