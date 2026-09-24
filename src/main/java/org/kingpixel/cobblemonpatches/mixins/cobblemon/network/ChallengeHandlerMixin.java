package org.kingpixel.cobblemonpatches.mixins.cobblemon.network;

import com.cobblemon.mod.common.net.messages.server.BattleChallengePacket;
import com.cobblemon.mod.common.net.serverhandling.ChallengeHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link ChallengeHandler} to guarantee that incoming battle challenge packets
 * are dispatched and processed on the main Minecraft server thread.
 */
@Mixin(ChallengeHandler.class)
public class ChallengeHandlerMixin {

  /**
   * Ensures that challenge packet processing executes on the main thread.
   *
   * @param packet incoming challenge packet
   * @param server server instance
   * @param player challenging player
   * @param ci     callback info
   */
  @Inject(
    method = "handle(Lcom/cobblemon/mod/common/net/messages/server/BattleChallengePacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/network/ServerPlayerEntity;)V",
    at = @At("HEAD"),
    cancellable = true
  )
  private void ensureMain(BattleChallengePacket packet, MinecraftServer server, ServerPlayerEntity player, CallbackInfo ci) {
    if (!server.isOnThread()) {
      ci.cancel();
      server.execute(() -> ChallengeHandler.INSTANCE.handle(packet, server, player));
    }
  }
}
