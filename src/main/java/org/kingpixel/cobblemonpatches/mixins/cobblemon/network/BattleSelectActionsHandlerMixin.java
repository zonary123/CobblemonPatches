package org.kingpixel.cobblemonpatches.mixins.cobblemon.network;

import com.cobblemon.mod.common.net.messages.server.battle.BattleSelectActionsPacket;
import com.cobblemon.mod.common.net.serverhandling.battle.BattleSelectActionsHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link BattleSelectActionsHandler} to ensure that incoming battle action selection packets
 * are dispatched and processed on the main Minecraft server thread, avoiding race conditions and
 * concurrent modification locks with Showdown and battle tick loops.
 *
 * @author Carlos Varas Alonso
 */
@Mixin(value = BattleSelectActionsHandler.class, remap = false)
public class BattleSelectActionsHandlerMixin {

  /**
   * Ensures that battle action selection packet processing executes on the main server thread.
   *
   * @param packet incoming action packet
   * @param server server instance
   * @param player player selecting action
   * @param ci     callback info
   */
  @Inject(
    method = "handle(Lcom/cobblemon/mod/common/net/messages/server/battle/BattleSelectActionsPacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/network/ServerPlayerEntity;)V",
    at = @At("HEAD"),
    cancellable = true
  )
  private void ensureMain(BattleSelectActionsPacket packet, MinecraftServer server, ServerPlayerEntity player, CallbackInfo ci) {
    if (!server.isOnThread()) {
      ci.cancel();
      server.execute(() -> BattleSelectActionsHandler.INSTANCE.handle(packet, server, player));
    }
  }
}
