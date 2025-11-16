package com.xbodw.blink;

import com.xbodw.blink.client.FillerClientHandler;
import com.xbodw.blink.client.FillerRenderer;
import com.xbodw.blink.client.FillerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Blink.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BlinkForgeClient {
    public static void init() {
        FillerClientHandler.init();
        MinecraftForge.EVENT_BUS.register(BlinkForgeClient.class);
    }
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册客户端屏幕
            MenuScreens.register(Blink.FILLER_MENU_TYPE.get(), FillerScreen::new);
        });
    }
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            FillerClientHandler.tick();
        }
    }
    
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            MultiBufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            FillerRenderer.renderFillerEffects(event.getPoseStack(), bufferSource);
        }
    }
}