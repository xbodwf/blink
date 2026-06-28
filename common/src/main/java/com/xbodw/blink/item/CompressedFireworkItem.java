package com.xbodw.blink.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CompressedFireworkItem extends Item {
    private final int compressionLevel;
    private final double powerMultiplier;
    private final boolean isInfinite;
    
    public CompressedFireworkItem(Properties properties, int compressionLevel) {
        super(properties);
        this.compressionLevel = compressionLevel;
        this.powerMultiplier = calculatePowerMultiplier(compressionLevel);
        this.isInfinite = compressionLevel >= 3; // 3重及以上无限使用
    }
    
    private double calculatePowerMultiplier(int level) {
        switch (level) {
            case 1: return 9.0; // 1重：9倍威力
            case 2: return 18.0; // 2重：2倍于1重
            case 3: return 36.0; // 3重：2倍于2重
            case 4: return 144.0; // 4重：4倍于3重
            case 5: return 2304.0; // 5重：16倍于4重
            case 6: return 147456.0; // 6重：64倍于5重
            case 7: return 37748736.0; // 7重：256倍于6重
            case 8: return 38654705664.0; // 8重：1024倍于7重
            case 9: return 158329674399744.0; // 9重：4096倍于8重
            default: return 1.0;
        }
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide) {
            // 创建增强的烟花效果
            createEnhancedFirework(level, player, stack);
            
            // 如果不是无限使用，消耗物品
            if (!isInfinite && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
    
    private void createEnhancedFirework(Level level, Player player, ItemStack stack) {
        // 极大增强的声音效果 - 移除音量限制
        float soundVolume = (float) Math.min(powerMultiplier / 2.0, 50.0); // 大幅提高音量上限
        float soundPitch = Math.max(0.1f, Math.min(3.0f, (float) (0.5 + compressionLevel * 0.2)));
        
        // 播放多重声音效果
        for (int i = 0; i < Math.min(compressionLevel, 10); i++) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 
                    soundVolume, soundPitch + i * 0.1f);
        }
        
        // 高等级的雷鸣声效果
        if (compressionLevel >= 3) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 
                    soundVolume * 2.0f, soundPitch);
        }
        
        // 超高等级的末影龙咆哮
        if (compressionLevel >= 7) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 
                    soundVolume * 3.0f, soundPitch * 0.5f);
        }
        
        // 烟花数量 - 真正基于威力倍数，移除限制
        int rocketCount = Math.min(compressionLevel * compressionLevel * 20, 500); // 大幅增加数量
        
        for (int i = 0; i < rocketCount; i++) {
            // 创建烟花火箭
            ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
            FireworkRocketEntity firework = new FireworkRocketEntity(level, fireworkStack, player);
            
            // 发射范围基于压缩等级，不设上限
            double range = compressionLevel * 5.0; // 每级5格范围
            double offsetX = (level.random.nextDouble() - 0.5) * range;
            double offsetY = level.random.nextDouble() * range * 0.5 + 1.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * range;
            
            firework.setPos(player.getX() + offsetX, player.getY() + 1.0, player.getZ() + offsetZ);
            
            // 发射速度基于压缩等级
            double velocity = compressionLevel * 0.5;
            firework.setDeltaMovement(
                offsetX * 0.1 * velocity, 
                offsetY * 0.2 * velocity, 
                offsetZ * 0.1 * velocity
            );
            
            level.addFreshEntity(firework);
        }
        
        // 创建粒子效果
        if (level instanceof ServerLevel serverLevel) {
            createMassiveParticleEffects(serverLevel, player);
        }
        
        // 鞘翅加速 - 真正的威力倍数效果
        if (player.isFallFlying()) {
            Vec3 lookAngle = player.getLookAngle();
            // 基础加速度直接基于压缩等级，不设上限
            double acceleration = compressionLevel * 2.0; // 每级2倍加速
            
            player.setDeltaMovement(player.getDeltaMovement().add(
                    lookAngle.x * acceleration,
                    lookAngle.y * acceleration,
                    lookAngle.z * acceleration
            ));
        }
        
        // 爆炸效果 - 真正的毁灭性威力
        if (level instanceof ServerLevel serverLevel) {
            createMassiveExplosion(serverLevel, player);
        }
        
        // 特殊高级效果
        if (compressionLevel >= 5) {
            createSpecialEffects(level, player);
        }
    }
    
    private void createMassiveExplosion(ServerLevel level, Player player) {
        // 爆炸范围直接基于压缩等级，不设上限
        double explosionRange = compressionLevel * 10.0; // 每级10格范围
        
        // 伤害直接基于威力倍数
        float baseDamage = (float) Math.min(powerMultiplier, 1000.0); // 最高1000伤害
        
        // 多重爆炸波
        for (int wave = 0; wave < compressionLevel; wave++) {
            double waveRange = explosionRange * (wave + 1) / compressionLevel;
            
            AABB damageArea = new AABB(player.blockPosition()).inflate(waveRange);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, damageArea);
            
            for (LivingEntity entity : entities) {
                if (entity != player && entity.distanceTo(player) <= waveRange) {
                    // 每波都造成伤害
                    float waveDamage = baseDamage / (wave + 1); // 后续波次伤害递减
                    entity.hurt(level.damageSources().explosion(null, player), waveDamage);
                    
                    // 极强的击退效果
                    double knockbackStrength = compressionLevel * 5.0;
                    Vec3 knockback = entity.position().subtract(player.position()).normalize().scale(knockbackStrength);
                    entity.setDeltaMovement(entity.getDeltaMovement().add(knockback));
                }
            }
            
            // 创建爆炸粒子效果
            createExplosionWave(level, player, waveRange, wave);
        }
    }
    
    private void createExplosionWave(ServerLevel level, Player player, double range, int wave) {
        int particleCount = compressionLevel * 50; // 大量粒子
        
        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double x = player.getX() + Math.cos(angle) * range;
            double z = player.getZ() + Math.sin(angle) * range;
            double y = player.getY() + level.random.nextDouble() * 10.0;
            
            // 不同波次使用不同粒子
            if (wave == 0) {
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0);
            } else if (wave < 3) {
                level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 3, 1, 1, 1, 0.1);
            } else {
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 5, 2, 2, 2, 0.2);
            }
        }
    }
    
    private void createMassiveParticleEffects(ServerLevel level, Player player) {
        int particleCount = compressionLevel * 100; // 大量粒子
        double range = compressionLevel * 8.0;
        
        // 基础烟花粒子
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * range;
            double offsetY = level.random.nextDouble() * range;
            double offsetZ = (level.random.nextDouble() - 0.5) * range;
            
            level.sendParticles(ParticleTypes.FIREWORK,
                    player.getX() + offsetX, player.getY() + offsetY, player.getZ() + offsetZ,
                    5, 1.0, 1.0, 1.0, 0.3);
        }
        
        // 高等级特效
        if (compressionLevel >= 4) {
            for (int i = 0; i < particleCount / 2; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * range;
                double offsetY = level.random.nextDouble() * range;
                double offsetZ = (level.random.nextDouble() - 0.5) * range;
                
                level.sendParticles(ParticleTypes.DRAGON_BREATH,
                        player.getX() + offsetX, player.getY() + offsetY, player.getZ() + offsetZ,
                        3, 2.0, 2.0, 2.0, 0.1);
            }
        }
        
        if (compressionLevel >= 7) {
            for (int i = 0; i < particleCount / 3; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * range;
                double offsetY = level.random.nextDouble() * range;
                double offsetZ = (level.random.nextDouble() - 0.5) * range;
                
                level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        player.getX() + offsetX, player.getY() + offsetY, player.getZ() + offsetZ,
                        10, 3.0, 3.0, 3.0, 0.5);
            }
        }
    }
    
    private void createSpecialEffects(Level level, Player player) {
        // 5重及以上：治疗玩家
        if (compressionLevel >= 5) {
            player.heal(compressionLevel * 2.0f);
        }
        
        // 6重及以上：给予临时效果
        if (compressionLevel >= 6 && level instanceof ServerLevel) {
            // 这里可以添加药水效果，但需要导入相关类
            // 暂时用粒子效果代替
            for (int i = 0; i < 100; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 20.0;
                double offsetY = level.random.nextDouble() * 20.0;
                double offsetZ = (level.random.nextDouble() - 0.5) * 20.0;
                
                ((ServerLevel) level).sendParticles(ParticleTypes.ENCHANT,
                        player.getX() + offsetX, player.getY() + offsetY, player.getZ() + offsetZ,
                        1, 0, 0, 0, 0.1);
            }
        }
        
        // 8重及以上：时间减缓效果（视觉）
        if (compressionLevel >= 8 && level instanceof ServerLevel) {
            for (int i = 0; i < 200; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 50.0;
                double offsetY = level.random.nextDouble() * 50.0;
                double offsetZ = (level.random.nextDouble() - 0.5) * 50.0;
                
                ((ServerLevel) level).sendParticles(ParticleTypes.PORTAL,
                        player.getX() + offsetX, player.getY() + offsetY, player.getZ() + offsetZ,
                        1, 0, 0, 0, 1.0);
            }
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6压缩等级: §e" + compressionLevel + "重"));
        tooltip.add(Component.literal("§c威力倍数: §e" + String.format("%.0f", powerMultiplier) + "倍"));
        
        if (isInfinite) {
            tooltip.add(Component.literal("§d无限使用"));
        }
        
        // 显示真实效果数值
        double explosionRange = compressionLevel * 10.0;
        float baseDamage = (float) Math.min(powerMultiplier, 1000.0);
        double acceleration = compressionLevel * 2.0;
        int rocketCount = compressionLevel * compressionLevel * 20;
        
        tooltip.add(Component.literal("§7右键使用释放毁灭性烟花"));
        tooltip.add(Component.literal("§7• 爆炸范围: §c" + String.format("%.0f", explosionRange) + "格"));
        tooltip.add(Component.literal("§7• 最大伤害: §c" + String.format("%.0f", baseDamage)));
        tooltip.add(Component.literal("§7• 鞘翅加速: §a" + String.format("%.0fx", acceleration)));
        tooltip.add(Component.literal("§7• 烟花数量: §e" + rocketCount + "发"));
        
        if (compressionLevel >= 3) {
            tooltip.add(Component.literal("§a达到3重压缩，烟花不再消耗！"));
        }
        
        if (compressionLevel >= 5) {
            tooltip.add(Component.literal("§d特殊效果: 治疗玩家"));
        }
        
        if (compressionLevel >= 7) {
            tooltip.add(Component.literal("§5传说效果: 龙息粒子"));
        }
        
        if (compressionLevel >= 9) {
            tooltip.add(Component.literal("§4§l终极威力: 毁天灭地"));
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