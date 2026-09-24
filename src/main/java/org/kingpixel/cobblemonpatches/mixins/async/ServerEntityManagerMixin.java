package org.kingpixel.cobblemonpatches.mixins.async;

import net.minecraft.server.world.ServerEntityManager;
import org.kingpixel.cobblemonpatches.PatchesUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link ServerEntityManager} to detect asynchronous chunk unloading.
 */
@Mixin(ServerEntityManager.class)
public abstract class ServerEntityManagerMixin {

  /**
   * Asserts that chunk unloading runs on the main server thread.
   *
   * @param ci callback information
   */
  @Inject(method = "unloadChunks", at = @At("HEAD"))
  private void beforeUnloadChunks(CallbackInfo ci) {
    PatchesUtil.catchOp("unloadChunks is executing");
  }
}
