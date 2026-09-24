package org.kingpixel.cobblemonpatches.mixins.cobblemon;

import com.cobblemon.mod.common.pokemon.PokemonStats;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

/**
 * Accessor interface into {@link PokemonStats} to expose the internal stats map directly
 * for high-performance serialization and inspection without map copying.
 */
@Mixin(value = PokemonStats.class, remap = false)
public interface PokemonStatsAccessor {

  /**
   * Retrieves the raw backing map of stats to their values.
   *
   * @return the backing stat map
   */
  @Accessor("stats")
  Map<Stat, Integer> cobblemonPatches$getStats();
}
