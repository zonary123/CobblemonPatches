package org.kingpixel.cobblemonpatches.mixins.cobblemon.storage;

import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.api.storage.pc.PCBox;
import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Mixin for {@link PCStore} to optimize iterating through all Pokémon across all boxes.
 * <p>
 * Provides a lazy sequential iterator that avoids eagerly collecting all Pokémon into
 * intermediate collections when traversing PC storage, while snapshotting the box list
 * to prevent concurrent modification exceptions during multi-tick sync operations.
 * </p>
 */
@Mixin(value = PCStore.class, remap = false)
public abstract class PCStoreMixin extends PokemonStore<PCPosition> {

  @Shadow @Final
  private List<PCBox> boxes;

  /**
   * Reusable empty iterator fallback when advancing past empty boxes.
   */
  @Unique private static final Iterator<Pokemon> EMPTY = Collections.emptyIterator();

  /**
   * Overwrites the default PC store iterator to lazily traverse Pokémon across all boxes.
   *
   * @return A lazy {@link Iterator} yielding all Pokémon stored across every PC box.
   * @author Carlos Varas Alonso
   * @reason Lazy evaluation avoids allocating intermediate collections for large PC inventories,
   * while box array snapshotting prevents comodification crashes during multi-tick syncs.
   */
  @Overwrite
  @NotNull
  @Override
  public Iterator<Pokemon> iterator() {
    return new Iterator<>() {
      private final PCBox[] boxArray = boxes.toArray(new PCBox[0]);
      private int boxIndex = 0;
      private Iterator<Pokemon> pokemonIterator = EMPTY;

      @Override
      public boolean hasNext() {
        while (!pokemonIterator.hasNext() && boxIndex < boxArray.length) {
          PCBox box = boxArray[boxIndex++];
          if (box != null) {
            pokemonIterator = box.iterator();
          }
        }
        return pokemonIterator.hasNext();
      }

      @Override
      public Pokemon next() {
        if (!hasNext()) throw new NoSuchElementException();
        return pokemonIterator.next();
      }
    };
  }
}