package org.kingpixel.cobblemonpatches.mixins.cobblemon.campfire;

import com.cobblemon.mod.common.block.campfirepot.CampfireBlock;
import com.cobblemon.mod.common.block.entity.CampfireBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.kingpixel.cobblemonpatches.PatchesUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Optimizes CampfireBlockEntity.Companion.serverTick to avoid expensive per-tick
 * recipe lookups (~1000ms in Spark profiling). Caches recipe existence per entity
 * and only allows full recipe lookup when items change.
 */
@Mixin(targets = "com.cobblemon.mod.common.block.entity.CampfireBlockEntity$Companion", remap = false)
public abstract class CampfireServerTickMixin {

  /**
   * Cache per campfire entity.
   * Key: CampfireBlockEntity
   * Value: Object array where:
   * <ul>
   *   <li>[0] is the fingerprint (Long) of the current inventory</li>
   *   <li>[1] indicates if it has a recipe (Boolean.TRUE or null)</li>
   * </ul>
   */
  @Unique
  private static final WeakHashMap<CampfireBlockEntity, Object[]> RECIPE_CACHE = new WeakHashMap<>();

  /**
   * Optimizes the serverTick execution by bypassing expensive recipe lookups and checking
   * if the items have changed before proceeding with cooking evaluations.
   *
   * @param world  The world in which the campfire resides
   * @param pos    The position of the campfire block
   * @param state  The current block state
   * @param entity The campfire block entity
   * @param ci     The callback info to cancel the original execution if optimized
   */
  @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$optimizeServerTick(World world, BlockPos pos, BlockState state,
                                                   CampfireBlockEntity entity, CallbackInfo ci) {
    if (world.isClient) {
      ci.cancel();
      return;
    }

    CampfireBlockEntityAccessor accessor = (CampfireBlockEntityAccessor) (Object) entity;
    if (Objects.isNull(accessor)) {
      PatchesUtil.LOGGER.error("CampfireBlockEntityAccessor is null for entity at {} in world {}", pos, world);
      return;
    }
    int progress = accessor.cobblemonPatches$getCookingProgress();
    int totalTime = accessor.cobblemonPatches$getCookingTotalTime();

    if (progress > 0 && progress + 2 < totalTime) {
      long fingerprint = cobblemonPatches$computeFingerprint(entity);
      Object[] cached = RECIPE_CACHE.get(entity);

      if (cached != null && (long) cached[0] != fingerprint) {
        return;
      }

      boolean lidClosed = entity.getCachedState().get(CampfireBlock.Companion.getLID());
      if (!lidClosed) {
        accessor.cobblemonPatches$setCookingProgress(0);
        world.setBlockState(pos, state.with(CampfireBlock.Companion.getCOOKING(), false), 3);
        entity.markDirty();
      } else {
        accessor.cobblemonPatches$setCookingProgress(progress + 2);
      }
      ci.cancel();
      return;
    }

    if (progress == 0) {
      long fingerprint = cobblemonPatches$computeFingerprint(entity);
      Object[] cached = RECIPE_CACHE.get(entity);
      if (cached != null && (long) cached[0] == fingerprint && cached[1] == null) {
        ci.cancel();
        return;
      }
    }
  }

  /**
   * Caches the recipe status and inventory fingerprint after serverTick execution.
   *
   * @param world  The world in which the campfire resides
   * @param pos    The position of the campfire block
   * @param state  The current block state
   * @param entity The campfire block entity
   * @param ci     The callback info
   */
  @Inject(method = "serverTick", at = @At("TAIL"))
  private void cobblemonPatches$cacheAfterTick(World world, BlockPos pos, BlockState state,
                                               CampfireBlockEntity entity, CallbackInfo ci) {
    if (world.isClient) return;

    CampfireBlockEntityAccessor accessor = (CampfireBlockEntityAccessor) (Object) entity;
    if (Objects.isNull(accessor)) {
      PatchesUtil.LOGGER.error("CampfireBlockEntityAccessor is null for entity at {} in world {}", pos, world);
      return;
    }
    long fingerprint = cobblemonPatches$computeFingerprint(entity);
    int currentProgress = accessor.cobblemonPatches$getCookingProgress();
    boolean hasRecipe = currentProgress > 0;

    if (!hasRecipe) {
      Object[] previousCache = RECIPE_CACHE.get(entity);
      if (previousCache != null && previousCache[1] == Boolean.TRUE) {
        RECIPE_CACHE.remove(entity);
        return;
      }
    }

    RECIPE_CACHE.put(entity, new Object[]{fingerprint, hasRecipe ? Boolean.TRUE : null});
  }

  /**
   * Computes a unique inventory fingerprint based on the items and stack counts
   * inside the campfire's container.
   *
   * @param entity The campfire block entity to compute the fingerprint for
   * @return A fingerprint hash representing the current inventory contents
   */
  @Unique
  private static long cobblemonPatches$computeFingerprint(CampfireBlockEntity entity) {
    long hash = 1;
    int size = entity.size();
    for (int i = 0; i < size; i++) {
      ItemStack stack = entity.getStack(i);
      if (stack.isEmpty()) {
        hash = hash * 31;
      } else {
        hash = hash * 31 + System.identityHashCode(stack.getItem()) * 37L + stack.getCount();
      }
    }
    return hash;
  }
}
