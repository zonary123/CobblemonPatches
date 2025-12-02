package org.kingpixel.cobblemonpatches.mixins.things;

import com.cobblemon.mod.common.world.feature.SaccharineTreeFeature;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.entity.Entity;
import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @author Carlos Varas Alonso
 * 24/11/2025 21:59
 */
@Mixin(value = SaccharineTreeFeature.class, remap = false)
public abstract class SaccharineTreeFeatureMixin {

  @WrapOperation(
    method = "populateBeeNest$lambda$0",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/block/entity/BeehiveBlockEntity;tryEnterHive(Lnet/minecraft/entity/Entity;)V"
    )
  )
  private static void wrapAddBee(
    BeehiveBlockEntity instance, Entity entity, Operation<Void> original
  ) {
    if (!CobblemonPatches.server.isOnThread()){
      CobblemonPatches.server.execute(() ->  original.call(instance, entity));
      return;
    }
    original.call(instance, entity);
  }
}

