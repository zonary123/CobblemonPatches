package org.kingpixel.cobblemonpatches.mixins.async;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import org.kingpixel.cobblemonpatches.PatchesUtil;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(
  targets = {"net.minecraft.server.world.ServerWorld$ServerEntityHandler"}
)
public abstract class EntityCallBackMixin {

  @WrapMethod(
    method = "startTracking(Lnet/minecraft/entity/Entity;)V"
  )
  private void guardOnTrackingStart(Entity entity, Operation original) {
    PatchesUtil.catchOp("entity register");
    original.call(entity);
  }

  @WrapMethod(
    method = "stopTracking(Lnet/minecraft/entity/Entity;)V"
  )
  private void guardOnTrackingEnd(Entity entity, Operation original) {
    PatchesUtil.catchOp("entity unregister");
    original.call(entity);
  }
}
