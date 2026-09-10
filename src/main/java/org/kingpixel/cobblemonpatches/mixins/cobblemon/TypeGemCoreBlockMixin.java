package org.kingpixel.cobblemonpatches.mixins.cobblemon;

import com.cobblemon.mod.common.api.tags.CobblemonBlockTags;
import com.cobblemon.mod.common.block.TypeGemClusterBlock;
import com.cobblemon.mod.common.block.TypeGemCoreBlock;
import kotlin.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optimizes TypeGemCoreBlock growth during world generation and random ticking.
 * Eliminates massive allocations from BFS (LinkedList/HashSet/Pair), removes redundant
 * collections shuffling, avoids map/registry key conversions, and skips wasteful block updates.
 */
@Mixin(value = TypeGemCoreBlock.class, remap = false)
public abstract class TypeGemCoreBlockMixin {

  @Unique
  private static final Direction[] DIRECTIONS = Direction.values();

  @Inject(method = "forceGrow", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatchesForceGrow(StructureWorldAccess level, BlockPos pos, Random random, float percentage,
                                         CallbackInfo ci) {
    float desiredLength = TypeGemCoreBlock.MAX_CONNECTED_GEMS * percentage;
    while (true) {
      Pair<Boolean, Integer> growInfo = performOptimizedGrow(level, pos, random, true);
      boolean grown = growInfo.getFirst();
      int clusterSize = growInfo.getSecond();

      if (!grown || clusterSize >= desiredLength) {
        break;
      }
    }
    ci.cancel();
  }

  @Inject(method = "grow", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatchesGrow(StructureWorldAccess level, BlockPos pos, Random random, boolean forced,
                                    CallbackInfoReturnable<Pair<Boolean, Integer>> cir) {
    cir.setReturnValue(performOptimizedGrow(level, pos, random, forced));
    cir.cancel();
  }

  @Unique
  private Pair<Boolean, Integer> performOptimizedGrow(StructureWorldAccess level, BlockPos pos,
                                                      Random random, boolean forced) {
    BlockPos[] gemPositions = new BlockPos[16];
    BlockState[] gemStates = new BlockState[16];
    int gemCount = collectConnectedGems(level, pos, gemPositions, gemStates);

    if (gemCount >= TypeGemCoreBlock.MAX_CONNECTED_GEMS) {
      if (!hasBreathingRoom(level, gemPositions, gemCount)) {
        return new Pair<>(false, gemCount);
      }
      updateStuntState(level, gemPositions, gemCount, true);
    }

    int[] gemIndices = new int[gemCount];
    for (int i = 0; i < gemCount; i++) {
      gemIndices[i] = i;
    }
    shuffleIndices(gemIndices, gemCount, random);

    for (int g = 0; g < gemCount; g++) {
      int idx = gemIndices[g];
      BlockPos gemPos = gemPositions[idx];
      BlockState gemState = gemStates[idx];

      TypeGemClusterBlock clusterBlock = TypeGemClusterBlock.Companion.clusterFromGemBlock(gemState.getBlock());
      if (clusterBlock == null) {
        continue;
      }


      int newGemCount = tryGrowFromGem(level, gemPos, clusterBlock, random, forced, gemCount);
      if (newGemCount > 0) {
        return new Pair<>(true, newGemCount);
      }
    }

    return new Pair<>(false, gemCount);
  }

  @Unique
  private static int tryGrowFromGem(StructureWorldAccess level, BlockPos gemPos, TypeGemClusterBlock clusterBlock,
                                    Random random, boolean forced, int currentGemCount) {
    int[] dirIndices = {0, 1, 2, 3, 4, 5};
    shuffleIndices(dirIndices, 6, random);

    for (int d = 0; d < 6; d++) {
      Direction dir = DIRECTIONS[dirIndices[d]];
      BlockPos targetPos = gemPos.offset(dir);

      if (!level.getBlockState(targetPos).isAir()) {
        continue;
      }

      placeClusterBlock(level, targetPos, clusterBlock, dir);

      int finalGemCount = currentGemCount;
      if (forced) {
        advanceCluster(level, targetPos, random);
        if (isGemBlock(level.getBlockState(targetPos))) {
          finalGemCount++;
        }
      }
      return finalGemCount;
    }
    return 0;
  }

  @Unique
  private static void placeClusterBlock(StructureWorldAccess level, BlockPos targetPos,
                                        TypeGemClusterBlock clusterBlock, Direction dir) {
    BlockState placeState = clusterBlock.getDefaultState()
      .with(TypeGemClusterBlock.Companion.getFACING(), dir)
      .with(TypeGemClusterBlock.Companion.getSTAGE(), 0)
      .with(TypeGemClusterBlock.Companion.getSHOULD_GROW(), true)
      .with(TypeGemClusterBlock.Companion.getSTUNTED(), false);
    level.setBlockState(targetPos, placeState, getUpdateFlags(level));
  }

  @Unique
  private static int getUpdateFlags(WorldAccess level) {
    return level instanceof ServerWorld ? Block.NOTIFY_ALL : Block.NOTIFY_LISTENERS;
  }

  @Unique
  private static int collectConnectedGems(BlockView level, BlockPos origin,
                                          BlockPos[] outPositions, BlockState[] outStates) {
    int count = 0;
    outPositions[count] = origin;
    outStates[count] = level.getBlockState(origin);
    count++;

    int head = 0;
    while (head < count) {
      BlockPos current = outPositions[head++];
      for (Direction dir : DIRECTIONS) {
        BlockPos neighbor = current.offset(dir);
        if (isAlreadyVisited(outPositions, count, neighbor)) {
          continue;
        }

        BlockState neighborState = level.getBlockState(neighbor);
        if (isGemBlock(neighborState) && count < outPositions.length) {
          outPositions[count] = neighbor;
          outStates[count] = neighborState;
          count++;
        }
      }
    }
    return count;
  }

  @Unique
  private static boolean isAlreadyVisited(BlockPos[] positions, int count, BlockPos target) {
    for (int i = 0; i < count; i++) {
      if (positions[i].equals(target)) {
        return true;
      }
    }
    return false;
  }

  @Unique
  private static void updateStuntState(WorldAccess level, BlockPos[] gemPositions, int gemCount, boolean stunted) {
    for (int i = 0; i < gemCount; i++) {
      BlockPos gemPos = gemPositions[i];
      for (Direction dir : DIRECTIONS) {
        applyStuntToNeighbor(level, gemPos.offset(dir), stunted);
      }
    }
  }

  @Unique
  private static void applyStuntToNeighbor(WorldAccess level, BlockPos neighborPos, boolean stunted) {
    BlockState state = level.getBlockState(neighborPos);
    if (!(state.getBlock() instanceof TypeGemClusterBlock)) {
      return;
    }

    BooleanProperty stuntedProp = TypeGemClusterBlock.Companion.getSTUNTED();
    if (state.contains(stuntedProp) && state.get(stuntedProp) != stunted) {
      BlockState updated = state.with(stuntedProp, stunted);
      BooleanProperty growProp = TypeGemClusterBlock.Companion.getSHOULD_GROW();
      if (updated.contains(growProp)) {
        updated = updated.with(growProp, !stunted);
      }
      level.setBlockState(neighborPos, updated, getUpdateFlags(level));
    }
  }

  @Unique
  private static boolean hasBreathingRoom(StructureWorldAccess level, BlockPos[] gemPositions, int gemCount) {
    for (int i = 0; i < gemCount; i++) {
      BlockPos gemPos = gemPositions[i];
      for (Direction dir : DIRECTIONS) {
        if (level.getBlockState(gemPos.offset(dir)).isAir()) {
          return true;
        }
      }
    }
    return false;
  }

  @Unique
  private static void advanceCluster(StructureWorldAccess level, BlockPos clusterPos, Random random) {
    for (int i = 0; i < 5; i++) {
      BlockState clusterState = level.getBlockState(clusterPos);
      if (clusterState.getBlock() instanceof TypeGemClusterBlock clusterBlock) {
        clusterBlock.advanceGrowth(clusterState, level, clusterPos, random);
      } else {
        break;
      }
    }
  }

  @Unique
  private static void shuffleIndices(int[] array, int length, Random random) {
    for (int i = length - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      int tmp = array[i];
      array[i] = array[j];
      array[j] = tmp;
    }
  }

  @Unique
  private static boolean isGemBlock(BlockState state) {
    return state.isIn(CobblemonBlockTags.TYPE_GEM_BLOCKS);
  }
}
