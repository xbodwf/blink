package com.xbodw.blink;

import com.google.common.base.Suppliers;
import com.mojang.brigadier.CommandDispatcher;
import com.xbodw.blink.block.BlinkPortalBlock;
import com.xbodw.blink.command.BlinkCommand;
import com.xbodw.blink.dimension.BlinkDimensions;
import com.xbodw.blink.gui.FillerMenu;
import com.xbodw.blink.item.CompressedFireworkItem;
import com.xbodw.blink.item.DimensionKeyItem;
import com.xbodw.blink.network.NetworkHandler;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import com.xbodw.blink.item.FillerItem;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;

public class Blink {
    public static final String MOD_ID = "blink";
    
    // We can use this if we don't want to use DeferredRegister
    public static final Supplier<RegistrarManager> REGISTRIES = Suppliers.memoize(() -> RegistrarManager.get(MOD_ID));

    // 方块注册
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MOD_ID, Registries.BLOCK);
    public static final RegistrySupplier<Block> BLINK_PORTAL_BLOCK = BLOCKS.register("blink_portal", () ->
            new BlinkPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noCollission()
                    .lightLevel((state) -> 11)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .sound(SoundType.GLASS)));

    // 物品注册
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MOD_ID, Registries.ITEM);
    public static final RegistrySupplier<Item> FILLER_ITEM = ITEMS.register("filler", () ->
            new FillerItem(new Item.Properties().stacksTo(1)));

    // 维度钥匙物品
    public static final RegistrySupplier<Item> DIMENSION_KEY = ITEMS.register("dimension_key", () ->
            new DimensionKeyItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    // 传送门方块物品
    public static final RegistrySupplier<Item> BLINK_PORTAL_ITEM = ITEMS.register("blink_portal", () ->
            new BlockItem(BLINK_PORTAL_BLOCK.get(), new Item.Properties()));

    // 压缩烟花物品注册
    public static final RegistrySupplier<Item> COMPRESSED_FIREWORK_1 = ITEMS.register("compressed_firework_1", () ->
            new CompressedFireworkItem(new Item.Properties().stacksTo(64), 1));
    public static final RegistrySupplier<Item> COMPRESSED_FIREWORK_2 = ITEMS.register("compressed_firework_2", () ->
            new CompressedFireworkItem(new Item.Properties().stacksTo(64), 2));
    public static final RegistrySupplier<Item> COMPRESSED_FIREWORK_3 = ITEMS.register("compressed_firework_3", () ->
            new CompressedFireworkItem(new Item.Properties().stacksTo(64), 3));
    public static final RegistrySupplier<Item> COMPRESSED_FIREWORK_4 = ITEMS.register("compressed_firework_4", () ->
            new CompressedFireworkItem(new Item.Properties().stacksTo(64), 4));
    public static final RegistrySupplier<Item> COMPRESSED_FIREWORK_5 = ITEMS.register("compressed_firework_5", () ->
            new CompressedFireworkItem(new Item.Properties().stacksTo(64), 5));
    public static final RegistrySupplier<Item> COMPRESSED_FIREWORK_6 = ITEMS.register("compressed_firework_6", () ->
            new CompressedFireworkItem(new Item.Properties().stacksTo(64), 6));
    public static final RegistrySupplier<Item> COMPRESSED_FIREWORK_7 = ITEMS.register("compressed_firework_7", () ->
            new CompressedFireworkItem(new Item.Properties().stacksTo(64), 7));
    public static final RegistrySupplier<Item> COMPRESSED_FIREWORK_8 = ITEMS.register("compressed_firework_8", () ->
            new CompressedFireworkItem(new Item.Properties().stacksTo(64), 8));
    public static final RegistrySupplier<Item> COMPRESSED_FIREWORK_9 = ITEMS.register("compressed_firework_9", () ->
            new CompressedFireworkItem(new Item.Properties().stacksTo(64), 9));

    // 注册菜单类型
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(MOD_ID, Registries.MENU);
    public static final RegistrySupplier<MenuType<FillerMenu>> FILLER_MENU_TYPE = MENU_TYPES.register("filler_menu", () ->
            new MenuType<>(FillerMenu::new, FeatureFlags.VANILLA_SET));

    // Registering a new creative tab
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(MOD_ID, Registries.CREATIVE_MODE_TAB);
    public static final RegistrySupplier<CreativeModeTab> BLINK_TAB = TABS.register("blink_tab", () ->
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, -1)
                    .title(Component.translatable("itemGroup." + MOD_ID + ".blink_tab"))
                    .icon(() -> new ItemStack(Blink.DIMENSION_KEY.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(Blink.FILLER_ITEM.get());
                        output.accept(Blink.DIMENSION_KEY.get());
                        // 添加压缩烟花到创造模式标签
                        output.accept(Blink.COMPRESSED_FIREWORK_1.get());
                        output.accept(Blink.COMPRESSED_FIREWORK_2.get());
                        output.accept(Blink.COMPRESSED_FIREWORK_3.get());
                        output.accept(Blink.COMPRESSED_FIREWORK_4.get());
                        output.accept(Blink.COMPRESSED_FIREWORK_5.get());
                        output.accept(Blink.COMPRESSED_FIREWORK_6.get());
                        output.accept(Blink.COMPRESSED_FIREWORK_7.get());
                        output.accept(Blink.COMPRESSED_FIREWORK_8.get());
                        output.accept(Blink.COMPRESSED_FIREWORK_9.get());
                    })
                    .build());
    
    public static void init() {
        // 注册所有内容
        BLOCKS.register();
        ITEMS.register();
        MENU_TYPES.register();
        TABS.register();
        
        // 初始化维度（不再注册维度类型，只初始化维度键）
        BlinkDimensions.init();
        
        // 注册网络处理器
        NetworkHandler.init();
        
        System.out.println("Blink Mod initialized!");
        System.out.println("Filler item registered: " + FILLER_ITEM.getId());
        System.out.println("Dimension key registered: " + DIMENSION_KEY.getId());
        System.out.println("Portal block registered: " + BLINK_PORTAL_BLOCK.getId());
        System.out.println("Compressed fireworks registered!");
        System.out.println("Filler menu type registered: " + FILLER_MENU_TYPE.getId());
        System.out.println("Creative tab registered: " + BLINK_TAB.getId());
        System.out.println(BlinkExpectPlatform.getConfigDirectory().toAbsolutePath().normalize().toString());
    }
    
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
        BlinkCommand.register(dispatcher, commandBuildContext);
        System.out.println("Blink commands registered!");
    }
}