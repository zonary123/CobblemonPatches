package org.kingpixel.cobblemonpatches.mixins.cobblemon.storage;

import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.api.storage.StorePosition;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 25/11/2025 1:48
 */
@Mixin(value = PokemonStore.class, remap = false)
public abstract class PokemonStoreMixin<T extends StorePosition> {

  @Unique private final Cache<UUID, Pokemon> cobblemon$uuidIndex = Caffeine.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build();

  @Inject(method = "remove(Lcom/cobblemon/mod/common/pokemon/Pokemon;)Z",
          at = @At("HEAD"))
  private void cobblemon$onRemove(Pokemon pokemon, CallbackInfoReturnable<Boolean> cir) {
    if (pokemon != null) {
      cobblemon$uuidIndex.invalidate(pokemon.getUuid());
    }
  }

  @WrapMethod(method = "get(Ljava/util/UUID;)Lcom/cobblemon/mod/common/pokemon/Pokemon;")
  private Pokemon cobblemon$optimizedGet(UUID uuid, Operation<Pokemon> original) {
    return cobblemon$uuidIndex.get(uuid, original::call);
  }
}

