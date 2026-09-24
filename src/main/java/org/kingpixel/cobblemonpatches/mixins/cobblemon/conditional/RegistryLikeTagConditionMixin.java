package org.kingpixel.cobblemonpatches.mixins.cobblemon.conditional;

import com.cobblemon.mod.common.api.conditional.RegistryLikeTagCondition;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Caches tag membership checks in {@link RegistryLikeTagCondition} to eliminate
 * redundant O(N) tag set iterations and TagKey equals calls during spawning passes.
 */
@Mixin(value = RegistryLikeTagCondition.class, remap = false)
public abstract class RegistryLikeTagConditionMixin<T> {

  @Shadow
  public abstract TagKey<T> getTag();

  @Unique
  private final ConcurrentHashMap<RegistryEntry<T>, Boolean> cobblemonPatches$cache = new ConcurrentHashMap<>(16);

  /**
   * Evaluates if the given registry entry is a member of the configured tag, using a memoization cache.
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
    boolean result = t.isIn(this.getTag());
    this.cobblemonPatches$cache.put(t, result);
    cir.setReturnValue(result);
  }
}
