package org.kingpixel.cobblemonpatches.mixins.cobblemon;

import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.pokemon.properties.PropertiesCompletionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Carlos Varas Alonso
 */
@Mixin(value = PropertiesCompletionProvider.class, remap = false)
public abstract class PropertiesCompletionProviderMixin {

  @Unique
  private static final String MOVES_KEY = "moves";

  @Unique
  private static boolean cobblemonPatches$movesPopulated = false;

  @Shadow
  @Final
  private static HashSet<PropertiesCompletionProvider.SuggestionHolder> providers;

  @Shadow
  public abstract void inject(Iterable<String> keys, Collection<String> suggestions);

  @Inject(method = "addDefaults", at = @At("RETURN"))
  private void cobblemonPatches$addMovesToDefaults(CallbackInfo ci) {
    cobblemonPatches$movesPopulated = false;
    cobblemonPatches$populateMoves();
  }

  @Inject(method = "sync", at = @At("HEAD"))
  private void cobblemonPatches$syncMoves(ServerPlayerEntity player, CallbackInfo ci) {
    if (!cobblemonPatches$movesPopulated) {
      cobblemonPatches$populateMoves();
    }
  }

  @Unique
  private void cobblemonPatches$populateMoves() {
    Collection<String> moveNames = Moves.names();
    if (moveNames == null || moveNames.isEmpty()) {
      return;
    }
    if (providers != null) {
      providers.removeIf(holder -> holder.getKeys().contains(MOVES_KEY));
    }
    inject(List.of(MOVES_KEY), new ArrayList<>(moveNames));
    cobblemonPatches$movesPopulated = true;
  }

  @Inject(method = "suggestValues", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$suggestMovesValues(
      String key,
      String currentValue,
      SuggestionsBuilder builder,
      CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
    if (!MOVES_KEY.equalsIgnoreCase(key)) {
      return;
    }

    Collection<String> moveNames = Moves.names();
    if (moveNames == null || moveNames.isEmpty()) {
      cir.setReturnValue(builder.buildFuture());
      return;
    }

    int lastCommaIndex = currentValue.lastIndexOf(',');
    Set<String> chosenMoves = extractChosenMoves(currentValue, lastCommaIndex);

    // A Pokémon can have at most 4 moves in a moveset.
    if (chosenMoves.size() >= 4) {
      cir.setReturnValue(builder.buildFuture());
      return;
    }

    String currentMovePrefix = lastCommaIndex != -1
        ? currentValue.substring(lastCommaIndex + 1)
        : currentValue;

    suggestAvailableMoves(moveNames, chosenMoves, currentMovePrefix, builder);
    cir.setReturnValue(builder.buildFuture());
  }

  @Unique
  private static Set<String> extractChosenMoves(String currentValue, int lastCommaIndex) {
    if (lastCommaIndex == -1) {
      return Collections.emptySet();
    }

    Set<String> chosen = new HashSet<>(4);
    int start = 0;
    while (start < lastCommaIndex) {
      int nextComma = currentValue.indexOf(',', start);
      if (nextComma == -1 || nextComma > lastCommaIndex) {
        nextComma = lastCommaIndex;
      }
      String part = currentValue.substring(start, nextComma).trim().toLowerCase(Locale.ROOT);
      if (!part.isEmpty()) {
        chosen.add(part);
      }
      start = nextComma + 1;
    }
    return chosen;
  }

  @Unique
  private static void suggestAvailableMoves(
      Collection<String> moveNames,
      Set<String> chosenMoves,
      String currentMovePrefix,
      SuggestionsBuilder builder) {
    String currentMovePrefixLower = currentMovePrefix.toLowerCase(Locale.ROOT);
    String remaining = builder.getRemaining();
    int prefixLength = currentMovePrefix.length();
    boolean hasChosen = !chosenMoves.isEmpty();

    for (String move : moveNames) {
      String moveLower = move.toLowerCase(Locale.ROOT);
      if ((!hasChosen || !chosenMoves.contains(moveLower)) && moveLower.startsWith(currentMovePrefixLower)) {
        String after = move.substring(prefixLength);
        builder.suggest(remaining + after);
      }
    }
  }
}
