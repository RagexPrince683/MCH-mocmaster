package mcheli.aircraft;

import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

public class MCH_DummyCommandSender implements ICommandSender {

   public static MCH_DummyCommandSender instance = new MCH_DummyCommandSender();


   public static void execCommand(String s) {
      ICommandManager icommandmanager = MinecraftServer.getServer().getCommandManager();
      icommandmanager.executeCommand(instance, s);
   }

   public String getCommandSenderName() {
      return "";
   }

   public IChatComponent func_145748_c_() {
      return null;
   }

   public void addChatMessage(IChatComponent p_145747_1_) {}

   public boolean canCommandSenderUseCommand(int p_70003_1_, String p_70003_2_) {
      return true;
   }

   public ChunkCoordinates getPlayerCoordinates() {
      return null;
   }

   public World getEntityWorld() {
      return null;
   }

}
