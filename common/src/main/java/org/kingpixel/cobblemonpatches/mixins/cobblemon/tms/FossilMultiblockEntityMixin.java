package org.kingpixel.cobblemonpatches.mixins.cobblemon.tms;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.tms.TechnicalMachine;
import com.cobblemon.mod.common.api.tms.TechnicalMachines;
import com.cobblemon.mod.common.block.entity.FossilMultiblockEntity;
import com.cobblemon.mod.common.item.components.TMMoveComponent;
import com.cobblemon.mod.common.item.interactive.TechnicalMachineItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.kingpixel.cobblemonpatches.util.TMObtainMethodUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Patches {@link FossilMultiblockEntity} (Data Monitor) so inserting a TM item
 * only unlocks
 * TMs whose {@code obtainMethods} allow gameplay unlocking
 * ({@code cobblemon:unlockable}).
 *
 * @author Carlos Varas Alonso
 */
@Mixin(value = FossilMultiblockEntity.class, remap = false)
public abstract class FossilMultiblockEntityMixin {

  @Inject(method = "unlockTmForPlayer", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$enforceObtainMethodOnMonitorUnlock(
      ServerPlayer player,
      ItemStack stack,
      CallbackInfo ci) {
    if (!TMObtainMethodUtils.isPatchEnabled()) {
      return;
    }
    if (!(stack.getItem() instanceof TechnicalMachineItem)) {
      return;
    }
    MoveTemplate move = TMMoveComponent.Companion.getTMMove(stack);
    if (move == null) {
      return;
    }
    TechnicalMachine tm = TechnicalMachines.INSTANCE.getMoveToTM().get(move);
    if (tm != null && !TMObtainMethodUtils.canUnlockFromGameplay(tm)) {
      ci.cancel();
    }
  }
}
