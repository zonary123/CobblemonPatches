package org.kingpixel.cobblemonpatches.mixins.cobblemon.things;

import com.cobblemon.mod.common.pokemon.activestate.SentOutState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.spongepowered.asm.mixin.Mixin;

/**
 * @author Carlos Varas Alonso - 24/11/2025 6:59
 */
@Mixin(value = SentOutState.class, remap = false)
public abstract class SentOutStateMixin {

  @WrapMethod(method = "recall")
  private void ensureMain(Operation<Void> original) {
    if (!CobblemonPatches.server.isOnThread()) {
      CobblemonPatches.server.execute(original::call);
      return;
    }

    original.call();
  }
}
