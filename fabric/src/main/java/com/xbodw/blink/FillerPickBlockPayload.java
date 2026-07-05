package com.xbodw.blink;

import com.xbodw.blink.network.FillerPickBlockPacket;
import com.xbodw.blink.network.NetworkHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FillerPickBlockPayload(FillerPickBlockPacket packet) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FillerPickBlockPayload> TYPE = new CustomPacketPayload.Type<>(NetworkHandler.FILLER_PICK_BLOCK_PACKET);

    public static final StreamCodec<ByteBuf, FillerPickBlockPayload> CODEC = StreamCodec.of(
            (buf, payload) -> payload.packet.write(new FriendlyByteBuf(buf)),
            buf -> new FillerPickBlockPayload(new FillerPickBlockPacket(new FriendlyByteBuf(buf)))
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void write(FriendlyByteBuf buf) {
        packet.write(buf);
    }
}
