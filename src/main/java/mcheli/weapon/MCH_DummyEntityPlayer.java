package mcheli.weapon;

import mcheli.wrapper.ChatMessageComponent;
import mcheli.wrapper.W_EntityPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

public class MCH_DummyEntityPlayer extends W_EntityPlayer {

   public MCH_DummyEntityPlayer(World p_i45324_1_, EntityPlayer player) {
      super(p_i45324_1_, player);
   }

   public void addChatMessage(IChatComponent var1) {}

   public boolean canCommandSenderUseCommand(int var1, String var2) {
      return false;
   }

   public ChunkCoordinates getPlayerCoordinates() {
      return null;
   }

   public void sendChatToPlayer(ChatMessageComponent chatmessagecomponent) {}
}
