package org.kingpixel.cobblemonpatches.mixins.cobblemon.battles;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import java.util.concurrent.TimeUnit;

@Mixin(value = BattleRegistry.class, remap = false)
public abstract class BattleRegistryMixin {

  /**
   * BattleId → PokemonBattle
   * Automatically managed cache with eviction.
   */
  @Unique
  private static final Cache<UUID, PokemonBattle> ACTIVE_BATTLES = Caffeine.newBuilder()
    .maximumSize(5000)
    .expireAfterAccess(30, TimeUnit.MINUTES)
    .build();

  /**
   * PlayerUUID → BattleId
   * O(1) player lookup index.
   */
  @Unique
  private static final ConcurrentHashMap<UUID, UUID> PLAYER_TO_BATTLE = new ConcurrentHashMap<>();

  /* ============================= */
  /* ========== EVENTS =========== */
  /* ============================= */

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

      UUID battleId = battle.getBattleId();

      ACTIVE_BATTLES.put(battleId, battle);

      indexSide(side1, battleId);
      indexSide(side2, battleId);

      return Unit.INSTANCE;
    });
  }

  @Inject(method = "closeBattle", at = @At("HEAD"))
  private static void onCloseBattle(PokemonBattle battle, CallbackInfo ci) {
    if (battle == null) return;

    UUID battleId = battle.getBattleId();

    ACTIVE_BATTLES.invalidate(battleId);

    PLAYER_TO_BATTLE.entrySet().removeIf(
      entry -> entry.getValue().equals(battleId)
    );
  }

  @Inject(method = "onPlayerDisconnect", at = @At("HEAD"))
  private void onPlayerDisconnect(ServerPlayerEntity player, CallbackInfo ci) {
    if (player == null) return;
    PLAYER_TO_BATTLE.remove(player.getUuid());
  }

  /* ============================= */
  /* ========== QUERIES ========== */
  /* ============================= */

  @Inject(method = "getBattle", at = @At("HEAD"), cancellable = true)
  private static void getBattle(UUID id, CallbackInfoReturnable<PokemonBattle> cir) {
    PokemonBattle battle = ACTIVE_BATTLES.getIfPresent(id);
    if (battle != null) {
      cir.setReturnValue(battle);
    }
  }

  @Inject(method = "getBattleByParticipatingPlayer", at = @At("HEAD"), cancellable = true)
  private static void getBattleByPlayer(
    ServerPlayerEntity player,
    CallbackInfoReturnable<PokemonBattle> cir
  ) {

    if (player == null) return;

    UUID battleId = PLAYER_TO_BATTLE.get(player.getUuid());
    if (battleId == null) return;

    PokemonBattle battle = ACTIVE_BATTLES.getIfPresent(battleId);
    if (battle != null) {
      cir.setReturnValue(battle);
    }
  }

  @Inject(method = "getBattleByParticipatingPlayerId", at = @At("HEAD"), cancellable = true)
  private static void getBattleByPlayerId(
    UUID playerId,
    CallbackInfoReturnable<PokemonBattle> cir
  ) {

    if (playerId == null) return;

    UUID battleId = PLAYER_TO_BATTLE.get(playerId);
    if (battleId == null) return;

    PokemonBattle battle = ACTIVE_BATTLES.getIfPresent(battleId);
    if (battle != null) {
      cir.setReturnValue(battle);
    }
  }

  /* ============================= */

  @Unique
  private static void indexSide(BattleSide side, UUID battleId) {
    for (BattleActor actor : side.getActors()) {
      if (actor instanceof PlayerBattleActor playerActor) {
        PLAYER_TO_BATTLE.put(playerActor.getUuid(), battleId);
      }
    }
  }
}
