package org.kingpixel.cobblemonpatches.mixins.cobblemon.dupe;

import com.cobblemon.mod.common.block.entity.GildedChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = GildedChestBlockEntity.class, remap = false)
public abstract class GildedChestBlockEntityMixin {

  @Inject(
    method = "canPlayerUse",
    at = @At("HEAD"),
    cancellable = true
  )
  private void checkDistance(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
    if (player.isSpectator()) {
      cir.setReturnValue(false);
      return;
    }
    GildedChestBlockEntity instance = (GildedChestBlockEntity) (Object) this;
    double maxDistance = 8.0D;
    double maxDistanceSq = maxDistance * maxDistance;
    
    BlockPos pos = instance.getPos();
    double distanceSq = player.squaredDistanceTo(
      pos.getX() + 0.5,
      pos.getY() + 0.5,
      pos.getZ() + 0.5
    );

    if (distanceSq > maxDistanceSq) cir.setReturnValue(false);
  }
}
