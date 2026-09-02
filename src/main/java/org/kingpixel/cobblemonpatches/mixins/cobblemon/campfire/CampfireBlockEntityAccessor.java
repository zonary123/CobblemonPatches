package org.kingpixel.cobblemonpatches.mixins.cobblemon.campfire;

import com.cobblemon.mod.common.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CampfireBlockEntity.class, remap = false)
public interface CampfireBlockEntityAccessor {
  @Accessor("cookingProgress")
  int cobblemonPatches$getCookingProgress();

  @Accessor("cookingProgress")
  void cobblemonPatches$setCookingProgress(int value);

  @Accessor("cookingTotalTime")
  int cobblemonPatches$getCookingTotalTime();
}

