/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  com.google.common.io.ByteArrayDataInput
 *  com.google.common.io.ByteStreams
 *  cpw.mods.fml.common.network.simpleimpl.IMessage
 *  io.netty.buffer.ByteBuf
 */
package mcheli.wrapper;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class W_PacketBase
implements IMessage {
    ByteArrayDataInput data;

    public byte[] createData() {
        return null;
    }

    public void fromBytes(ByteBuf buf) {
        byte[] dst = new byte[buf.array().length - 1];
        buf.getBytes(0, dst);
        this.data = ByteStreams.newDataInput((byte[])dst);
    }

    public void toBytes(ByteBuf buf) {
        buf.writeBytes(this.createData());
    }
}

