package org.kingpixel.cobblemonpatches.mixins.cobblemon.battles;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.FleeableBattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleFledEvent;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import kotlin.Pair;
import kotlin.Unit;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.cobblemon.mod.common.util.LocalizationUtilsKt.battleLang;

/**
 * Pokémon Battle optimization without the use of Streams, lambdas, or temporary objects.
 * Reduces GC pressure, direct access, and intelligent caching.
 * <p>
 * Author: Carlos Varas Alonso - 27/10/2025
 */
@Mixin(value = PokemonBattle.class, remap = false)
public abstract class PokemonBattleMixin {
  @Unique
  private List<BattleSide> cachedSides;
  @Unique
  private List<BattleActor> cachedActors;
  @Unique
  private List<ActiveBattlePokemon> cachedActivePokemon;
  @Unique
  private List<UUID> cachedPlayerUUIDs;
  @Unique
  private List<ServerPlayerEntity> cachedPlayers;
  @Unique
  private boolean cacheDirty = true;

  @Unique
  private void invalidateCache() {
    cacheDirty = true;
    cachedSides = null;
    cachedActors = null;
    cachedActivePokemon = null;
    cachedPlayerUUIDs = null;
    cachedPlayers = null;
  }

  @Unique
  private List<BattleSide> computeSides() {
    PokemonBattle self = (PokemonBattle) (Object) this;
    List<BattleSide> result = new ArrayList<>(2);
    result.add(self.getSide1());
    result.add(self.getSide2());
    return result;
  }

  @Unique
  private List<BattleActor> computeActors() {
    if (cachedSides == null) cachedSides = computeSides();
    List<BattleActor> result = new ArrayList<>(4);
    for (BattleSide side : cachedSides) {
      BattleActor[] actors = side.getActors();
      Collections.addAll(result, actors);
    }
    return result;
  }

  @Unique
  private List<ActiveBattlePokemon> computeActivePokemon() {
    if (cachedActors == null) cachedActors = computeActors();
    List<ActiveBattlePokemon> result = new ArrayList<>(6);
    for (BattleActor actor : cachedActors) {
      List<ActiveBattlePokemon> actives = actor.getActivePokemon();
      result.addAll(actives);
    }
    return result;
  }

  @Unique
  private List<UUID> computePlayerUUIDs() {
    if (cachedActors == null) cachedActors = computeActors();
    List<UUID> result = new ArrayList<>();
    for (BattleActor actor : cachedActors) {
      if (actor.getType() == ActorType.PLAYER) {
        UUID uuid = actor.getUuid();
        result.add(uuid);
      }
    }
    return result;
  }

  @Unique
  private List<ServerPlayerEntity> computePlayers() {
    if (cachedPlayerUUIDs == null) cachedPlayerUUIDs = computePlayerUUIDs();
    List<ServerPlayerEntity> result = new ArrayList<>(cachedPlayerUUIDs.size());
    for (UUID uuid : cachedPlayerUUIDs) {
      ServerPlayerEntity player = CobblemonPatches.server.getPlayerManager().getPlayer(uuid);
      if (player != null) result.add(player);
    }
    return result;
  }

  @Inject(method = "getSides", at = @At("HEAD"), cancellable = true)
  private void optimizeGetSides(CallbackInfoReturnable<Iterable<BattleSide>> cir) {
    if (!cacheDirty && cachedSides != null) {
      cir.setReturnValue(cachedSides);
      cir.cancel();
      return;
    }
    cachedSides = computeSides();
    cir.setReturnValue(cachedSides);
    cir.cancel();
  }

  @Inject(method = "getActors", at = @At("HEAD"), cancellable = true)
  private void optimizeGetActors(CallbackInfoReturnable<Iterable<BattleActor>> cir) {
    if (!cacheDirty && cachedActors != null) {
      cir.setReturnValue(cachedActors);
      cir.cancel();
      return;
    }
    cachedActors = computeActors();
    cir.setReturnValue(cachedActors);
    cir.cancel();
  }

  @Inject(method = "getActivePokemon", at = @At("HEAD"), cancellable = true)
  private void optimizeActivePokemon(CallbackInfoReturnable<Iterable<ActiveBattlePokemon>> cir) {
    if (!cacheDirty && cachedActivePokemon != null) {
      cir.setReturnValue(cachedActivePokemon);
      cir.cancel();
      return;
    }
    cachedActivePokemon = computeActivePokemon();
    cir.setReturnValue(cachedActivePokemon);
    cir.cancel();
  }

  @Inject(method = "getPlayerUUIDs", at = @At("HEAD"), cancellable = true)
  private void optimizePlayerUUIDs(CallbackInfoReturnable<Iterable<UUID>> cir) {
    if (!cacheDirty && cachedPlayerUUIDs != null) {
      cir.setReturnValue(cachedPlayerUUIDs);
      cir.cancel();
      return;
    }
    cachedPlayerUUIDs = computePlayerUUIDs();
    cir.setReturnValue(cachedPlayerUUIDs);
    cir.cancel();
  }

  @Inject(method = "getPlayers", at = @At("HEAD"), cancellable = true)
  private void optimizePlayers(CallbackInfoReturnable<Iterable<ServerPlayerEntity>> cir) {
    if (!cacheDirty && cachedPlayers != null) {
      cir.setReturnValue(cachedPlayers);
      cir.cancel();
      return;
    }
    cachedPlayers = computePlayers();
    cir.setReturnValue(cachedPlayers);
    cir.cancel();
  }

  @Unique
  private Boolean isPvNCache = null;

