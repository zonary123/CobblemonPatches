package org.kingpixel.cobblemonpatches.mixins.entity;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 25/11/2025 5:35
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

  @Unique
  private static final Cache<Entity, ServerCommandSource> COMMAND_SOURCE_CACHE = Caffeine.newBuilder()
    .expireAfterWrite(15, TimeUnit.SECONDS)
    .maximumSize(500)
    .weakKeys()
    .build();

  @Inject(method = "getCommandSource", at = @At("RETURN"))
  private void EntityMixin$cacheCommandSource(CallbackInfoReturnable<ServerCommandSource> cir) {
    Entity self = (Entity) (Object) this;

    if (self.getWorld() instanceof ServerWorld && self.isPlayer()) {
      ServerCommandSource result = cir.getReturnValue();
      COMMAND_SOURCE_CACHE.put(self, result);
    }
  }

  // Mixin separado para leer de la cache
  @Inject(method = "getCommandSource", at = @At("HEAD"), cancellable = true)
  private void EntityMixin$getCachedCommandSource(CallbackInfoReturnable<ServerCommandSource> cir) {
    Entity self = (Entity) (Object) this;

    if (self.getWorld() instanceof ServerWorld && self.isPlayer()) {
      ServerCommandSource cached = COMMAND_SOURCE_CACHE.getIfPresent(self);
      if (cached != null) {
        cir.setReturnValue(cached);
      }
    }
  }
}
