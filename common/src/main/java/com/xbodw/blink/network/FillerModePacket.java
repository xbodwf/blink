package com.xbodw.blink.network;

import com.xbodw.blink.Blink;
import com.xbodw.blink.item.FillerDataComponent;
import com.xbodw.blink.item.FillMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

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
        if (mainHand.getItem() == Blink.FILLER_ITEM) {
            fillerStack = mainHand;
        } else if (offHand.getItem() == Blink.FILLER_ITEM) {
            fillerStack = offHand;
        }
        
        if (fillerStack != null) {
            FillerDataComponent.FillerData data = fillerStack.getOrDefault(FillerDataComponent.FILLER_DATA, FillerDataComponent.empty());
            fillerStack.set(FillerDataComponent.FILLER_DATA, new FillerDataComponent.FillerData(
                data.pos1(), data.pos2(), data.fillBlock(), mode.name(), data.state()
            ));
        }
    }
    
    public FillMode getMode() {
        return mode;
    }
}