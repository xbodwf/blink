package com.xbodw.blink.recipe;

import com.xbodw.blink.Blink;
import com.xbodw.blink.item.CompressedFireworkItem;
import com.xbodw.blink.item.CompressedWindChargeItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class CompressedFireworkRecipe extends CustomRecipe {
    private static final int FIREWORK_COUNT = 8;

    public CompressedFireworkRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        List<ItemStack> fireworks = new ArrayList<>();
        ItemStack catalyst = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (isFirework(stack) && fireworks.size() < FIREWORK_COUNT) {
                fireworks.add(stack);
            } else if (isCatalyst(stack) && catalyst.isEmpty()) {
                catalyst = stack;
            } else {
                return false;
            }
        }

        return fireworks.size() == FIREWORK_COUNT && !catalyst.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack catalyst = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (isCatalyst(stack)) {
                catalyst = stack;
                break;
            }
        }

        if (catalyst.isEmpty()) return ItemStack.EMPTY;

        int tier = getCatalystTier(catalyst);
        if (tier < 1 || tier > 9) return ItemStack.EMPTY;

        ItemStack result = getFireworkForTier(tier);
        if (result.isEmpty()) return ItemStack.EMPTY;

        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= FIREWORK_COUNT + 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Blink.COMPRESSED_FIREWORK_RECIPE_SERIALIZER;
    }

    private static boolean isFirework(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.FIREWORK_ROCKET) return true;
        if (item instanceof CompressedFireworkItem) return true;
        return false;
    }

    private static boolean isCatalyst(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof CompressedWindChargeItem) return true;
        if (item instanceof CompressedFireworkItem) return true;
        return false;
    }

    private static int getCatalystTier(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof CompressedWindChargeItem wc) return wc.getCompressionLevel();
        if (item instanceof CompressedFireworkItem fw) return fw.getCompressionLevel();
        return 0;
    }

    private static ItemStack getFireworkForTier(int tier) {
        return switch (tier) {
            case 1 -> new ItemStack(Blink.COMPRESSED_FIREWORK_1);
            case 2 -> new ItemStack(Blink.COMPRESSED_FIREWORK_2);
            case 3 -> new ItemStack(Blink.COMPRESSED_FIREWORK_3);
            case 4 -> new ItemStack(Blink.COMPRESSED_FIREWORK_4);
            case 5 -> new ItemStack(Blink.COMPRESSED_FIREWORK_5);
            case 6 -> new ItemStack(Blink.COMPRESSED_FIREWORK_6);
            case 7 -> new ItemStack(Blink.COMPRESSED_FIREWORK_7);
            case 8 -> new ItemStack(Blink.COMPRESSED_FIREWORK_8);
            case 9 -> new ItemStack(Blink.COMPRESSED_FIREWORK_9);
            default -> ItemStack.EMPTY;
        };
    }
}
