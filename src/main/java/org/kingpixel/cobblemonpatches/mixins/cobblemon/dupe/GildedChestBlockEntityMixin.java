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
    GildedChestBlockEntity chest = (GildedChestBlockEntity) (Object) this;
    if (chest == null) return false;
    BlockPos pos = chest.getPos();
    if (pos == null || player == null) return false;

    return player.squaredDistanceTo(
      pos.getX() + 0.5,
      pos.getY() + 0.5,
      pos.getZ() + 0.5
    ) <= MAX_DISTANCE_SQ;
  }

}
