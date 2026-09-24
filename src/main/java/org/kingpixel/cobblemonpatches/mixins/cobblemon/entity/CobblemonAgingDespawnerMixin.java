package org.kingpixel.cobblemonpatches.mixins.cobblemon.entity;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.entity.pokemon.CobblemonAgingDespawner;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.util.List;
import kotlin.jvm.functions.Function1;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optimizes entity despawning checks in {@link CobblemonAgingDespawner}.
 * Throttles player distance checks to 1-second intervals and uses squared-distance
 * comparisons to eliminate continuous Math.sqrt and collection allocations.
 */
@Mixin(value = CobblemonAgingDespawner.class, remap = false)
public abstract class CobblemonAgingDespawnerMixin<T extends Entity> {

  @Shadow
  @Final
  private Function1<T, Integer> getAgeTicks;

  @Shadow
  @Final
  private float nearToFar;

  @Shadow
  @Final
  private int youngToOld;

  /**
   * Evaluates entity despawning rules every 20 ticks (1 second) using squared-distance checks
   * against active players, avoiding unnecessary square root calculations and collection allocations.
   *
   * @param entity the entity being evaluated for despawn
   * @param cir    callback returnable with despawn decision
   */
  @Inject(method = "shouldDespawn", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$optimizedShouldDespawn(T entity, CallbackInfoReturnable<Boolean> cir) {
    if (entity == null) {
      cir.setReturnValue(false);
      return;
    }

    if (entity.age % 20 != 0) {
      cir.setReturnValue(false);
      return;
    }

    int age = this.getAgeTicks.invoke(entity);
    if (age < Cobblemon.INSTANCE.getConfig().getDespawnerMinAgeTicks()
        || (entity instanceof PokemonEntity pokemonEntity && pokemonEntity.isBusy())
        || entity.hasPassengers()) {
      cir.setReturnValue(false);
      return;
    }

    List<? extends PlayerEntity> players = entity.getWorld().getPlayers();
    if (players == null || players.isEmpty()) {
      cir.setReturnValue(true);
      return;
    }

    float nearDist = Cobblemon.INSTANCE.getConfig().getDespawnerNearDistance();
    float farDist = Cobblemon.INSTANCE.getConfig().getDespawnerFarDistance();
    float nearDistSq = nearDist * nearDist;
    float farDistSq = farDist * farDist;

    double closestDistSq = Double.MAX_VALUE;
    for (PlayerEntity player : players) {
      if (player == null) continue;
      double distSq = entity.squaredDistanceTo(player);
      if (distSq < closestDistSq) {
        closestDistSq = distSq;
        if (closestDistSq < nearDistSq) {
          cir.setReturnValue(false);
          return;
        }
      }
    }

    if (age > Cobblemon.INSTANCE.getConfig().getDespawnerMaxAgeTicks() || closestDistSq > farDistSq) {
      cir.setReturnValue(true);
      return;
    }

    float closestDistance = (float) Math.sqrt(closestDistSq);
    float distanceRatio = (closestDistance - nearDist) / this.nearToFar;
    float maximumAge = (1.0f - distanceRatio) * this.youngToOld;
    cir.setReturnValue(age > maximumAge);
  }
}
