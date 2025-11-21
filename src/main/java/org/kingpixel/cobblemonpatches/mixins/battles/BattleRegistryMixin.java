package org.kingpixel.cobblemonpatches.mixins.battles;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import kotlin.Unit;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mixin que optimiza y gestiona el cacheo de batallas activas en el BattleRegistry.
 * Mejora el rendimiento de las búsquedas y asegura limpieza de memoria al cerrar batallas.
 *
 * @author Carlos Varas Alonso - Optimizado 27/10/2025
 */
@Mixin(BattleRegistry.class)
abstract class BattleRegistryMixin {

  /**
   * BattleId → PokemonBattle
   */
  @Unique private static final Map<UUID, PokemonBattle> ACTIVE_BATTLES = new ConcurrentHashMap<>();

  /**
   * PlayerUUID → PokemonBattle
   */
  @Unique private static final Map<UUID, PokemonBattle> PLAYER_BATTLES = new ConcurrentHashMap<>();

  /**
   * PlayerUUID → BattleId
   */
  @Unique private static final Map<UUID, UUID> PLAYER_TO_BATTLE_ID = new ConcurrentHashMap<>();

  // ============================
  // EVENTOS DE VIDA DE BATALLA
  // ============================

  @Inject(method = "onPlayerDisconnect", at = @At("HEAD"))
  private void onPlayerDisconnect(ServerPlayerEntity player, CallbackInfo ci) {
    removePlayerFromCaches(player.getUuid());
  }

  @Inject(method = "startBattle", at = @At("RETURN"))
  private void onStartBattle(BattleFormat format, BattleSide side1, BattleSide side2,
                             boolean canPreempt, CallbackInfoReturnable<BattleStartResult> cir) {
    BattleStartResult result = cir.getReturnValue();
    if (result != null) {
      updateCachesForSide(side1, result);
      updateCachesForSide(side2, result);
    }
  }

  @Inject(method = "closeBattle", at = @At("HEAD"))
  private void onCloseBattle(PokemonBattle battle, CallbackInfo ci) {
    if (battle == null) return;
    UUID battleId = battle.getBattleId();
    ACTIVE_BATTLES.remove(battleId);

    // Limpieza de todos los jugadores involucrados
    PLAYER_TO_BATTLE_ID.entrySet().removeIf(entry -> {
      if (entry.getValue().equals(battleId)) {
        UUID playerId = entry.getKey();
        PLAYER_BATTLES.remove(playerId);
        return true;
      }
      return false;
    });
  }

  // ============================
  // CACHÉ / OPTIMIZACIONES
  // ============================

  @Inject(method = "getBattle", at = @At("HEAD"), cancellable = true)
  private void onGetBattle(UUID id, CallbackInfoReturnable<PokemonBattle> cir) {
    Optional.ofNullable(ACTIVE_BATTLES.get(id)).ifPresent(cir::setReturnValue);
  }

  @Inject(method = "getBattleByParticipatingPlayer", at = @At("HEAD"), cancellable = true)
  private void onGetBattleByPlayer(ServerPlayerEntity player, CallbackInfoReturnable<PokemonBattle> cir) {
    if (player != null) {
      Optional.ofNullable(PLAYER_BATTLES.get(player.getUuid())).ifPresent(cir::setReturnValue);
    }
  }

  @Inject(method = "getBattleByParticipatingPlayerId", at = @At("HEAD"), cancellable = true)
  private void onGetBattleByPlayerId(UUID playerId, CallbackInfoReturnable<PokemonBattle> cir) {
    Optional.ofNullable(PLAYER_BATTLES.get(playerId)).ifPresent(cir::setReturnValue);
  }

  // ============================
  // MÉTODOS AUXILIARES
  // ============================

  @Unique
  private void updateCachesForSide(BattleSide side, BattleStartResult result) {
    result.ifSuccessful(battle -> {
      for (BattleActor actor : side.getActors()) {
        if (actor instanceof PlayerBattleActor playerActor) {
          UUID playerUUID = playerActor.getUuid();
          UUID battleId = battle.getBattleId();
          ACTIVE_BATTLES.put(battleId, battle);
          PLAYER_BATTLES.put(playerUUID, battle);
          PLAYER_TO_BATTLE_ID.put(playerUUID, battleId);
        }
      }
      return Unit.INSTANCE;
    });
  }

  @Unique
  private void removePlayerFromCaches(UUID playerUUID) {
    if (playerUUID == null) return;
    UUID battleId = PLAYER_TO_BATTLE_ID.remove(playerUUID);
    if (battleId != null) ACTIVE_BATTLES.remove(battleId);
    PLAYER_BATTLES.remove(playerUUID);
  }
}
