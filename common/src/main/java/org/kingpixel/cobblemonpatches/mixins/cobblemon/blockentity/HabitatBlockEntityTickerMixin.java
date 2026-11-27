package org.kingpixel.cobblemonpatches.mixins.cobblemon.blockentity;

import com.cobblemon.mod.common.api.habitats.spawningstyle.ActivatedHabitatSpawning;
import com.cobblemon.mod.common.block.habitat.HabitatBlockEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Optimizes HabitatBlockEntity ticking by throttling ActivatedHabitatSpawning activations
 * from every tick (20 Hz) to once every 20 ticks (1 Hz), distributed uniformly across ticks
 * based on the block's coordinate hash, and skipping activations when no alive non-spectator
 * player is within range.
 */
@Mixin(value = HabitatBlockEntity.class, remap = false)
public abstract class HabitatBlockEntityTickerMixin {

  @Unique
  private static final double MIN_PLAYER_ACTIVATION_RANGE = 64.0;

  @Unique
  private static final double PLAYER_RANGE_BUFFER = 48.0;

  private HabitatBlockEntityTickerMixin() {
  }

  /**
   * Throttles the continuous TICK trigger activation of habitat blocks to 1 Hz,
   * staggered uniformly by coordinate hash to avoid tick spikes, and skips spawning
   * work when no players are nearby (e.g. spawn chunks or force-loaded areas).
   */
  @WrapOperation(
      method = "TICKER$lambda$0",
      at = @At(
          value = "INVOKE",
          target = "Lcom/cobblemon/mod/common/api/habitats/spawningstyle/ActivatedHabitatSpawning;activate",
          ordinal = 0
      )
  )
  private static void cobblemonPatches$throttleActivatedHabitatSpawning(
      ActivatedHabitatSpawning instance,
      ServerLevel world,
      BlockPos pos,
      Operation<Void> original
  ) {
    if (Math.floorMod(world.getGameTime() + pos.hashCode(), 20L) != 0L) {
      return;
    }

    double activationRange = Math.max(MIN_PLAYER_ACTIVATION_RANGE, instance.getSpawnRange() + PLAYER_RANGE_BUFFER);
    if (!world.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, activationRange)) {
      return;
    }

    original.call(instance, world, pos);
  }
}
