package org.kingpixel.cobblemonpatches.mixins.cobblemon.network;

import com.cobblemon.mod.common.net.messages.server.pokemon.update.ServerboundUpdateRidingStatePacket;
import com.cobblemon.mod.common.net.serverhandling.pokemon.update.ServerboundUpdateRidingStateHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link ServerboundUpdateRidingStateHandler} to guarantee that incoming player riding state
 * packets are dispatched and processed on the main Minecraft server thread.
 */
@Mixin(value = ServerboundUpdateRidingStateHandler.class)
public class ServerboundUpdateRidingStateHandlerMixin {

  /**
   * Ensures that riding state update packet processing executes on the main server thread.
   *
   * @param packet riding state update packet
   * @param server server instance
   * @param player riding player entity
   * @param ci     callback info
   */
  @Inject(
    method = "handle(Lcom/cobblemon/mod/common/net/messages/server/pokemon/update/ServerboundUpdateRidingStatePacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/network/ServerPlayerEntity;)V",
    at = @At("HEAD"),
    cancellable = true
  )
  private void ensureMain(ServerboundUpdateRidingStatePacket packet, MinecraftServer server, ServerPlayerEntity player, CallbackInfo ci) {
    if (!server.isOnThread()) {
      ci.cancel();
      if (CobblemonPatches.getConfig().isDebug()) {
        CobblemonPatches.LOGGER.info("Redirected riding state packet for player '{}' to main server thread", player.getName().getString());
      }
      server.execute(() -> ServerboundUpdateRidingStateHandler.INSTANCE.handle(packet, server, player));
    }
  }
}
