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
   private static final int MAX_LINES = 128, MAX_BYTES = 512;
   private static final AtomicReference<PacketWheelDiagnostics> PENDING = new AtomicReference<PacketWheelDiagnostics>();
   private static PacketWheelDiagnostics current;
   private static EntityPlayer clientPlayer;
   private static World clientWorld;
   private static long receivedAt;
   private static boolean clientEnabled;
   private int observer, dimension, vehicle = -1;
   private String uuid = "";
   private boolean enabled;
   public List<String> left = Collections.emptyList(), right = Collections.emptyList();

   public PacketWheelDiagnostics() {}
   public PacketWheelDiagnostics(EntityPlayerMP player, MCH_EntityTank car, boolean enabled,
         List<String> left, List<String> right) {
      this.observer = player.getEntityId(); this.dimension = player.dimension; this.enabled = enabled;
      if(car != null) { this.vehicle = car.getEntityId(); this.uuid = car.getUniqueID().toString(); }
      this.left = left; this.right = right;
   }

   public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
      data.writeInt(observer); data.writeInt(dimension); data.writeInt(vehicle); data.writeBoolean(enabled);
      writeString(data, uuid); writeLines(data, left); writeLines(data, right);
   }
   public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
      observer = data.readInt(); dimension = data.readInt(); vehicle = data.readInt(); enabled = data.readBoolean();
      uuid = readString(data); left = readLines(data); right = readLines(data);
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
   }
   public static void tickClient(EntityPlayer player) {
      if(player != clientPlayer || player.worldObj != clientWorld) {
         current = null; clientEnabled = false;
         clientPlayer = player; clientWorld = player.worldObj;
      }
      PacketWheelDiagnostics packet = PENDING.getAndSet(null);
      if(packet != null && packet.observer == player.getEntityId() && packet.dimension == player.dimension) {
         clientEnabled = packet.enabled;
         current = packet.enabled && matches(packet, player) ? packet : null;
         receivedAt = System.nanoTime();
      }
      if(current != null && (!matches(current, player) || System.nanoTime() - receivedAt > 1000000000L)) current = null;
   }
   private static boolean matches(PacketWheelDiagnostics packet, EntityPlayer player) {
      MCH_EntityTank car = MCH_WheelDiagnostics.riddenCar(player);
      return car != null && car.getEntityId() == packet.vehicle && car.getUniqueID().toString().equals(packet.uuid);
   }
   public static boolean isVisible(EntityPlayer player) {
      return clientEnabled && player == clientPlayer && player.worldObj == clientWorld
            && MCH_WheelDiagnostics.riddenCar(player) != null;
   }
   public static PacketWheelDiagnostics snapshot(EntityPlayer player) {
      return current != null && matches(current, player) && System.nanoTime() - receivedAt <= 1000000000L ? current : null;
   }
}
