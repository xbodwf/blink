package com.xbodw.blink.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class CompressedFireworkRecipeSerializer implements RecipeSerializer<CompressedFireworkRecipe> {
    public static final MapCodec<CompressedFireworkRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(r -> r.category())
        ).apply(instance, CompressedFireworkRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CompressedFireworkRecipe> STREAM_CODEC =
        StreamCodec.of(
            (buf, recipe) -> buf.writeUtf(recipe.category().getSerializedName()),
            buf -> {
                String name = buf.readUtf();
                for (CraftingBookCategory c : CraftingBookCategory.values()) {
                    if (c.getSerializedName().equals(name)) {
                        return new CompressedFireworkRecipe(c);
                    }
                }
                return new CompressedFireworkRecipe(CraftingBookCategory.MISC);
            }
        );

    @Override
    public MapCodec<CompressedFireworkRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, CompressedFireworkRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
