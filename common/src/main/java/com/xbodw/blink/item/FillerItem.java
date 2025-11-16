package com.xbodw.blink.item;

import com.xbodw.blink.Blink;
import com.xbodw.blink.gui.FillerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class FillerItem extends Item {
    private static final String POS1_KEY = "pos1";
    private static final String POS2_KEY = "pos2";
    private static final String FILL_BLOCK_KEY = "fillBlock";
    private static final String FILL_MODE_KEY = "fillMode";
    private static final String STATE_KEY = "state";
    
    public FillerItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        
        CompoundTag nbt = stack.getOrCreateTag();
        
        if (player.isShiftKeyDown()) {
            // Shift+右键直接打开GUI面板
            if (player instanceof ServerPlayer serverPlayer) {
                openFillerGui(serverPlayer, stack);
            }
            return InteractionResultHolder.success(stack);
        }
        
        // 改进的位置选择逻辑 - 支持在空气中选择位置
        BlockPos targetPos = getTargetPosition(player);
        if (targetPos != null) {
            InteractionResult result = handlePositionSelection(stack, player, targetPos);
            return result == InteractionResult.SUCCESS ? 
                InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
        }
        
        return InteractionResultHolder.pass(stack);
    }
    
    /**
     * 获取目标位置 - 支持在空气中选择位置
     */
    private BlockPos getTargetPosition(Player player) {
        // 扩大检测范围，支持在空气中选择
        HitResult hitResult = player.pick(10.0D, 0.0F, true); // 包含流体
        
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            return blockHitResult.getBlockPos();
        } else if (hitResult.getType() == HitResult.Type.MISS) {
            // 如果没有击中任何方块，计算准心方向上的位置
            Vec3 eyePos = player.getEyePosition();
            Vec3 lookVec = player.getLookAngle();
            
            // 在准心方向上寻找合适的位置（距离玩家5格）
            Vec3 targetVec = eyePos.add(lookVec.scale(5.0));
            return new BlockPos((int) Math.floor(targetVec.x), 
                              (int) Math.floor(targetVec.y), 
                              (int) Math.floor(targetVec.z));
        }
        
        return null;
    }
    
    private InteractionResult handlePositionSelection(ItemStack stack, Player player, BlockPos pos) {
        CompoundTag nbt = stack.getOrCreateTag();
        FillerState currentState = FillerState.fromString(nbt.getString(STATE_KEY));
        
        switch (currentState) {
            case SELECTING_POS1:
                nbt.putLong(POS1_KEY, pos.asLong());
                nbt.putString(STATE_KEY, FillerState.SELECTING_POS2.name());
                player.sendSystemMessage(Component.literal("§a位置1已设置: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
                player.sendSystemMessage(Component.literal("§7现在选择位置2..."));
                break;
                
            case SELECTING_POS2:
                nbt.putLong(POS2_KEY, pos.asLong());
                nbt.putString(STATE_KEY, FillerState.READY_TO_FILL.name());
                player.sendSystemMessage(Component.literal("§a位置2已设置: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
                
                // 显示区域信息（保留原有功能）
                if (nbt.contains(POS1_KEY)) {
                    BlockPos pos1 = BlockPos.of(nbt.getLong(POS1_KEY));
                    int volume = Math.abs(pos.getX() - pos1.getX() + 1) * 
                               Math.abs(pos.getY() - pos1.getY() + 1) * 
                               Math.abs(pos.getZ() - pos1.getZ() + 1);
                    player.sendSystemMessage(Component.literal("§b区域大小: " + volume + " 个方块"));
                    player.sendSystemMessage(Component.literal("§7再次右键开始填充..."));
                }
                break;
                
            case READY_TO_FILL:
                // 执行填充
                if (!nbt.contains(FILL_BLOCK_KEY)) {
                    player.sendSystemMessage(Component.literal("§c请先设置填充方块！(Shift+右键打开设置面板)"));
                    return InteractionResult.FAIL;
                }
                
                BlockPos pos1 = BlockPos.of(nbt.getLong(POS1_KEY));
                BlockPos pos2 = BlockPos.of(nbt.getLong(POS2_KEY));
                String blockId = nbt.getString(FILL_BLOCK_KEY);
                FillMode fillMode = FillMode.fromString(nbt.getString(FILL_MODE_KEY));
                
                // 获取填充方块
                Block fillBlock = getBlockFromId(blockId);
                if (fillBlock == null) {
                    player.sendSystemMessage(Component.literal("§c无效的填充方块！"));
                    return InteractionResult.FAIL;
                }
                
                // 执行填充，确保不会报错
                try {
                    int result = fillRegion(player.level(), player, pos1, pos2, fillBlock, fillMode);
                    if (result >= 0) {
                        if (result > 0) {
                            player.sendSystemMessage(Component.literal("§a成功处理了 " + result + " 个方块！"));
                        } else {
                            player.sendSystemMessage(Component.literal("§e没有需要处理的方块。"));
                        }
                    }
                } catch (Exception e) {
                    player.sendSystemMessage(Component.literal("§e填充完成，但可能遇到了一些问题。"));
                }
                
                // 重置状态
                nbt.putString(STATE_KEY, FillerState.SELECTING_POS1.name());
                nbt.remove(POS1_KEY);
                nbt.remove(POS2_KEY);
                player.sendSystemMessage(Component.literal("§7状态已重置，可以重新选择位置。"));
                break;
        }
        
        return InteractionResult.SUCCESS;
    }
    
    private void openFillerGui(ServerPlayer player, ItemStack stack) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("填充器设置");
            }
            
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new FillerMenu(containerId, playerInventory);
            }
        });
    }
    
    private Block getBlockFromId(String blockId) {
        for (Block block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (block.getDescriptionId().equals(blockId)) {
                return block;
            }
        }
        return null;
    }
    
    private int fillRegion(Level level, Player player, BlockPos pos1, BlockPos pos2, Block fillBlock, FillMode fillMode) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());
        
        int processed = 0;
        BlockState fillState = fillBlock.defaultBlockState();
        int totalBlocks = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        int currentBlock = 0;
        
        // 如果没有设置填充模式，默认使用创建模式
        if (fillMode == null) {
            fillMode = FillMode.FILL;
        }
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos currentPos = new BlockPos(x, y, z);
                    BlockState currentState = level.getBlockState(currentPos);
                    currentBlock++;
                    
                    // 显示进度（每100个方块显示一次）
                    if (currentBlock % 100 == 0 || currentBlock == totalBlocks) {
                        int progress = (currentBlock * 100) / totalBlocks;
                        player.sendSystemMessage(Component.literal("§6填充进度: " + progress + "% (" + currentBlock + "/" + totalBlocks + ")"));
                    }
                    
                    boolean shouldProcess = false;
                    
                    switch (fillMode) {
                        case FILL: // 创建 - 只填充空白区域
                            shouldProcess = currentState.isAir() || currentState.canBeReplaced();
                            break;
                        case REPLACE: // 覆盖 - 替换所有方块
                            shouldProcess = true;
                            break;
                        case REMOVE: // 破坏 - 只破坏指定方块
                            shouldProcess = currentState.getBlock() == fillBlock;
                            break;
                    }
                    
                    if (shouldProcess) {
                        try {
                            // 生存模式下检查并消耗物品（除了破坏模式）
                            if (!player.isCreative() && fillMode != FillMode.REMOVE) {
                                ItemStack requiredItem = new ItemStack(fillBlock.asItem());
                                if (!player.getInventory().contains(requiredItem)) {
                                    continue; // 跳过这个方块，继续处理其他的
                                }
                                // 消耗物品
                                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                                    ItemStack slotStack = player.getInventory().getItem(i);
                                    if (slotStack.getItem() == fillBlock.asItem()) {
                                        slotStack.shrink(1);
                                        break;
                                    }
                                }
                            }
                            
                            // 执行操作
                            if (fillMode == FillMode.REMOVE) {
                                level.setBlock(currentPos, Blocks.AIR.defaultBlockState(), 3);
                            } else {
                                level.setBlock(currentPos, fillState, 3);
                            }
                            processed++;
                        } catch (Exception e) {
                            // 忽略单个方块的错误，继续处理其他方块
                            continue;
                        }
                    }
                }
            }
        }
        
        return processed;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        // 更新操作说明
        tooltip.add(Component.literal("§7右键: 选择位置/执行填充"));
        tooltip.add(Component.literal("§7Shift+右键: 打开设置面板"));
        
        CompoundTag nbt = stack.getTag();
        FillerState currentState = FillerState.SELECTING_POS1;
        
        if (nbt != null) {
            currentState = FillerState.fromString(nbt.getString(STATE_KEY));
            
            // 显示当前状态
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§7当前状态: §e" + currentState.getDisplayName()));
            tooltip.add(Component.literal("§7" + currentState.getDescription()));
            
            // 保留原有的位置和方块信息显示
            tooltip.add(Component.literal(""));
            if (nbt.contains(POS1_KEY)) {
                BlockPos pos1 = BlockPos.of(nbt.getLong(POS1_KEY));
                tooltip.add(Component.literal("§a位置1: " + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ()));
            }
            if (nbt.contains(POS2_KEY)) {
                BlockPos pos2 = BlockPos.of(nbt.getLong(POS2_KEY));
                tooltip.add(Component.literal("§a位置2: " + pos2.getX() + ", " + pos2.getY() + ", " + pos2.getZ()));
                
                // 如果两个位置都设置了，显示区域大小
                if (nbt.contains(POS1_KEY)) {
                    BlockPos pos1 = BlockPos.of(nbt.getLong(POS1_KEY));
                    int volume = Math.abs(pos2.getX() - pos1.getX() + 1) * 
                               Math.abs(pos2.getY() - pos1.getY() + 1) * 
                               Math.abs(pos2.getZ() - pos1.getZ() + 1);
                    tooltip.add(Component.literal("§b区域大小: " + volume + " 个方块"));
                }
            }
            if (nbt.contains(FILL_BLOCK_KEY)) {
                String blockId = nbt.getString(FILL_BLOCK_KEY);
                // 尝试获取方块的友好名称
                Block block = getBlockFromId(blockId);
                String displayName = block != null ? block.getName().getString() : blockId;
                tooltip.add(Component.literal("§b填充方块: " + displayName));
            }
            if (nbt.contains(FILL_MODE_KEY)) {
                FillMode fillMode = FillMode.fromString(nbt.getString(FILL_MODE_KEY));
                tooltip.add(Component.literal("§d填充模式: " + fillMode.getDisplayName()));
                tooltip.add(Component.literal("§7" + fillMode.getDescription()));
            }
        } else {
            // 如果没有NBT数据，显示基本状态
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§7当前状态: §e" + currentState.getDisplayName()));
            tooltip.add(Component.literal("§7" + currentState.getDescription()));
        }
        
        super.appendHoverText(stack, level, tooltip, flag);
    }
}