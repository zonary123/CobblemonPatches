package org.kingpixel.cobblemonpatches.mixins.cobblemon.campfire;

import com.cobblemon.mod.common.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor interface into {@link CampfireBlockEntity} exposing cooking progress and total time properties.
 */
@Mixin(value = CampfireBlockEntity.class, remap = false)
public interface CampfireBlockEntityAccessor {

  /**
   * Retrieves the current cooking progress ticks for the campfire entity.
   *
   * @return current cooking progress ticks
   */
  @Accessor("cookingProgress")
  int cobblemonPatches$getCookingProgress();

  /**
   * Sets the current cooking progress ticks for the campfire entity.
   *
   * @param value new cooking progress value
   */
  @Accessor("cookingProgress")
  void cobblemonPatches$setCookingProgress(int value);

  /**
   * Retrieves the total cooking duration ticks required for the active recipe.
   *
   * @return total cooking time in ticks
   */
  @Accessor("cookingTotalTime")
  int cobblemonPatches$getCookingTotalTime();
}

