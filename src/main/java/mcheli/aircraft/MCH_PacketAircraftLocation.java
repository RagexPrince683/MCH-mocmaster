package mcheli.aircraft;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Packet;
import mcheli.plane.MCP_EntityPlane;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Network;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_PacketAircraftLocation extends MCH_Packet {
    public double x,y,z;
    public double rotX, rotY, rotZ;
    public String model;
    public String texture;
    public int entityId;


    @Override
    public void readData(ByteArrayDataInput var1) {
        x = var1.readDouble();
        y = var1.readDouble();
        z = var1.readDouble();


        rotX = var1.readDouble();
        rotY = var1.readDouble();
        rotZ = var1.readDouble();

        entityId = var1.readInt();


        model = var1.readUTF();

        texture = var1.readUTF();
    }

    @Override
    public void writeData(DataOutputStream var1) {
        try {
            var1.writeDouble(x);
            var1.writeDouble(y);
            var1.writeDouble(z);


            var1.writeDouble(rotX);
            var1.writeDouble(rotY);
            var1.writeDouble(rotZ);

            var1.writeInt(entityId);

            var1.writeUTF(model);
            var1.writeUTF(texture);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void send(MCH_EntityAircraft ac, EntityPlayer target) {
        if(target != null && ac != null) {
            if(!(ac instanceof MCP_EntityPlane)){return;}
            MCH_PacketAircraftLocation s = new MCH_PacketAircraftLocation();

            s.x = ac.posX;
            s.y = ac.posY;
            s.z = ac.posZ;

            s.rotX = ac.rotationRoll;
            s.rotY = ac.rotationPitch;
            s.rotZ = ac.rotationYaw;

            s.model = ac.getAcInfo().name;
            s.texture = "textures/planes/" + ac.getTextureName() + ".png";

            s.entityId = ac.getEntityId();

            W_Network.sendToPlayer(s, target);
        }
    }

    @Override
    public int getMessageID() {
        return 536875026;
    }
}