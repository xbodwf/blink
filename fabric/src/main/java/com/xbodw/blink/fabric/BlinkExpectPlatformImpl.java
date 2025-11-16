package com.xbodw.blink.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class BlinkExpectPlatformImpl {
    /**
     * This is our actual method to {@link BlinkExpectPlatform#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
}