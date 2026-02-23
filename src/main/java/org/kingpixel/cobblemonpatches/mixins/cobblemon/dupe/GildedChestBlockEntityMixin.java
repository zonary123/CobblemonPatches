package org.kingpixel.cobblemonpatches.mixins.cobblemon.dupe;

import com.cobblemon.mod.common.block.entity.GildedChestBlockEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(value = GildedChestBlockEntity.class, remap = false)
public abstract class GildedChestBlockEntityMixin {

  @Unique
  private static final double MAX_DISTANCE_SQ = 8.0D * 8.0D;

  @WrapMethod(method = "canPlayerUse")
  public boolean canPlayerUse(PlayerEntity player, Operation<Boolean> original) {
    GildedChestBlockEntity instance = ((GildedChestBlockEntity) (Object) this);

    BlockPos pos = instance.getPos();

    double distanceSq = player.squaredDistanceTo(
      pos.getX() + 0.5,
      pos.getY() + 0.5,
      pos.getZ() + 0.5
    );
    return distanceSq <= MAX_DISTANCE_SQ;
  }

}
