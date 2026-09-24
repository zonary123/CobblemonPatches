package org.kingpixel.cobblemonpatches.mixins.async;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import org.kingpixel.cobblemonpatches.PatchesUtil;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin targeting {@code net.minecraft.server.world.ServerWorld$ServerEntityHandler}
 * to guarantee that entity tracking start and stop callbacks are dispatched on the main server thread,
 * preventing race conditions during asynchronous chunk/entity operations.
 */
@Mixin(
  targets = {"net.minecraft.server.world.ServerWorld$ServerEntityHandler"}
)
public abstract class EntityCallBackMixin {

  /**
   * Guards entity tracking start operations, redirecting execution to the main server thread if called asynchronously.
   *
   * @param entity   the entity being tracked
   * @param original the wrapped original method operation
   */
  @WrapMethod(
    method = "startTracking(Lnet/minecraft/entity/Entity;)V"
  )
  private void guardOnTrackingStart(Entity entity, Operation<?> original) {
    PatchesUtil.catchOp("entity register");

    if (entity == null) {
      original.call(null);
      return;
    }

    final Entity finalEntity = entity;
    final Operation<?> finalOriginal = original;

    MinecraftServer server = finalEntity.getServer();
    if (server != null && !server.isOnThread()) {
      server.execute(() -> finalOriginal.call(finalEntity));
    } else {
      original.call(entity);
    }
  }

  /**
   * Guards entity tracking stop operations, redirecting execution to the main server thread if called asynchronously.
   *
   * @param entity   the entity being untracked
   * @param original the wrapped original method operation
   */
  @WrapMethod(
    method = "stopTracking(Lnet/minecraft/entity/Entity;)V"
  )
  private void guardOnTrackingEnd(Entity entity, Operation<?> original) {
    PatchesUtil.catchOp("entity unregister");

    if (entity == null) {
      original.call(null);
      return;
    }

    final Entity finalEntity = entity;
    final Operation<?> finalOriginal = original;

    MinecraftServer server = finalEntity.getServer();
    if (server != null && !server.isOnThread()) {
      server.execute(() -> finalOriginal.call(finalEntity));
    } else {
      original.call(entity);
    }
  }
}
