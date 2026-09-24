package org.kingpixel.cobblemonpatches.mixins.cobblemon.berry;

import com.cobblemon.mod.common.api.berry.Berries;
import com.cobblemon.mod.common.api.berry.Berry;
import com.cobblemon.mod.common.block.BerryBlock;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link BerryBlock} to cache the resolved {@link Berry} model instance,
 * avoiding repetitive identifier lookups in the berry registry.
 *
 * @author Carlos Varas Alonso
 */
@Mixin(value = BerryBlock.class, remap = false)
public abstract class BerryBlockMixin {
  @Final @Shadow private Identifier berryIdentifier;
  @Unique private Berry berry;

  /**
   * Returns the cached {@link Berry} or resolves it once from the berry registry.
   *
   * @param cir callback returnable
   */
  @Inject(method = "berry", at = @At("HEAD"), cancellable = true)
  private void berryBlockMixin$Berry(CallbackInfoReturnable<Berry> cir) {
    if (berry == null) {
      berry = Berries.getByIdentifier(berryIdentifier);
    }
    cir.setReturnValue(berry);
    cir.cancel();
  }
}
