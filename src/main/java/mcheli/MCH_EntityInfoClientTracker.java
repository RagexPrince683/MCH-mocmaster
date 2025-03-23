package mcheli;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MCH_EntityInfoClientTracker {

    private static final Map<Integer, MCH_EntityInfo> trackedEntities = new ConcurrentHashMap<>();

    public static void updateEntities(List<MCH_EntityInfo> infos) {
        infos.forEach(info -> trackedEntities.put(info.entityId, info));
    }

    public static void removeEntities(List<MCH_EntityInfo> infos) {
        infos.forEach(info -> trackedEntities.remove(info.entityId));
    }

    public static MCH_EntityInfo getEntityInfo(int entityId) {
        return trackedEntities.get(entityId);
    }

    public static Collection<MCH_EntityInfo> getAllTrackedEntities() {
        return Collections.unmodifiableCollection(trackedEntities.values());
    }
}