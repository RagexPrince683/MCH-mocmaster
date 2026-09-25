package mcheli.mixin;

import mcheli.aircraft.MCH_BaseVehiclePacketHandler;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Guards vanilla's actual Sneak dismount branch before it calls mountEntity(null). */
@Mixin(EntityPlayer.class)
public abstract class EntityPlayerMixin {

   @Inject(method = "updateRidden", at = @At("HEAD"))
   private void mcheli$guardVehicleSneakDismount(CallbackInfo callbackInfo) {
      MCH_BaseVehiclePacketHandler.suppressEarlyVanillaDismount((EntityPlayer)(Object)this);
   }
}
