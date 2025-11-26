package org.kingpixel.cobblemonpatches.mixins.network;

import com.cobblemon.mod.common.net.messages.server.BattleChallengePacket;
import com.cobblemon.mod.common.net.serverhandling.ChallengeHandler;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChallengeHandler.class)
public class ChallengeHandlerMixin {

  @WrapMethod(method = "handle(Lcom/cobblemon/mod/common/net/messages/server/BattleChallengePacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/network/ServerPlayerEntity;)V")
  private void ensureMain(BattleChallengePacket packet, MinecraftServer server, ServerPlayerEntity player, Operation<Void> original) {
    if (!server.isOnThread()) {
      server.execute(() -> original.call(packet, server, player));
      return;
    }

    original.call(packet, server, player);
  }
}
