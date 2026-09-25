package mcheli.mixin;

import mcheli.aircraft.MCH_BaseVehiclePacketHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents vanilla's server player update from detaching an early Sneaking rider. */
@Mixin(EntityPlayerMP.class)
public abstract class EntityPlayerMPMixin {

   @Inject(method = "onUpdate", at = @At("HEAD"))
   private void mcheli$guardVehicleSneakDismount(CallbackInfo callbackInfo) {
      MCH_BaseVehiclePacketHandler.suppressEarlyVanillaDismount((EntityPlayerMP)(Object)this);
   }
}
