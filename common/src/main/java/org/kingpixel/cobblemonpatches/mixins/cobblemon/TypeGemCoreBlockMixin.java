package org.kingpixel.cobblemonpatches.mixins.cobblemon;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.tags.CobblemonBlockTags;
import com.cobblemon.mod.common.block.TypeGemClusterBlock;
import com.cobblemon.mod.common.block.TypeGemClusterBlock.GrowthType;
import com.cobblemon.mod.common.block.TypeGemCoreBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optimizes {@link TypeGemCoreBlock} expansion and ticking for Cobblemon 1.8.2+.
 * Eliminates redundant nested BFS traversals, removes massive heap allocations
 * (LinkedList, LinkedHashSet, ArrayList, Kotlin Pairs), and avoids debug logging in ticks.
 */
@Mixin(value = TypeGemCoreBlock.class, remap = false)
public abstract class TypeGemCoreBlockMixin {

  @Unique
  private static final Direction[] DIRECTIONS = Direction.values();

  /**
   * Intercepts standard expansion routine and executes an optimized, allocation-free expansion.
   *
   * @param level  world access
   * @param pos    core position
   * @param random random source
   * @param forced whether growth is forced
   * @param ci     callback info
   */
  @Inject(method = "expand", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatchesExpand(LevelAccessor level, BlockPos pos, RandomSource random, boolean forced,
                                      CallbackInfo ci) {
    int maxConfig = Cobblemon.INSTANCE.getConfig().getTypeGemMaxConnected();
    int maxCapacity = Math.max(64, maxConfig * 2);
    BlockPos[] gemPositions = new BlockPos[maxCapacity];
    BlockState[] gemStates = new BlockState[maxCapacity];
    int gemCount = collectConnectedGems(level, pos, gemPositions, gemStates);

    performOptimizedExpand(level, random, forced, gemPositions, gemStates, gemCount, maxConfig);
    ci.cancel();
  }

  /**
   * Intercepts random ticks to prevent redundant BFS scans, remove debug println,
   * and reuse flat array collections.
   *
   * @param state  block state
   * @param level  server level
   * @param pos    core block position
   * @param random random source
   * @param ci     callback info
   */
  @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true, remap = true)
  private void cobblemonPatchesRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
                                          CallbackInfo ci) {
    float tickChance = Cobblemon.INSTANCE.getConfig().getTypeGemTickChance();
    if (random.nextFloat() >= tickChance) {
      ci.cancel();
      return;
    }

    int maxConfig = Cobblemon.INSTANCE.getConfig().getTypeGemMaxConnected();
    int maxCapacity = Math.max(64, maxConfig * 2);
    BlockPos[] gemPositions = new BlockPos[maxCapacity];
    BlockState[] gemStates = new BlockState[maxCapacity];
    int gemCount = collectConnectedGems(level, pos, gemPositions, gemStates);

    int minGrowth = Cobblemon.INSTANCE.getConfig().getTypeGemMinGrowth();
    if (gemCount < minGrowth) {
      ensureFullGrowthCluster(level, gemPositions, gemCount, random);
    }

    performOptimizedExpand(level, random, false, gemPositions, gemStates, gemCount, maxConfig);
    ci.cancel();
  }

  /**
   * Performs cluster placement and advancement without nested BFS calls.
   *
   * @param level        world access
   * @param random       random source
   * @param forced       whether growth is forced
   * @param gemPositions array of connected gem positions
   * @param gemStates    array of corresponding block states
   * @param gemCount     number of connected gems
   * @param maxConfig    maximum allowed connected gems
   */
  @Unique
  private static void performOptimizedExpand(LevelAccessor level, RandomSource random, boolean forced,
                                             BlockPos[] gemPositions, BlockState[] gemStates,
                                             int gemCount, int maxConfig) {
    boolean overCapacity = gemCount > maxConfig;
    float growthChance = Cobblemon.INSTANCE.getConfig().getTypeGemGrowthChance();

    for (int i = 0; i < gemCount; i++) {
      BlockPos gemPos = gemPositions[i];
      BlockState gemState = gemStates[i];

      for (Direction dir : DIRECTIONS) {
        tryPlaceAdjacentCluster(level, gemPos, gemState, dir, growthChance, overCapacity, random);
        tryAdvanceAdjacentCluster(level, gemPos, dir, growthChance, forced, random);
      }
    }
  }

  /**
   * Attempts to spawn a new cluster block in an adjacent air position.
   */
  @Unique
  private static void tryPlaceAdjacentCluster(LevelAccessor level, BlockPos gemPos, BlockState gemState,
                                              Direction dir, float growthChance, boolean overCapacity,
                                              RandomSource random) {
    if (random.nextFloat() >= growthChance) {
      return;
    }

    BlockPos neighborPos = gemPos.relative(dir);
    if (!level.getBlockState(neighborPos).isAir()) {
      return;
    }

    TypeGemClusterBlock clusterBlock = TypeGemClusterBlock.Companion.clusterFromGemBlock(gemState.getBlock());
    if (clusterBlock == null) {
      return;
    }

    GrowthType growthType = pickGrowthType(overCapacity, random);
    BlockState placeState = clusterBlock.defaultBlockState()
      .setValue(TypeGemClusterBlock.Companion.getFACING(), dir)
      .setValue(TypeGemClusterBlock.Companion.getGROWTH_TYPE(), growthType);
    level.setBlock(neighborPos, placeState, Block.UPDATE_ALL);
  }

  /**
   * Advances growth stage of an adjacent cluster if it faces the parent gem block.
   */
  @Unique
  private static void tryAdvanceAdjacentCluster(LevelAccessor level, BlockPos gemPos, Direction dir,
                                                float growthChance, boolean forced, RandomSource random) {
    BlockPos neighborPos = gemPos.relative(dir);
    BlockState neighborState = level.getBlockState(neighborPos);

    if (!(neighborState.getBlock() instanceof TypeGemClusterBlock clusterBlock)) {
      return;
    }

    if (neighborState.getValue(TypeGemClusterBlock.Companion.getFACING()) != dir) {
      return;
    }

    if (forced || random.nextFloat() < growthChance) {
      clusterBlock.advanceGrowth(neighborState, level, neighborPos, random);
    }
  }

  /**
   * Ensures at least one cluster is marked as FULL if below min growth, without debug logging.
   *
   * @param level        server level
   * @param gemPositions array of connected gem positions
   * @param gemCount     number of connected gems
   * @param random       random source
   */
  @Unique
  private static void ensureFullGrowthCluster(ServerLevel level, BlockPos[] gemPositions, int gemCount,
                                              RandomSource random) {
    int clusterCapacity = Math.min(64, gemCount * 6);
    BlockPos[] clusterPositions = new BlockPos[clusterCapacity];
    BlockState[] clusterStates = new BlockState[clusterCapacity];

    int candidateCount = collectClusterCandidates(level, gemPositions, gemCount, clusterPositions, clusterStates);
    if (candidateCount <= 0) {
      return;
    }

    int chosen = random.nextInt(candidateCount);
    BlockPos chosenPos = clusterPositions[chosen];
    BlockState chosenState = clusterStates[chosen];
    BlockState updated = chosenState.setValue(TypeGemClusterBlock.Companion.getGROWTH_TYPE(), GrowthType.FULL);
    level.setBlockAndUpdate(chosenPos, updated);
  }

  /**
   * Scans adjacent clusters and returns the number of candidate clusters, or -1 if a FULL cluster already exists.
   */
  @Unique
  private static int collectClusterCandidates(ServerLevel level, BlockPos[] gemPositions, int gemCount,
                                              BlockPos[] clusterPositions, BlockState[] clusterStates) {
    int count = 0;
    for (int i = 0; i < gemCount; i++) {
      BlockPos gemPos = gemPositions[i];
      for (Direction dir : DIRECTIONS) {
        count = checkNeighborCluster(level, gemPos.relative(dir), clusterPositions, clusterStates, count);
        if (count == -1) {
          return -1;
        }
      }
    }
    return count;
  }

  @Unique
  private static int checkNeighborCluster(ServerLevel level, BlockPos neighborPos,
                                          BlockPos[] clusterPositions, BlockState[] clusterStates, int count) {
    if (isAlreadyVisited(clusterPositions, count, neighborPos)) {
      return count;
    }

    BlockState neighborState = level.getBlockState(neighborPos);
    if (!(neighborState.getBlock() instanceof TypeGemClusterBlock)) {
      return count;
    }

    if (neighborState.getValue(TypeGemClusterBlock.Companion.getGROWTH_TYPE()) == GrowthType.FULL) {
      return -1;
    }

    if (count < clusterPositions.length) {
      clusterPositions[count] = neighborPos;
      clusterStates[count] = neighborState;
      return count + 1;
    }
    return count;
  }

  /**
   * Selects a growth type according to weighted distribution and capacity constraints.
   *
   * @param overCapacity whether connected gems exceed max capacity
   * @param random       random source
   * @return selected growth type
   */
  @Unique
  private static GrowthType pickGrowthType(boolean overCapacity, RandomSource random) {
    if (overCapacity) {
      float chanceDeco = GrowthType.DECORATION.getChance();
      float chanceStunted = GrowthType.STUNTED.getChance();
      float total = chanceDeco + chanceStunted;
      if (total <= 0.0f) {
        return GrowthType.DECORATION;
      }
      float roll = random.nextFloat() * total;
      return roll < chanceDeco ? GrowthType.DECORATION : GrowthType.STUNTED;
    }

    float chanceDeco = GrowthType.DECORATION.getChance();
    float chanceFull = GrowthType.FULL.getChance();
    float chanceStunted = GrowthType.STUNTED.getChance();
    float total = chanceDeco + chanceFull + chanceStunted;
    if (total <= 0.0f) {
      return GrowthType.DECORATION;
    }
    float roll = random.nextFloat() * total;
    if (roll < chanceDeco) {
      return GrowthType.DECORATION;
    }
    roll -= chanceDeco;
    if (roll < chanceFull) {
      return GrowthType.FULL;
    }
    return GrowthType.STUNTED;
  }

  /**
   * Collects connected gem blocks using an array-based BFS traversal to avoid heap allocations.
   *
   * @param level        block view
   * @param origin       origin block pos
   * @param outPositions output array for discovered positions
   * @param outStates    output array for corresponding block states
   * @return total number of connected gem blocks discovered
   */
  @Unique
  private static int collectConnectedGems(BlockGetter level, BlockPos origin,
                                          BlockPos[] outPositions, BlockState[] outStates) {
    int count = 0;
    outPositions[count] = origin;
    outStates[count] = level.getBlockState(origin);
    count++;

    int head = 0;
    while (head < count) {
      BlockPos current = outPositions[head++];
      for (Direction dir : DIRECTIONS) {
        BlockPos neighbor = current.relative(dir);
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

  /**
   * Linear search to determine whether a block position is already recorded in the working array.
   *
   * @param positions array of positions
   * @param count     number of valid entries in array
   * @param target    position being searched
   * @return true if position is present
   */
  @Unique
  private static boolean isAlreadyVisited(BlockPos[] positions, int count, BlockPos target) {
    for (int i = 0; i < count; i++) {
      if (positions[i].equals(target)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if the given block state belongs to Cobblemon type gem blocks.
   *
   * @param state block state to check
   * @return true if tagged as a type gem block
   */
  @Unique
  private static boolean isGemBlock(BlockState state) {
    return state.is(CobblemonBlockTags.TYPE_GEM_BLOCKS);
  }
}
