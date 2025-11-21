package org.kingpixel.cobblemonpatches.mixins.pc;

import com.cobblemon.mod.common.api.storage.pc.PCBox;
import com.cobblemon.mod.common.pokemon.Pokemon;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.NoSuchElementException;


@Mixin(value = PCBox.class, remap = false)
public abstract class PCBoxMixin implements Iterable<Pokemon> {

  @Shadow
  @Final
  private Pokemon[] pokemon;

  @Overwrite
  @Override
  @NotNull
  public Iterator<Pokemon> iterator() {
    return new Iterator<>() {
      private final Pokemon[] data = pokemon;
      private int idx = 0;

      @Override
      public boolean hasNext() {
        while (idx < data.length && data[idx] == null) idx++;
        return idx < data.length;
      }

      @Override
      public Pokemon next() {
        if (!hasNext()) throw new NoSuchElementException();
        return data[idx++];
      }
    };
  }
}

