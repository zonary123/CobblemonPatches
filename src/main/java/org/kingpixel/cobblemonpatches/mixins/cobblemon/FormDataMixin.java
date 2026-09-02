package org.kingpixel.cobblemonpatches.mixins.cobblemon;

import com.cobblemon.mod.common.pokemon.FormData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 *
 * @author Carlos Varas Alonso - 26/07/2026 19:13
 */
@Mixin(value = FormData.class, remap = false)
public abstract class FormDataMixin {

  @Unique private String formOnlyShowdownIdCache = null;
  @Unique private String showdownIdCache = null;

  @Inject(method = "showdownId", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$headShowdownId(CallbackInfoReturnable<String> cir) {
    if (showdownIdCache != null) {
      cir.setReturnValue(showdownIdCache);
    }
  }

  @Inject(method = "showdownId", at = @At("RETURN"))
  private void cobblemonPatches$returnShowdownId(CallbackInfoReturnable<String> cir) {
    if (showdownIdCache == null && cir.getReturnValue() != null) {
      showdownIdCache = cir.getReturnValue().intern();
    }
  }

  @Inject(method = "formOnlyShowdownId", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$headFormOnlyShowdownId(CallbackInfoReturnable<String> cir) {
    if (formOnlyShowdownIdCache != null) {
      cir.setReturnValue(formOnlyShowdownIdCache);
    }
  }

  @Inject(method = "formOnlyShowdownId", at = @At("RETURN"))
  private void cobblemonPatches$returnFormOnlyShowdownId(CallbackInfoReturnable<String> cir) {
    if (formOnlyShowdownIdCache == null && cir.getReturnValue() != null) {
      formOnlyShowdownIdCache = cir.getReturnValue().intern();
    }
  }
}
