package com.xbodw.blink;

import com.xbodw.blink.command.BlinkCommand;
import com.xbodw.blink.gui.FillerMenu;
import com.xbodw.blink.item.CompressedFireworkItem;
import com.xbodw.blink.item.CompressedWindChargeItem;
import com.xbodw.blink.item.FillerDataComponent;
import com.xbodw.blink.item.FillerItem;
import com.xbodw.blink.neoforge.BlinkPlatformImpl;
import com.xbodw.blink.neoforge.FillerPayload;
import com.xbodw.blink.neoforge.FillerPickBlockPayload;
import com.xbodw.blink.recipe.CompressedFireworkRecipe;
import com.xbodw.blink.recipe.CompressedFireworkRecipeSerializer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;

@Mod(Blink.MOD_ID)
public class BlinkNeoForge {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Blink.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Blink.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Blink.MOD_ID);
    private static final DeferredRegister<DataComponentType<?>> DATA = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Blink.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Blink.MOD_ID);

    private static final DeferredItem<FillerItem> FILLER_ITEM = ITEMS.register("filler",
            () -> new FillerItem(new Item.Properties().stacksTo(1).fireResistant()));

    private static final DeferredItem<CompressedFireworkItem> CF1 = ITEMS.register("compressed_firework_1", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 1));
    private static final DeferredItem<CompressedFireworkItem> CF2 = ITEMS.register("compressed_firework_2", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 2));
    private static final DeferredItem<CompressedFireworkItem> CF3 = ITEMS.register("compressed_firework_3", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 3));
    private static final DeferredItem<CompressedFireworkItem> CF4 = ITEMS.register("compressed_firework_4", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 4));
    private static final DeferredItem<CompressedFireworkItem> CF5 = ITEMS.register("compressed_firework_5", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 5));
    private static final DeferredItem<CompressedFireworkItem> CF6 = ITEMS.register("compressed_firework_6", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 6));
    private static final DeferredItem<CompressedFireworkItem> CF7 = ITEMS.register("compressed_firework_7", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 7));
    private static final DeferredItem<CompressedFireworkItem> CF8 = ITEMS.register("compressed_firework_8", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 8));
    private static final DeferredItem<CompressedFireworkItem> CF9 = ITEMS.register("compressed_firework_9", () -> new CompressedFireworkItem(new Item.Properties().stacksTo(64), 9));

    private static final DeferredItem<CompressedWindChargeItem> CWC1 = ITEMS.register("compressed_wind_charge_1", () -> new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 1));
    private static final DeferredItem<CompressedWindChargeItem> CWC2 = ITEMS.register("compressed_wind_charge_2", () -> new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 2));
    private static final DeferredItem<CompressedWindChargeItem> CWC3 = ITEMS.register("compressed_wind_charge_3", () -> new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 3));
    private static final DeferredItem<CompressedWindChargeItem> CWC4 = ITEMS.register("compressed_wind_charge_4", () -> new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 4));
    private static final DeferredItem<CompressedWindChargeItem> CWC5 = ITEMS.register("compressed_wind_charge_5", () -> new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 5));
    private static final DeferredItem<CompressedWindChargeItem> CWC6 = ITEMS.register("compressed_wind_charge_6", () -> new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 6));
    private static final DeferredItem<CompressedWindChargeItem> CWC7 = ITEMS.register("compressed_wind_charge_7", () -> new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 7));
    private static final DeferredItem<CompressedWindChargeItem> CWC8 = ITEMS.register("compressed_wind_charge_8", () -> new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 8));
    private static final DeferredItem<CompressedWindChargeItem> CWC9 = ITEMS.register("compressed_wind_charge_9", () -> new CompressedWindChargeItem(new Item.Properties().stacksTo(64), 9));

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
                        output.accept(CWC1.get());
                        output.accept(CWC2.get());
                        output.accept(CWC3.get());
                        output.accept(CWC4.get());
                        output.accept(CWC5.get());
                        output.accept(CWC6.get());
                        output.accept(CWC7.get());
                        output.accept(CWC8.get());
                        output.accept(CWC9.get());
                    })
                    .build());

    private static final DeferredHolder<DataComponentType<?>, DataComponentType<?>> FILLER_DATA = DATA.register("filler_data", () -> FillerDataComponent.FILLER_DATA);
    private static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> COMPRESSED_FIREWORK_SERIALIZER = RECIPE_SERIALIZERS.register("compressed_firework", CompressedFireworkRecipeSerializer::new);

    public BlinkNeoForge(IEventBus modBus) {
        BlinkPlatform.setImpl(new BlinkPlatformImpl());

        ITEMS.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);
        DATA.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);

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
                Blink.COMPRESSED_WIND_CHARGE_1 = CWC1.get();
                Blink.COMPRESSED_WIND_CHARGE_2 = CWC2.get();
                Blink.COMPRESSED_WIND_CHARGE_3 = CWC3.get();
                Blink.COMPRESSED_WIND_CHARGE_4 = CWC4.get();
                Blink.COMPRESSED_WIND_CHARGE_5 = CWC5.get();
                Blink.COMPRESSED_WIND_CHARGE_6 = CWC6.get();
                Blink.COMPRESSED_WIND_CHARGE_7 = CWC7.get();
                Blink.COMPRESSED_WIND_CHARGE_8 = CWC8.get();
                Blink.COMPRESSED_WIND_CHARGE_9 = CWC9.get();

                registerDispenserBehaviors();
            } else if (event.getRegistryKey() == Registries.MENU) {
                Blink.FILLER_MENU_TYPE = FILLER_MENU_TYPE.get();
            } else if (event.getRegistryKey() == Registries.CREATIVE_MODE_TAB) {
                Blink.BLINK_TAB = BLINK_TAB.get();
            } else if (event.getRegistryKey() == Registries.RECIPE_SERIALIZER) {
                Blink.COMPRESSED_FIREWORK_RECIPE_SERIALIZER = (RecipeSerializer<CompressedFireworkRecipe>) COMPRESSED_FIREWORK_SERIALIZER.get();
            }
        });

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        modBus.addListener(this::onRegisterPayloadHandlers);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        Blink.registerCommands(event.getDispatcher(), event.getBuildContext());
    }

    private void onServerTick(ServerTickEvent.Post event) {
        FillerItem.processPendingFills();
        BlinkCommand.processPendingCommandFills();
    }

    private void registerDispenserBehaviors() {
        var windChargeBehavior = new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource source, ItemStack stack) {
                Level level = source.level();
                BlockPos pos = source.pos();
                RandomSource random = level.getRandom();
                Direction direction = source.state().getValue(DispenserBlock.FACING);

                if (stack.getItem() instanceof CompressedWindChargeItem cwc) {
                    int count = cwc.getCompressionLevel();
                    double x = (double)pos.getX() + (double)direction.getStepX() * 1.125;
                    double y = (double)pos.getY() + (double)direction.getStepY() * 1.125;
                    double z = (double)pos.getZ() + (double)direction.getStepZ() * 1.125;
                    for (int i = 0; i < count; i++) {
                        WindCharge windCharge = new WindCharge(null, level, x, y, z);
                        windCharge.setDeltaMovement(
                                direction.getStepX() * 1.5 + (random.nextDouble() - 0.5) * 0.2,
                                direction.getStepY() * 1.5 + (random.nextDouble() - 0.5) * 0.2,
                                direction.getStepZ() * 1.5 + (random.nextDouble() - 0.5) * 0.2
                        );
                        level.addFreshEntity(windCharge);
                    }
                }

                stack.shrink(1);
                return stack;
            }
        };

        var fireworkBehavior = new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource source, ItemStack stack) {
                Level level = source.level();
                BlockPos pos = source.pos();
                RandomSource random = level.getRandom();
                Direction direction = source.state().getValue(DispenserBlock.FACING);

                if (stack.getItem() instanceof CompressedFireworkItem cfi) {
                    int rocketCount = Math.min(cfi.getCompressionLevel() * 3, 20);

                    double x = (double)pos.getX() + (double)direction.getStepX() * 1.125;
                    double y = (double)pos.getY() + (double)direction.getStepY() * 1.125;
                    double z = (double)pos.getZ() + (double)direction.getStepZ() * 1.125;

                    for (int i = 0; i < rocketCount; i++) {
                        ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
                        fireworkStack.set(net.minecraft.core.component.DataComponents.FIREWORKS, new Fireworks(
                                Math.min(cfi.getCompressionLevel() * 2, 30),
                                List.of(new FireworkExplosion(
                                        FireworkExplosion.Shape.BURST,
                                        IntArrayList.of(0xFF4444, 0xFFAA00),
                                        IntArrayList.of(0xFFFFFF),
                                        true, false
                                ))
                        ));
                        FireworkRocketEntity firework = new FireworkRocketEntity(level, fireworkStack,
                                null, x, y, z, true);
                        firework.setDeltaMovement(
                                direction.getStepX() * 0.5 + random.nextGaussian() * 0.1,
                                direction.getStepY() * 0.5 + random.nextGaussian() * 0.1,
                                direction.getStepZ() * 0.5 + random.nextGaussian() * 0.1
                        );
                        level.addFreshEntity(firework);
                    }
                }

                stack.shrink(1);
                return stack;
            }
        };

        registerBehavior(Blink.COMPRESSED_WIND_CHARGE_1, windChargeBehavior);
        registerBehavior(Blink.COMPRESSED_WIND_CHARGE_2, windChargeBehavior);
        registerBehavior(Blink.COMPRESSED_WIND_CHARGE_3, windChargeBehavior);
        registerBehavior(Blink.COMPRESSED_WIND_CHARGE_4, windChargeBehavior);
        registerBehavior(Blink.COMPRESSED_WIND_CHARGE_5, windChargeBehavior);
        registerBehavior(Blink.COMPRESSED_WIND_CHARGE_6, windChargeBehavior);
        registerBehavior(Blink.COMPRESSED_WIND_CHARGE_7, windChargeBehavior);
        registerBehavior(Blink.COMPRESSED_WIND_CHARGE_8, windChargeBehavior);
        registerBehavior(Blink.COMPRESSED_WIND_CHARGE_9, windChargeBehavior);

        registerBehavior(Blink.COMPRESSED_FIREWORK_1, fireworkBehavior);
        registerBehavior(Blink.COMPRESSED_FIREWORK_2, fireworkBehavior);
        registerBehavior(Blink.COMPRESSED_FIREWORK_3, fireworkBehavior);
        registerBehavior(Blink.COMPRESSED_FIREWORK_4, fireworkBehavior);
        registerBehavior(Blink.COMPRESSED_FIREWORK_5, fireworkBehavior);
        registerBehavior(Blink.COMPRESSED_FIREWORK_6, fireworkBehavior);
        registerBehavior(Blink.COMPRESSED_FIREWORK_7, fireworkBehavior);
        registerBehavior(Blink.COMPRESSED_FIREWORK_8, fireworkBehavior);
        registerBehavior(Blink.COMPRESSED_FIREWORK_9, fireworkBehavior);
    }

    private static void registerBehavior(Item item, DispenseItemBehavior behavior) {
        DispenserBlock.registerBehavior(item, behavior);
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
        registrar.playToServer(
                FillerPickBlockPayload.TYPE,
                FillerPickBlockPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        payload.packet().handle((net.minecraft.server.level.ServerPlayer) context.player());
                    });
                }
        );
    }
}
