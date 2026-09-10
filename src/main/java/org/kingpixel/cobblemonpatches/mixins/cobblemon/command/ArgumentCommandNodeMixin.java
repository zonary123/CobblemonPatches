package org.kingpixel.cobblemonpatches.mixins.cobblemon.command;

import com.cobblemon.mod.common.command.argument.PokemonPropertiesArgumentType;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.minecraft.command.suggestion.SuggestionProviders;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ensures that all PokemonProperties argument nodes ask the server for suggestions,
 * enabling full server-side autocomplete (such as comma-separated movesets) for vanilla clients.
 *
 * @author Carlos Varas Alonso
 */
@Mixin(value = ArgumentCommandNode.class, remap = false)
public abstract class ArgumentCommandNodeMixin<S, T> {

  @Shadow
  @Final
  private ArgumentType<T> type;

  @Shadow
  @Final
  private SuggestionProvider<S> customSuggestions;

  @SuppressWarnings("unchecked")
  @Inject(method = "getCustomSuggestions", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$askServerForPokemonProperties(
      CallbackInfoReturnable<SuggestionProvider<S>> cir) {
    if (this.type instanceof PokemonPropertiesArgumentType && this.customSuggestions == null) {
      cir.setReturnValue((SuggestionProvider<S>) (Object) SuggestionProviders.ASK_SERVER);
    }
  }
}
