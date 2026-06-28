package com.xbodw.blink.neoforge;

import com.xbodw.blink.BlinkPlatform;
import com.xbodw.blink.network.FillerModePacket;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.nio.file.Path;

public class BlinkPlatformImpl implements BlinkPlatform.Impl {
    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void sendToServer(FillerModePacket packet) {
        PacketDistributor.sendToServer(new FillerPayload(packet));
    }
}
