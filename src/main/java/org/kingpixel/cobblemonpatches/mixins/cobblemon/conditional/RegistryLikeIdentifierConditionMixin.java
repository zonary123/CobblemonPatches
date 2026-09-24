package org.kingpixel.cobblemonpatches.mixins.cobblemon.conditional;

import com.cobblemon.mod.common.api.conditional.RegistryLikeIdentifierCondition;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Caches identifier matching checks in {@link RegistryLikeIdentifierCondition}
 * to prevent repeated Identifier comparisons during spawning passes.
 */
@Mixin(value = RegistryLikeIdentifierCondition.class, remap = false)
public abstract class RegistryLikeIdentifierConditionMixin<T> {

  @Shadow
  public abstract Identifier getIdentifier();

  @Unique
  private final ConcurrentHashMap<RegistryEntry<T>, Boolean> cobblemonPatches$cache = new ConcurrentHashMap<>(16);

  /**
   * Evaluates if the given registry entry matches the configured identifier, utilizing a thread-safe memoization cache.
   *
   * @param t   the registry entry to test
   * @param cir callback returnable with matching result
   */
  @Inject(method = "fits(Lnet/minecraft/registry/entry/RegistryEntry;)Z", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$cachedFits(RegistryEntry<T> t, CallbackInfoReturnable<Boolean> cir) {
    if (t == null) {
      cir.setReturnValue(false);
      return;
    }
    Boolean cached = this.cobblemonPatches$cache.get(t);
    if (cached != null) {
      cir.setReturnValue(cached);
      return;
    }
    boolean result = t.matchesId(this.getIdentifier());
    this.cobblemonPatches$cache.put(t, result);
    cir.setReturnValue(result);
  }
}
