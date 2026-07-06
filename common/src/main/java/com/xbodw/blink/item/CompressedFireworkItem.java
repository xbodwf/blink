package com.xbodw.blink.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CompressedFireworkItem extends Item {
    private static final int MAX_PARTICLES_PER_CALL = 50;
    private static final double BASE_POWER = 9.0;

    private final int compressionLevel;
    private final double powerMultiplier;
    private final boolean isInfinite;

    public CompressedFireworkItem(Properties properties, int compressionLevel) {
        super(properties);
        this.compressionLevel = compressionLevel;
        this.powerMultiplier = compressionLevel * BASE_POWER;
        this.isInfinite = compressionLevel >= 3;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            createEnhancedFirework(level, player, stack);
            if (!isInfinite && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void createEnhancedFirework(Level level, Player player, ItemStack stack) {
        float soundVolume = Math.min((float) (powerMultiplier / 2.0), 10.0f);
        float soundPitch = Math.max(0.1f, Math.min(3.0f, (float) (0.5 + compressionLevel * 0.2)));

        for (int i = 0; i < Math.min(compressionLevel, 5); i++) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS,
                    soundVolume, soundPitch + i * 0.1f);
        }

        if (compressionLevel >= 3) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS,
                    Math.min(soundVolume * 2.0f, 10.0f), soundPitch);
        }

        if (compressionLevel >= 7) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS,
                    Math.min(soundVolume * 3.0f, 10.0f), soundPitch * 0.5f);
        }

        int rocketCount = Math.min(compressionLevel * 3, 20);
        int flightDuration = Math.min(compressionLevel * 2, 30);

        for (int i = 0; i < rocketCount; i++) {
            ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
            fireworkStack.set(DataComponents.FIREWORKS, new Fireworks(
                flightDuration,
                List.of()
            ));
            double range = compressionLevel * 5.0;
            double offsetX = (level.random.nextDouble() - 0.5) * range;
            double offsetY = level.random.nextDouble() * range * 0.5 + 1.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * range;

            FireworkRocketEntity firework = new FireworkRocketEntity(level, fireworkStack, player);
            firework.setPos(player.getX() + offsetX, player.getY() + 1.0, player.getZ() + offsetZ);

            double velocity = compressionLevel * 0.5;
            firework.setDeltaMovement(
                    offsetX * 0.1 * velocity,
                    offsetY * 0.2 * velocity,
                    offsetZ * 0.1 * velocity
            );

            level.addFreshEntity(firework);
        }

        if (level instanceof ServerLevel serverLevel) {
            createParticleEffects(serverLevel, player);
            createExplosionWave(serverLevel, player);
        }

        if (player.isFallFlying()) {
            Vec3 lookAngle = player.getLookAngle();
            double acceleration = compressionLevel * 10.0;
            player.setDeltaMovement(player.getDeltaMovement().add(
                    lookAngle.x * acceleration,
                    lookAngle.y * acceleration,
                    lookAngle.z * acceleration
            ));
        }

        if (compressionLevel >= 5) {
            createSpecialEffects(level, player);
        }
    }

    private void createExplosionWave(ServerLevel level, Player player) {
        int particleCount = Math.min(compressionLevel * 5, MAX_PARTICLES_PER_CALL);
        double range = Math.min(compressionLevel * 10.0, 50.0);

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double x = player.getX() + Math.cos(angle) * range;
            double z = player.getZ() + Math.sin(angle) * range;
            double y = player.getY() + level.random.nextDouble() * 10.0;

            level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 1, 1, 1, 0.1);
        }
    }

    private void createParticleEffects(ServerLevel level, Player player) {
        int particleCount = Math.min(compressionLevel * 5, MAX_PARTICLES_PER_CALL);
        double range = compressionLevel * 4.0;

        for (int i = 0; i < particleCount; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * range;
            double offsetY = level.random.nextDouble() * range;
            double offsetZ = (level.random.nextDouble() - 0.5) * range;

            level.sendParticles(ParticleTypes.FIREWORK,
                    player.getX() + offsetX, player.getY() + offsetY, player.getZ() + offsetZ,
                    1, 1.0, 1.0, 1.0, 0.3);
        }
    }

    private void createSpecialEffects(Level level, Player player) {
        if (compressionLevel >= 5) {
            player.heal(compressionLevel * 2.0f);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6压缩等级: §e" + compressionLevel + "重"));
        tooltip.add(Component.literal("§c威力倍数: §e" + String.format("%.0f", powerMultiplier) + "倍"));

        if (isInfinite) {
            tooltip.add(Component.literal("§d无限使用"));
        }

        double acceleration = compressionLevel * 10.0;

        tooltip.add(Component.literal("§7右键使用释放炫目烟花"));
        tooltip.add(Component.literal("§7• 鞘翅加速: §a" + String.format("%.0fx", acceleration)));

        if (compressionLevel >= 3) {
            tooltip.add(Component.literal("§a达到3重压缩，烟花不再消耗！"));
        }

        if (compressionLevel >= 5) {
            tooltip.add(Component.literal("§d特殊效果: 治疗玩家"));
        }
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public double getPowerMultiplier() {
        return powerMultiplier;
    }

    public boolean isInfinite() {
        return isInfinite;
    }
}
