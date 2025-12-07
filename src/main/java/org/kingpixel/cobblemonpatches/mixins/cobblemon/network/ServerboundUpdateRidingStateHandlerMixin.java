package org.kingpixel.cobblemonpatches.mixins.cobblemon.network;

import com.cobblemon.mod.common.net.messages.server.pokemon.update.ServerboundUpdateRidingStatePacket;
import com.cobblemon.mod.common.net.serverhandling.pokemon.update.ServerboundUpdateRidingStateHandler;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerboundUpdateRidingStateHandler.class)
public class ServerboundUpdateRidingStateHandlerMixin {

  @WrapMethod(method = "handle(Lcom/cobblemon/mod/common/net/messages/server/pokemon/update/ServerboundUpdateRidingStatePacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/network/ServerPlayerEntity;)V")
  private void ensureMain(ServerboundUpdateRidingStatePacket packet, MinecraftServer server, ServerPlayerEntity player, Operation<Void> original) {
    if (!server.isOnThread()) {
      server.execute(() -> original.call(packet, server, player));
      return;
    }

    original.call(packet, server, player);
  }
}
