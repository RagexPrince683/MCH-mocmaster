package mcheli.uav;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** Runtime lookup cache only; persistent truth lives on aircraft/station/player NBT. */
public final class MCH_UavRegistry {
    private static final Map<String, MCH_EntityAircraft> BY_ENTITY_UUID = new HashMap<String, MCH_EntityAircraft>();
    private static final Map<String, MCH_EntityAircraft> BY_COMMON_ID = new HashMap<String, MCH_EntityAircraft>();
    private static final Map<String, MCH_EntityAircraft> BY_OWNER = new HashMap<String, MCH_EntityAircraft>();

    private MCH_UavRegistry() {}

    public static void register(MCH_EntityAircraft ac) {
        if (!isValidUav(ac)) {
            return;
        }
        BY_ENTITY_UUID.put(ac.getUniqueID().toString(), ac);
        if (ac.getCommonUniqueId() != null && !ac.getCommonUniqueId().isEmpty()) {
            BY_COMMON_ID.put(ac.getCommonUniqueId(), ac);
        }
        if (ac.getOwnerUUID() != null) {
            BY_OWNER.put(ac.getOwnerUUID().toString(), ac);
        }
    }

    public static void unregister(MCH_EntityAircraft ac) {
        if (ac == null) {
            return;
        }
        BY_ENTITY_UUID.remove(ac.getUniqueID().toString());
        if (ac.getCommonUniqueId() != null) {
            BY_COMMON_ID.remove(ac.getCommonUniqueId());
        }
        if (ac.getOwnerUUID() != null) {
            BY_OWNER.remove(ac.getOwnerUUID().toString());
        }
    }

    public static MCH_EntityAircraft findLinkedUav(World world, UUID entityUuid, String commonId, UUID owner) {
        MCH_EntityAircraft ac = getLive(BY_ENTITY_UUID.get(entityUuid == null ? "" : entityUuid.toString()), world);
        if (ac != null) return ac;
        ac = getLive(BY_COMMON_ID.get(commonId == null ? "" : commonId), world);
        if (ac != null) return ac;
        ac = getLive(BY_OWNER.get(owner == null ? "" : owner.toString()), world);
        if (ac != null) return ac;
        return searchLoaded(world, entityUuid, commonId, owner);
    }

    public static MCH_EntityAircraft findByOwner(World world, UUID owner) {
        return findLinkedUav(world, null, null, owner);
    }

    public static void rebuildUavRegistry(World world) {
        if (world == null) return;
        searchLoaded(world, null, null, null);
    }

    private static MCH_EntityAircraft searchLoaded(World world, UUID entityUuid, String commonId, UUID owner) {
        if (world == null) return null;
        List list = world.loadedEntityList;
        MCH_EntityAircraft first = null;
        for (int i = 0; i < list.size(); ++i) {
            Object obj = list.get(i);
            if (obj instanceof MCH_EntityAircraft) {
                MCH_EntityAircraft ac = (MCH_EntityAircraft)obj;
                if (isValidUav(ac)) {
                    register(ac);
                    boolean entityMatch = entityUuid != null && entityUuid.equals(ac.getUniqueID());
                    boolean commonMatch = commonId != null && !commonId.isEmpty() && commonId.equals(ac.getCommonUniqueId());
                    boolean ownerMatch = owner != null && owner.equals(ac.getOwnerUUID());
                    if (entityUuid == null && (commonId == null || commonId.isEmpty()) && owner == null && first == null) {
                        first = ac;
                    } else if (entityMatch || commonMatch || ownerMatch) {
                        return ac;
                    }
                }
            }
        }
        return first;
    }

    private static MCH_EntityAircraft getLive(MCH_EntityAircraft ac, World world) {
        if (isValidUav(ac) && (world == null || ac.worldObj == world)) {
            return ac;
        }
        unregister(ac);
        return null;
    }

    private static boolean isValidUav(MCH_EntityAircraft ac) {
        return ac != null && !ac.isDead && (ac.isUAV() || ac.isNewUAV());
    }
}
