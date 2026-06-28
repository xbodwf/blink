package com.xbodw.blink;

import com.xbodw.blink.gui.FillerMenu;
import com.xbodw.blink.item.CompressedFireworkItem;
import com.xbodw.blink.item.FillerDataComponent;
import com.xbodw.blink.item.FillerItem;
import com.xbodw.blink.neoforge.BlinkPlatformImpl;
import com.xbodw.blink.neoforge.FillerPayload;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Blink.MOD_ID)
public class BlinkNeoForge {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Blink.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Blink.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Blink.MOD_ID);
    private static final DeferredRegister<DataComponentType<?>> DATA = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Blink.MOD_ID);

    private static final DeferredItem<FillerItem> FILLER_ITEM = ITEMS.register("filler",
            () -> new FillerItem(new Item.Properties().stacksTo(1)));

    private static final DeferredItem<CompressedFireworkItem> CF1 = ITEMS.register("compressed_firework_1", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 1));
    private static final DeferredItem<CompressedFireworkItem> CF2 = ITEMS.register("compressed_firework_2", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 2));
    private static final DeferredItem<CompressedFireworkItem> CF3 = ITEMS.register("compressed_firework_3", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 3));
    private static final DeferredItem<CompressedFireworkItem> CF4 = ITEMS.register("compressed_firework_4", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 4));
    private static final DeferredItem<CompressedFireworkItem> CF5 = ITEMS.register("compressed_firework_5", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 5));
    private static final DeferredItem<CompressedFireworkItem> CF6 = ITEMS.register("compressed_firework_6", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 6));
    private static final DeferredItem<CompressedFireworkItem> CF7 = ITEMS.register("compressed_firework_7", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 7));
    private static final DeferredItem<CompressedFireworkItem> CF8 = ITEMS.register("compressed_firework_8", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 8));
    private static final DeferredItem<CompressedFireworkItem> CF9 = ITEMS.register("compressed_firework_9", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 9));

    private static final DeferredHolder<MenuType<?>, MenuType<FillerMenu>> FILLER_MENU_TYPE = MENUS.register("filler_menu",
            () -> new MenuType<>(FillerMenu::new, FeatureFlags.VANILLA_SET));

    private static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLINK_TAB = TABS.register("blink_tab",
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, -1)
                    .title(Component.translatable("itemGroup." + Blink.MOD_ID + ".blink_tab"))
                    .icon(() -> new ItemStack(FILLER_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(FILLER_ITEM.get());
                        output.accept(CF1.get());
                        output.accept(CF2.get());
                        output.accept(CF3.get());
                        output.accept(CF4.get());
                        output.accept(CF5.get());
                        output.accept(CF6.get());
                        output.accept(CF7.get());
                        output.accept(CF8.get());
                        output.accept(CF9.get());
                    })
                    .build());

    private static final DeferredHolder<DataComponentType<?>, DataComponentType<?>> FILLER_DATA = DATA.register("filler_data", () -> FillerDataComponent.FILLER_DATA);

    public BlinkNeoForge(IEventBus modBus) {
        BlinkPlatform.setImpl(new BlinkPlatformImpl());

        ITEMS.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);
        DATA.register(modBus);

        modBus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey() == Registries.ITEM) {
                Blink.FILLER_ITEM = FILLER_ITEM.get();
                Blink.COMPRESSED_FIREWORK_1 = CF1.get();
                Blink.COMPRESSED_FIREWORK_2 = CF2.get();
                Blink.COMPRESSED_FIREWORK_3 = CF3.get();
                Blink.COMPRESSED_FIREWORK_4 = CF4.get();
                Blink.COMPRESSED_FIREWORK_5 = CF5.get();
                Blink.COMPRESSED_FIREWORK_6 = CF6.get();
                Blink.COMPRESSED_FIREWORK_7 = CF7.get();
                Blink.COMPRESSED_FIREWORK_8 = CF8.get();
                Blink.COMPRESSED_FIREWORK_9 = CF9.get();
            } else if (event.getRegistryKey() == Registries.MENU) {
                Blink.FILLER_MENU_TYPE = FILLER_MENU_TYPE.get();
            } else if (event.getRegistryKey() == Registries.CREATIVE_MODE_TAB) {
                Blink.BLINK_TAB = BLINK_TAB.get();
            }
        });

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        modBus.addListener(this::onRegisterPayloadHandlers);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        Blink.registerCommands(event.getDispatcher(), event.getBuildContext());
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(
                FillerPayload.TYPE,
                FillerPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        payload.packet().handle((net.minecraft.server.level.ServerPlayer) context.player());
                    });
                }
        );
    }
}
