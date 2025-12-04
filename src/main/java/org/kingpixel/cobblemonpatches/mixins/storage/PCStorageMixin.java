package org.kingpixel.cobblemonpatches.mixins.storage;

import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.api.storage.pc.PCBox;
import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

@Mixin(value = PCStore.class, remap = false)
public abstract class PCStorageMixin extends PokemonStore<PCPosition> {

  @Shadow @Final
  private List<PCBox> boxes;

  @Unique private static final Iterator<Pokemon> EMPTY = Collections.emptyIterator();

  /**
   * @author
   * @reason Iterate through all boxes to get all Pokemons in the PCStore
   */
  @Overwrite
  @NotNull
  @Override
  public Iterator<Pokemon> iterator() {
    return new Iterator<>() {
      private final Iterator<PCBox> boxIterator = boxes.iterator();
      private Iterator<Pokemon> pokemonIterator = EMPTY;

      @Override
      public boolean hasNext() {
        if (pokemonIterator.hasNext()) return true;

        while (boxIterator.hasNext()) {
          pokemonIterator = boxIterator.next().iterator();
          if (pokemonIterator.hasNext()) return true;
        }
        return false;
      }

      @Override
      public Pokemon next() {
        if (!hasNext()) throw new NoSuchElementException();
        return pokemonIterator.next();
      }
    };
  }
}

