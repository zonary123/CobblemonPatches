package org.kingpixel.cobblemonpatches.mixins.cobblemon.tms;

import com.cobblemon.mod.common.api.storage.player.client.ClientTMMoveManager;
import com.cobblemon.mod.common.api.tms.TMMoveManager;
import com.cobblemon.mod.common.pokemon.Pokemon;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.kingpixel.cobblemonpatches.util.TMObtainMethodUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Patches {@link TMMoveManager} to enforce and optimize {@code obtainMethods}:
 * <ul>
 *   <li>Replaces the O(N) full-TM scan in {@code getLearnableTMsFromPokemon} with O(1) indexed
 *       move lookups restricted to {@code cobblemon:unlockable} TMs.</li>
 *   <li>Validates predicate-only custom {@code obtainMethods} in {@code learn()} with an O(1) fast-path.</li>
 *   <li>Includes dynamically matched passive TMs in non-incremental client syncs without mutating
 *       persistent player data or copying sets unnecessarily.</li>
 * </ul>
 *
 * @author Carlos Varas Alonso
 */
@Mixin(value = TMMoveManager.class, remap = false)
public abstract class TMMoveManagerMixin {

  @Shadow private UUID uuid;
  @Shadow private Set<ResourceLocation> learnedTMs;

  @Shadow
  public abstract void syncClient(Set<ResourceLocation> updateSet, boolean isIncremental);

  @Unique
  private boolean cobblemonPatches$syncingDynamicTMs = false;

  @Inject(method = "getLearnableTMsFromPokemon", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$fastGameplayUnlockableTMs(
    Pokemon pokemon,
    CallbackInfoReturnable<Collection<ResourceLocation>> cir
  ) {
    if (!TMObtainMethodUtils.isPatchEnabled()) {
      return;
    }
    cir.setReturnValue(TMObtainMethodUtils.getGameplayUnlockableTMsForPokemon(pokemon));
  }

  @ModifyVariable(method = "learn", at = @At("HEAD"), argsOnly = true)
  private Collection<ResourceLocation> cobblemonPatches$filterEligibleDirectUnlocks(
    Collection<ResourceLocation> tmIds
  ) {
    if (!TMObtainMethodUtils.isPatchEnabled()) {
      return tmIds;
    }
    return TMObtainMethodUtils.filterEligibleDirectUnlocks(tmIds, this.uuid);
  }

  @Inject(
    method = "toClientData()Lcom/cobblemon/mod/common/api/storage/player/client/ClientTMMoveManager;",
    at = @At("RETURN"),
    cancellable = true
  )
  private void cobblemonPatches$includeDynamicPassiveTMsInClientData(
    CallbackInfoReturnable<ClientTMMoveManager> cir
  ) {
    if (!TMObtainMethodUtils.isPatchEnabled() || !TMObtainMethodUtils.hasAnyDynamicPassiveTM()) {
      return;
    }
    ServerPlayer player = TMObtainMethodUtils.resolvePlayer(this.uuid);
    if (player == null) {
      return;
    }

    Set<ResourceLocation> effective = TMObtainMethodUtils.buildEffectiveClientTMs(player, this.learnedTMs);
    if (effective != this.learnedTMs) {
      cir.setReturnValue(new ClientTMMoveManager(effective));
    }
  }

  @Inject(method = "syncClient", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$includeDynamicPassiveTMsOnFullSync(
    Set<ResourceLocation> updateSet,
    boolean isIncremental,
    CallbackInfo ci
  ) {
    if (!TMObtainMethodUtils.isPatchEnabled()
      || isIncremental
      || this.cobblemonPatches$syncingDynamicTMs
      || !TMObtainMethodUtils.hasAnyDynamicPassiveTM()) {
      return;
    }

    ServerPlayer player = TMObtainMethodUtils.resolvePlayer(this.uuid);
    if (player == null) {
      return;
    }

    Set<ResourceLocation> effective = TMObtainMethodUtils.buildEffectiveClientTMs(player, updateSet);
    if (effective == updateSet) {
      return;
    }

    this.cobblemonPatches$syncingDynamicTMs = true;
    try {
      this.syncClient(effective, false);
    } finally {
      this.cobblemonPatches$syncingDynamicTMs = false;
    }
    ci.cancel();
  }
}
