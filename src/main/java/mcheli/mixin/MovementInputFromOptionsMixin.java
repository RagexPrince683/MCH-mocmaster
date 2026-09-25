package mcheli.mixin;

import mcheli.MCH_DismountInputGate;
import mcheli.MCH_DismountDiagnostics;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MovementInputFromOptions.class)
public abstract class MovementInputFromOptionsMixin {

   @Inject(method = "updatePlayerMoveState", at = @At("RETURN"))
   private void mcheli$filterVehicleSneak(CallbackInfo callbackInfo) {
      MCH_DismountDiagnostics.callback("MovementInputFromOptions.updatePlayerMoveState", Minecraft.getMinecraft().theWorld);
      MCH_DismountInputGate.filterVehicleSneak((MovementInput)(Object)this);
   }
}
