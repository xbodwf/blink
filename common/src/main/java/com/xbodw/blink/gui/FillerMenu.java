package com.xbodw.blink.gui;

import com.xbodw.blink.Blink;
import com.xbodw.blink.item.FillerDataComponent;
import com.xbodw.blink.item.FillMode;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class FillerMenu extends AbstractContainerMenu {
    private final Container container;
    private final Player player;
    
    public FillerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(1));
    }
    
    public FillerMenu(int containerId, Inventory playerInventory, Container container) {
        super(Blink.FILLER_MENU_TYPE, containerId);
        this.container = container;
        this.player = playerInventory.player;
        
        // 添加填充方块槽位
        this.addSlot(new Slot(container, 0, 80, 35) {
            @Override
            public void setChanged() {
                super.setChanged();
                // 当槽位内容改变时，更新填充器的NBT数据
                updateFillerBlock();
            }
        });
        
        // 添加玩家背包槽位
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        
        // 添加玩家快捷栏槽位
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
        
        // 初始化时从填充器读取当前设置的方块
        loadFillerBlock();
    }
    
    private void updateFillerBlock() {
        ItemStack fillerStack = getFillerStack();
        if (fillerStack != null) {
            ItemStack blockStack = this.container.getItem(0);
            FillerDataComponent.FillerData data = fillerStack.getOrDefault(FillerDataComponent.FILLER_DATA, FillerDataComponent.empty());
            
            if (!blockStack.isEmpty()) {
                fillerStack.set(FillerDataComponent.FILLER_DATA, new FillerDataComponent.FillerData(
                    data.pos1(), data.pos2(), Optional.of(blockStack.getItem().getDescriptionId()),
                    data.fillMode(), data.state()
                ));
            } else {
                fillerStack.set(FillerDataComponent.FILLER_DATA, new FillerDataComponent.FillerData(
                    data.pos1(), data.pos2(), Optional.empty(),
                    data.fillMode(), data.state()
                ));
            }
        }
    }
    
    private void loadFillerBlock() {
        ItemStack fillerStack = getFillerStack();
        if (fillerStack != null) {
            FillerDataComponent.FillerData data = fillerStack.get(FillerDataComponent.FILLER_DATA);
            if (data != null && data.fillBlock().isPresent()) {
                // 这里可以根据blockId创建对应的ItemStack并放入槽位
                // 但为了简化，我们暂时不实现这个功能
            }
        }
    }
    
    private ItemStack getFillerStack() {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        if (mainHand.getItem() == Blink.FILLER_ITEM) {
            return mainHand;
        } else if (offHand.getItem() == Blink.FILLER_ITEM) {
            return offHand;
        }
        return null;
    }
    
    public void setFillMode(FillMode mode) {
        ItemStack fillerStack = getFillerStack();
        if (fillerStack != null) {
            FillerDataComponent.FillerData data = fillerStack.getOrDefault(FillerDataComponent.FILLER_DATA, FillerDataComponent.empty());
            fillerStack.set(FillerDataComponent.FILLER_DATA, new FillerDataComponent.FillerData(
                data.pos1(), data.pos2(), data.fillBlock(), mode.name(), data.state()
            ));
        }
    }
    
    public FillMode getFillMode() {
        ItemStack fillerStack = getFillerStack();
        if (fillerStack != null) {
            FillerDataComponent.FillerData data = fillerStack.get(FillerDataComponent.FILLER_DATA);
            if (data != null) {
                return FillMode.fromString(data.fillMode());
            }
        }
        return FillMode.FILL; // 默认模式
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();
            
            if (index < this.container.getContainerSize()) {
                if (!this.moveItemStackTo(slotStack, this.container.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 0, this.container.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }
            
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        
        return itemStack;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
    
    public Container getContainer() {
        return this.container;
    }
}