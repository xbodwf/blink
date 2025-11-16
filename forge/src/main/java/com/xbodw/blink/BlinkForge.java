package com.xbodw.blink;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Blink.MOD_ID)
public class BlinkForge {
    public BlinkForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(Blink.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        Blink.init();
        
        // 注册指令
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        
        // 初始化客户端
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> BlinkForgeClient::init);
    }
    
    private void onRegisterCommands(RegisterCommandsEvent event) {
        Blink.registerCommands(event.getDispatcher(), event.getBuildContext());
    }
}
