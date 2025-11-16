package com.xbodw.blink.event;

import com.xbodw.blink.Blink;
import com.xbodw.blink.block.BlinkPortalActivator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class PortalActivationHandler {
    
    public static void onItemDropped(ItemEntity itemEntity) {
        Level level = itemEntity.level();
        ItemStack stack = itemEntity.getItem();
        BlockPos pos = itemEntity.blockPosition();
        
        // 检查是否是维度钥匙
        if (stack.is(Blink.DIMENSION_KEY.get())) {
            // 检查下方是否是水
            if (level.getBlockState(pos).is(Blocks.WATER)) {
                // 尝试激活传送门
                if (BlinkPortalActivator.tryActivatePortal(level, pos)) {
                    // 消耗维度钥匙
                    itemEntity.discard();
                }
            }
        }
    }
}