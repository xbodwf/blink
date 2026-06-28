package com.xbodw.blink;

import com.xbodw.blink.fabric.BlinkPlatformImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class BlinkFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BlinkPlatform.setImpl(new BlinkPlatformImpl());
        Blink.fabricRegisterAll();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            Blink.registerCommands(dispatcher, registryAccess);
        });

        PayloadTypeRegistry.playC2S().register(FillerPayload.TYPE, FillerPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FillerPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                payload.packet().handle(context.player());
            });
        });
    }
}
