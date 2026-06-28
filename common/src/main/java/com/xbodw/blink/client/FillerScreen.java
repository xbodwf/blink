package com.xbodw.blink.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xbodw.blink.gui.FillerMenu;
import com.xbodw.blink.item.FillMode;
import com.xbodw.blink.network.FillerModePacket;
import com.xbodw.blink.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class FillerScreen extends AbstractContainerScreen<FillerMenu> {
    private static final ResourceLocation CONTAINER_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int CONTAINER_ROWS = 1;
    private CycleButton<FillMode> modeButton;
    private FillMode currentMode = FillMode.FILL;
    
    public FillerScreen(FillerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 114 + CONTAINER_ROWS * 18;
        // 从菜单获取当前模式
        this.currentMode = menu.getFillMode();
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.leftPos + this.imageWidth / 2;
        
        // 创建填充模式选择按钮
        this.modeButton = CycleButton.<FillMode>builder(mode -> Component.literal(mode.getDisplayName()))
            .withValues(FillMode.values())
            .withInitialValue(currentMode)
            .withTooltip(mode -> net.minecraft.client.gui.components.Tooltip.create(Component.literal(mode.getDescription())))
            .create(centerX - 80, this.topPos + 30, 160, 20, Component.literal("填充模式"), (button, mode) -> {
                this.currentMode = mode;
                // 发送网络包到服务端更新模式
                NetworkHandler.sendToServer(new FillerModePacket(mode));
                // 同时更新本地菜单
                this.menu.setFillMode(mode);
            });
        
        this.addRenderableWidget(modeButton);
        
        // 添加关闭按钮（右上角叉）
        Button closeButton = Button.builder(Component.literal("×"), button -> this.onClose())
            .bounds(this.leftPos + this.imageWidth - 20, this.topPos + 4, 16, 16)
            .build();
        this.addRenderableWidget(closeButton);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        // 渲染容器背景
        guiGraphics.blit(CONTAINER_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, CONTAINER_ROWS * 18 + 17);
        guiGraphics.blit(CONTAINER_TEXTURE, this.leftPos, this.topPos + CONTAINER_ROWS * 18 + 17, 0, 126, this.imageWidth, 96);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int centerX = this.imageWidth / 2;
        
        // 渲染标题
        guiGraphics.drawString(this.font, this.title, 8, 6, 4210752, false);
        
        // 渲染物品槽标签
        guiGraphics.drawString(this.font, Component.literal("填充方块:"), centerX - 40, 20, 4210752, false);
        
        // 渲染背包标签
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 18 + 1 * 18 + 4, 4210752, false);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC键关闭界面
        if (keyCode == 256) { // ESC key
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    public void setCurrentMode(FillMode mode) {
        this.currentMode = mode;
        if (this.modeButton != null) {
            this.modeButton.setValue(mode);
        }
    }
}