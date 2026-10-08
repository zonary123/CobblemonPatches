package org.kingpixel.cobblemonpatches.mixins.cobblemon.entity;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonMemories;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.entity.PokemonEntitySaveToWorldEvent;
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.activestate.ActivePokemonState;
import com.cobblemon.mod.common.pokemon.activestate.InactivePokemonState;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.ShoulderRidingEntity;
import net.minecraft.world.level.Level;
import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.kingpixel.cobblemonpatches.util.PokemonEntityUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cobblemon.mod.common.pokemon.ai.FormPokemonBehaviour;
import com.cobblemon.mod.common.pokemon.ai.MoveBehaviour;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Mixin into {@link PokemonEntity} optimizing persistence checks, deferred despawn processing
 * on server tick end, owner entity caching, and world saving logic.
 */
@Mixin(PokemonEntity.class)
public abstract class PokemonEntityMixin extends ShoulderRidingEntity {

  @Unique private static final Queue<PokemonEntity> DESPAWN_QUEUE = new ConcurrentLinkedQueue<>();

  @Shadow private Pokemon pokemon;
  @Shadow private PokemonPastureBlockEntity.Tethering tethering;
  @Shadow public abstract FormPokemonBehaviour getBehaviour();


  /**
   * Protected entity constructor.
   *
   * @param entityType entity type definition
   * @param world      world level
   */
  protected PokemonEntityMixin(EntityType<? extends ShoulderRidingEntity> entityType, Level world) {
    super(entityType, world);
  }

  /**
   * Evaluates if the Pokemon entity should persist, short-circuiting non-Combee species
   * before querying brain memory states.
   *
   * @return true if entity is persistent
   * @author MemencioPerez
   * @reason The only Pokémon that should have these memories as of Cobblemon 1.7.1
   * is Combee, so checking against the Species name should make for a faster route
   * by short-circuiting
   */
  @Overwrite
  public boolean isPersistenceRequired() {
    return super.isPersistenceRequired()
      || this.pokemon.getCanDropHeldItem$common() && !this.pokemon.getHeldItem$common().isEmpty()
      || (this.pokemon.getSpecies().getName().equals("Combee") && (this.brain.checkMemory(CobblemonMemories.HIVE_LOCATION, MemoryStatus.VALUE_PRESENT)
      || this.brain.checkMemory(CobblemonMemories.HIVE_COOLDOWN, MemoryStatus.VALUE_PRESENT)
      || this.brain.checkMemory(CobblemonMemories.NEARBY_SACC_LEAVES, MemoryStatus.VALUE_PRESENT)));
  }

  /**
   * Registers a server tick end listener to flush and safely discard queued despawning Pokemon entities.
   *
   * @param ci callback info
   */
  @Inject(method = "<clinit>", at = @At("TAIL"))
  private static void registerEndServerTickListener(CallbackInfo ci) {
    TickEvent.SERVER_POST.register(server -> {
      PokemonEntity pokemonEntity;
      while ((pokemonEntity = DESPAWN_QUEUE.poll()) != null) {
        if (canSafelyDiscard(pokemonEntity)) {
          discardAndDeactivate(pokemonEntity);
        }
      }
    });
  }

  /**
   * Determines whether a Pokémon entity can safely be discarded from the world.
   * <p>
   * This check governs entity lifecycle state and prevents both softlocks and exploits:
   * <ul>
   *   <li>Prevents battle softlocks by ensuring entities in active battles are not removed
   *       when chunk tracking stops or players walk away.</li>
   *   <li>Prevents item duplication exploits and orphaned ghost entities by allowing immediate
   *       purging of player-owned Pokémon whose owner has disconnected or changed servers.</li>
   *   <li>Protects tethered pasture Pokémon from unintended removal.</li>
   * </ul>
   *
   * @param entity the Pokémon entity to evaluate
   * @return {@code true} if the entity can safely be discarded; {@code false} otherwise
   */
  @Unique
  private static boolean canSafelyDiscard(PokemonEntity entity) {
    if (entity == null || entity.isRemoved()) {
      return false;
    }
    if (PokemonEntityUtils.isOrphanedPlayerPokemon(entity)) {
      return true;
    }
    return entity.getBattleId() == null && !entity.isBattling();
  }

  /**
   * Safely discards a Pokémon entity, cleanly terminating any linked battle and resetting
   * its Pokémon data state back to inactive.
   *
   * @param entity the Pokémon entity to discard and deactivate
   */
  @Unique
  private static void discardAndDeactivate(PokemonEntity entity) {
    if (entity.getBattleId() != null) {
      PokemonBattle battle = BattleRegistry.getBattle(entity.getBattleId());
      if (battle != null && !battle.getEnded()) {
        battle.stop();
      }
    }
    if (!entity.isRemoved()) {
      entity.discard();
    }
    Pokemon poke = entity.getPokemon();
    if (poke != null && poke.getState() instanceof ActivePokemonState) {
      poke.setState(new InactivePokemonState());
    }
  }

