package com.xbodw.blink.network;

import com.xbodw.blink.Blink;
import com.xbodw.blink.item.FillerDataComponent;
import com.xbodw.blink.item.FillerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;

public class FillerPickBlockPacket {

    public FillerPickBlockPacket() {}

    public FillerPickBlockPacket(FriendlyByteBuf buf) {}

    public void write(FriendlyByteBuf buf) {}

    public void handle(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        ItemStack fillerStack = null;
        if (mainHand.getItem() == Blink.FILLER_ITEM) {
            fillerStack = mainHand;
        } else if (offHand.getItem() == Blink.FILLER_ITEM) {
            fillerStack = offHand;
        }

        if (fillerStack == null) return;

        Level level = player.level();
        HitResult hit = player.pick(10.0D, 0.0F, true);
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c没有瞄准任何方块！"));
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos();
        BlockState targetState = level.getBlockState(pos);

        if (targetState.isAir()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c准星处没有方块！"));
            return;
        }

        ResourceLocation blockId = targetState.getBlock().builtInRegistryHolder().key().location();
        String stateString = FillerItem.blockStateToString(blockId, targetState);

        FillerDataComponent.FillerData data = fillerStack.getOrDefault(FillerDataComponent.FILLER_DATA, FillerDataComponent.empty());
        fillerStack.set(FillerDataComponent.FILLER_DATA, new FillerDataComponent.FillerData(
            data.pos1(), data.pos2(), Optional.of(stateString), data.fillMode(), data.state()
        ));

        String displayName = targetState.getBlock().getName().getString();
        if (targetState.getProperties().size() > 1) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a已拾取: §e" + displayName + " §7(" + stateString + ")"));
        } else {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a已拾取: §e" + displayName));
        }

        player.closeContainer();
    }
}
