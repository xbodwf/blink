package com.xbodw.blink;

import com.xbodw.blink.client.FillerClientHandler;
import com.xbodw.blink.client.FillerRenderer;
import com.xbodw.blink.client.FillerScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gui.screens.MenuScreens;

public class BlinkFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FillerClientHandler.init();
        
        // 注册客户端屏幕
        MenuScreens.register(Blink.FILLER_MENU_TYPE.get(), FillerScreen::new);
        
        // 注册客户端tick事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            FillerClientHandler.tick();
        });
        
        // 注册世界渲染事件
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            FillerRenderer.renderFillerEffects(context.matrixStack(), context.consumers());
        });
    }
}