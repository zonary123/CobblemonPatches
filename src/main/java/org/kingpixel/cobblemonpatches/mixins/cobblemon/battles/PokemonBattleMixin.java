package org.kingpixel.cobblemonpatches.mixins.cobblemon.battles;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.FleeableBattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleFledEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import kotlin.Pair;
import kotlin.Unit;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.kingpixel.cobblemonpatches.config.ModConfig;
import org.kingpixel.cobblemonpatches.util.TextUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

import static com.cobblemon.mod.common.util.LocalizationUtilsKt.battleLang;

/**
 * Optimizes {@link PokemonBattle} operations by providing robust wild entity despawn detection,
 * anti-freeze watchdog timers, and optimized battle flee resolution.
 *
 * @author Carlos Varas Alonso
 */
@Mixin(value = PokemonBattle.class, remap = false)
public abstract class PokemonBattleMixin {

  @Unique
  private int inactivityTicks = 0;

  /**
   * Watchdog on battle ticking to detect removed wild entities and check inactivity timeout.
   *
   * @param ci callback info
   */
  @Inject(method = "tick", at = @At("TAIL"))
  private void watchdogTick(CallbackInfo ci) {
    PokemonBattle self = (PokemonBattle) (Object) this;
    if (!self.getStarted() || self.getEnded()) {
      this.inactivityTicks = 0;
      return;
    }

    if (checkWildEntitiesRemoved(self)) {
      Cobblemon.LOGGER.warn("Wild Pokémon entity removed from world during battle {}. Ending battle safely.", self.getBattleId());
      self.checkFlee();
      return;
    }

    checkInactivityTimeout(self);
  }

  @Unique
  private boolean checkWildEntitiesRemoved(PokemonBattle self) {
    if (!self.isPvW()) return false;
    for (BattleActor actor : self.getActors()) {
      if (actor.getType() == ActorType.WILD && actor instanceof EntityBackedBattleActor<?> entityActor) {
        Entity entity = entityActor.getEntity();
        if (entity != null && !entity.isRemoved()) {
          return false;
        }
      }
    }
    return true;
  }

  @Unique
  private void checkInactivityTimeout(PokemonBattle self) {
    boolean waitingForInput = false;
    for (BattleActor actor : self.getActors()) {
      if (actor.getMustChoose()) {
        waitingForInput = true;
        break;
      }
    }

    if (waitingForInput) {
      this.inactivityTicks++;
      if (this.inactivityTicks > 2400) {
        Cobblemon.LOGGER.warn("Battle {} timed out after 120s of total inactivity/lock. Resolving to prevent freeze.", self.getBattleId());
        ModConfig config = CobblemonPatches.getConfig();
        Text timeoutMsg = TextUtils.parse(config.getBattleInactivityTimeoutMessage());
        self.broadcastChatMessage(timeoutMsg);
        self.stop();
        this.inactivityTicks = 0;
      }
    } else {
      this.inactivityTicks = 0;
    }
  }

  /**
   * Evaluates if a wild Pokemon battle has ended due to distance or entity removal without creating intermediate stream allocations.
   *
   * @author Carlos Varas Alonso
   * @reason Optimized flee check with proper empty/despawn resolution
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

  /**
   * Categorizes battle actors into fleeable, player entity-backed, and wild Pokemon entity lists.
   *
   * @param allActors      all actors in the battle
   * @param fleeableActors output list for fleeable actors
   * @param playerEntities output list for player entity actors
   * @param wildEntities   output list for wild Pokemon entities
   */
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
        if (entity == null || entity.isRemoved()) continue;
        if (actor.getType() == ActorType.PLAYER) playerEntities.add(entityActor);
        else if (actor.getType() == ActorType.WILD && entity instanceof PokemonEntity pokemon) {
          wildEntities.add(pokemon);
        }
      }
    }
  }

  /**
   * Determines whether all fleeable actors are beyond their maximum allowed flee distance from all players
   * or have been removed/unloaded from the world.
   *
   * @param fleeableActors list of fleeable actors
   * @param playerEntities list of player entity actors
   * @return true if all fleeable actors are beyond flee distance or removed
   */
  @Unique
  private boolean allWildOutOfRange(List<FleeableBattleActor> fleeableActors,
                                    List<EntityBackedBattleActor<?>> playerEntities) {
    if (fleeableActors.isEmpty()) return true;

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

  /**
   * Computes the distance to the nearest participating player in the same world.
   *
   * @param pos            position of the Pokemon
   * @param world          world where the Pokemon is located
   * @param playerEntities list of player entity actors
   * @return Euclidean distance to nearest player or Float.MAX_VALUE if none found
   */
  @Unique
  private float nearestPlayerDistance(Vec3d pos, World world, List<EntityBackedBattleActor<?>> playerEntities) {
    float nearest = Float.MAX_VALUE;
    for (EntityBackedBattleActor<?> playerActor : playerEntities) {
      Entity entity = playerActor.getEntity();
      if (entity == null || entity.isRemoved() || entity.getWorld() != world) continue;

      float dist = (float) pos.distanceTo(entity.getPos());
      if (dist < nearest) nearest = dist;
    }
    return nearest;
  }

  /**
   * Fully heals all wild Pokemon involved in the fled battle.
   *
   * @param wildEntities list of wild Pokemon entities
   */
  @Unique
  private void healWildPokemon(List<PokemonEntity> wildEntities) {
    for (PokemonEntity entity : wildEntities) {
      if (entity != null && !entity.isRemoved() && entity.getPokemon() != null) {
        entity.getPokemon().heal();
      }
    }
  }

  /**
   * Finds any player battle actor present among the given actors.
   *
   * @param allActors iterable of battle actors
   * @return player battle actor or null
   */
  @Unique
  private PlayerBattleActor findAnyPlayer(Iterable<BattleActor> allActors) {
    for (BattleActor actor : allActors) {
      if (actor instanceof PlayerBattleActor player) return player;
    }
    return null;
  }

  /**
   * Posts the {@link BattleFledEvent} on the Cobblemon event bus.
   *
   * @param battle the fled battle instance
   * @param player the player battle actor
   */
  @Unique
  private void postBattleFledEvent(PokemonBattle battle, PlayerBattleActor player) {
    if (player != null) {
      BattleFledEvent[] events = {new BattleFledEvent(battle, player)};
      CobblemonEvents.BATTLE_FLED.post(events, e -> Unit.INSTANCE);
    }
  }

  /**
   * Sends the flee localization message to all participating actors.
   *
   * @param allActors iterable of battle actors
   */
  @Unique
  private void sendFleeMessages(Iterable<BattleActor> allActors) {
    Text text = battleLang("flee")
      .setStyle(Style.EMPTY.withColor(Formatting.YELLOW));
    for (BattleActor actor : allActors) {
      if (actor instanceof EntityBackedBattleActor<?> entityActor) {
        Entity entity = entityActor.getEntity();
        if (entity != null && !entity.isRemoved()) {
          entity.sendMessage(text);
        }
      }
    }
  }
}
