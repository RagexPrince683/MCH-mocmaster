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

public class MCH_PacketBaseVehicleLocation extends MCH_Packet {
    public double x,y,z;
    public double rotX, rotY, rotZ;
    public String model;
    public String texture;
    public int entityId;


    @Override
    public void readData(ByteArrayDataInput byteArrayDataInput) {
        x = byteArrayDataInput.readDouble();
        y = byteArrayDataInput.readDouble();
        z = byteArrayDataInput.readDouble();


        rotX = byteArrayDataInput.readDouble();
        rotY = byteArrayDataInput.readDouble();
        rotZ = byteArrayDataInput.readDouble();

        entityId = byteArrayDataInput.readInt();


        model = byteArrayDataInput.readUTF();

        texture = byteArrayDataInput.readUTF();
    }

    @Override
    public void writeData(DataOutputStream dataOutputStream) {
        try {
            dataOutputStream.writeDouble(x);
            dataOutputStream.writeDouble(y);
            dataOutputStream.writeDouble(z);


            dataOutputStream.writeDouble(rotX);
            dataOutputStream.writeDouble(rotY);
            dataOutputStream.writeDouble(rotZ);

            dataOutputStream.writeInt(entityId);

            dataOutputStream.writeUTF(model);
            dataOutputStream.writeUTF(texture);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void send(MCH_EntityBaseVehicle ac, EntityPlayer target) {
        if(target != null && ac != null) {
            if(!(ac instanceof MCP_EntityPlane)){return;}
            MCH_PacketBaseVehicleLocation s = new MCH_PacketBaseVehicleLocation();

            s.x = ac.posX;
            s.y = ac.posY;
            s.z = ac.posZ;

            s.rotX = ac.rotationRoll;
            s.rotY = ac.rotationPitch;
            s.rotZ = ac.rotationYaw;

            s.model = ac.getAcInfo().name;
            try {
                s.texture = "textures/planes/" + ac.getTextureName() + ".png";
            } catch (Exception exception) {
                System.out.println("Texture not found : " + ac.getTextureName());
                s.texture = "textures/blocks/planks_oak.png";
            }

            s.entityId = ac.getEntityId();

            W_Network.sendToPlayer(s, target);
        }
    }

    @Override
    public int getMessageID() {
        return 536875026;
    }
}