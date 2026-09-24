package org.kingpixel.cobblemonpatches.mixins.cobblemon.battles;

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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mixin into {@link BattleRegistry} providing high-performance O(1) player-to-battle indexing
 * with strict lifecycle validation to eliminate ghost battle locks.
 *
 * @author Carlos Varas Alonso
 */
@Mixin(value = BattleRegistry.class, remap = false)
public abstract class BattleRegistryMixin {

  /**
   * Index mapping player UUID to active battle UUID for O(1) lookups.
   */
  @Unique
  private static final ConcurrentHashMap<UUID, UUID> PLAYER_TO_BATTLE = new ConcurrentHashMap<>();

  /**
   * Resets index on server start.
   *
   * @param ci callback info
   */
  @Inject(method = "onServerStarted", at = @At("HEAD"))
  private static void onServerStarted(CallbackInfo ci) {
    PLAYER_TO_BATTLE.clear();
  }

  /**
   * Indexes all participating players upon successful battle start.
   *
   * @param format     battle format
   * @param side1      battle side 1
   * @param side2      battle side 2
   * @param canPreempt whether battle can preempt
   * @param cir        callback returnable with start result
   */
  @Inject(method = "startBattle", at = @At("RETURN"))
  private static void onStartBattle(
    BattleFormat format,
    BattleSide side1,
    BattleSide side2,
    boolean canPreempt,
    CallbackInfoReturnable<BattleStartResult> cir
  ) {
    BattleStartResult result = cir.getReturnValue();
    if (result == null) return;

    result.ifSuccessful(battle -> {
      if (battle != null && !battle.getEnded()) {
        UUID battleId = battle.getBattleId();
        indexSide(side1, battleId);
        indexSide(side2, battleId);
      }
      return Unit.INSTANCE;
    });
  }

  /**
   * Evicts the battle and cleans up player mappings when a battle concludes.
   *
   * @param battle the battle being closed
   * @param ci     callback info
   */
  @Inject(method = "closeBattle", at = @At("HEAD"))
  private static void onCloseBattle(PokemonBattle battle, CallbackInfo ci) {
    if (battle == null) return;
    UUID battleId = battle.getBattleId();
    PLAYER_TO_BATTLE.entrySet().removeIf(
      entry -> entry.getValue().equals(battleId)
    );
  }

  /**
   * Cleans up player mapping after player disconnect handling finishes.
   *
   * @param player the disconnecting player entity
   * @param ci     callback info
   */
  @Inject(method = "onPlayerDisconnect", at = @At("RETURN"))
  private void onPlayerDisconnect(ServerPlayerEntity player, CallbackInfo ci) {
    if (player == null) return;
    PLAYER_TO_BATTLE.remove(player.getUuid());
  }

  /**
   * Retrieves active battle for a participating player entity using the player index.
   *
   * @param player player entity
   * @param cir    callback returnable
   */
  @Inject(method = "getBattleByParticipatingPlayer", at = @At("HEAD"), cancellable = true)
  private static void getBattleByPlayer(
    ServerPlayerEntity player,
    CallbackInfoReturnable<PokemonBattle> cir
  ) {
    if (player == null) return;

    UUID playerId = player.getUuid();
    UUID battleId = PLAYER_TO_BATTLE.get(playerId);
    if (battleId == null) return;

    PokemonBattle battle = BattleRegistry.getBattle(battleId);
    if (battle != null && !battle.getEnded()) {
      cir.setReturnValue(battle);
      cir.cancel();
    } else {
      PLAYER_TO_BATTLE.remove(playerId);
    }
  }

  /**
   * Retrieves active battle for a participating player UUID using the player index.
   *
   * @param playerId player UUID
   * @param cir      callback returnable
   */
  @Inject(method = "getBattleByParticipatingPlayerId", at = @At("HEAD"), cancellable = true)
  private static void getBattleByPlayerId(
    UUID playerId,
    CallbackInfoReturnable<PokemonBattle> cir
  ) {
    if (playerId == null) return;

    UUID battleId = PLAYER_TO_BATTLE.get(playerId);
    if (battleId == null) return;

    PokemonBattle battle = BattleRegistry.getBattle(battleId);
    if (battle != null && !battle.getEnded()) {
      cir.setReturnValue(battle);
      cir.cancel();
    } else {
      PLAYER_TO_BATTLE.remove(playerId);
    }
  }

  /**
   * Maps all player actors on a battle side to the associated battle UUID.
   *
   * @param side     battle side
   * @param battleId battle identifier
   */
  @Unique
  private static void indexSide(BattleSide side, UUID battleId) {
    if (side == null || side.getActors() == null) return;
    for (BattleActor actor : side.getActors()) {
      if (actor instanceof PlayerBattleActor playerActor) {
        PLAYER_TO_BATTLE.put(playerActor.getUuid(), battleId);
      }
    }
  }
}
