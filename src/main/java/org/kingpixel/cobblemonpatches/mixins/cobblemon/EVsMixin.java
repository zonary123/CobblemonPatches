package org.kingpixel.cobblemonpatches.mixins.cobblemon;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.pokemon.EVs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin into {@link EVs} to optimize serialization codecs.
 * Overwrites default codecs to read directly from the underlying stats map
 * via {@link PokemonStatsAccessor}, avoiding unnecessary intermediate HashMap allocations.
 */
@Mixin(value = EVs.class, remap = false)
public abstract class EVsMixin {

  /**
   * Optimized Codec instance that directly accesses the backing stats map.
   */
  @Unique
  private static final Codec<EVs> OPTIMIZED_CODEC = Codec.unboundedMap(Stat.Companion.getPERMANENT_ONLY_CODEC(), Codec.intRange(0, EVs.MAX_STAT_VALUE))
    .comapFlatMap(
      map -> {
        int total = 0;
        for (int v : map.values()) total += v;
        if (total > EVs.MAX_TOTAL_VALUE) {
          return DataResult.error(() -> "EVs cannot exceed a total of " + EVs.MAX_TOTAL_VALUE);
        }
        EVs evs = Cobblemon.INSTANCE.getStatProvider().createEmptyEVs();
        map.forEach(evs::set);
        return DataResult.success(evs);
      },
      evs -> ((PokemonStatsAccessor) (Object) evs).cobblemonPatches$getStats()
    );

  /**
   * Optimized PacketCodec instance wrapping {@link #OPTIMIZED_CODEC} for network serialization.
   */
  @Unique private static final PacketCodec<ByteBuf, EVs> OPTIMIZED_STREAM_CODEC = PacketCodecs.codec(OPTIMIZED_CODEC);

  /**
   * Overrides the default EV codec with an optimized version that avoids intermediate collection allocations.
   *
   * @return the optimized EVs Codec
   * @author Carlos Varas Alonso
   * @reason Overwrite getCODEC to return the optimized codec that avoids copying the stats map to a new HashMap.
   */
  @Overwrite
  public static final @NotNull Codec<EVs> getCODEC() {
    return OPTIMIZED_CODEC;
  }

  /**
   * Overrides the default EV stream codec with the optimized packet codec.
   *
   * @return the optimized EVs PacketCodec
   * @author Carlos Varas Alonso
   * @reason Overwrite getSTREAM_CODEC to return the optimized stream codec based on the optimized codec.
   */
  @Overwrite
  public static final @NotNull PacketCodec<ByteBuf, EVs> getSTREAM_CODEC() {
    return OPTIMIZED_STREAM_CODEC;
  }
}
