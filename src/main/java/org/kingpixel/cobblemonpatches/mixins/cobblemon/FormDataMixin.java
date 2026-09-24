package org.kingpixel.cobblemonpatches.mixins.cobblemon;

import com.cobblemon.mod.common.pokemon.FormData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link FormData} to cache calculated Showdown identifiers.
 * Avoids repeated string formatting and regex operations on frequent Showdown lookups.
 *
 * @author Carlos Varas Alonso
 */
@Mixin(value = FormData.class, remap = false)
public abstract class FormDataMixin {

  @Unique private String formOnlyShowdownIdCache = null;
  @Unique private String showdownIdCache = null;

  /**
   * Returns the cached full Showdown identifier if previously calculated.
   *
   * @param cir callback returnable containing the cached identifier
   */
  @Inject(method = "showdownId", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$headShowdownId(CallbackInfoReturnable<String> cir) {
    if (showdownIdCache != null) {
      cir.setReturnValue(showdownIdCache);
    }
  }

  /**
   * Caches and interns the newly computed full Showdown identifier.
   *
   * @param cir callback returnable containing the result
   */
  @Inject(method = "showdownId", at = @At("RETURN"))
  private void cobblemonPatches$returnShowdownId(CallbackInfoReturnable<String> cir) {
    if (showdownIdCache == null && cir.getReturnValue() != null) {
      showdownIdCache = cir.getReturnValue().intern();
    }
  }

  /**
   * Returns the cached form-only Showdown identifier if previously calculated.
   *
   * @param cir callback returnable containing the cached identifier
   */
  @Inject(method = "formOnlyShowdownId", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$headFormOnlyShowdownId(CallbackInfoReturnable<String> cir) {
    if (formOnlyShowdownIdCache != null) {
      cir.setReturnValue(formOnlyShowdownIdCache);
    }
  }

  /**
   * Caches and interns the newly computed form-only Showdown identifier.
   *
   * @param cir callback returnable containing the result
   */
  @Inject(method = "formOnlyShowdownId", at = @At("RETURN"))
  private void cobblemonPatches$returnFormOnlyShowdownId(CallbackInfoReturnable<String> cir) {
    if (formOnlyShowdownIdCache == null && cir.getReturnValue() != null) {
      formOnlyShowdownIdCache = cir.getReturnValue().intern();
    }
  }
}
