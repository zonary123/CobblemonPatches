package org.kingpixel.cobblemonpatches.mixins.async;

import net.minecraft.server.world.ServerEntityManager;
import org.kingpixel.cobblemonpatches.PatchesUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntityManager.class)
public abstract class ServerEntityManagerMixin {
  @Inject(method = "unloadChunks", at = @At("HEAD"))
  private void beforeUnloadChunks(CallbackInfo ci) {
    PatchesUtil.catchOp("unloadChunks is executing");
  }
}
