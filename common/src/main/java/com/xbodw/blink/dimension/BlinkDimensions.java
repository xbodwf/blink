package com.xbodw.blink.dimension;

import com.xbodw.blink.Blink;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class BlinkDimensions {
    // 维度键 - 只定义维度键，维度类型通过数据包定义
    public static final ResourceKey<Level> BLINK_DIMENSION = 
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(Blink.MOD_ID, "blink_dimension"));
    
    public static void init() {
        // 维度类型现在通过数据包定义，这里不需要注册任何内容
        System.out.println("Blink dimension key initialized: " + BLINK_DIMENSION.location());
    }
}