package mcheli.weapon;

import com.mojang.authlib.GameProfile;
import mcheli.wrapper.ChatMessageComponent;
import mcheli.wrapper.W_EntityPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;

public class MCH_DummyEntityPlayer extends W_EntityPlayer {

   private int prevChunkX = Integer.MIN_VALUE;
   private int prevChunkZ = Integer.MIN_VALUE;
   private int chunkRadius = 8; // Adjust the radius as needed


   @Override
   public GameProfile getGameProfile() {
      // Return a blank or dummy GameProfile
      return new GameProfile(null, "DummyPlayer");
   }

   //todone? stop trying to render steve skin

   public MCH_DummyEntityPlayer(World world, EntityPlayer player) {
      super(world, player);
      //new shit for chunk loading dummy entity
      //this.setPosition(player.posX, player.posY, player.posZ);
      //nvm
   }

 //  @Override
 //  public void onUpdate() {
 //     super.onUpdate();
 //     if (!worldObj.isRemote) {
 //        manageChunks();
 //     }
 //  }
//
 //  private void manageChunks() {
 //     int currentChunkX = MathHelper.floor_double(this.posX) >> 4;
 //     int currentChunkZ = MathHelper.floor_double(this.posZ) >> 4;
//
 //     if (currentChunkX != prevChunkX || currentChunkZ != prevChunkZ) {
 //        // Unload old chunks
 //        unloadOldChunks(prevChunkX, prevChunkZ, currentChunkX, currentChunkZ);
//
 //        // Load new chunks
 //        loadNewChunks(currentChunkX, currentChunkZ);
//
 //        prevChunkX = currentChunkX;
 //        prevChunkZ = currentChunkZ;
 //     }
 //  }
//
 //  private void loadNewChunks(int chunkX, int chunkZ) {
 //     WorldServer worldServer = (WorldServer) this.worldObj;
 //     ChunkProviderServer chunkProvider = worldServer.theChunkProviderServer;
 //     for (int x = chunkX - chunkRadius; x <= chunkX + chunkRadius; x++) {
 //        for (int z = chunkZ - chunkRadius; z <= chunkZ + chunkRadius; z++) {
 //           chunkProvider.loadChunk(x, z);
 //        }
 //     }
 //  }
//
 //  private void unloadOldChunks(int oldChunkX, int oldChunkZ, int newChunkX, int newChunkZ) {
 //     WorldServer worldServer = (WorldServer) this.worldObj;
 //     ChunkProviderServer chunkProvider = worldServer.theChunkProviderServer;
 //     for (int x = oldChunkX - chunkRadius; x <= oldChunkX + chunkRadius; x++) {
 //        for (int z = oldChunkZ - chunkRadius; z <= oldChunkZ + chunkRadius; z++) {
 //           if (Math.abs(x - newChunkX) > chunkRadius || Math.abs(z - newChunkZ) > chunkRadius) {
 //              // Unload the chunk if it's out of the range
 //              Chunk chunk = chunkProvider.loadChunk(x, z);
 //              if (chunk != null) {
 //                 chunkProvider.unloadChunksIfNotNearSpawn(x, z);
 //              }
 //           }
 //        }
 //     }
 //  }
   //end of newshit

   public void addChatMessage(IChatComponent var1) {}

   public boolean canCommandSenderUseCommand(int var1, String var2) {
      return false;
   }

   public ChunkCoordinates getPlayerCoordinates() {
      return null;
   }

   public void sendChatToPlayer(ChatMessageComponent chatmessagecomponent) {}
}
