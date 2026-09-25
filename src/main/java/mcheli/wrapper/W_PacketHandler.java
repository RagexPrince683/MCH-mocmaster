/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  com.google.common.io.ByteArrayDataInput
 *  cpw.mods.fml.common.network.simpleimpl.IMessage
 *  cpw.mods.fml.common.network.simpleimpl.IMessageHandler
 *  cpw.mods.fml.common.network.simpleimpl.MessageContext
 *  cpw.mods.fml.relauncher.Side
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.network.NetHandlerPlayServer
 */
package mcheli.wrapper;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.MCH_ServerTickHandler;
import mcheli.MCH_DismountDiagnostics;
import mcheli.wrapper.IPacketHandler;
import mcheli.wrapper.W_NetworkRegistry;
import mcheli.wrapper.W_PacketBase;
import mcheli.wrapper.W_PacketDummy;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;

public class W_PacketHandler
implements IPacketHandler,
IMessageHandler<W_PacketBase, W_PacketDummy> {
    public void onPacket(ByteArrayDataInput data, EntityPlayer player) {
    }

    public W_PacketDummy onMessage(W_PacketBase message, MessageContext ctx) {
        final byte[] packetBytes = message.copyPacketData();
        if (packetBytes == null) {
            return null;
        }

        if (ctx.side.isClient()) {
            MCH_MOD.proxy.scheduleClientTask(new Runnable() {
                @Override
                public void run() {
                    dispatchClientPacket(packetBytes);
                }
            });
        } else {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (isDismountPacket(packetBytes)) {
                MCH_DismountDiagnostics.log(player.worldObj,
                        "session=%s dismount-packet stage=queued packetId=%d player=%s",
                        MCH_DismountDiagnostics.session(player), Integer.valueOf(packetId(packetBytes)),
                        MCH_DismountDiagnostics.identity(player));
            }
            MCH_ServerTickHandler.scheduleServerTask(new Runnable() {
                @Override
                public void run() {
                    if (isDismountPacket(packetBytes)) {
                        MCH_DismountDiagnostics.log(player.worldObj,
                                "session=%s dismount-packet stage=game-thread packetId=%d player=%s",
                                MCH_DismountDiagnostics.session(player), Integer.valueOf(packetId(packetBytes)),
                                MCH_DismountDiagnostics.identity(player));
                    }
                    dispatchPacket(ByteStreams.newDataInput(packetBytes), player);
                }
            });
        }
        return null;
    }

    private static int packetId(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return -1;
        }
        return (bytes[0] & 255) << 24 | (bytes[1] & 255) << 16 | (bytes[2] & 255) << 8 | bytes[3] & 255;
    }

    private static boolean isDismountPacket(byte[] bytes) {
        int id = packetId(bytes);
        return id == 536875040 || id == 536879120 || id == 536903696 || id == 536903698
                || id == 537002000 || id == 537919504;
    }

    private static void dispatchClientPacket(byte[] packetBytes) {
        Entity clientPlayer = MCH_Lib.getClientPlayer();
        if (clientPlayer instanceof EntityPlayer) {
            dispatchPacket(ByteStreams.newDataInput(packetBytes), (EntityPlayer)clientPlayer);
        }
    }

    private static void dispatchPacket(ByteArrayDataInput packetData, EntityPlayer player) {
        try {
            W_NetworkRegistry.packetHandler.onPacket(packetData, player);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
