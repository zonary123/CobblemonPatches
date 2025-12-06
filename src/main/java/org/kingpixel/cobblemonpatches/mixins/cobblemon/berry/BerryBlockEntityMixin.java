package org.kingpixel.cobblemonpatches.mixins.cobblemon.berry;

import com.cobblemon.mod.common.api.berry.Berries;
import com.cobblemon.mod.common.api.berry.Berry;
import com.cobblemon.mod.common.block.entity.BerryBlockEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Carlos Varas Alonso - 23/11/2025 22:13
 */
@Mixin(value = BerryBlockEntity.class, remap = false)
public abstract class BerryBlockEntityMixin {
  @Shadow public Identifier berryIdentifier;
  @Shadow private int stageTimer;
  @Unique private Berry berry;
  @Unique private short ticks;

  @Inject(method = "berry", at = @At("HEAD"), cancellable = true)
  private void berryBlockMixin$Berry(CallbackInfoReturnable<Berry> cir) {
    if (berry == null) {
      berry = Berries.getByIdentifier(berryIdentifier);
    }
    cir.setReturnValue(berry);
    cir.cancel();
  }

  @Inject(method = "setStageTimer", at = @At("HEAD"), cancellable = true)
  private void berryBlockEntityMixin$SetStageTimer(int value, CallbackInfo ci) {
    this.stageTimer = value;
    if (ticks < 20) {
      ticks++;
      ci.cancel();
    } else {
      ticks = 0;
    }
  }


  @Redirect(
    method = "resetGrowTimers",
    at = @At(
      value = "INVOKE",
      target = "Lcom/cobblemon/mod/common/api/berry/Berries;getByIdentifier(Lnet/minecraft/util/Identifier;)Lcom/cobblemon/mod/common/api/berry/Berry;"
    )
  )
  private Berry redirectGetBerry(Identifier identifier) {
    if (berry == null) {
      berry = Berries.getByIdentifier(berryIdentifier);
    }
    return berry;
  }

}
