package org.kingpixel.cobblemonpatches.mixins.cobblemon.spawning;

import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.spawner.Spawner;
import java.util.ArrayList;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optimizes Spawner.copyInfluences() by filtering non-expired influences directly into a single
 * pre-sized ArrayList instead of allocating an intermediate list and copying it via toMutableList().
 */
@Mixin(value = Spawner.class, remap = false)
public interface SpawnerMixin {

  @Inject(method = "copyInfluences", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$fastCopyInfluences(CallbackInfoReturnable<List<SpawningInfluence>> cir) {
    List<SpawningInfluence> influences = ((Spawner) this).getInfluences();
    int size = influences.size();
    if (size == 0) {
      cir.setReturnValue(new ArrayList<>(0));
      return;
    }

    List<SpawningInfluence> copy = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      SpawningInfluence influence = influences.get(i);
      if (!influence.isExpired()) {
        copy.add(influence);
      }
    }
    cir.setReturnValue(copy);
  }
}
