package com.xbodw.blink.item;

import com.xbodw.blink.block.BlinkPortalActivator;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class DimensionKeyItem extends Item {
    public DimensionKeyItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        
        if (!level.isClientSide && player != null) {
            // 尝试激活传送门
            if (BlinkPortalActivator.tryActivatePortal(level, context.getClickedPos())) {
                // 激活成功，消耗钥匙
                ItemStack stack = context.getItemInHand();
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                
                // 播放成功音效
                level.playSound(null, context.getClickedPos(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
                
                // 发送成功消息
                player.sendSystemMessage(Component.literal("§a传送门已激活！"));
                
                return InteractionResult.SUCCESS;
            } else {
                // 激活失败，播放失败音效
                level.playSound(null, context.getClickedPos(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.5F, 0.8F);
                
                // 发送失败消息
                player.sendSystemMessage(Component.literal("§c未找到有效的传送门框架！"));
                player.sendSystemMessage(Component.literal("§7请用强化深板岩搭建4x5的传送门框架"));
                
                return InteractionResult.FAIL;
            }
        }
        
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6维度钥匙"));
        tooltip.add(Component.literal("§7用于激活Blink维度传送门"));
        tooltip.add(Component.literal("§7用强化深板岩搭建4x5框架"));
        tooltip.add(Component.literal("§7然后右键框架激活传送门"));
        tooltip.add(Component.literal("§e稀有物品 - 小心使用"));
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // 添加附魔光效
    }
}