  /**
   * Periodically validates that player-owned party Pokemon entities do not linger in the world
   * if their owner player has disconnected or switched servers.
   *
   * @param ci callback info
   */
  @Inject(method = "tick", at = @At("HEAD"))
  private void guardOrphanedPokemon(CallbackInfo ci) {
    if (this.level().isClientSide() || this.isRemoved() || this.tickCount % 20 != 0) {
      return;
    }

    PokemonEntity self = (PokemonEntity) (Object) this;
    if (PokemonEntityUtils.isOrphanedPlayerPokemon(self)) {
      discardAndDeactivate(self);
    }
  }

  /**
   * Defers entity removal by queuing the entity to be discarded at the end of the server world tick.
   *
   * @param pokemonEntity pokemon entity to discard
   * @param reason        removal reason
   * @param original      wrapped method operation
   * @author MemencioPerez
   * @reason This method shouldn't be calling Entity#remove, it can cause a
   * ConcurrentModificationException under specific conditions. We're going
   * to add the PokemonEntity to a queue that will be processed later in the
   * Fabric END_WORLD_TICK event listener
   */
  @WrapOperation(method = "stopSeenByPlayer", at = @At(value = "INVOKE", target = "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
  public void cobblemonpatches$doNotCallEntityRemove(PokemonEntity pokemonEntity, RemovalReason reason, Operation<Void> original) {
    if (this.isRemoved()) return;
    DESPAWN_QUEUE.add(pokemonEntity);
  }

  @Unique private LivingEntity cobblemonpatches$cachedOwner = null;
  @Unique private long cobblemonpatches$ownerCacheTick = -1;

  /**
   * Returns cached owner entity for the current server tick, avoiding expensive entity lookups.
   *
   * @param cir callback returnable with owner living entity
   */
  @Inject(method = "getOwner", at = @At("HEAD"), cancellable = true)
  private void cobblemonpatches$fastGetOwner(CallbackInfoReturnable<LivingEntity> cir) {
    if (this.pokemon == null || this.pokemon.isWild()) {
      cir.setReturnValue(null);
      return;
    }
    long currentTick = this.level().getGameTime();
    if (this.cobblemonpatches$ownerCacheTick == currentTick
      && this.cobblemonpatches$cachedOwner != null
      && this.cobblemonpatches$cachedOwner.isAlive()
      && !this.cobblemonpatches$cachedOwner.isRemoved()) {
      cir.setReturnValue(this.cobblemonpatches$cachedOwner);
      return;
    }
    this.cobblemonpatches$cachedOwner = this.pokemon.getOwnerEntity();
    this.cobblemonpatches$ownerCacheTick = currentTick;
    cir.setReturnValue(this.cobblemonpatches$cachedOwner);
  }

  /**
   * Evaluates if the Pokemon entity should be saved to world data, firing save events when appropriate.
   *
   * @return true if entity should be persisted to world storage
   * @author MemencioPerez
   * @reason Remove unnecessary condition in PokemonEntity#shouldSave method
   */
  @Overwrite
  public boolean shouldBeSaved() {
    if (this.getOwnerUUID() == null
      && !this.pokemon.isNPCOwned()
      && (Cobblemon.INSTANCE.getConfig().getSavePokemonToWorld() || this.isPersistenceRequired())) {
      var event = new PokemonEntitySaveToWorldEvent((PokemonEntity) (Object) this);
      CobblemonEvents.POKEMON_ENTITY_SAVE_TO_WORLD.emit(event);
      if (!event.isCanceled()) return true;
    }

    return this.tethering != null;
  }



  /**
   * Throttles despawn evaluation to once every 20 ticks (1 Hz), matching CobblemonAgingDespawner's
   * internal interval and saving redundant persistence memory queries on all other ticks.
   *
   * @param ci callback info
   */
  @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
  private void cobblemonpatches$throttleCheckDespawn(CallbackInfo ci) {
    if (this.tickCount % 20 != 0) {
      ci.cancel();
    }
  }

  /**
   * Optimizes onGround collision queries by fast-pathing unridden Pokemon,
   * avoiding Kotlin riding supplier closures and controller queries on every physics step.
   *
   * @param cir callback returnable
   */
  @Inject(method = "onGround", at = @At("HEAD"), cancellable = true)
  private void cobblemonpatches$fastOnGround(CallbackInfoReturnable<Boolean> cir) {
    if (this.isVehicle()) {
      return;
    }
    if (this.pokemon != null && isPureFlyer(this.getBehaviour())) {
      cir.setReturnValue(false);
      return;
    }
    cir.setReturnValue(((EntityAccessor) this).cobblemonpatches$isOnGround());
  }

  @Unique
  private static boolean isPureFlyer(FormPokemonBehaviour behaviour) {
    if (behaviour == null) return false;
    MoveBehaviour moving = behaviour.getMoving();
    return moving != null && !moving.getWalk().getCanWalk() && moving.getFly().getCanFly();
  }
}
