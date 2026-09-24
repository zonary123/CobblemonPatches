package org.kingpixel.cobblemonpatches.mixins.cobblemon.blockentity;

import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.jetbrains.annotations.Nullable;
import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

/**
 * Mixin into {@code PokemonPastureBlockEntity$Tethering} to memoize the {@link PCPosition}
 * of tethered pasture Pokémon, avoiding linear box scans on every tether retrieval.
 */
@Mixin(targets = "com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity$Tethering", remap = false)
public abstract class PokemonPastureBlockEntityTetheringMixin {

  @Shadow
  @Final
  private UUID pokemonId;

  @Unique
  private PCPosition cachedPCPosition;

  /**
   * Retrieves the Pokémon using cached PC position if available, updating the cache on lookup misses.
   *
   * @param pc       the player's PC store
   * @param uuid     the Pokemon's UUID
   * @param original original operation
   * @return the Pokemon instance or null if not found
   * @author MemencioPerez
   * @reason Cache the PCPosition for faster retrieval
   */
  @WrapOperation(method = "getPokemon", at = @At(value = "INVOKE", target = "Lcom/cobblemon/mod/common/api/storage/pc/PCStore;get(Ljava/util/UUID;)Lcom/cobblemon/mod/common/pokemon/Pokemon;"))
  public final @Nullable Pokemon getPokemon(PCStore pc, UUID uuid, Operation<Pokemon> original) {
    Pokemon pokemon;

    if (cachedPCPosition != null && (pokemon = pc.get(cachedPCPosition)) != null && pokemon.getUuid().equals(pokemonId)) {
      if (CobblemonPatches.getConfig().isDebug()) {
        CobblemonPatches.LOGGER.info("Pasture tether retrieved cached Pokemon {} at {}", pokemonId, cachedPCPosition);
      }
      return pokemon;
    }

    if ((pokemon = original.call(pc, uuid)) != null) {
      var storeCoordinates = pokemon.getStoreCoordinates().get();
      if (storeCoordinates != null && storeCoordinates.getPosition() instanceof PCPosition pcPosition) {
        cachedPCPosition = pcPosition;
        if (CobblemonPatches.getConfig().isDebug()) {
          CobblemonPatches.LOGGER.info("Pasture tether memoized position for Pokemon {}: {}", uuid, pcPosition);
        }
      }
      return pokemon;
    }

    return null;
  }
}
