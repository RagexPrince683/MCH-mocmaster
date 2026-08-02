package mcheli.tank;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.wrapper.modelloader.W_MetasequoiaObject;
import net.minecraft.util.Vec3;

/** Client-owned, source-model keyed views used by every wreck of a tank type. */
@SideOnly(Side.CLIENT)
public final class MCH_TurretPopModelCache {
   private static final Map CACHE = new IdentityHashMap();
   private static final Set WARNED = new HashSet();

   public static final class Entry {
      public final W_MetasequoiaObject source;
      public final W_MetasequoiaObject wreck;
      public final W_MetasequoiaObject detached;
      public final Vec3 pivot;
      public final List includedNames;
      public final List excludedNames;
      public final MCH_BaseVehicleInfo.PartWeapon mainGun;

      private Entry(W_MetasequoiaObject source, W_MetasequoiaObject wreck,
            W_MetasequoiaObject detached, Vec3 pivot, List names,
            MCH_BaseVehicleInfo.PartWeapon mainGun) {
         this.source = source;
         this.wreck = wreck;
         this.detached = detached;
         this.pivot = pivot;
         this.includedNames = Collections.unmodifiableList(new ArrayList(names));
         this.excludedNames = this.includedNames;
         this.mainGun = mainGun;
      }
   }

   private MCH_TurretPopModelCache() {}

   public static synchronized Entry get(MCH_TankInfo info, MCH_BaseVehicleInfo.PartWeapon mainGun) {
      if(info == null || !(info.model instanceof W_MetasequoiaObject)) {
         warn(info, info == null || info.model == null ? "null" : info.model.getClass().getName());
         return null;
      }
      W_MetasequoiaObject source = (W_MetasequoiaObject)info.model;
      Map byType = (Map)CACHE.get(source);
      if(byType == null) {
         byType = new java.util.HashMap();
         CACHE.put(source, byType);
      }
      Entry cached = (Entry)byType.get(info.name);
      if(cached != null) return cached;

      W_MetasequoiaObject.GroupRange turret = source.resolveGroupRange("$turret");
      if(turret == null) {
         warn(info, source.getClass().getName());
         return null;
      }
      List ranges = new ArrayList();
      List names = new ArrayList();
      ranges.add(turret);
      names.add("$turret");
      addWeaponRange(source, mainGun, ranges, names);
      if(mainGun != null) for(Object object : mainGun.child) {
         MCH_BaseVehicleInfo.PartWeaponChild child = (MCH_BaseVehicleInfo.PartWeaponChild)object;
         addRange(source, "$" + child.modelName, ranges, names);
      }
      // Both views own independent ordered lists. Never remove groups from the
      // shared TankInfo model: doing so would alter every living tank of this type.
      Entry entry = new Entry(source, source.createViewExcluding(ranges),
            source.createView(ranges), info.turretPosition, names, mainGun);
      byType.put(info.name, entry);
      return entry;
   }

   private static void addWeaponRange(W_MetasequoiaObject source,
         MCH_BaseVehicleInfo.PartWeapon weapon, List ranges, List names) {
      if(weapon != null) addRange(source, "$" + weapon.modelName, ranges, names);
   }

   private static void addRange(W_MetasequoiaObject source, String name, List ranges, List names) {
      W_MetasequoiaObject.GroupRange range = source.resolveGroupRange(name);
      if(range != null && !names.contains(name)) {
         ranges.add(range);
         names.add(name);
      }
   }

   private static void warn(MCH_TankInfo info, String modelClass) {
      String name = info == null ? "<unknown>" : info.name;
      if(WARNED.add(name + ":" + modelClass)) {
         MCH_Lib.Log("Turret pop disabled for tank '%s': model class %s has no complete $turret MQO section",
               new Object[]{name, modelClass});
      }
   }

   public static synchronized void clear() {
      CACHE.clear();
      WARNED.clear();
   }

   public static synchronized void invalidate(MCH_BaseVehicleInfo info) {
      if(info != null && info.model != null) CACHE.remove(info.model);
   }
}
