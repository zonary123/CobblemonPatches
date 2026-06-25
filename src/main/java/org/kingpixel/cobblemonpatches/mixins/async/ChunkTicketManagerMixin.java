package org.kingpixel.cobblemonpatches.mixins.async;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkTicketManager.class)
public abstract class ChunkTicketManagerMixin {

  @Shadow
  private Long2ObjectMap<ObjectSet<ServerPlayerEntity>> playersByChunkPos;

  @Inject(
    method = "handleChunkLeave",
    at = @At("HEAD"),
    cancellable = true
  )
  private void guardHandleChunkLeave(
    ChunkSectionPos pos,
    ServerPlayerEntity player,
    CallbackInfo ci
  ) {
    long l = pos.toChunkPos().toLong();
    ObjectSet<ServerPlayerEntity> objectSet = this.playersByChunkPos.get(l);
    if (objectSet == null) {
      ci.cancel();
    }
  }
}
