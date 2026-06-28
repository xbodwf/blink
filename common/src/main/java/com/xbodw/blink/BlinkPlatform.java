package com.xbodw.blink;

import com.xbodw.blink.network.FillerModePacket;
import java.nio.file.Path;

public class BlinkPlatform {
    private static Impl impl;

    public static void setImpl(Impl impl) {
        BlinkPlatform.impl = impl;
    }

    public static Path getConfigDirectory() {
        return impl.getConfigDirectory();
    }

    public static void sendToServer(FillerModePacket packet) {
        impl.sendToServer(packet);
    }

    public interface Impl {
        Path getConfigDirectory();
        void sendToServer(FillerModePacket packet);
    }
}
