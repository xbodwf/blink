package com.xbodw.blink.neoforge;

import com.xbodw.blink.network.NetworkHandler;
import com.xbodw.blink.network.FillerPickBlockPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FillerPickBlockPayload(FillerPickBlockPacket packet) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FillerPickBlockPayload> TYPE = new CustomPacketPayload.Type<>(NetworkHandler.FILLER_PICK_BLOCK_PACKET);

    public static final StreamCodec<FriendlyByteBuf, FillerPickBlockPayload> STREAM_CODEC = StreamCodec.of(
        (FriendlyByteBuf buf, FillerPickBlockPayload payload) -> payload.packet.write(buf),
        (FriendlyByteBuf buf) -> new FillerPickBlockPayload(new FillerPickBlockPacket(buf))
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
