package org.kingpixel.cobblemonpatches.mixins.cobblemon.blockentity;

import com.cobblemon.mod.common.block.entity.PCBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optimizes PCBlockEntity ticking by throttling the expensive getInRangeViewerCount
 * player scans from 20 times per second to once every 20 ticks (1 second), evenly
 * distributed across ticks using the block position hash.
 */
@Mixin(value = PCBlockEntity.class, remap = false)
public abstract class PCBlockEntityTickerMixin {

  /**
   * Throttles player viewer distance scans to 1 Hz per block, distributed uniformly across ticks
   * based on the block's coordinate hash to prevent tick lag spikes.
   *
   * @param world       the world instance
   * @param pos         block position
   * @param state       block state
   * @param blockEntity PC block entity
   * @param ci          callback info
   */
  @Inject(method = "TICKER$lambda$0", at = @At("HEAD"), cancellable = true)
  private static void cobblemonPatches$throttlePCTicker(World world, BlockPos pos, BlockState state,
                                                        PCBlockEntity blockEntity, CallbackInfo ci) {
    if (world.isClient()) {
      ci.cancel();
      return;
    }

    if ((world.getTime() + Math.abs(pos.hashCode())) % 20L != 0L) {
      ci.cancel();
    }
  }
}
