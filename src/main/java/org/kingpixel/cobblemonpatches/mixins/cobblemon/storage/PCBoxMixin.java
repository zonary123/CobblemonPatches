package org.kingpixel.cobblemonpatches.mixins.cobblemon.storage;

import com.cobblemon.mod.common.api.storage.pc.PCBox;
import com.cobblemon.mod.common.pokemon.Pokemon;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Mixin into {@link PCBox} providing an allocation-free iterator directly traversing
 * the underlying Pokémon array and skipping null entries.
 */
@Mixin(value = PCBox.class, remap = false)
public abstract class PCBoxMixin implements Iterable<Pokemon> {

  @Shadow
  @Final
  private Pokemon[] pokemon;

  /**
   * Returns a lightweight iterator over non-null Pokemon in this PC box without allocating intermediate collections.
   *
   * @return iterator of present Pokemon
   * @author Carlos Varas Alonso
   * @reason Provide memory-efficient array iteration over non-null Pokemon entries.
   */
  @Overwrite
  @Override
  @NotNull
  public Iterator<Pokemon> iterator() {
    return new Iterator<>() {
      private final Pokemon[] data = pokemon;
      private int idx = 0;

      @Override
      public boolean hasNext() {
        while (idx < data.length && data[idx] == null) {
          idx++;
        }
        return idx < data.length;
      }

      @Override
      public Pokemon next() {
        while (hasNext()) {
          Pokemon p = data[idx++];
          if (p != null) {
            return p;
          }
        }
        throw new NoSuchElementException();
      }
    };
  }
}