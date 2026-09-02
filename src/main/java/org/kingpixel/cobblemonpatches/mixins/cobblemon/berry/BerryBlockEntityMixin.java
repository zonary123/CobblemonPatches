package org.kingpixel.cobblemonpatches.mixins.cobblemon.berry;

import com.cobblemon.mod.common.api.berry.Berries;
import com.cobblemon.mod.common.api.berry.Berry;
import com.cobblemon.mod.common.block.entity.BerryBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
    if (value > 0) {
      ci.cancel();
    }
  }

  @Inject(method = "TICKER$lambda$0", at = @At("HEAD"), cancellable = true)
  private static void cobblemonPatches$optimizeTicker(World world, BlockPos pos, BlockState state, BerryBlockEntity blockEntity, CallbackInfo ci) {
    if (world.isClient) {
      ci.cancel();
      return;
    }
    int timer = blockEntity.getStageTimer();
    if (timer > 1) {
      blockEntity.setStageTimer(timer - 1);
      ci.cancel();
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
