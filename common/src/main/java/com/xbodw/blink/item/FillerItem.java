package com.xbodw.blink.item;

import com.xbodw.blink.Blink;
import com.xbodw.blink.gui.FillerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FillerItem extends Item {

    private static final int MAX_FILL_BLOCKS = 10000;
    private static final int BATCH_SIZE = 200;
    private static final Map<UUID, FillTask> PENDING_FILLS = new ConcurrentHashMap<>();

    public FillerItem(Properties properties) {
        super(properties);
    }

    private static class FillTask {
        final Player player;
        final Level level;
        final int minX, minY, minZ;
        final int sizeX, sizeY, sizeZ;
        final BlockState fillState;
        final FillMode fillMode;
        final int totalBlocks;
        final boolean resetState;
        int currentIndex;
        int processed;
        int lastProgressPercent;

        FillTask(Player player, Level level, int minX, int minY, int minZ,
                 int sizeX, int sizeY, int sizeZ, BlockState fillState, FillMode fillMode,
                 int totalBlocks, boolean resetState) {
            this.player = player;
            this.level = level;
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.sizeX = sizeX; this.sizeY = sizeY; this.sizeZ = sizeZ;
            this.fillState = fillState;
            this.fillMode = fillMode;
            this.totalBlocks = totalBlocks;
            this.resetState = resetState;
            this.currentIndex = 0;
            this.processed = 0;
            this.lastProgressPercent = -1;
        }
    }

    public static void processPendingFills() {
        for (Map.Entry<UUID, FillTask> entry : PENDING_FILLS.entrySet()) {
            FillTask task = entry.getValue();
            if (task.player.isRemoved() || !task.player.isAlive()) {
                PENDING_FILLS.remove(entry.getKey());
                continue;
            }
            processBatch(task);
            if (task.currentIndex >= task.totalBlocks) {
                PENDING_FILLS.remove(entry.getKey());
            }
        }
    }

    private static void processBatch(FillTask task) {
        Player player = task.player;
        Level level = task.level;
        BlockState fillState = task.fillState;
        FillMode fillMode = task.fillMode;

        int count = 0;
        while (task.currentIndex < task.totalBlocks && count < BATCH_SIZE) {
            int idx = task.currentIndex;
            int localX = idx / (task.sizeY * task.sizeZ);
            int remainder = idx % (task.sizeY * task.sizeZ);
            int localY = remainder / task.sizeZ;
            int localZ = remainder % task.sizeZ;
            int x = task.minX + localX;
            int y = task.minY + localY;
            int z = task.minZ + localZ;

            BlockPos currentPos = new BlockPos(x, y, z);
            BlockState currentState = level.getBlockState(currentPos);
            boolean shouldProcess = false;

            switch (fillMode) {
                case FILL:
                    shouldProcess = currentState.isAir() || currentState.canBeReplaced();
                    break;
                case REPLACE:
                    shouldProcess = true;
                    break;
                case REMOVE:
                    shouldProcess = currentState.getBlock() == fillState.getBlock();
                    break;
            }

            if (shouldProcess) {
                try {
                    if (!player.isCreative() && fillMode != FillMode.REMOVE) {
                        boolean hasItem = false;
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            if (player.getInventory().getItem(i).getItem() == fillState.getBlock().asItem()) {
                                hasItem = true;
                                break;
                            }
                        }
                        if (!hasItem) {
                            task.currentIndex++;
                            count++;
                            continue;
                        }
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            ItemStack slotStack = player.getInventory().getItem(i);
                            if (slotStack.getItem() == fillState.getBlock().asItem()) {
                                slotStack.shrink(1);
                                break;
                            }
                        }
                    }

                    if (fillMode == FillMode.REMOVE) {
                        level.setBlock(currentPos, Blocks.AIR.defaultBlockState(), 2);
                    } else {
                        level.setBlock(currentPos, fillState, 2);
                    }
                    task.processed++;
                } catch (Exception e) {
                    // ignore single block errors
                }
            }

            task.currentIndex++;
            count++;
        }

        int currentPercent = (task.currentIndex * 100) / task.totalBlocks;
        if (currentPercent >= task.lastProgressPercent + 10 || task.currentIndex >= task.totalBlocks) {
            player.sendSystemMessage(Component.literal("§6填充进度: " + Math.min(currentPercent, 100) + "% (" + task.processed + "/" + task.totalBlocks + ")"));
            task.lastProgressPercent = currentPercent;
        }

        if (task.currentIndex >= task.totalBlocks) {
            if (task.processed > 0) {
                player.sendSystemMessage(Component.literal("§a成功处理了 " + task.processed + " 个方块！"));
            } else {
                player.sendSystemMessage(Component.literal("§e没有需要处理的方块。"));
            }
            if (task.resetState && player instanceof ServerPlayer) {
                ItemStack fillerStack = getHeldFiller(player);
                if (fillerStack != null) {
                    FillerDataComponent.FillerData oldData = fillerStack.get(FillerDataComponent.FILLER_DATA);
                    Optional<String> fillBlock = oldData != null ? oldData.fillBlock() : Optional.empty();
                    String fillModeStr = oldData != null ? oldData.fillMode() : "FILL";
                    fillerStack.set(FillerDataComponent.FILLER_DATA, new FillerDataComponent.FillerData(
                        Optional.empty(), Optional.empty(), fillBlock, fillModeStr, "SELECTING_POS1"
                    ));
                }
            }
            player.sendSystemMessage(Component.literal("§7位置已重置，可以重新选择区域。填充方块和模式保持不变。"));
        }
    }

    private static ItemStack getHeldFiller(Player player) {
        if (player.getMainHandItem().getItem() instanceof FillerItem) {
            return player.getMainHandItem();
        } else if (player.getOffhandItem().getItem() instanceof FillerItem) {
            return player.getOffhandItem();
        }
        return null;
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
                if (data.fillBlock().isEmpty()) {
                    player.sendSystemMessage(Component.literal("§c请先设置填充方块！(Shift+右键打开设置面板)"));
                    return InteractionResult.FAIL;
                }

                if (data.pos1().isEmpty() || data.pos2().isEmpty()) {
                    player.sendSystemMessage(Component.literal("§c请先选择两个位置！"));
                    return InteractionResult.FAIL;
                }

                if (PENDING_FILLS.containsKey(player.getUUID())) {
                    player.sendSystemMessage(Component.literal("§c已有填充任务在执行中，请等待完成！"));
                    return InteractionResult.FAIL;
                }

                BlockPos p1 = BlockPos.of(data.pos1().get());
                BlockPos p2 = BlockPos.of(data.pos2().get());
                String blockStateStr = data.fillBlock().get();
                FillMode fillMode = FillMode.fromString(data.fillMode());

                BlockState fillState = parseBlockState(blockStateStr);
                if (fillState == null) {
                    player.sendSystemMessage(Component.literal("§c无效的填充方块！"));
                    return InteractionResult.FAIL;
                }

                int minX = Math.min(p1.getX(), p2.getX());
                int maxX = Math.max(p1.getX(), p2.getX());
                int minY = Math.min(p1.getY(), p2.getY());
                int maxY = Math.max(p1.getY(), p2.getY());
                int minZ = Math.min(p1.getZ(), p2.getZ());
                int maxZ = Math.max(p1.getZ(), p2.getZ());

                int sizeX = maxX - minX + 1;
                int sizeY = maxY - minY + 1;
                int sizeZ = maxZ - minZ + 1;
                int totalBlocks = sizeX * sizeY * sizeZ;

                if (totalBlocks > MAX_FILL_BLOCKS) {
                    player.sendSystemMessage(Component.literal("§c区域太大！单次最多填充 " + MAX_FILL_BLOCKS + " 个方块，当前区域有 " + totalBlocks + " 个。"));
                    return InteractionResult.FAIL;
                }

                if (totalBlocks <= BATCH_SIZE) {
                    FillTask task = new FillTask(player, player.level(), minX, minY, minZ,
                            sizeX, sizeY, sizeZ, fillState, fillMode, totalBlocks, true);
                    processBatch(task);
                } else {
                    player.sendSystemMessage(Component.literal("§6填充中... 共 " + totalBlocks + " 个方块"));
                    FillTask task = new FillTask(player, player.level(), minX, minY, minZ,
                            sizeX, sizeY, sizeZ, fillState, fillMode, totalBlocks, true);
                    PENDING_FILLS.put(player.getUUID(), task);
                    processBatch(task);
                }
                break;
        }
        
        return InteractionResult.SUCCESS;
    }
    
    private void openFillerGui(ServerPlayer player, ItemStack stack) {
        FillerDataComponent.FillerData data = stack.getOrDefault(FillerDataComponent.FILLER_DATA, FillerDataComponent.empty());
        net.minecraft.world.SimpleContainer container = new net.minecraft.world.SimpleContainer(1);

        if (data.fillBlock().isPresent()) {
            Block block = getBlockFromStateString(data.fillBlock().get());
            if (block != null) {
                container.setItem(0, new ItemStack(block.asItem()));
            }
        }

        final net.minecraft.world.SimpleContainer finalContainer = container;
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("填充器设置");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new FillerMenu(containerId, playerInventory, finalContainer);
            }
        });
    }
    
    /**
     * Converts an ItemStack to a block state string format: "namespace:block[prop1=val1,prop2=val2]"
     */
    public static String getBlockStateString(ItemStack stack) {
        if (stack.isEmpty()) return "";
        Block block = Block.byItem(stack.getItem());
        if (block == Blocks.AIR) return "";
        ResourceLocation id = block.builtInRegistryHolder().key().location();
        BlockState state = block.defaultBlockState();
        return blockStateToString(id, state);
    }

    public static String blockStateToString(ResourceLocation id, BlockState state) {
        StringBuilder sb = new StringBuilder(id.toString());
        Map<Property<?>, Comparable<?>> values = state.getValues();
        if (!values.isEmpty()) {
            sb.append('[');
            boolean first = true;
            for (Map.Entry<Property<?>, Comparable<?>> entry : values.entrySet()) {
                if (!first) sb.append(',');
                sb.append(entry.getKey().getName());
                sb.append('=');
                sb.append(entry.getValue().toString());
                first = false;
            }
            sb.append(']');
        }
        return sb.toString();
    }

    /**
     * Parses a block state string like "minecraft:stone_slab[type=top,waterlogged=false]"
     * and returns the full BlockState, or null if invalid.
     */
    public static BlockState parseBlockState(String stateString) {
        if (stateString == null || stateString.isEmpty()) return null;

        String blockPart;
        String propsPart = null;

        int bracketStart = stateString.indexOf('[');
        if (bracketStart >= 0) {
            blockPart = stateString.substring(0, bracketStart);
            propsPart = stateString.substring(bracketStart + 1);
            if (propsPart.endsWith("]")) {
                propsPart = propsPart.substring(0, propsPart.length() - 1);
            }
        } else {
            blockPart = stateString;
        }

        ResourceLocation blockId = ResourceLocation.parse(blockPart);
        Block block = BuiltInRegistries.BLOCK.get(blockId);
        if (block == null || block == Blocks.AIR) return null;

        BlockState state = block.defaultBlockState();

        if (propsPart != null && !propsPart.isEmpty()) {
            String[] props = propsPart.split(",");
            for (String prop : props) {
                String[] kv = prop.split("=", 2);
                if (kv.length != 2) continue;
                String propName = kv[0].trim();
                String propValue = kv[1].trim();

                for (Property<?> property : state.getProperties()) {
                    if (property.getName().equals(propName)) {
                        state = setPropertyValue(state, property, propValue);
                        break;
                    }
                }
            }
        }

        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState setPropertyValue(BlockState state, Property property, String value) {
        Optional opt = property.getValue(value);
        if (opt.isPresent()) {
            return state.setValue(property, (Comparable) opt.get());
        }
        return state;
    }

    /**
     * Extracts the Block from a block state string.
     */
    public static Block getBlockFromStateString(String stateString) {
        if (stateString == null || stateString.isEmpty()) return null;
        String blockPart = stateString.contains("[") ? stateString.substring(0, stateString.indexOf('[')) : stateString;
        ResourceLocation blockId = ResourceLocation.parse(blockPart);
        Block block = BuiltInRegistries.BLOCK.get(blockId);
        return (block != null && block != Blocks.AIR) ? block : null;
    }

    public static String getBlockDisplayName(String stateString) {
        if (stateString == null || stateString.isEmpty()) return "无";
        BlockState state = parseBlockState(stateString);
        if (state != null) {
            return state.getBlock().getName().getString();
        }
        Block block = getBlockFromStateString(stateString);
        return block != null ? block.getName().getString() : stateString;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7右键: 选择位置/执行填充"));
        tooltip.add(Component.literal("§7Shift+右键: 打开设置面板"));
        
        FillerDataComponent.FillerData data = stack.get(FillerDataComponent.FILLER_DATA);
        FillerState currentState = FillerState.SELECTING_POS1;
        
        if (data != null) {
            currentState = FillerState.fromString(data.state());
            
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§7当前状态: §e" + currentState.getDisplayName()));
            tooltip.add(Component.literal("§7" + currentState.getDescription()));
            
            tooltip.add(Component.literal(""));
            data.pos1().ifPresent(pos1 -> {
                BlockPos p1 = BlockPos.of(pos1);
                tooltip.add(Component.literal("§a位置1: " + p1.getX() + ", " + p1.getY() + ", " + p1.getZ()));
            });
            data.pos2().ifPresent(pos2 -> {
                BlockPos p2 = BlockPos.of(pos2);
                tooltip.add(Component.literal("§a位置2: " + p2.getX() + ", " + p2.getY() + ", " + p2.getZ()));
                
                data.pos1().ifPresent(pos1 -> {
                    BlockPos p1 = BlockPos.of(pos1);
                    int volume = Math.abs(p2.getX() - p1.getX() + 1) * 
                               Math.abs(p2.getY() - p1.getY() + 1) * 
                               Math.abs(p2.getZ() - p1.getZ() + 1);
                    tooltip.add(Component.literal("§b区域大小: " + volume + " 个方块"));
                });
            });
            data.fillBlock().ifPresent(blockStateStr -> {
                String displayName = getBlockDisplayName(blockStateStr);
                tooltip.add(Component.literal("§b填充方块: " + displayName));
                if (blockStateStr.contains("[")) {
                    tooltip.add(Component.literal("§7状态: " + blockStateStr));
                }
            });
            FillMode fillMode = FillMode.fromString(data.fillMode());
            tooltip.add(Component.literal("§d填充模式: " + fillMode.getDisplayName()));
            tooltip.add(Component.literal("§7" + fillMode.getDescription()));
        } else {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§7当前状态: §e" + currentState.getDisplayName()));
            tooltip.add(Component.literal("§7" + currentState.getDescription()));
        }
        
        super.appendHoverText(stack, context, tooltip, flag);
    }
}