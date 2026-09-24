package org.kingpixel.cobblemonpatches.mixins.cobblemon.network;

import com.cobblemon.mod.common.net.messages.server.BattleChallengeResponsePacket;
import com.cobblemon.mod.common.net.serverhandling.ChallengeResponseHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link ChallengeResponseHandler} to guarantee that incoming battle challenge response packets
 * are dispatched and processed on the main Minecraft server thread.
 *
 * @author Carlos Varas Alonso
 */
@Mixin(value = ChallengeResponseHandler.class, remap = false)
public class ChallengeResponseHandlerMixin {

  /**
   * Ensures that challenge response packet processing executes on the main thread.
   *
   * @param packet incoming challenge response packet
   * @param server server instance
   * @param player responding player
   * @param ci     callback info
   */
  @Inject(
    method = "handle(Lcom/cobblemon/mod/common/net/messages/server/BattleChallengeResponsePacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/network/ServerPlayerEntity;)V",
    at = @At("HEAD"),
    cancellable = true
  )
  private void ensureMain(BattleChallengeResponsePacket packet, MinecraftServer server, ServerPlayerEntity player, CallbackInfo ci) {
    if (!server.isOnThread()) {
      ci.cancel();
      server.execute(() -> ChallengeResponseHandler.INSTANCE.handle(packet, server, player));
    }
  }
}
