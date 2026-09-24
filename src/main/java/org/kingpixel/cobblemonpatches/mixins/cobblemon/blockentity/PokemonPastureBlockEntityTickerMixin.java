package org.kingpixel.cobblemonpatches.mixins.cobblemon.blockentity;

import com.cobblemon.mod.common.block.PastureBlock;
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Optimizes PokemonPastureBlockEntity ticking by throttling player viewer scans
 * from 20 times per second to once every 20 ticks (1 second), distributed across ticks.
 */
@Mixin(value = PokemonPastureBlockEntity.class, remap = false)
public abstract class PokemonPastureBlockEntityTickerMixin {

  /**
   * Throttles pasture viewer range checks to once every 20 ticks (1 Hz), distributed uniformly across ticks
   * based on the block's coordinate hash, falling back to the current block state when un-ticked.
   *
   * @param instance pasture block entity
   * @param world    world instance
   * @param pos      block position
   * @param distance viewer radius
   * @param flags    scan flags
   * @param obj      extra parameter object
   * @param original wrapped method operation
   * @return viewer count
   */
  @WrapOperation(
      method = "TICKER$lambda$0",
      at = @At(
          value = "INVOKE",
          target = "Lcom/cobblemon/mod/common/block/entity/PokemonPastureBlockEntity;getInRangeViewerCount$default(Lcom/cobblemon/mod/common/block/entity/PokemonPastureBlockEntity;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;DILjava/lang/Object;)I"
      )
  )
  private static int cobblemonPatches$throttlePastureViewerCheck(
      PokemonPastureBlockEntity instance,
      World world,
      BlockPos pos,
      double distance,
      int flags,
      Object obj,
      Operation<Integer> original
  ) {
    if (world.isClient()) {
      return 0;
    }

    if ((world.getTime() + Math.abs(pos.hashCode())) % 20L != 0L) {
      BlockState state = world.getBlockState(pos);
      if (state.contains(PastureBlock.Companion.getON()) && Boolean.TRUE.equals(state.get(PastureBlock.Companion.getON()))) {
        return 1;
      }
      return 0;
    }

    return original.call(instance, world, pos, distance, flags, obj);
  }
}
