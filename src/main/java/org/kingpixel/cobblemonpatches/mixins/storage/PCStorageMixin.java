package org.kingpixel.cobblemonpatches.mixins.storage;

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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

@Mixin(value = PCStore.class, remap = false)
abstract class PCStorageMixin extends PokemonStore<PCPosition> {

  @Shadow @Final
  private List<PCBox> boxes;

  private static final Iterator<Pokemon> EMPTY = Collections.emptyIterator();

  @Overwrite
  @NotNull
  @Override
  public Iterator<Pokemon> iterator() {
    return new PCIterator(boxes);
  }

  private static final class PCIterator implements Iterator<Pokemon> {

    private final Iterator<PCBox> boxIterator;
    private Iterator<Pokemon> pokemonIterator;

    PCIterator(List<PCBox> boxes) {
      this.boxIterator = boxes.iterator();
      this.pokemonIterator = EMPTY;
    }

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
  }
}

