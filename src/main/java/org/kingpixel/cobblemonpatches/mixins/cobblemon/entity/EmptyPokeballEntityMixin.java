package org.kingpixel.cobblemonpatches.mixins.cobblemon.entity;

import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin into {@link EmptyPokeBallEntity} to prevent {@link NullPointerException} crashes
 * during Pokeball capture collisions if the throwing player disconnected in the interim.
 */
@Mixin(value = EmptyPokeBallEntity.class, remap = false)
public abstract class EmptyPokeballEntityMixin extends ThrownItemEntity {

  /**
   * Constructs an instance of EmptyPokeballEntityMixin.
   *
   * @param entityType entity type definition
   * @param d          x position
   * @param e          y position
   * @param f          z position
   * @param world      world instance
   */
  public EmptyPokeballEntityMixin(EntityType<? extends ThrownItemEntity> entityType, double d, double e, double f, World world) {
    super(entityType, d, e, f, world);
  }

  /**
   * Guards beginCapture execution to ensure the throwing owner entity is still present and valid.
   *
   * @param instance empty pokeball entity
   * @param original wrapped operation
   * @author MemencioPerez
   * @reason There seems to be a rare case where a NullPointerException can
   * be thrown if a player throws a PokeBall and leaves the server after it
   * beams the Pokémon and starts falling and the PokeBall lands in a block
   * in the next server tick, because null cannot be cast to LivingEntity
   * in the EmptyPokeBallEntity#beginCapture method
   */
  @WrapOperation(method = "onCollision", at = @At(value = "INVOKE", target = "Lcom/cobblemon/mod/common/entity/pokeball/EmptyPokeBallEntity;beginCapture()V"))
  private void guardBeginCapture(EmptyPokeBallEntity instance, Operation<Void> original) {
    if (this.getOwner() == null) return;
    original.call(instance);
  }
}
