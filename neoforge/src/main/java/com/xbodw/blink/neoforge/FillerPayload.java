package com.xbodw.blink.neoforge;

import com.xbodw.blink.network.NetworkHandler;
import com.xbodw.blink.network.FillerModePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FillerPayload(FillerModePacket packet) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FillerPayload> TYPE = new CustomPacketPayload.Type<>(NetworkHandler.FILLER_MODE_PACKET);

    public static final StreamCodec<FriendlyByteBuf, FillerPayload> STREAM_CODEC = StreamCodec.of(
        (FriendlyByteBuf buf, FillerPayload payload) -> payload.packet.write(buf),
        (FriendlyByteBuf buf) -> new FillerPayload(new FillerModePacket(buf))
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
