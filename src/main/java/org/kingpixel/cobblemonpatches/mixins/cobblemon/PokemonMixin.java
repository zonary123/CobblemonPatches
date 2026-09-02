package org.kingpixel.cobblemonpatches.mixins.cobblemon;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokemonAspectsChangedEvent;
import com.cobblemon.mod.common.api.pokemon.aspect.AspectProvider;
import com.cobblemon.mod.common.net.messages.client.PokemonUpdatePacket;
import com.cobblemon.mod.common.net.messages.client.pokemon.update.AspectsUpdatePacket;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Carlos Varas Alonso - 27/10/2025 6:05
 */
@Mixin(value = Pokemon.class, remap = false)
public abstract class PokemonMixin {

  @Shadow private boolean isClient;
  @Shadow private Set<String> aspects;
  @Shadow private Set<String> forcedAspects;
  @Shadow public abstract boolean isWild();
  @Shadow public abstract UUID getOwnerUUID();
  @Shadow public abstract void updateForm();
  @Shadow public abstract void onChange(PokemonUpdatePacket<?> packet);

  // Improve performance of showdownId() by caching the result
  @Unique private String showdownIdCache = null;

  @Inject(method = "showdownId", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$headShowdownId(CallbackInfoReturnable<String> cir) {
    if (showdownIdCache != null) {
      cir.setReturnValue(showdownIdCache);
    }
  }

  @Inject(method = "showdownId", at = @At("RETURN"))
  private void cobblemonPatches$returnShowdownId(CallbackInfoReturnable<String> cir) {
    if (showdownIdCache == null && cir.getReturnValue() != null) {
      showdownIdCache = cir.getReturnValue().intern();
    }
  }

  @Inject(method = "setSpecies", at = @At("HEAD"))
  private void cobblemonPatches$onSetSpecies(Species value, CallbackInfo ci) {
    showdownIdCache = null;
  }

  @Inject(method = "setForm", at = @At("HEAD"))
  private void cobblemonPatches$onSetForm(FormData value, CallbackInfo ci) {
    showdownIdCache = null;
  }

  @Inject(method = "updateAspects", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$updateAspects(CallbackInfo ci) {
    if (!this.isClient) {
      Set<String> oldAspects = this.aspects;

      Set<String> newAspects = new LinkedHashSet<>(16);
      for (AspectProvider provider : AspectProvider.Companion.getProviders()) {
        newAspects.addAll(provider.provide((Pokemon) (Object) this));
      }
      newAspects.addAll(this.forcedAspects);

      if (!newAspects.equals(oldAspects)) {
        this.aspects = newAspects;
        this.updateForm();
        this.onChange(new AspectsUpdatePacket(() -> (Pokemon) (Object) this, newAspects));

        if (!this.isWild()) {
          CobblemonEvents.POKEMON_ASPECTS_CHANGED.post(
              new PokemonAspectsChangedEvent(this.getOwnerUUID(), (Pokemon) (Object) this));
        }
      }
    } else {
      this.aspects = this.forcedAspects;
    }
    ci.cancel();
  }
}
