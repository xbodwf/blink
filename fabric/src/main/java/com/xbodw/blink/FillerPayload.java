package com.xbodw.blink;

import com.xbodw.blink.network.FillerModePacket;
import com.xbodw.blink.network.NetworkHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FillerPayload(FillerModePacket packet) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FillerPayload> TYPE = new CustomPacketPayload.Type<>(NetworkHandler.FILLER_MODE_PACKET);

    public static final StreamCodec<ByteBuf, FillerPayload> CODEC = StreamCodec.of(
            (buf, payload) -> payload.packet.write(new FriendlyByteBuf(buf)),
            buf -> new FillerPayload(new FillerModePacket(new FriendlyByteBuf(buf)))
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public void write(FriendlyByteBuf buf) {
        packet.write(buf);
    }
}
