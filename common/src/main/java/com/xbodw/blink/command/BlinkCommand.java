package com.xbodw.blink.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BlinkCommand {

    private static final int BATCH_SIZE = 200;
    private static final Map<UUID, CommandFillTask> PENDING_COMMAND_FILLS = new ConcurrentHashMap<>();

    private static class CommandFillTask {
        final ServerPlayer player;
        final ServerLevel level;
        final BlockState blockState;
        final int minX, minY, minZ;
        final int sizeX, sizeY, sizeZ;
        final int totalBlocks;
        int currentIndex;
        int filled;
        int lastProgressPercent;

        CommandFillTask(ServerPlayer player, ServerLevel level, BlockState blockState,
                        int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ, int totalBlocks) {
            this.player = player;
            this.level = level;
            this.blockState = blockState;
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.sizeX = sizeX; this.sizeY = sizeY; this.sizeZ = sizeZ;
            this.totalBlocks = totalBlocks;
            this.currentIndex = 0;
            this.filled = 0;
            this.lastProgressPercent = -1;
        }
    }

    public static void processPendingCommandFills() {
        for (Map.Entry<UUID, CommandFillTask> entry : PENDING_COMMAND_FILLS.entrySet()) {
            CommandFillTask task = entry.getValue();
            if (task.player.isRemoved() || !task.player.isAlive()) {
                PENDING_COMMAND_FILLS.remove(entry.getKey());
                continue;
            }
            processCommandBatch(task);
            if (task.currentIndex >= task.totalBlocks) {
                PENDING_COMMAND_FILLS.remove(entry.getKey());
            }
        }
    }

    private static void processCommandBatch(CommandFillTask task) {
        int count = 0;
        while (task.currentIndex < task.totalBlocks && count < BATCH_SIZE) {
            int idx = task.currentIndex;
            int localX = idx / (task.sizeY * task.sizeZ);
            int remainder = idx % (task.sizeY * task.sizeZ);
            int localY = remainder / task.sizeZ;
            int localZ = remainder % task.sizeZ;

            BlockPos pos = new BlockPos(task.minX + localX, task.minY + localY, task.minZ + localZ);
            if (task.level.setBlock(pos, task.blockState, 2)) {
                task.filled++;
            }

            task.currentIndex++;
            count++;
        }

        int currentPercent = (task.currentIndex * 100) / task.totalBlocks;
        if (currentPercent >= task.lastProgressPercent + 10 || task.currentIndex >= task.totalBlocks) {
            task.player.sendSystemMessage(Component.literal("§6填充进度: " + Math.min(currentPercent, 100) + "% (" + task.filled + "/" + task.totalBlocks + ")"));
            task.lastProgressPercent = currentPercent;
        }

        if (task.currentIndex >= task.totalBlocks) {
            final int finalFilled = task.filled;
            task.player.sendSystemMessage(Component.literal("§a成功填充了 " + finalFilled + " 个方块！"));
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
        dispatcher.register(Commands.literal("blink")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("fill")
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                    .then(Commands.argument("to", BlockPosArgument.blockPos())
                        .then(Commands.argument("block", BlockStateArgument.block(commandBuildContext))
                            .executes(BlinkCommand::executeUnlimitedFill)
                            .then(Commands.argument("limit", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                                .executes(BlinkCommand::executeFillWithLimit)
                            )
                        )
                    )
                )
            )
            .then(Commands.literal("help")
                .executes(BlinkCommand::executeHelp)
            )
        );
    }

    private static int executeUnlimitedFill(CommandContext<CommandSourceStack> context) {
        return executeFillWithLimit(context, Integer.MAX_VALUE);
    }

    private static int executeFillWithLimit(CommandContext<CommandSourceStack> context) {
        int limit = IntegerArgumentType.getInteger(context, "limit");
        return executeFillWithLimit(context, limit);
    }

    private static int executeFillWithLimit(CommandContext<CommandSourceStack> context, int limit) {
        try {
            CommandSourceStack source = context.getSource();
            ServerLevel level = source.getLevel();

            BlockPos from = BlockPosArgument.getBlockPos(context, "from");
            BlockPos to = BlockPosArgument.getBlockPos(context, "to");
            BlockState blockState = BlockStateArgument.getBlock(context, "block").getState();

            int minX = Math.min(from.getX(), to.getX());
            int maxX = Math.max(from.getX(), to.getX());
            int minY = Math.min(from.getY(), to.getY());
            int maxY = Math.max(from.getY(), to.getY());
            int minZ = Math.min(from.getZ(), to.getZ());
            int maxZ = Math.max(from.getZ(), to.getZ());

            int sizeX = maxX - minX + 1;
            int sizeY = maxY - minY + 1;
            int sizeZ = maxZ - minZ + 1;
            long totalBlocks = (long) sizeX * sizeY * sizeZ;

            if (limit != Integer.MAX_VALUE && totalBlocks > limit) {
                source.sendFailure(Component.literal("§c区域太大！最多只能填充 " + limit + " 个方块，但选择的区域有 " + totalBlocks + " 个方块。"));
                return 0;
            }

            ServerPlayer player = source.getPlayer();
            if (player != null) {
                if (PENDING_COMMAND_FILLS.containsKey(player.getUUID())) {
                    source.sendFailure(Component.literal("§c已有填充任务在执行中，请等待完成！"));
                    return 0;
                }

                if (totalBlocks > 1000000) {
                    source.sendSuccess(() -> Component.literal("§e警告：即将填充 " + totalBlocks + " 个方块，这可能需要一些时间..."), false);
                }

                CommandFillTask task = new CommandFillTask(player, level, blockState,
                        minX, minY, minZ, sizeX, sizeY, sizeZ, (int) totalBlocks);
                PENDING_COMMAND_FILLS.put(player.getUUID(), task);
                processCommandBatch(task);
                return 1;
            } else {
                int filled = 0;
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (level.setBlock(pos, blockState, 3)) {
                                filled++;
                            }
                        }
                    }
                }
                final int finalFilled = filled;
                source.sendSuccess(() -> Component.literal("§a成功填充了 " + finalFilled + " 个方块！"), true);
                return filled;
            }

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§c执行填充时出错: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6=== Blink 指令帮助 ==="), false);
        source.sendSuccess(() -> Component.literal("§e/blink fill <from> <to> <block> [limit]"), false);
        source.sendSuccess(() -> Component.literal("§7  - 填充指定区域"), false);
        source.sendSuccess(() -> Component.literal("§7  - from: 起始位置"), false);
        source.sendSuccess(() -> Component.literal("§7  - to: 结束位置"), false);
        source.sendSuccess(() -> Component.literal("§7  - block: 要填充的方块"), false);
        source.sendSuccess(() -> Component.literal("§7  - limit: 最大方块数量 (可选，默认无限制)"), false);
        source.sendSuccess(() -> Component.literal("§e示例: /blink fill ~ ~ ~ ~10 ~5 ~10 minecraft:stone"), false);
        source.sendSuccess(() -> Component.literal("§e示例: /blink fill ~ ~ ~ ~100 ~100 ~100 minecraft:stone 500000"), false);
        return 1;
    }
}