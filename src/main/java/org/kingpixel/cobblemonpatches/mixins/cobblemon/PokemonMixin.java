package org.kingpixel.cobblemonpatches.mixins.cobblemon;

import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Carlos Varas Alonso - 27/10/2025 6:05
 */
@Mixin(value = Pokemon.class, remap = false)
public abstract class PokemonMixin {

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

  @Inject(method = "setSpecies", at = @At("HEAD"))
  private void PokemonMixin$setSpecies(Species value, CallbackInfo ci) {
    showdownIdCache = null;
  }

  @Inject(method = "setForm", at = @At("HEAD"))
  private void PokemonMixin$setForm(FormData value, CallbackInfo ci) {
    showdownIdCache = null;
  }
}
