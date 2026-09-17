package org.kingpixel.cobblemonpatches.mixins.cobblemon;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokemonAspectsChangedEvent;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.pokemon.aspect.AspectProvider;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.client.PokemonUpdatePacket;
import com.cobblemon.mod.common.net.messages.client.pokemon.update.AspectsUpdatePacket;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.pokemon.status.PersistentStatusContainer;
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
  @Shadow public abstract boolean isFainted();
  @Shadow public abstract void setCurrentHealth(int value);
  @Shadow public abstract int getMaxHealth();
  @Shadow public abstract MoveSet getMoveSet();
  @Shadow public abstract void setStatus(PersistentStatusContainer status);
  @Shadow public abstract void setFaintedTimer(int value);
  @Shadow public abstract void setHealTimer(int value);
  @Shadow public abstract PokemonEntity getEntity();
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

  /**
   * {@code heal()} runs HP/PP/{@code faintedTimer} only inside {@code POKEMON_HEALED.postThen}.
   * If that event is cancelled, fainted party Pokémon stay fainted while injured ones
   * still heal (they are not fainted when the event fires). Revive items use
   * {@code amount > 0}; the passive timer writes {@code currentHealth} directly.
   */
  @Inject(method = "heal", at = @At("RETURN"))
  private void cobblemonPatches$reviveIfHealEventCancelled(CallbackInfo ci) {
    if (this.isClient || this.isWild() || !this.isFainted()) {
      return;
    }
    this.setCurrentHealth(this.getMaxHealth());
    this.getMoveSet().heal();
    this.setStatus(null);
    this.setFaintedTimer(-1);
    this.setHealTimer(-1);
    PokemonEntity entity = this.getEntity();
    if (entity != null) {
      entity.heal(entity.getMaxHealth() - entity.getHealth());
    }
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
