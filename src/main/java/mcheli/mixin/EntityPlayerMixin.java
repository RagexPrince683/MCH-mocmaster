package mcheli.mixin;

import mcheli.aircraft.MCH_BaseVehiclePacketHandler;
import mcheli.MCH_DismountDiagnostics;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Guards vanilla's actual Sneak dismount branch before it calls mountEntity(null). */
@Mixin(EntityPlayer.class)
public abstract class EntityPlayerMixin {
   @Unique private Entity mcheli$mountBefore;

   @Inject(method = "mountEntity(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), require = 1)
   private void mcheli$captureMount(Entity requestedMount, CallbackInfo callbackInfo) {
      EntityPlayer player = (EntityPlayer)(Object)this;
      MCH_DismountDiagnostics.callback("EntityPlayer.mountEntity", player.worldObj);
      this.mcheli$mountBefore = player.ridingEntity;
   }

   @Inject(method = "mountEntity(Lnet/minecraft/entity/Entity;)V", at = @At("RETURN"), require = 1)
   private void mcheli$traceMount(Entity requestedMount, CallbackInfo callbackInfo) {
      EntityPlayer player = (EntityPlayer)(Object)this;
      if(this.mcheli$mountBefore != player.ridingEntity) {
         MCH_DismountDiagnostics.observedMountEntity(player, player.ridingEntity);
         MCH_DismountDiagnostics.mountChange(player, this.mcheli$mountBefore,
               player.ridingEntity, "EntityPlayer.mountEntity", false);
      }
      this.mcheli$mountBefore = null;
   }

   @Inject(method = "updateRidden()V", at = @At("HEAD"), require = 1)
   private void mcheli$guardVehicleSneakDismount(CallbackInfo callbackInfo) {
      EntityPlayer player = (EntityPlayer)(Object)this;
      MCH_DismountDiagnostics.updateHead(player);
      MCH_BaseVehiclePacketHandler.suppressEarlyVanillaDismount(player);
   }

   @Inject(method = "updateRidden()V", at = @At("RETURN"), require = 1)
   private void mcheli$observeRiddenReference(CallbackInfo callbackInfo) {
      MCH_DismountDiagnostics.updateReturn((EntityPlayer)(Object)this);
   }
}
