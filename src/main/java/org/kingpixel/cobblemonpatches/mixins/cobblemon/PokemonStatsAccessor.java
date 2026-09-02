package org.kingpixel.cobblemonpatches.mixins.cobblemon;

import com.cobblemon.mod.common.pokemon.PokemonStats;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

@Mixin(value = PokemonStats.class, remap = false)
public interface PokemonStatsAccessor {
  @Accessor("stats")
  Map<Stat, Integer> cobblemonPatches$getStats();
}
