package org.kingpixel.cobblemonpatches.mixins.cobblemon.tms;

import com.cobblemon.mod.common.api.tms.TechnicalMachine;
import com.cobblemon.mod.common.api.tms.TechnicalMachines;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.kingpixel.cobblemonpatches.util.TMObtainMethodUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Patches {@link TechnicalMachines} to refresh the precomputed TM obtain-method lookup index
 * whenever TM datapacks are loaded or synchronized.
 *
 * @author Carlos Varas Alonso
 */
@Mixin(value = TechnicalMachines.class, remap = false)
public abstract class TechnicalMachinesMixin {

  @Inject(method = "reload", at = @At("RETURN"))
  private void cobblemonPatches$rebuildTMIndex(
    Map<ResourceLocation, TechnicalMachine> data,
    CallbackInfo ci
  ) {
    if (TMObtainMethodUtils.isPatchEnabled()) {
      TMObtainMethodUtils.rebuildIndex();
    } else {
      TMObtainMethodUtils.clearIndex();
    }
  }
}
