package com.xbodw.blink;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

public class BlinkExpectPlatform {
    @ExpectPlatform
    public static Path getConfigDirectory() {
        // Just throw an error, the content should get replaced at runtime.
        throw new AssertionError();
    }
}