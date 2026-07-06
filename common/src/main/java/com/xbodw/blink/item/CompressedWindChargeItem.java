package com.xbodw.blink.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CompressedWindChargeItem extends Item {
    private final int level;

    public CompressedWindChargeItem(Properties properties, int level) {
        super(properties);
        this.level = level;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            int count = this.level;
            Vec3 look = player.getLookAngle();
            for (int i = 0; i < count; i++) {
                WindCharge windCharge = new WindCharge(player, level, player.getX(), player.getEyeY(), player.getZ());

                double spread = (level.random.nextDouble() - 0.5) * 0.05;
                double ySpread = (level.random.nextDouble() - 0.5) * 0.05;
                windCharge.setDeltaMovement(
                    look.x * 1.5 + spread,
                    look.y * 1.5 + ySpread,
                    look.z * 1.5 + spread
                );

                level.addFreshEntity(windCharge);
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS,
            Math.min(1.0F + this.level * 0.5F, 4.0F), 1.0F);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6压缩等级: §e" + level + "级"));
        tooltip.add(Component.literal("§7发射 " + level + " 个风弹"));
        tooltip.add(Component.literal("§7右键发射压缩风弹"));
    }

    public int getCompressionLevel() {
        return level;
    }
}
