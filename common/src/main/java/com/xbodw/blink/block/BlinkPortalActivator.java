package com.xbodw.blink.block;

import com.xbodw.blink.Blink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BlinkPortalActivator {
    
    /**
     * 尝试激活传送门框架
     * @param level 世界
     * @param pos 点击的位置
     * @return 是否成功激活
     */
    public static boolean tryActivatePortal(Level level, BlockPos pos) {
        // 检查垂直方向的传送门框架
        BlockPos framePos = findPortalFrame(level, pos, Direction.Axis.Z);
        if (framePos != null) {
            activatePortal(level, framePos, Direction.Axis.Z);
            return true;
        }
        
        // 检查水平方向的传送门框架
        framePos = findPortalFrame(level, pos, Direction.Axis.X);
        if (framePos != null) {
            activatePortal(level, framePos, Direction.Axis.X);
            return true;
        }
        
        return false;
    }
    
    /**
     * 查找传送门框架
     * @param level 世界
     * @param pos 起始位置
     * @param axis 框架方向
     * @return 框架的左下角位置，如果没找到返回null
     */
    private static BlockPos findPortalFrame(Level level, BlockPos pos, Direction.Axis axis) {
        // 搜索范围：在点击位置周围寻找可能的框架
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos testPos = pos.offset(dx, dy, dz);
                    if (isValidPortalFrame(level, testPos, axis)) {
                        return testPos;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * 检查是否是有效的传送门框架
     * @param level 世界
     * @param cornerPos 框架左下角位置
     * @param axis 框架方向
     * @return 是否有效
     */
    private static boolean isValidPortalFrame(Level level, BlockPos cornerPos, Direction.Axis axis) {
        // 传送门尺寸：4x5 (宽x高)，内部3x4空间
        int width = 4;
        int height = 5;
        
        // 检查框架方块（强化深板岩）
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                BlockPos frameBlockPos;
                if (axis == Direction.Axis.X) {
                    frameBlockPos = cornerPos.offset(i, j, 0);
                } else {
                    frameBlockPos = cornerPos.offset(0, j, i);
                }
                
                boolean shouldBeFrame = (i == 0 || i == width - 1) || (j == 0 || j == height - 1);
                BlockState state = level.getBlockState(frameBlockPos);
                
                if (shouldBeFrame) {
                    // 框架位置应该是强化深板岩
                    if (!state.is(Blocks.REINFORCED_DEEPSLATE)) {
                        return false;
                    }
                } else {
                    // 内部应该是空气
                    if (!state.isAir()) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * 激活传送门
     * @param level 世界
     * @param cornerPos 框架左下角位置
     * @param axis 框架方向
     */
    private static void activatePortal(Level level, BlockPos cornerPos, Direction.Axis axis) {
        // 在框架内部放置传送门方块
        for (int i = 1; i < 3; i++) { // 内部宽度3
            for (int j = 1; j < 4; j++) { // 内部高度4
                BlockPos portalPos;
                if (axis == Direction.Axis.X) {
                    portalPos = cornerPos.offset(i, j, 0);
                } else {
                    portalPos = cornerPos.offset(0, j, i);
                }
                level.setBlock(portalPos, Blink.BLINK_PORTAL_BLOCK.get().defaultBlockState(), 3);
            }
        }
        
        // 播放激活声音和效果
        BlockPos centerPos = cornerPos.offset(2, 2, axis == Direction.Axis.X ? 0 : 2);
        level.playSound(null, centerPos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, centerPos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
        
        // 添加粒子效果
        for (int i = 0; i < 30; i++) {
            double x = centerPos.getX() + (level.random.nextDouble() - 0.5) * 3;
            double y = centerPos.getY() + (level.random.nextDouble() - 0.5) * 4;
            double z = centerPos.getZ() + (level.random.nextDouble() - 0.5) * 3;
            
            if (level.isClientSide) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT, x, y, z, 0, 0.1, 0);
                level.addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL, x, y, z, 0, 0.05, 0);
            }
        }
    }
}