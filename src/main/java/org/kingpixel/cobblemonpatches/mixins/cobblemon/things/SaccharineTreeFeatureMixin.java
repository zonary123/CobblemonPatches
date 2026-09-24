package org.kingpixel.cobblemonpatches.mixins.cobblemon.things;

import com.cobblemon.mod.common.world.feature.SaccharineTreeFeature;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.entity.Entity;
import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin for {@link SaccharineTreeFeature} to ensure world feature generation adds bees to hives on the server thread.
 * <p>
 * During asynchronous world generation or tree feature population, attempting to insert bee entities
 * directly into hive block entities from worker threads can trigger concurrency issues. This mixin
 * redirects the operation to the main server thread if not already running on it.
 * </p>
 *
 * @author Carlos Varas Alonso
 */
@Mixin(value = SaccharineTreeFeature.class, remap = false)
public abstract class SaccharineTreeFeatureMixin {

  /**
   * Wraps the bee nest population to ensure the hive entrance logic runs on the main server thread.
   *
   * @param instance The target beehive block entity.
   * @param entity   The bee entity to insert.
   * @param original The original entity-hive insertion operation.
   */
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
    if (CobblemonPatches.server != null && !CobblemonPatches.server.isOnThread()) {
      CobblemonPatches.server.execute(() -> original.call(instance, entity));
      return;
    }
    original.call(instance, entity);
  }
}


