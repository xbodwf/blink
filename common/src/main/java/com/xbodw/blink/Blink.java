package com.xbodw.blink;

import com.mojang.brigadier.CommandDispatcher;
import com.xbodw.blink.command.BlinkCommand;
import com.xbodw.blink.gui.FillerMenu;
import com.xbodw.blink.item.CompressedFireworkItem;
import com.xbodw.blink.item.CompressedWindChargeItem;
import com.xbodw.blink.item.FillerDataComponent;
import com.xbodw.blink.item.FillerItem;
import com.xbodw.blink.recipe.CompressedFireworkRecipe;
import com.xbodw.blink.recipe.CompressedFireworkRecipeSerializer;
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
import net.minecraft.world.item.crafting.RecipeSerializer;

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
    public static Item COMPRESSED_WIND_CHARGE_1;
    public static Item COMPRESSED_WIND_CHARGE_2;
    public static Item COMPRESSED_WIND_CHARGE_3;
    public static Item COMPRESSED_WIND_CHARGE_4;
    public static Item COMPRESSED_WIND_CHARGE_5;
    public static Item COMPRESSED_WIND_CHARGE_6;
    public static Item COMPRESSED_WIND_CHARGE_7;
    public static Item COMPRESSED_WIND_CHARGE_8;
    public static Item COMPRESSED_WIND_CHARGE_9;
    public static MenuType<FillerMenu> FILLER_MENU_TYPE;
    public static CreativeModeTab BLINK_TAB;
    public static RecipeSerializer<CompressedFireworkRecipe> COMPRESSED_FIREWORK_RECIPE_SERIALIZER;

    public static void fabricRegisterAll() {
        FILLER_ITEM = Registry.register(
                BuiltInRegistries.ITEM, id("filler"),
                new FillerItem(new Item.Properties().stacksTo(1).fireResistant()));

        COMPRESSED_FIREWORK_1 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_1"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 1));
        COMPRESSED_FIREWORK_2 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_2"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 2));
        COMPRESSED_FIREWORK_3 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_3"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 3));
        COMPRESSED_FIREWORK_4 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_4"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 4));
        COMPRESSED_FIREWORK_5 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_5"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 5));
        COMPRESSED_FIREWORK_6 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_6"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 6));
        COMPRESSED_FIREWORK_7 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_7"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 7));
        COMPRESSED_FIREWORK_8 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_8"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 8));
        COMPRESSED_FIREWORK_9 = Registry.register(BuiltInRegistries.ITEM, id("compressed_firework_9"), new CompressedFireworkItem(new Item.Properties().stacksTo(64), 9));

        COMPRESSED_WIND_CHARGE_1 = Registry.register(BuiltInRegistries.ITEM, id("compressed_wind_charge_1"), new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 1));
        COMPRESSED_WIND_CHARGE_2 = Registry.register(BuiltInRegistries.ITEM, id("compressed_wind_charge_2"), new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 2));
        COMPRESSED_WIND_CHARGE_3 = Registry.register(BuiltInRegistries.ITEM, id("compressed_wind_charge_3"), new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 3));
        COMPRESSED_WIND_CHARGE_4 = Registry.register(BuiltInRegistries.ITEM, id("compressed_wind_charge_4"), new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 4));
        COMPRESSED_WIND_CHARGE_5 = Registry.register(BuiltInRegistries.ITEM, id("compressed_wind_charge_5"), new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 5));
        COMPRESSED_WIND_CHARGE_6 = Registry.register(BuiltInRegistries.ITEM, id("compressed_wind_charge_6"), new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 6));
        COMPRESSED_WIND_CHARGE_7 = Registry.register(BuiltInRegistries.ITEM, id("compressed_wind_charge_7"), new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 7));
        COMPRESSED_WIND_CHARGE_8 = Registry.register(BuiltInRegistries.ITEM, id("compressed_wind_charge_8"), new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 8));
        COMPRESSED_WIND_CHARGE_9 = Registry.register(BuiltInRegistries.ITEM, id("compressed_wind_charge_9"), new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 9));

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
                            output.accept(Blink.COMPRESSED_WIND_CHARGE_1);
                            output.accept(Blink.COMPRESSED_WIND_CHARGE_2);
                            output.accept(Blink.COMPRESSED_WIND_CHARGE_3);
                            output.accept(Blink.COMPRESSED_WIND_CHARGE_4);
                            output.accept(Blink.COMPRESSED_WIND_CHARGE_5);
                            output.accept(Blink.COMPRESSED_WIND_CHARGE_6);
                            output.accept(Blink.COMPRESSED_WIND_CHARGE_7);
                            output.accept(Blink.COMPRESSED_WIND_CHARGE_8);
                            output.accept(Blink.COMPRESSED_WIND_CHARGE_9);
                        })
                        .build());

        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("filler_data"), FillerDataComponent.FILLER_DATA);

        COMPRESSED_FIREWORK_RECIPE_SERIALIZER = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER, id("compressed_firework"),
            new CompressedFireworkRecipeSerializer());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
        BlinkCommand.register(dispatcher, commandBuildContext);
    }
}
