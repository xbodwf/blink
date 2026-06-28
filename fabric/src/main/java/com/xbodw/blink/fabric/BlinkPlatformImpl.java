package com.xbodw.blink.fabric;

import com.xbodw.blink.BlinkPlatform;
import com.xbodw.blink.FillerPayload;
import com.xbodw.blink.network.FillerModePacket;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.nio.file.Path;

public class BlinkPlatformImpl implements BlinkPlatform.Impl {
    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void sendToServer(FillerModePacket packet) {
        ClientPlayNetworking.send(new FillerPayload(packet));
    }
}
