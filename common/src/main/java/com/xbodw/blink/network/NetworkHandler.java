package com.xbodw.blink.network;

import com.xbodw.blink.Blink;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import io.netty.buffer.Unpooled;

public class NetworkHandler {
    public static final ResourceLocation FILLER_MODE_PACKET = new ResourceLocation(Blink.MOD_ID, "filler_mode");
    
    public static void init() {
        // 注册服务端接收器
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, FILLER_MODE_PACKET, (buf, context) -> {
            FillerModePacket packet = new FillerModePacket(buf);
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                    packet.handle(serverPlayer);
                }
            });
        });
    }
    
    public static void sendToServer(FillerModePacket packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.write(buf);
        NetworkManager.sendToServer(FILLER_MODE_PACKET, buf);
    }
}