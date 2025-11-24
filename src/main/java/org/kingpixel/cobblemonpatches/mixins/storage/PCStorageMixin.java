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

  @Shadow
  @Final
  private List<PCBox> boxes;


  @Overwrite
  @NotNull
  @Override
  public Iterator<Pokemon> iterator() {
    return new Iterator<>() {
      private final Iterator<PCBox> boxIt = boxes.iterator();
      private Iterator<Pokemon> currentBoxIt = Collections.emptyIterator();

      @Override
      public boolean hasNext() {
        while (!currentBoxIt.hasNext() && boxIt.hasNext()) {
          currentBoxIt = boxIt.next().iterator();
        }
        return currentBoxIt.hasNext();
      }

      @Override
      public Pokemon next() {
        if (!hasNext()) throw new NoSuchElementException();
        return currentBoxIt.next();
      }
    };
  }
}
