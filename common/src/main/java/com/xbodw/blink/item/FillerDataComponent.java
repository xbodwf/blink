package com.xbodw.blink.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

public class FillerDataComponent {
    public record FillerData(
            Optional<Long> pos1,
            Optional<Long> pos2,
            Optional<String> fillBlock,
            String fillMode,
            String state
    ) {
        public static final Codec<FillerData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.LONG.optionalFieldOf("pos1").forGetter(FillerData::pos1),
                        Codec.LONG.optionalFieldOf("pos2").forGetter(FillerData::pos2),
                        Codec.STRING.optionalFieldOf("fillBlock").forGetter(FillerData::fillBlock),
                        Codec.STRING.fieldOf("fillMode").forGetter(FillerData::fillMode),
                        Codec.STRING.fieldOf("state").forGetter(FillerData::state)
                ).apply(instance, FillerData::new)
        );
    }

    public static final DataComponentType<FillerData> FILLER_DATA = DataComponentType.<FillerData>builder()
            .persistent(FillerData.CODEC)
            .networkSynchronized(ByteBufCodecs.fromCodec(FillerData.CODEC))
            .build();

    public static FillerData empty() {
        return new FillerData(Optional.empty(), Optional.empty(), Optional.empty(), "FILL", "SELECTING_POS1");
    }
}
