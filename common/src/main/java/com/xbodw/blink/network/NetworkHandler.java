package com.xbodw.blink.network;

import com.xbodw.blink.Blink;
import com.xbodw.blink.BlinkPlatform;
import net.minecraft.resources.ResourceLocation;

public class NetworkHandler {
    public static final ResourceLocation FILLER_MODE_PACKET = ResourceLocation.fromNamespaceAndPath(Blink.MOD_ID, "filler_mode");

    public static void sendToServer(FillerModePacket packet) {
        BlinkPlatform.sendToServer(packet);
    }
}
