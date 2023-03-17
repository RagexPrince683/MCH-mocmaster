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
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import mcheli.MCH_Lib;
import net.minecraft.entity.player.EntityPlayer;

public class W_PacketHandler
implements IPacketHandler,
IMessageHandler<W_PacketBase, W_PacketDummy> {
    public void onPacket(ByteArrayDataInput data, EntityPlayer player) {
    }

    public W_PacketDummy onMessage(W_PacketBase message, MessageContext ctx) {
        try {
            if (message.data != null) {
                if (ctx.side.isClient()) {
                    if (MCH_Lib.getClientPlayer() != null) {
                        W_NetworkRegistry.packetHandler.onPacket(message.data, (EntityPlayer)MCH_Lib.getClientPlayer());
                    }
                } else {
                    W_NetworkRegistry.packetHandler.onPacket(message.data, (EntityPlayer)ctx.getServerHandler().playerEntity);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

