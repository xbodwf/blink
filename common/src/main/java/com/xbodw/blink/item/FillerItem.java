package com.xbodw.blink.item;

import com.xbodw.blink.Blink;
import com.xbodw.blink.gui.FillerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
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
import java.util.Optional;

public class FillerItem extends Item {
    
    public FillerItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        
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
        FillerDataComponent.FillerData data = stack.getOrDefault(FillerDataComponent.FILLER_DATA, FillerDataComponent.empty());
        FillerState currentState = FillerState.fromString(data.state());
        
        switch (currentState) {
            case SELECTING_POS1:
                stack.set(FillerDataComponent.FILLER_DATA, new FillerDataComponent.FillerData(
                    Optional.of(pos.asLong()), data.pos2(), data.fillBlock(), data.fillMode(),
                    FillerState.SELECTING_POS2.name()
                ));
                player.sendSystemMessage(Component.literal("§a位置1已设置: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
                player.sendSystemMessage(Component.literal("§7现在选择位置2..."));
                break;
                
            case SELECTING_POS2:
                stack.set(FillerDataComponent.FILLER_DATA, new FillerDataComponent.FillerData(
                    data.pos1(), Optional.of(pos.asLong()), data.fillBlock(), data.fillMode(),
                    FillerState.READY_TO_FILL.name()
                ));
                player.sendSystemMessage(Component.literal("§a位置2已设置: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
                
                // 显示区域信息（保留原有功能）
                if (data.pos1().isPresent()) {
                    BlockPos pos1 = BlockPos.of(data.pos1().get());
                    int volume = Math.abs(pos.getX() - pos1.getX() + 1) * 
                               Math.abs(pos.getY() - pos1.getY() + 1) * 
                               Math.abs(pos.getZ() - pos1.getZ() + 1);
                    player.sendSystemMessage(Component.literal("§b区域大小: " + volume + " 个方块"));
                    player.sendSystemMessage(Component.literal("§7再次右键开始填充..."));
                }
                break;
                
            case READY_TO_FILL:
                // 执行填充
                if (data.fillBlock().isEmpty()) {
                    player.sendSystemMessage(Component.literal("§c请先设置填充方块！(Shift+右键打开设置面板)"));
                    return InteractionResult.FAIL;
                }
                
                if (data.pos1().isEmpty() || data.pos2().isEmpty()) {
                    player.sendSystemMessage(Component.literal("§c请先选择两个位置！"));
                    return InteractionResult.FAIL;
                }
                
                BlockPos p1 = BlockPos.of(data.pos1().get());
                BlockPos p2 = BlockPos.of(data.pos2().get());
                String blockId = data.fillBlock().get();
                FillMode fillMode = FillMode.fromString(data.fillMode());
                
                // 获取填充方块
                Block fillBlock = getBlockFromId(blockId);
                if (fillBlock == null) {
                    player.sendSystemMessage(Component.literal("§c无效的填充方块！"));
                    return InteractionResult.FAIL;
                }
                
                // 执行填充，确保不会报错
                try {
                    int result = fillRegion(player.level(), player, p1, p2, fillBlock, fillMode);
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
                stack.set(FillerDataComponent.FILLER_DATA, FillerDataComponent.empty());
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
    
    private static final int MAX_FILL_BLOCKS = 10000;

    private int fillRegion(Level level, Player player, BlockPos pos1, BlockPos pos2, Block fillBlock, FillMode fillMode) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        int totalBlocks = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (totalBlocks > MAX_FILL_BLOCKS) {
            player.sendSystemMessage(Component.literal("§c区域太大！单次最多填充 " + MAX_FILL_BLOCKS + " 个方块，当前区域有 " + totalBlocks + " 个。"));
            return 0;
        }

        int processed = 0;
        BlockState fillState = fillBlock.defaultBlockState();
        int currentBlock = 0;

        if (fillMode == null) {
            fillMode = FillMode.FILL;
        }

        int progressInterval = Math.max(totalBlocks / 10, 100);

        for (int x = minX; x <= maxX && processed < MAX_FILL_BLOCKS; x++) {
            for (int y = minY; y <= maxY && processed < MAX_FILL_BLOCKS; y++) {
                for (int z = minZ; z <= maxZ && processed < MAX_FILL_BLOCKS; z++) {
                    BlockPos currentPos = new BlockPos(x, y, z);
                    BlockState currentState = level.getBlockState(currentPos);
                    currentBlock++;

                    if (currentBlock % progressInterval == 0 || currentBlock >= totalBlocks) {
                        int progress = Math.min((currentBlock * 100) / totalBlocks, 100);
                        player.sendSystemMessage(Component.literal("§6填充进度: " + progress + "% (" + processed + "/" + totalBlocks + ")"));
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // 更新操作说明
        tooltip.add(Component.literal("§7右键: 选择位置/执行填充"));
        tooltip.add(Component.literal("§7Shift+右键: 打开设置面板"));
        
        FillerDataComponent.FillerData data = stack.get(FillerDataComponent.FILLER_DATA);
        FillerState currentState = FillerState.SELECTING_POS1;
        
        if (data != null) {
            currentState = FillerState.fromString(data.state());
            
            // 显示当前状态
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§7当前状态: §e" + currentState.getDisplayName()));
            tooltip.add(Component.literal("§7" + currentState.getDescription()));
            
            // 保留原有的位置和方块信息显示
            tooltip.add(Component.literal(""));
            data.pos1().ifPresent(pos1 -> {
                BlockPos p1 = BlockPos.of(pos1);
                tooltip.add(Component.literal("§a位置1: " + p1.getX() + ", " + p1.getY() + ", " + p1.getZ()));
            });
            data.pos2().ifPresent(pos2 -> {
                BlockPos p2 = BlockPos.of(pos2);
                tooltip.add(Component.literal("§a位置2: " + p2.getX() + ", " + p2.getY() + ", " + p2.getZ()));
                
                // 如果两个位置都设置了，显示区域大小
                data.pos1().ifPresent(pos1 -> {
                    BlockPos p1 = BlockPos.of(pos1);
                    int volume = Math.abs(p2.getX() - p1.getX() + 1) * 
                               Math.abs(p2.getY() - p1.getY() + 1) * 
                               Math.abs(p2.getZ() - p1.getZ() + 1);
                    tooltip.add(Component.literal("§b区域大小: " + volume + " 个方块"));
                });
            });
            data.fillBlock().ifPresent(blockId -> {
                // 尝试获取方块的友好名称
                Block block = getBlockFromId(blockId);
                String displayName = block != null ? block.getName().getString() : blockId;
                tooltip.add(Component.literal("§b填充方块: " + displayName));
            });
            FillMode fillMode = FillMode.fromString(data.fillMode());
            tooltip.add(Component.literal("§d填充模式: " + fillMode.getDisplayName()));
            tooltip.add(Component.literal("§7" + fillMode.getDescription()));
        } else {
            // 如果没有数据，显示基本状态
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§7当前状态: §e" + currentState.getDisplayName()));
            tooltip.add(Component.literal("§7" + currentState.getDescription()));
        }
        
        super.appendHoverText(stack, context, tooltip, flag);
    }
}