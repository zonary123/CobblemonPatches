package org.kingpixel.cobblemonpatches.mixins.entity;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Carlos Varas Alonso - 25/11/2025 6:15
 */
@Mixin(ServerCommandSource.class)
public abstract class ServerCommandSourceMixin {
  @Inject(method = "getPosition", at = @At("HEAD"), cancellable = true)
  private void ServerCommandSourceMixin$fixGetPosition(CallbackInfoReturnable<Vec3d> cir) {
    ServerCommandSource self = (ServerCommandSource) (Object) this;
    if (self.getEntity() != null) {
      cir.setReturnValue(self.getEntity().getPos());
    }
  }
}
