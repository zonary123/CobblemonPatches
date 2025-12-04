package org.kingpixel.cobblemonpatches.mixins.berry;

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
 * @author Carlos Varas Alonso - 23/11/2025 22:13
 */
@Mixin(value = BerryBlock.class, remap = false)
public abstract class BerryBlockMixin {
  @Final @Shadow private Identifier berryIdentifier;
  @Unique private Berry berry;

  @Inject(method = "berry", at = @At("HEAD"), cancellable = true)
  private void berryBlockMixin$Berry(CallbackInfoReturnable<Berry> cir) {
    if (berry == null) {
      berry = Berries.getByIdentifier(berryIdentifier);
    }
    cir.setReturnValue(berry);
    cir.cancel();
  }

}
