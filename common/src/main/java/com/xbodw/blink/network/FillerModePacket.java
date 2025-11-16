package com.xbodw.blink.network;

import com.xbodw.blink.Blink;
import com.xbodw.blink.item.FillMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class FillerModePacket {
    private final FillMode mode;
    
    public FillerModePacket(FillMode mode) {
        this.mode = mode;
    }
    
    public FillerModePacket(FriendlyByteBuf buf) {
        this.mode = FillMode.valueOf(buf.readUtf());
    }
    
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(mode.name());
    }
    
    public void handle(ServerPlayer player) {
        // 在服务端处理模式更新
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        ItemStack fillerStack = null;
        if (mainHand.getItem() == Blink.FILLER_ITEM.get()) {
            fillerStack = mainHand;
        } else if (offHand.getItem() == Blink.FILLER_ITEM.get()) {
            fillerStack = offHand;
        }
        
        if (fillerStack != null) {
            CompoundTag nbt = fillerStack.getOrCreateTag();
            nbt.putString("fillMode", mode.name());
        }
    }
    
    public FillMode getMode() {
        return mode;
    }
}