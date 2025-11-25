package org.kingpixel.cobblemonpatches.mixins.things;

import com.cobblemon.mod.common.world.feature.SaccharineTreeFeature;
import net.minecraft.block.entity.BeehiveBlockEntity;
import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Carlos Varas Alonso
 * 24/11/2025 21:59
 */
@Mixin(value = SaccharineTreeFeature.class, remap = false)
public abstract class SaccharineTreeFeatureMixin {

  @Redirect(
    method = "populateBeeNest$lambda$0",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/block/entity/BeehiveBlockEntity;addBee(Lnet/minecraft/block/entity/BeehiveBlockEntity$BeeData;)V"
    )
  )
  private static void redirectAddBee(
    BeehiveBlockEntity hive,
    BeehiveBlockEntity.BeeData bee
  ) {
    CobblemonPatches.server.executeSync(() -> hive.addBee(bee));
  }
}
