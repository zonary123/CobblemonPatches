package org.kingpixel.cobblemonpatches.mixins.cobblemon.entity;

import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = EmptyPokeBallEntity.class, remap = false)
public abstract class EmptyPokeballEntityMixin extends ThrownItemEntity {

  public EmptyPokeballEntityMixin(EntityType<? extends ThrownItemEntity> entityType, double d, double e, double f, World world) {
    super(entityType, d, e, f, world);
  }

  /**
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
