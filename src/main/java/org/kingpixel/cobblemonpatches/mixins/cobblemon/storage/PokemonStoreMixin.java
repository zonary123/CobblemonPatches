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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Mixin for {@link PokemonStore} to provide indexed caching for Pokémon UUID lookups.
 * <p>
 * Standard Cobblemon store implementations perform linear scans over positions when looking
 * up Pokémon by UUID. This mixin injects a high-performance Caffeine cache mapping UUIDs
 * to their respective {@link Pokemon} instances, invalidating entries on removal or update.
 * </p>
 *
 * @param <T> The storage position coordinate type.
 * @author Carlos Varas Alonso
 */
@Mixin(value = PokemonStore.class, remap = false)
public abstract class PokemonStoreMixin<T extends StorePosition> {

  /**
   * Short-lived cache storing resolved Pokémon UUID mappings with a 5-minute write expiration.
   */
  @Unique
  private final Cache<UUID, Pokemon> cobblemon$uuidIndex = Caffeine.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build();

  /**
   * Wraps Pokémon removal to invalidate the cached UUID entry upon successful deletion.
   *
   * @param pokemon  The Pokémon being removed from this store.
   * @param original The original removal operation.
   * @return {@code true} if the Pokémon was removed successfully, {@code false} otherwise.
   */
  @WrapMethod(method = "remove(Lcom/cobblemon/mod/common/pokemon/Pokemon;)Z")
  private boolean optimizedRemove(Pokemon pokemon, Operation<Boolean> original) {
    boolean result = original.call(pokemon);

    if (result && pokemon != null) {
      cobblemon$uuidIndex.invalidate(pokemon.getUuid());
    }

    return result;
  }

  /**
   * Wraps position-based Pokémon removal to invalidate the cached UUID entry of the removed Pokémon.
   *
   * @param position The position from which the Pokémon is being removed.
   * @param original The original position removal operation.
   * @return {@code true} if a Pokémon was at the position and removed, {@code false} otherwise.
   */
  @WrapMethod(method = "remove(Lcom/cobblemon/mod/common/api/storage/StorePosition;)Z")
  private boolean optimizedRemovePosition(T position, Operation<Boolean> original) {
    PokemonStore<T> self = (PokemonStore<T>) (Object) this;
    Pokemon old = self.get(position);
    boolean result = original.call(position);

    if (result && old != null) {
      cobblemon$uuidIndex.invalidate(old.getUuid());
    }

    return result;
  }

  /**
   * Wraps setting a Pokémon at a given position, invalidating both old and new UUID entries to maintain cache consistency.
   *
   * @param position The target storage position.
   * @param pokemon  The new Pokémon to place at the position.
   * @param original The original set operation.
   */
  @WrapMethod(method = "set(Lcom/cobblemon/mod/common/api/storage/StorePosition;Lcom/cobblemon/mod/common/pokemon/Pokemon;)V")
  private void optimizedSet(T position, Pokemon pokemon, Operation<Void> original) {
    PokemonStore<T> self = (PokemonStore<T>) (Object) this;
    Pokemon old = self.get(position);
    original.call(position, pokemon);

    if (old != null) {
      cobblemon$uuidIndex.invalidate(old.getUuid());
    }
    if (pokemon != null) {
      cobblemon$uuidIndex.invalidate(pokemon.getUuid());
    }
  }

  /**
   * Optimizes UUID-based lookup by retrieving or computing values via the local Caffeine cache.
   *
   * @param uuid     The unique identifier of the target Pokémon.
   * @param original The original lookup operation performing standard iteration.
   * @return The cached or newly resolved {@link Pokemon}, or {@code null} if not found.
   */
  @WrapMethod(method = "get(Ljava/util/UUID;)Lcom/cobblemon/mod/common/pokemon/Pokemon;")
  private Pokemon cobblemon$optimizedGet(UUID uuid, Operation<Pokemon> original) {
    return cobblemon$uuidIndex.get(uuid, original::call);
  }
}


