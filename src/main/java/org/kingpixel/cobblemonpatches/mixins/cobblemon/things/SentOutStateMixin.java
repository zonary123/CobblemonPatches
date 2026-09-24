package org.kingpixel.cobblemonpatches.mixins.cobblemon.things;

import com.cobblemon.mod.common.pokemon.activestate.SentOutState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin for {@link SentOutState} to ensure Pokémon recall operations run on the main server thread.
 * <p>
 * Calling recall asynchronously from network packet threads or background routines can result in
 * race conditions during entity removal or Pokémon state transitions. This mixin marshals the recall
 * call back to the main thread.
 * </p>
 *
 * @author Carlos Varas Alonso
 */
@Mixin(value = SentOutState.class, remap = false)
public abstract class SentOutStateMixin {

  /**
   * Wraps the recall operation to schedule execution on the main server thread if called off-thread.
   *
   * @param original The original recall operation.
   */
  @WrapMethod(method = "recall")
  private void ensureMain(Operation<Void> original) {
    if (CobblemonPatches.server != null && !CobblemonPatches.server.isOnThread()) {
      CobblemonPatches.server.execute(original::call);
      return;
    }

    original.call();
  }
}
