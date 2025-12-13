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
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.passive.TameableShoulderEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Queue;

@Mixin(PokemonEntity.class)
public abstract class PokemonEntityMixin extends TameableShoulderEntity {

  // @formatter:off
  @Unique private static Queue<PokemonEntity> DESPAWN_QUEUE;

  @Shadow private Pokemon pokemon;
  @Shadow private PokemonPastureBlockEntity.Tethering tethering;
  // @formatter:on

  protected PokemonEntityMixin(EntityType<? extends TameableShoulderEntity> entityType, World world) {
    super(entityType, world);
  }

  /**
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

  @Inject(method = "<clinit>", at = @At("TAIL"))
  private static void registerEndWorldTickListener(CallbackInfo ci) {
    DESPAWN_QUEUE = new ArrayDeque<>();
    ServerTickEvents.END_WORLD_TICK.register(world -> {
      while (!DESPAWN_QUEUE.isEmpty()) {
        var pokemonEntity = DESPAWN_QUEUE.remove();
        if (!pokemonEntity.isRemoved()) {
          pokemonEntity.discard();
          continue;
        }

        var pokemon = pokemonEntity.getPokemon();
        if (pokemon.getState() instanceof ActivePokemonState) {
          pokemon.setState(new InactivePokemonState());
        }
      }
    });
  }

  /**
   * @author MemencioPerez
   * @reason This method shouldn't be calling Entity#remove, it can cause a
   * ConcurrentModificationException under specific conditions. We're going
   * to add the PokemonEntity to a queue that will be processed later in the
   * Fabric END_WORLD_TICK event listener
   */
  @WrapOperation(method = "onStoppedTrackingBy", at = @At(value = "INVOKE", target = "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;remove(Lnet/minecraft/entity/Entity$RemovalReason;)V"))
  public void cobblemonpatches$doNotCallEntityRemove(PokemonEntity pokemonEntity, RemovalReason reason, Operation<Void> original) {
    if (!this.isRemoved()) DESPAWN_QUEUE.add(pokemonEntity);
  }

  /**
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
