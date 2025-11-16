package com.xbodw.blink;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class BlinkFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Blink.init();
        
        // 注册指令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            Blink.registerCommands(dispatcher, registryAccess);
        });
    }
}