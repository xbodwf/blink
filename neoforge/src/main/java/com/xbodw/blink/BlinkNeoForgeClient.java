package com.xbodw.blink;

import com.xbodw.blink.client.FillerClientHandler;
import com.xbodw.blink.client.FillerRenderer;
import com.xbodw.blink.client.FillerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = Blink.MOD_ID, value = Dist.CLIENT)
public class BlinkNeoForgeClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        FillerClientHandler.init();
        NeoForge.EVENT_BUS.addListener(BlinkNeoForgeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(BlinkNeoForgeClient::onRenderLevelStage);
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(Blink.FILLER_MENU_TYPE, FillerScreen::new);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        FillerClientHandler.tick();
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            MultiBufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            FillerRenderer.renderFillerEffects(event.getPoseStack(), bufferSource);
        }
    }
}
