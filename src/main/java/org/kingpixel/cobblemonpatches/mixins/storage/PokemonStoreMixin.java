package org.kingpixel.cobblemonpatches.mixins.storage;

import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.api.storage.StorePosition;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Carlos Varas Alonso - 25/11/2025 1:48
 */
@Mixin(value = PokemonStore.class, remap = false)
public abstract class PokemonStoreMixin<T extends StorePosition> {

  // Índice O(1) por UUID
  @Unique private final Map<UUID, Pokemon> cobblemon$uuidIndex = new ConcurrentHashMap<>();

  // -----------------------------------------------------------
  // 3. Mantener el índice cuando se remueve un Pokémon
  // -----------------------------------------------------------
  @Inject(method = "remove(Lcom/cobblemon/mod/common/pokemon/Pokemon;)Z",
          at = @At("HEAD"))
  private void cobblemon$onRemove(Pokemon pokemon, CallbackInfoReturnable<Boolean> cir) {
    if (pokemon != null) {
      cobblemon$uuidIndex.remove(pokemon.getUuid());
    }
  }

  // -----------------------------------------------------------
  // 4. Reemplazar get(UUID) por acceso O(1)
  // -----------------------------------------------------------
  @WrapMethod(method = "get(Ljava/util/UUID;)Lcom/cobblemon/mod/common/pokemon/Pokemon;")
  private Pokemon cobblemon$optimizedGet(UUID uuid, Operation<Pokemon> original) {
    return cobblemon$uuidIndex.computeIfAbsent(uuid, original::call);
  }
}

