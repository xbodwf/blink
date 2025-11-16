package com.xbodw.blink;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class BlinkExpectPlatformImpl {
    /**
     * This is our actual method to {@link BlinkExpectPlatform#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}