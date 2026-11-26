package org.kingpixel.cobblemonpatches.mixins.cobblemon.tms;

import com.cobblemon.mod.common.api.tms.TechnicalMachine;
import com.cobblemon.mod.common.client.net.gui.SetActiveTMPacketHandler;
import com.cobblemon.mod.common.net.messages.client.ui.SetActiveTMPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.kingpixel.cobblemonpatches.util.TMObtainMethodUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Patches {@link SetActiveTMPacketHandler} so dynamic passive {@code ObtainMethod}s
 * (such as advancements, Y-level, or CobbleUtils conditions/permissions with {@code passive: true})
 * are evaluated server-side when selecting a TM in the TM Machine.
 *
 * @author Carlos Varas Alonso
 */
@Mixin(SetActiveTMPacketHandler.class)
public abstract class SetActiveTMPacketHandlerMixin {

  @Redirect(
    method = "handle(Lcom/cobblemon/mod/common/net/messages/client/ui/SetActiveTMPacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V",
    at = @At(
      value = "INVOKE",
      target = "Lcom/cobblemon/mod/common/api/tms/TechnicalMachine;isPassivelyObtained()Z",
      remap = false
    )
  )
  private boolean cobblemonPatches$evaluateDynamicPassiveObtainMethods(
    TechnicalMachine tm,
    SetActiveTMPacket packet,
    MinecraftServer server,
    ServerPlayer player
  ) {
    if (!TMObtainMethodUtils.isPatchEnabled()) {
      return tm.isPassivelyObtained();
    }
    return tm.isPassivelyObtained() || TMObtainMethodUtils.matchesDynamicPassive(tm, player);
  }
}
