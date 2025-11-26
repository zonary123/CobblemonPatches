package org.kingpixel.cobblemonpatches.mixins;

import com.cobblemon.mod.common.pokemon.Species;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Carlos Varas Alonso - 27/10/2025 6:05
 */
@Mixin(value = Species.class, remap = false)
public abstract class SpeciesMixin {

  // Improve performance of showdownId() by caching the result
  @Unique private String showdownIdCache = null;

  @Inject(method = "showdownId", at = @At("HEAD"), cancellable = true)
  private void PokemonMixin$HeadShowdownId(CallbackInfoReturnable<String> cir) {
    if (showdownIdCache != null) {
      cir.setReturnValue(showdownIdCache);
    }
  }

  @Inject(method = "showdownId", at = @At("RETURN"))
  private void PokemonMixin$ReturnShowdownId(CallbackInfoReturnable<String> cir) {
    if (showdownIdCache == null) showdownIdCache = cir.getReturnValue().intern();
  }
}
