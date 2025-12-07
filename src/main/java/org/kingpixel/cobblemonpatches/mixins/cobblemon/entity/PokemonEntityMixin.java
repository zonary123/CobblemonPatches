package org.kingpixel.cobblemonpatches.mixins.cobblemon.entity;

import com.cobblemon.mod.common.CobblemonMemories;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.passive.TameableShoulderEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PokemonEntity.class)
public abstract class PokemonEntityMixin extends TameableShoulderEntity {

  @Shadow
  private Pokemon pokemon;

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

  /**
   * @author MemencioPerez
   * @reason This method will still be called if the condition to call the recall
   * method in a Pokémon if it moves too far from its corresponding pasture block
   * passes, trying to remove the entity twice
   */
  @WrapMethod(method = "onStoppedTrackingBy")
  public void doNotCallEntityRemoveTwiceForOwnedPokemon(ServerPlayerEntity player, Operation<Void> original) {
    if (this.isRemoved()) return;
    original.call(player);
  }
}
