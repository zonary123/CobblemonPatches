package org.kingpixel.cobblemonpatches.mixins.cobblemon.dupe;

import com.cobblemon.mod.common.block.entity.GildedChestBlockEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(value = GildedChestBlockEntity.class, remap = false)
public abstract class GildedChestBlockEntityMixin {

  @WrapMethod(method = "canPlayerUse")
  public boolean canPlayerUse(PlayerEntity player, Operation<Boolean> original) {
    double maxDistance = 8.0D;
    double maxDistanceSq = maxDistance * maxDistance;

    BlockPos pos = ((GildedChestBlockEntity) (Object) this).getPos();

    double distanceSq = player.squaredDistanceTo(
      pos.getX() + 0.5,
      pos.getY() + 0.5,
      pos.getZ() + 0.5
    );

    return distanceSq <= maxDistanceSq;
  }
}
