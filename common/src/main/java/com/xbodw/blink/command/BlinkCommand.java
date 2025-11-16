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
import net.minecraft.world.level.block.state.BlockState;

public class BlinkCommand {
    
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
        return executeFillWithLimit(context, Integer.MAX_VALUE); // 无限制
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
            
            // 计算区域大小
            int minX = Math.min(from.getX(), to.getX());
            int maxX = Math.max(from.getX(), to.getX());
            int minY = Math.min(from.getY(), to.getY());
            int maxY = Math.max(from.getY(), to.getY());
            int minZ = Math.min(from.getZ(), to.getZ());
            int maxZ = Math.max(from.getZ(), to.getZ());
            
            long totalBlocks = (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
            
            // 只有在设置了具体限制时才检查
            if (limit != Integer.MAX_VALUE && totalBlocks > limit) {
                source.sendFailure(Component.literal("§c区域太大！最多只能填充 " + limit + " 个方块，但选择的区域有 " + totalBlocks + " 个方块。"));
                return 0;
            }
            
            // 对于超大区域给出警告但仍然执行
            if (totalBlocks > 1000000) {
                source.sendSuccess(() -> Component.literal("§e警告：即将填充 " + totalBlocks + " 个方块，这可能需要一些时间..."), false);
            }
            
            // 执行填充
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