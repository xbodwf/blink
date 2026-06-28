package com.xbodw.blink;

import com.mojang.brigadier.CommandDispatcher;
import com.xbodw.blink.command.BlinkCommand;
import com.xbodw.blink.gui.FillerMenu;
import com.xbodw.blink.item.CompressedFireworkItem;
import com.xbodw.blink.item.FillerDataComponent;
import com.xbodw.blink.item.FillerItem;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class Blink {
    public static final String MOD_ID = "blink";

    public static Item FILLER_ITEM;
    public static Item COMPRESSED_FIREWORK_1;
    public static Item COMPRESSED_FIREWORK_2;
    public static Item COMPRESSED_FIREWORK_3;
    public static Item COMPRESSED_FIREWORK_4;
    public static Item COMPRESSED_FIREWORK_5;
    public static Item COMPRESSED_FIREWORK_6;
    public static Item COMPRESSED_FIREWORK_7;
    public static Item COMPRESSED_FIREWORK_8;
    public static Item COMPRESSED_FIREWORK_9;
    public static MenuType<FillerMenu> FILLER_MENU_TYPE;
    public static CreativeModeTab BLINK_TAB;

    public static void fabricRegisterAll() {
        FILLER_ITEM = Registry.register(
                BuiltInRegistries.ITEM, id("filler"),
                new FillerItem(new Item.Properties().stacksTo(1)));

        COMPRESSED_FIREWORK_1 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_1"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 1));
        COMPRESSED_FIREWORK_2 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_2"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 2));
        COMPRESSED_FIREWORK_3 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_3"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 3));
        COMPRESSED_FIREWORK_4 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_4"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 4));
        COMPRESSED_FIREWORK_5 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_5"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 5));
        COMPRESSED_FIREWORK_6 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_6"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 6));
        COMPRESSED_FIREWORK_7 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_7"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 7));
        COMPRESSED_FIREWORK_8 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_8"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 8));
        COMPRESSED_FIREWORK_9 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_9"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 9));

        FILLER_MENU_TYPE = Registry.register(
                BuiltInRegistries.MENU, id("filler_menu"),
                new MenuType<>(FillerMenu::new, FeatureFlags.VANILLA_SET));

        BLINK_TAB = Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB, id("blink_tab"),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, -1)
                        .title(Component.translatable("itemGroup." + MOD_ID + ".blink_tab"))
                        .icon(() -> new ItemStack(Blink.FILLER_ITEM))
                        .displayItems((parameters, output) -> {
                            output.accept(Blink.FILLER_ITEM);
                            output.accept(Blink.COMPRESSED_FIREWORK_1);
                            output.accept(Blink.COMPRESSED_FIREWORK_2);
                            output.accept(Blink.COMPRESSED_FIREWORK_3);
                            output.accept(Blink.COMPRESSED_FIREWORK_4);
                            output.accept(Blink.COMPRESSED_FIREWORK_5);
                            output.accept(Blink.COMPRESSED_FIREWORK_6);
                            output.accept(Blink.COMPRESSED_FIREWORK_7);
                            output.accept(Blink.COMPRESSED_FIREWORK_8);
                            output.accept(Blink.COMPRESSED_FIREWORK_9);
                        })
                        .build());

        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("filler_data"), FillerDataComponent.FILLER_DATA);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
        BlinkCommand.register(dispatcher, commandBuildContext);
    }
}