  @Inject(method = "isPvN", at = @At("HEAD"), cancellable = true)
  private void PokemonBattleMixin$isPvN(CallbackInfoReturnable<Boolean> cir) {
    if (isPvNCache != null) {
      cir.setReturnValue(isPvNCache);
      cir.cancel();
    }
  }

  @Inject(method = "isPvN", at = @At("RETURN"))
  private void PokemonBattleMixin$isPvNReturn(CallbackInfoReturnable<Boolean> cir) {
    isPvNCache = cir.getReturnValue();
  }

  @Unique
  private Boolean isPvPCache = null;

  @Inject(method = "isPvP", at = @At("HEAD"), cancellable = true)
  private void PokemonBattleMixin$isPvP(CallbackInfoReturnable<Boolean> cir) {
    if (isPvPCache != null) {
      cir.setReturnValue(isPvPCache);
      cir.cancel();
    }
  }

  @Inject(method = "isPvP", at = @At("RETURN"))
  private void PokemonBattleMixin$isPvPReturn(CallbackInfoReturnable<Boolean> cir) {
    isPvPCache = cir.getReturnValue();
  }

  @Unique
  private Boolean isPvWCache = null;

  @Inject(method = "isPvW", at = @At("HEAD"), cancellable = true)
  private void PokemonBattleMixin$isPvW(CallbackInfoReturnable<Boolean> cir) {
    if (isPvWCache != null) {
      cir.setReturnValue(isPvWCache);
      cir.cancel();
    }
  }

  @Inject(method = "isPvW", at = @At("RETURN"))
  private void PokemonBattleMixin$isPvWReturn(CallbackInfoReturnable<Boolean> cir) {
    isPvWCache = cir.getReturnValue();
  }

  /**
   * @author Carlos Varas Alonso
   * @reason Optimized flee check without streams or temporary objects
   */
  @Overwrite
  public void checkFlee() {
    PokemonBattle self = (PokemonBattle) (Object) this;

    List<FleeableBattleActor> fleeableActors = new ArrayList<>();
    List<EntityBackedBattleActor<?>> playerEntities = new ArrayList<>();
    List<PokemonEntity> wildEntities = new ArrayList<>();

    collectActors(self.getActors(), fleeableActors, playerEntities, wildEntities);

    if (allWildOutOfRange(fleeableActors, playerEntities)) {
      healWildPokemon(wildEntities);
      PlayerBattleActor playerActor = findAnyPlayer(self.getActors());
      postBattleFledEvent(self, playerActor);
      sendFleeMessages(self.getActors());
      self.stop();
    }
  }

  @Unique
  private void collectActors(Iterable<BattleActor> allActors,
                             List<FleeableBattleActor> fleeableActors,
                             List<EntityBackedBattleActor<?>> playerEntities,
                             List<PokemonEntity> wildEntities) {
    for (BattleActor actor : allActors) {
      if (actor instanceof FleeableBattleActor fleeable) {
        fleeableActors.add(fleeable);
      }
      if (actor instanceof EntityBackedBattleActor<?> entityActor) {
        Entity entity = entityActor.getEntity();
        if (entity == null) continue;
        if (actor.getType() == ActorType.PLAYER) playerEntities.add(entityActor);
        else if (actor.getType() == ActorType.WILD && entity instanceof PokemonEntity pokemon) {
          wildEntities.add(pokemon);
        }
      }
    }
  }

  @Unique
  private boolean allWildOutOfRange(List<FleeableBattleActor> fleeableActors,
                                    List<EntityBackedBattleActor<?>> playerEntities) {
    for (FleeableBattleActor pokemonActor : fleeableActors) {
      Pair<ServerWorld, Vec3d> wp = pokemonActor.getWorldAndPosition();
      if (wp == null) continue;

      World world = wp.getFirst();
      Vec3d pos = wp.getSecond();
      float fleeDist = pokemonActor.getFleeDistance();

      if (fleeDist == -1f) return false;

      if (nearestPlayerDistance(pos, world, playerEntities) < fleeDist) return false;
    }
    return true;
  }

  @Unique
  private float nearestPlayerDistance(Vec3d pos, World world, List<EntityBackedBattleActor<?>> playerEntities) {
    float nearest = Float.MAX_VALUE;
    for (EntityBackedBattleActor<?> playerActor : playerEntities) {
      Entity entity = playerActor.getEntity();
      if (entity == null || entity.getWorld() != world) continue;

      float dist = (float) pos.distanceTo(entity.getPos());
      if (dist < nearest) nearest = dist;
    }
    return nearest;
  }

  @Unique
  private void healWildPokemon(List<PokemonEntity> wildEntities) {
    for (PokemonEntity entity : wildEntities) {
      entity.getPokemon().heal();
    }
  }

  @Unique
  private PlayerBattleActor findAnyPlayer(Iterable<BattleActor> allActors) {
    for (BattleActor actor : allActors) {
      if (actor instanceof PlayerBattleActor player) return player;
    }
    return null;
  }

  @Unique
  private void postBattleFledEvent(PokemonBattle battle, PlayerBattleActor player) {
    if (player != null) {
      BattleFledEvent[] events = {new BattleFledEvent(battle, player)};
      CobblemonEvents.BATTLE_FLED.post(events, e -> Unit.INSTANCE);
    }
  }

  @Unique
  private void sendFleeMessages(Iterable<BattleActor> allActors) {
    Text text = battleLang("flee")
      .setStyle(Style.EMPTY.withColor(Formatting.YELLOW));
    for (BattleActor actor : allActors) {
      if (actor instanceof EntityBackedBattleActor<?> entityActor) {
        Entity entity = entityActor.getEntity();
        if (entity != null) {
          entity.sendMessage(text);
        }
      }
    }
  }
}
