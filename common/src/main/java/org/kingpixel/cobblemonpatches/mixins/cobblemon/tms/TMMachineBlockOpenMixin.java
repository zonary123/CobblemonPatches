package org.kingpixel.cobblemonpatches.mixins.cobblemon.tms;

import com.cobblemon.mod.common.block.tmmachine.TMMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.kingpixel.cobblemonpatches.util.TMObtainMethodUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Patches {@link TMMachineBlock} to refresh dynamic passive TM availability for the player
 * whenever they open a TM Machine, keeping unmodified clients in sync with real-time conditions.
 *
 * @author Carlos Varas Alonso
 */
@Mixin(value = TMMachineBlock.class, remap = false)
public abstract class TMMachineBlockOpenMixin {

  @Inject(method = "useWithoutItem", at = @At("HEAD"), remap = true)
  private void cobblemonPatches$syncDynamicPassiveTMsOnOpen(
    BlockState state,
    Level level,
    BlockPos pos,
    Player player,
    BlockHitResult hit,
    CallbackInfoReturnable<InteractionResult> cir
  ) {
    if (!TMObtainMethodUtils.isPatchEnabled()) {
      return;
    }
    if (!level.isClientSide && !player.isCrouching() && player instanceof ServerPlayer serverPlayer) {
      TMObtainMethodUtils.syncDynamicTMsToPlayer(serverPlayer);
    }
  }
}
