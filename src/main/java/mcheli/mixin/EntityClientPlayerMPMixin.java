package mcheli.mixin;

import mcheli.MCH_DismountInputGate;
import net.minecraft.client.entity.EntityClientPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Last-line guard immediately before vanilla publishes movement state to the server. */
@Mixin(EntityClientPlayerMP.class)
public abstract class EntityClientPlayerMPMixin {

   @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"))
   private void mcheli$guardVehicleSneakPacket(CallbackInfo callbackInfo) {
      MCH_DismountInputGate.filterCurrentPlayerSneak();
   }
}
