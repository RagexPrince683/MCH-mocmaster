package mcheli.mixin;

import mcheli.MCH_DismountDiagnostics;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Trace the vanilla Sneak action before it changes a mounted player's Sneak state. */
@Mixin(NetHandlerPlayServer.class)
public abstract class NetHandlerPlayServerMixin {
   @Shadow public EntityPlayerMP playerEntity;

   @Inject(method = "processEntityAction(Lnet/minecraft/network/play/client/C0BPacketEntityAction;)V",
         at = @At("HEAD"), require = 1)
   private void mcheli$traceMountedSneakAction(C0BPacketEntityAction packet, CallbackInfo callbackInfo) {
      int action = packet.func_149513_d();
      if((action == 1 || action == 2) && this.playerEntity != null
            && MCH_DismountDiagnostics.parent(this.playerEntity.ridingEntity) != null) {
         MCH_DismountDiagnostics.log(this.playerEntity.worldObj,
               "session=%s vanilla-player-action action=%s player=%s mount=%s parent=%s seat=%d",
               MCH_DismountDiagnostics.session(this.playerEntity),
               action == 1 ? "START_SNEAKING" : "STOP_SNEAKING",
               MCH_DismountDiagnostics.identity(this.playerEntity),
               MCH_DismountDiagnostics.identity(this.playerEntity.ridingEntity),
               MCH_DismountDiagnostics.identity(MCH_DismountDiagnostics.parent(this.playerEntity.ridingEntity)),
               Integer.valueOf(MCH_DismountDiagnostics.seat(this.playerEntity.ridingEntity)));
      }
   }
}
