package mcheli.aircraft;

import net.minecraft.world.World;

/**
 * Legacy persistent pilot-seat entity registration.
 *
 * Keeping this as a distinct class prevents Forge's class-to-registration map from
 * replacing the MCH.E.PSeat registration with MCH.E.HitBox (or vice versa).
 */
public class MCH_EntityPSeat extends MCH_EntityHitBox {

   public MCH_EntityPSeat(World world) {
      super(world);
   }
}
