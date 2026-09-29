package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import mcheli.network.PacketBase;
import mcheli.tank.MCH_EntityTank;
import mcheli.tank.MCH_WheelDiagnostics;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

/** Server-to-one-rider text snapshot; network callbacks never mutate world/entity state. */
public class PacketWheelDiagnostics extends PacketBase {
   private static final Charset UTF8 = Charset.forName("UTF-8");
   private static final int MAX_LINES = 256, MAX_BYTES = 512;
   private static final AtomicReference<PacketWheelDiagnostics> PENDING = new AtomicReference<PacketWheelDiagnostics>();
   private static PacketWheelDiagnostics current;
   private static EntityPlayer clientPlayer;
   private static World clientWorld;
   private static long receivedAt;
   private static boolean clientEnabled;
   private static String unavailable = "waiting for server packet";
   private static int lastServerTick = -1;
   private int observer, dimension, vehicle = -1;
   private String vehicleIdentity = "";
   public int serverTick = -1;
   private boolean enabled;
   public List<String> left = Collections.emptyList(), right = Collections.emptyList();

   public PacketWheelDiagnostics() {}
   public PacketWheelDiagnostics(EntityPlayerMP player, MCH_EntityTank car, boolean enabled,
         List<String> left, List<String> right) {
      this.observer = player.getEntityId(); this.dimension = player.dimension; this.enabled = enabled;
      if(car != null) {
         this.vehicle = car.getEntityId(); this.vehicleIdentity = car.getCommonUniqueId();
         this.serverTick = car.ticksExisted;
      }
      this.left = left; this.right = right;
   }

   public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
      data.writeInt(observer); data.writeInt(dimension); data.writeInt(vehicle); data.writeBoolean(enabled);
      data.writeInt(serverTick);
      writeString(data, vehicleIdentity); writeLines(data, left); writeLines(data, right);
   }
   public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
      observer = data.readInt(); dimension = data.readInt(); vehicle = data.readInt(); enabled = data.readBoolean();
      serverTick = data.readInt();
      vehicleIdentity = readString(data); left = readLines(data); right = readLines(data);
   }
   private static void writeString(ByteBuf data, String value) {
      byte[] bytes = value.getBytes(UTF8);
      if(bytes.length > MAX_BYTES) throw new IllegalArgumentException("Wheel diagnostic line too long");
      data.writeShort(bytes.length); data.writeBytes(bytes);
   }
   private static String readString(ByteBuf data) {
      int length = data.readUnsignedShort();
      if(length > MAX_BYTES || length > data.readableBytes()) throw new IllegalArgumentException("Invalid wheel diagnostic line");
      byte[] bytes = new byte[length]; data.readBytes(bytes); return new String(bytes, UTF8);
   }
   private static void writeLines(ByteBuf data, List<String> lines) {
      int count = Math.min(MAX_LINES, lines.size()); data.writeShort(count);
      for(int i = 0; i < count; ++i) writeString(data, lines.get(i));
   }
   private static List<String> readLines(ByteBuf data) {
      int count = data.readUnsignedShort();
      if(count > MAX_LINES) throw new IllegalArgumentException("Too many wheel diagnostic lines");
      List<String> lines = new ArrayList<String>(count);
      for(int i = 0; i < count; ++i) lines.add(readString(data));
      return Collections.unmodifiableList(lines);
   }
   public void handleServerSide(EntityPlayerMP player) { }
   public void handleClientSide(EntityPlayer player) { PENDING.set(this); }

   public static void clearClient() {
      PENDING.set(null); current = null; clientPlayer = null; clientWorld = null; clientEnabled = false;
      unavailable = "waiting for server packet"; lastServerTick = -1; receivedAt = 0;
   }
   public static void tickClient(EntityPlayer player) {
      if(player != clientPlayer || player.worldObj != clientWorld) {
         current = null; clientEnabled = false;
         unavailable = "player/world changed; waiting for server packet";
         lastServerTick = -1; receivedAt = 0;
         clientPlayer = player; clientWorld = player.worldObj;
      }
      PacketWheelDiagnostics packet = PENDING.getAndSet(null);
      if(packet != null) {
         if(packet.observer != player.getEntityId() || packet.dimension != player.dimension) {
            current = null;
            unavailable = "packet rejected: observer/dimension mismatch";
         } else {
            clientEnabled = packet.enabled;
            unavailable = !packet.enabled ? "disabled" : mismatch(packet, player);
            current = packet.enabled && unavailable == null ? packet : null;
            lastServerTick = packet.serverTick;
            receivedAt = System.nanoTime();
         }
      }
      if(current != null) {
         String reason = mismatch(current, player);
         if(reason != null || ageMillis() > 1000) {
            current = null;
            unavailable = reason != null ? reason : "expired: no packet for >1000 ms";
         }
      }
      if(MCH_WheelDiagnostics.riddenCar(player) == null) {
         current = null; lastServerTick = -1; receivedAt = 0;
         unavailable = "no client civilian vehicle (dismounted/dead)";
      }
   }
   private static String mismatch(PacketWheelDiagnostics packet, EntityPlayer player) {
      if(packet.vehicle < 0) return "no server civilian vehicle";
      MCH_EntityTank car = MCH_WheelDiagnostics.riddenCar(player);
      if(car == null) return "no client civilian vehicle";
      if(car.getEntityId() != packet.vehicle) return "vehicle entity ID mismatch";
      if(packet.vehicleIdentity.isEmpty() || !packet.vehicleIdentity.equals(car.getCommonUniqueId()))
         return "vehicle synchronized identity mismatch";
      return null;
   }
   private static boolean matches(PacketWheelDiagnostics packet, EntityPlayer player) {
      return mismatch(packet, player) == null;
   }
   public static long ageMillis() { return receivedAt == 0 ? -1 : (System.nanoTime() - receivedAt) / 1000000L; }
   public static String status(EntityPlayer player) {
      String reason = current != null ? mismatch(current, player) : unavailable;
      if(reason == null && ageMillis() > 1000) reason = "expired: no packet for >1000 ms";
      return "S snapshot=" + (reason == null ? "current" : "N/A (" + reason + ")")
            + " tick=" + lastServerTick + " age=" + ageMillis() + " ms";
   }
   public static boolean isVisible(EntityPlayer player) {
      return clientEnabled && player == clientPlayer && player.worldObj == clientWorld
            && MCH_WheelDiagnostics.riddenCar(player) != null;
   }
   public static PacketWheelDiagnostics snapshot(EntityPlayer player) {
      return current != null && matches(current, player) && System.nanoTime() - receivedAt <= 1000000000L ? current : null;
   }
}
