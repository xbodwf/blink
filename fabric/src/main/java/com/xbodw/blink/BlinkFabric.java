package com.xbodw.blink;

import com.xbodw.blink.command.BlinkCommand;
import com.xbodw.blink.fabric.BlinkPlatformImpl;
import com.xbodw.blink.item.CompressedFireworkItem;
import com.xbodw.blink.item.CompressedWindChargeItem;
import com.xbodw.blink.item.FillerItem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

import java.util.List;

public class BlinkFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BlinkPlatform.setImpl(new BlinkPlatformImpl());
        Blink.fabricRegisterAll();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            Blink.registerCommands(dispatcher, registryAccess);
        });

        PayloadTypeRegistry.playC2S().register(FillerPayload.TYPE, FillerPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FillerPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                payload.packet().handle(context.player());
            });
        });

        PayloadTypeRegistry.playC2S().register(FillerPickBlockPayload.TYPE, FillerPickBlockPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FillerPickBlockPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                payload.packet().handle(context.player());
            });
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            FillerItem.processPendingFills();
            BlinkCommand.processPendingCommandFills();
        });

        registerDispenserBehaviors();
    }

    private void registerDispenserBehaviors() {
        DispenseItemBehavior windChargeBehavior = new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource source, ItemStack stack) {
                Level level = source.level();
                BlockPos pos = source.pos();
                RandomSource random = level.getRandom();
                Direction direction = source.state().getValue(DispenserBlock.FACING);

                if (stack.getItem() instanceof CompressedWindChargeItem cwc) {
                    int count = cwc.getCompressionLevel();
                    for (int i = 0; i < count; i++) {
                        WindCharge windCharge = new WindCharge(null, level, pos.getX(), pos.getY(), pos.getZ());
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

        DispenseItemBehavior fireworkBehavior = new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource source, ItemStack stack) {
                Level level = source.level();
                BlockPos pos = source.pos();
                RandomSource random = level.getRandom();
                Direction direction = source.state().getValue(DispenserBlock.FACING);

                if (stack.getItem() instanceof CompressedFireworkItem cfi) {
                    int rocketCount = Math.min(cfi.getCompressionLevel() * 3, 20);

                    for (int i = 0; i < rocketCount; i++) {
                        ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
                        fireworkStack.set(DataComponents.FIREWORKS, new Fireworks(
                                Math.min(cfi.getCompressionLevel() * 2, 30),
                                List.of(new FireworkExplosion(
                                        FireworkExplosion.Shape.BURST,
                                        IntArrayList.of(0xFF4444, 0xFFAA00),
                                        IntArrayList.of(0xFFFFFF),
                                        true, false
                                ))
                        ));
                        FireworkRocketEntity firework = new FireworkRocketEntity(level, fireworkStack,
                                null, pos.getX(), pos.getY(), pos.getZ(), true);
                        firework.setDeltaMovement(
                                direction.getStepX() * 0.5 + (random.nextDouble() - 0.5) * 0.2,
                                0.5 + random.nextDouble() * 0.3,
                                direction.getStepZ() * 0.5 + (random.nextDouble() - 0.5) * 0.2
                        );
                        level.addFreshEntity(firework);
                    }
                }

                stack.shrink(1);
                return stack;
            }
        };

        registerItemBehavior(Blink.COMPRESSED_WIND_CHARGE_1, windChargeBehavior);
        registerItemBehavior(Blink.COMPRESSED_WIND_CHARGE_2, windChargeBehavior);
        registerItemBehavior(Blink.COMPRESSED_WIND_CHARGE_3, windChargeBehavior);
        registerItemBehavior(Blink.COMPRESSED_WIND_CHARGE_4, windChargeBehavior);
        registerItemBehavior(Blink.COMPRESSED_WIND_CHARGE_5, windChargeBehavior);
        registerItemBehavior(Blink.COMPRESSED_WIND_CHARGE_6, windChargeBehavior);
        registerItemBehavior(Blink.COMPRESSED_WIND_CHARGE_7, windChargeBehavior);
        registerItemBehavior(Blink.COMPRESSED_WIND_CHARGE_8, windChargeBehavior);
        registerItemBehavior(Blink.COMPRESSED_WIND_CHARGE_9, windChargeBehavior);

        registerItemBehavior(Blink.COMPRESSED_FIREWORK_1, fireworkBehavior);
        registerItemBehavior(Blink.COMPRESSED_FIREWORK_2, fireworkBehavior);
        registerItemBehavior(Blink.COMPRESSED_FIREWORK_3, fireworkBehavior);
        registerItemBehavior(Blink.COMPRESSED_FIREWORK_4, fireworkBehavior);
        registerItemBehavior(Blink.COMPRESSED_FIREWORK_5, fireworkBehavior);
        registerItemBehavior(Blink.COMPRESSED_FIREWORK_6, fireworkBehavior);
        registerItemBehavior(Blink.COMPRESSED_FIREWORK_7, fireworkBehavior);
        registerItemBehavior(Blink.COMPRESSED_FIREWORK_8, fireworkBehavior);
        registerItemBehavior(Blink.COMPRESSED_FIREWORK_9, fireworkBehavior);
    }

    private static void registerItemBehavior(Item item, DispenseItemBehavior behavior) {
        DispenserBlock.registerBehavior(item, behavior);
    }
}
