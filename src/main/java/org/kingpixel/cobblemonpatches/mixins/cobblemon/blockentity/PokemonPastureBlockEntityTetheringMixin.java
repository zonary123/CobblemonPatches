package org.kingpixel.cobblemonpatches.mixins.cobblemon.blockentity;

import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(targets = "com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity$Tethering", remap = false)
public abstract class PokemonPastureBlockEntityTetheringMixin {

  @Shadow
  @Final
  private UUID pokemonId;

  @Unique
  private PCPosition cachedPCPosition;

  /**
   * @author MemencioPerez
   * @reason Cache the PCPosition for faster retrieval
   */
  @WrapOperation(method = "getPokemon", at = @At(value = "INVOKE", target = "Lcom/cobblemon/mod/common/api/storage/pc/PCStore;get(Ljava/util/UUID;)Lcom/cobblemon/mod/common/pokemon/Pokemon;"))
  public final @Nullable Pokemon getPokemon(PCStore pc, UUID uuid, Operation<Pokemon> original) {
    Pokemon pokemon;

    if (cachedPCPosition != null && (pokemon = pc.get(cachedPCPosition)) != null && pokemon.getUuid().equals(pokemonId)) {
      return pokemon;
    }

    if ((pokemon = original.call(pc, uuid)) != null) {
      var storeCoordinates = pokemon.getStoreCoordinates().get();
      if (storeCoordinates != null && storeCoordinates.getPosition() instanceof PCPosition pcPosition) {
        cachedPCPosition = pcPosition;
      }
      return pokemon;
    }

    return null;
  }
}
