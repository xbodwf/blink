package com.xbodw.blink.client;

import com.xbodw.blink.Blink;
import com.xbodw.blink.item.FillerState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FillerClientHandler {
    private static BlockPos currentTargetPos = null;
    private static BlockPos pos1 = null;
    private static BlockPos pos2 = null;
    private static FillerState currentState = FillerState.SELECTING_POS1;
    
    public static void init() {
        System.out.println("Blink Client Handler initialized!");
    }
    
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null) {
            return;
        }
        
        // 检查玩家是否持有填充器
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        ItemStack fillerStack = null;
        if (mainHand.getItem() == Blink.FILLER_ITEM.get()) {
            fillerStack = mainHand;
        } else if (offHand.getItem() == Blink.FILLER_ITEM.get()) {
            fillerStack = offHand;
        }
        
        if (fillerStack != null) {
            updateFillerState(fillerStack);
            updateTargetPosition(player);
        } else {
            // 清除状态
            currentTargetPos = null;
            pos1 = null;
            pos2 = null;
            currentState = FillerState.SELECTING_POS1;
        }
    }
    
    private static void updateFillerState(ItemStack fillerStack) {
        CompoundTag nbt = fillerStack.getTag();
        if (nbt != null) {
            currentState = FillerState.fromString(nbt.getString("state"));
            
            if (nbt.contains("pos1")) {
                pos1 = BlockPos.of(nbt.getLong("pos1"));
            } else {
                pos1 = null;
            }
            
            if (nbt.contains("pos2")) {
                pos2 = BlockPos.of(nbt.getLong("pos2"));
            } else {
                pos2 = null;
            }
        }
    }
    
    /**
     * 改进的目标位置更新 - 支持在空气中选择位置
     */
    private static void updateTargetPosition(Player player) {
        // 扩大检测范围，支持在空气中选择
        HitResult hitResult = player.pick(10.0D, 0.0F, true);
        
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            currentTargetPos = blockHitResult.getBlockPos();
        } else if (hitResult.getType() == HitResult.Type.MISS) {
            // 如果没有击中任何方块，计算准心方向上的位置
            Vec3 eyePos = player.getEyePosition();
            Vec3 lookVec = player.getLookAngle();
            
            // 在准心方向上寻找合适的位置（距离玩家5格）
            Vec3 targetVec = eyePos.add(lookVec.scale(5.0));
            currentTargetPos = new BlockPos((int) Math.floor(targetVec.x), 
                                          (int) Math.floor(targetVec.y), 
                                          (int) Math.floor(targetVec.z));
        } else {
            currentTargetPos = null;
        }
    }
    
    public static BlockPos getCurrentTargetPos() {
        return currentTargetPos;
    }
    
    public static BlockPos getPos1() {
        return pos1;
    }
    
    public static BlockPos getPos2() {
        return pos2;
    }
    
    public static FillerState getCurrentState() {
        return currentState;
    }
    
    /**
     * 获取当前状态对应的渲染颜色
     */
    public static float[] getCurrentStateColor() {
        switch (currentState) {
            case SELECTING_POS1:
                return new float[]{0.0f, 1.0f, 0.0f}; // 绿色
            case SELECTING_POS2:
                return new float[]{0.0f, 0.0f, 1.0f}; // 蓝色
            case READY_TO_FILL:
                return new float[]{1.0f, 1.0f, 0.0f}; // 黄色
            default:
                return new float[]{1.0f, 1.0f, 1.0f}; // 白色
        }
    }
    
    /**
     * 获取区域渲染颜色
     */
    public static float[] getRegionColor() {
        switch (currentState) {
            case SELECTING_POS2:
                return new float[]{0.0f, 1.0f, 1.0f}; // 青色 - 正在选择第二个位置
            case READY_TO_FILL:
                return new float[]{1.0f, 0.5f, 0.0f}; // 橙色 - 准备填充
            default:
                return new float[]{0.5f, 0.5f, 0.5f}; // 灰色
        }
    }
}