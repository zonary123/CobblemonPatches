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
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.passive.TameableShoulderEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Mixin into {@link PokemonEntity} optimizing persistence checks, deferred despawn processing
 * on server world tick end, owner entity caching, and world saving logic.
 */
@Mixin(PokemonEntity.class)
public abstract class PokemonEntityMixin extends TameableShoulderEntity {

  @Unique private static Queue<PokemonEntity> DESPAWN_QUEUE;

  @Shadow private Pokemon pokemon;
  @Shadow private PokemonPastureBlockEntity.Tethering tethering;

  /**
   * Protected entity constructor.
   *
   * @param entityType entity type definition
   * @param world      world level
   */
  protected PokemonEntityMixin(EntityType<? extends TameableShoulderEntity> entityType, World world) {
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
  public boolean isPersistent() {
    return super.isPersistent()
      || this.pokemon.getCanDropHeldItem$common() && !this.pokemon.getHeldItem$common().isEmpty()
      || (this.pokemon.getSpecies().getName().equals("Combee") && (this.brain.isMemoryInState(CobblemonMemories.HIVE_LOCATION, MemoryModuleState.VALUE_PRESENT)
      || this.brain.isMemoryInState(CobblemonMemories.HIVE_COOLDOWN, MemoryModuleState.VALUE_PRESENT)
      || this.brain.isMemoryInState(CobblemonMemories.NEARBY_SACC_LEAVES, MemoryModuleState.VALUE_PRESENT)));
  }

  /**
   * Registers a server world tick end listener to flush and safely discard queued despawning Pokemon entities.
   *
   * @param ci callback info
   */
  @Inject(method = "<clinit>", at = @At("TAIL"))
  private static void registerEndWorldTickListener(CallbackInfo ci) {
    DESPAWN_QUEUE = new ArrayDeque<>();
    ServerTickEvents.END_WORLD_TICK.register(world -> {
      while (!DESPAWN_QUEUE.isEmpty()) {
        PokemonEntity pokemonEntity = DESPAWN_QUEUE.remove();
        if (canSafelyDiscard(pokemonEntity)) {
          discardAndDeactivate(pokemonEntity);
        }
      }
    });
  }

  @Unique
  private static boolean canSafelyDiscard(PokemonEntity entity) {
    return entity != null && !entity.isRemoved() && entity.getBattleId() == null && !entity.isBattling();
  }

  @Unique
  private static void discardAndDeactivate(PokemonEntity entity) {
    if (!entity.isRemoved()) {
      entity.discard();
    }
    Pokemon poke = entity.getPokemon();
    if (poke != null && poke.getState() instanceof ActivePokemonState) {
      poke.setState(new InactivePokemonState());
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
  @WrapOperation(method = "onStoppedTrackingBy", at = @At(value = "INVOKE", target = "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;remove(Lnet/minecraft/entity/Entity$RemovalReason;)V"))
  public void cobblemonpatches$doNotCallEntityRemove(PokemonEntity pokemonEntity, RemovalReason reason, Operation<Void> original) {
    if (this.isRemoved() || pokemonEntity.getBattleId() != null || pokemonEntity.isBattling()) return;
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
    long currentTick = this.getWorld().getTime();
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
  public boolean shouldSave() {
    if (this.getOwnerUuid() == null
      && !this.pokemon.isNPCOwned()
      && (Cobblemon.INSTANCE.getConfig().getSavePokemonToWorld() || this.isPersistent())) {
      var event = new PokemonEntitySaveToWorldEvent((PokemonEntity) (Object) this);
      CobblemonEvents.POKEMON_ENTITY_SAVE_TO_WORLD.emit(event);
      if (!event.isCanceled()) return true;
    }

    return this.tethering != null;
  }
}
