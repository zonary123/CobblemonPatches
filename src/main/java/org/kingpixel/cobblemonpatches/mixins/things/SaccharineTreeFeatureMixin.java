package org.kingpixel.cobblemonpatches.mixins.things;

import com.cobblemon.mod.common.world.feature.SaccharineTreeFeature;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.entity.BeehiveBlockEntity;
import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
      target = "Lnet/minecraft/block/entity/BeehiveBlockEntity;addBee(Lnet/minecraft/block/entity/BeehiveBlockEntity$BeeData;)V"
    )
  )
  private static void wrapAddBee(
    BeehiveBlockEntity hive,
    BeehiveBlockEntity.BeeData bee,
    Operation<Void> original
  ) {
    if (!CobblemonPatches.server.isOnThread()){
      CobblemonPatches.server.execute(() ->  original.call(hive, bee));
      return;
    }
    original.call(hive, bee);
  }
}

