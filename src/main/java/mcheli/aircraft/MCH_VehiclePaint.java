package mcheli.aircraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Server-authoritative serialization helpers for per-item vehicle paint. */
public final class MCH_VehiclePaint {
   public static final String TAG_PAINT = "MCH_VehiclePaint";
   private static final String TAG_DEFAULTS = "MCH_VehiclePaintDefaults";
   private static final String TAG_PLAYER = "PlayerPersisted";

   private MCH_VehiclePaint() {
   }

   public static final class Design {
      public final int color;
      public final int opacity;
      public final List parts;
      public final String camo;

      public Design(int color, int opacity, List parts) {
         this(color, opacity, parts, "");
      }

      public Design(int color, int opacity, List parts, String camo) {
         this.color = color & 0xFFFFFF;
         this.opacity = Math.max(0, Math.min(255, opacity));
         ArrayList clean = new ArrayList();
         if(parts != null) {
            for(Object object : parts) {
               String part = String.valueOf(object);
               if(part.length() > 0 && part.length() <= 64 && !clean.contains(part)) clean.add(part);
            }
         }
         this.parts = Collections.unmodifiableList(clean);
         this.camo = camo != null && camo.length() <= 64 ? camo : "";
      }

      public boolean isVisible() {
         return (this.opacity > 0 || !this.camo.isEmpty()) && !this.parts.isEmpty();
      }
   }

   public static boolean isVehicle(ItemStack stack) {
      return stack != null && stack.getItem() instanceof MCH_ItemBaseVehicle;
   }

   public static String getTypeKey(ItemStack stack) {
      if(!isVehicle(stack)) return "";
      MCH_BaseVehicleInfo info = ((MCH_ItemBaseVehicle)stack.getItem()).getAircraftInfo();
      return info == null ? "" : info.getDirectoryName() + ":" + info.name;
   }

   public static Design read(ItemStack stack) {
      if(stack == null || !stack.hasTagCompound()) return null;
      return read(stack.getTagCompound().getCompoundTag(TAG_PAINT));
   }

   public static Design read(NBTTagCompound root) {
      if(root == null || !root.hasKey("Color") || !root.hasKey("Parts")) return null;
      ArrayList parts = new ArrayList();
      String serialized = root.getString("Parts");
      if(!serialized.isEmpty()) {
         String[] values = serialized.split("\\n");
         for(String value : values) if(!value.isEmpty()) parts.add(value);
      }
      return new Design(root.getInteger("Color"), root.getInteger("Opacity"), parts, root.getString("Camo"));
   }

   public static void write(ItemStack stack, Design design) {
      if(stack == null) return;
      if(!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
      if(design == null) {
         stack.getTagCompound().removeTag(TAG_PAINT);
      } else {
         stack.getTagCompound().setTag(TAG_PAINT, write(design));
      }
   }

   public static NBTTagCompound write(Design design) {
      NBTTagCompound tag = new NBTTagCompound();
      tag.setInteger("Color", design.color);
      tag.setInteger("Opacity", design.opacity);
      StringBuilder parts = new StringBuilder();
      for(Object object : design.parts) {
         if(parts.length() > 0) parts.append('\n');
         parts.append((String)object);
      }
      tag.setString("Parts", parts.toString());
      tag.setString("Camo", design.camo);
      return tag;
   }

   public static Design getDefault(EntityPlayer player, String typeKey) {
      if(player == null || typeKey == null || typeKey.isEmpty()) return null;
      NBTTagCompound defaults = getDefaults(player, false);
      return defaults == null ? null : read(defaults.getCompoundTag(typeKey));
   }

   public static void setDefault(EntityPlayer player, String typeKey, Design design) {
      if(player == null || typeKey == null || typeKey.isEmpty()) return;
      NBTTagCompound defaults = getDefaults(player, true);
      if(design == null) defaults.removeTag(typeKey);
      else defaults.setTag(typeKey, write(design));
   }

   public static boolean applyDefaultIfUnpainted(EntityPlayer player, ItemStack stack) {
      if(!isVehicle(stack) || read(stack) != null) return false;
      Design design = getDefault(player, getTypeKey(stack));
      if(design == null) return false;
      write(stack, design);
      return true;
   }

   private static NBTTagCompound getDefaults(EntityPlayer player, boolean create) {
      NBTTagCompound entity = player.getEntityData();
      NBTTagCompound persisted = entity.getCompoundTag(TAG_PLAYER);
      if(create && !entity.hasKey(TAG_PLAYER)) entity.setTag(TAG_PLAYER, persisted);
      NBTTagCompound defaults = persisted.getCompoundTag(TAG_DEFAULTS);
      if(create && !persisted.hasKey(TAG_DEFAULTS)) persisted.setTag(TAG_DEFAULTS, defaults);
      return !create && !persisted.hasKey(TAG_DEFAULTS) ? null : defaults;
   }
}
