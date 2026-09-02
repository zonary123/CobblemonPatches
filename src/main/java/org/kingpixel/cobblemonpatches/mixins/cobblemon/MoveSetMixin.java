package org.kingpixel.cobblemonpatches.mixins.cobblemon;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Overwrite;
import java.util.Iterator;
import java.util.NoSuchElementException;

@Mixin(value = MoveSet.class, remap = false)
public abstract class MoveSetMixin {

  @Shadow private Move[] moves;

  /**
   * @author Carlos Varas Alonso
   * @reason Optimize iterator to avoid allocating intermediate lists from filterNotNull()
   */
  @Overwrite
  public Iterator<Move> iterator() {
    return new Iterator<Move>() {
      private int index = 0;
      private Move nextElement = null;
      private boolean hasSearched = false;

      private void findNext() {
        while (index < moves.length) {
          Move element = moves[index++];
          if (element != null) {
            nextElement = element;
            hasSearched = true;
            return;
          }
        }
        nextElement = null;
        hasSearched = true;
      }

      @Override
      public boolean hasNext() {
        if (!hasSearched) {
          findNext();
        }
        return nextElement != null;
      }

      @Override
      public Move next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        Move result = nextElement;
        hasSearched = false;
        return result;
      }
    };
  }
}
