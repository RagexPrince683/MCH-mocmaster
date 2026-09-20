package mcheli.aircraft;

import java.util.UUID;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;

/** Optional Forge event, never a request to move the player. See docs/dismount-contract.md. */
public final class MCH_DismountEvent extends Event {
    public final EntityPlayer player;
    public final UUID vehicleUUID;
    public final int vehicleId;
    public final int seatId;
    public final int operationId;
    public final double feetX, feetY, feetZ;
    public final boolean clientAccepted;

    public MCH_DismountEvent(EntityPlayer player, UUID vehicleUUID, int vehicleId, int seatId,
                             int operationId, Vec3 feet, boolean clientAccepted) {
        this.player = player;
        this.vehicleUUID = vehicleUUID;
        this.vehicleId = vehicleId;
        this.seatId = seatId;
        this.operationId = operationId;
        this.feetX = feet.xCoord;
        this.feetY = feet.yCoord;
        this.feetZ = feet.zCoord;
        this.clientAccepted = clientAccepted;
    }
}
