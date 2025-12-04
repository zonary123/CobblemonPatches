package org.kingpixel.cobblemonpatches.mixins.things;

import com.cobblemon.mod.common.pokemon.activestate.SentOutState;
import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Carlos Varas Alonso - 24/11/2025 6:59
 */
@Mixin(value = SentOutState.class, remap = false)
public abstract class SentOutStateMixin {

  @Inject(method = "recall", at = @At("HEAD"), cancellable = true)
  private void onRecallHead(CallbackInfo ci) {
    SentOutState thisState = (SentOutState) (Object) this;
    CobblemonPatches.server.execute(() -> {
      var entity = thisState.getEntity();
      if (entity == null) return;
      entity.discard();
    });
    ci.cancel();
  }


}

