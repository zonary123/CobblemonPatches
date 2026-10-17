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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Patches {@link TypeGemClusterBlock} for Cobblemon 1.8.2+.
 * <p>
 * Fixes upstream regression where cluster {@code randomTick} was commented out,
 * corrects placement state resolving to {@link GrowthType#DECORATION} on gem blocks,
 * and allows existing player-placed clusters on valid gem bases to grow properly.
 */
@Mixin(value = TypeGemClusterBlock.class, remap = false)
public abstract class TypeGemClusterBlockMixin {

  @Shadow
  public abstract void advanceGrowth(BlockState state, LevelAccessor level, BlockPos pos, RandomSource random);

  @Shadow
  public abstract boolean gemClusterCanGrow(BlockState state, LevelReader level, BlockPos pos);

  /**
   * Re-enables cluster ticking so clusters planted on gem blocks or crystal cores
   * can advance their growth stages naturally over time.
   */
  @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true, remap = true)
  private void cobblemonPatchesRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
                                          CallbackInfo ci) {
    if (!this.gemClusterCanGrow(state, level, pos)) {
      ci.cancel();
      return;
    }

    float growthChance = Cobblemon.INSTANCE.getConfig().getTypeGemGrowthChance();
    if (random.nextFloat() >= growthChance) {
      ci.cancel();
      return;
    }

    BlockState workingState = state;
    if (state.getValue(TypeGemClusterBlock.Companion.getGROWTH_TYPE()) == GrowthType.DECORATION) {
      workingState = state.setValue(TypeGemClusterBlock.Companion.getGROWTH_TYPE(), GrowthType.FULL);
      level.setBlock(pos, workingState, Block.UPDATE_ALL);
    }

    this.advanceGrowth(workingState, level, pos, random);
    ci.cancel();
  }

  /**
   * Fixes placement state calculation so clusters placed on Type Gem blocks (not just core blocks)
   * receive {@link GrowthType#FULL} instead of being forced into {@link GrowthType#DECORATION}.
   */
  @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true, remap = true)
  private void cobblemonPatchesGetStateForPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
    BlockState state = cir.getReturnValue();
    if (state == null) {
      return;
    }

    Direction facing = context.getClickedFace();
    BlockPos supportPos = context.getClickedPos().relative(facing.getOpposite());
    BlockState supportState = context.getLevel().getBlockState(supportPos);

    boolean shouldGrow = cobblemonPatches$isSupportedByGrowthBase(supportState);
    cir.setReturnValue(state.setValue(
        TypeGemClusterBlock.Companion.getGROWTH_TYPE(),
        shouldGrow ? GrowthType.FULL : GrowthType.DECORATION
    ));
  }

  /**
   * Ensures clusters supported by a valid gem block or core can grow, even if they were
   * previously tagged as {@link GrowthType#DECORATION} due to the placement bug.
   */
  @Inject(method = "gemClusterCanGrow", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatchesGemClusterCanGrow(BlockState state, LevelReader level, BlockPos pos,
                                                 CallbackInfoReturnable<Boolean> cir) {
    if (state.getValue(TypeGemClusterBlock.Companion.getGROWTH_TYPE()) == GrowthType.STUNTED
        && state.getValue(TypeGemClusterBlock.Companion.getSTAGE()) == 3) {
      cir.setReturnValue(false);
      return;
    }

    Direction facing = state.getValue(TypeGemClusterBlock.Companion.getFACING());
    BlockPos supportPos = pos.relative(facing.getOpposite());
    BlockState supportState = level.getBlockState(supportPos);

    cir.setReturnValue(cobblemonPatches$isSupportedByGrowthBase(supportState));
  }

  /**
   * Allows clusters on valid support bases to receive random ticks even if previously placed as decorative.
   */
  @Inject(method = "isRandomlyTicking", at = @At("HEAD"), cancellable = true, remap = true)
  private void cobblemonPatchesIsRandomlyTicking(BlockState state, CallbackInfoReturnable<Boolean> cir) {
    if (state.getValue(TypeGemClusterBlock.Companion.getGROWTH_TYPE()) == GrowthType.STUNTED
        && state.getValue(TypeGemClusterBlock.Companion.getSTAGE()) == 3) {
      cir.setReturnValue(false);
      return;
    }
    cir.setReturnValue(true);
  }

  /**
   * Checks whether the given block state is a valid foundation for cluster growth.
   */
  @Unique
  private static boolean cobblemonPatches$isSupportedByGrowthBase(BlockState supportState) {
    return supportState.getBlock() instanceof TypeGemCoreBlock
        || supportState.is(CobblemonBlockTags.TYPE_GEM_BLOCKS);
  }
}
