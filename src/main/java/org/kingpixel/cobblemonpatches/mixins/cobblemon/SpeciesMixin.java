package org.kingpixel.cobblemonpatches.mixins.cobblemon;

import com.cobblemon.mod.common.pokemon.Species;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link Species} to cache computed Showdown identifiers across Pokémon lookups.
 *
 * @author Carlos Varas Alonso
 */
@Mixin(value = Species.class, remap = false)
public abstract class SpeciesMixin {

  @Unique private String showdownIdCache = null;

  /**
   * Returns the cached showdown identifier if previously computed.
   *
   * @param cir callback returnable
   */
  @Inject(method = "showdownId", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$headShowdownId(CallbackInfoReturnable<String> cir) {
    if (showdownIdCache != null) {
      cir.setReturnValue(showdownIdCache);
    }
  }

  /**
   * Computes, interns, and caches the showdown identifier on first access.
   *
   * @param cir callback returnable
   */
  @Inject(method = "showdownId", at = @At("RETURN"))
  private void cobblemonPatches$returnShowdownId(CallbackInfoReturnable<String> cir) {
    if (showdownIdCache == null && cir.getReturnValue() != null) {
      showdownIdCache = cir.getReturnValue().intern();
    }
  }
}
