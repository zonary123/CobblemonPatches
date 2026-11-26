package org.kingpixel.cobblemonpatches.util;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.evolution.PreEvolution;
import com.cobblemon.mod.common.api.pokemon.moves.Learnset;
import com.cobblemon.mod.common.api.tms.ObtainMethod;
import com.cobblemon.mod.common.api.tms.TMMoveManager;
import com.cobblemon.mod.common.api.tms.TechnicalMachine;
import com.cobblemon.mod.common.api.tms.TechnicalMachines;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.tms.obtain.DefaultObtainMethod;
import com.cobblemon.mod.common.tms.obtain.ImpossibleObtainMethod;
import com.cobblemon.mod.common.tms.obtain.UnlockableObtainMethod;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.kingpixel.cobblemonpatches.CobblemonPatches;

/**
 * Pre-indexed utility methods that enforce and accelerate Cobblemon's {@link ObtainMethod} contract
 * across gameplay TM unlocks, direct command unlocks, and dynamic passive TM access.
 *
 * @author Carlos Varas Alonso
 */
public final class TMObtainMethodUtils {

  private static volatile TMIndex cachedIndex;

  private TMObtainMethodUtils() {
  }

  private record TMIndex(
    int sourceSize,
    Map<MoveTemplate, List<ResourceLocation>> gameplayUnlockableByMove,
    Set<ResourceLocation> gameplayUnlockableIds,
    Map<ResourceLocation, TechnicalMachine> directUnlockPredicateTMs,
    List<TechnicalMachine> dynamicPassiveTMs
  ) {
  }

  /**
   * Checks whether the TM {@link ObtainMethod} enforcement and dynamic passive sync patch
   * is enabled in {@code cobblemonpatches.json}.
   *
   * @return {@code true} if the TM obtain-method patch is enabled
   */
  public static boolean isPatchEnabled() {
    return CobblemonPatches.getConfig().isEnableTMObtainMethodPatch();
  }

  /**
   * Rebuilds the immutable TM obtain-method lookup index from {@link TechnicalMachines}.
   */
  public static void rebuildIndex() {
    Map<ResourceLocation, TechnicalMachine> tmMap = TechnicalMachines.INSTANCE.getTmMap();
    Map<MoveTemplate, List<ResourceLocation>> byMove = new HashMap<>();
    Set<ResourceLocation> unlockableIds = new HashSet<>();
    Map<ResourceLocation, TechnicalMachine> predicateTMs = new HashMap<>();
    List<TechnicalMachine> dynamicTMs = new ArrayList<>();

    for (Map.Entry<ResourceLocation, TechnicalMachine> entry : tmMap.entrySet()) {
      ResourceLocation id = entry.getKey();
      TechnicalMachine tm = entry.getValue();
      if (tm == null) {
        continue;
      }

      if (isGameplayUnlockable(tm)) {
        unlockableIds.add(id);
        byMove.computeIfAbsent(tm.getMoveName(), ignored -> new ArrayList<>(1)).add(id);
      }

      if (requiresPredicateOnDirectUnlock(tm)) {
        predicateTMs.put(id, tm);
      }

      if (hasDynamicPassiveMethod(tm)) {
        dynamicTMs.add(tm);
      }
    }

    cachedIndex = new TMIndex(
      tmMap.size(),
      byMove.isEmpty() ? Collections.emptyMap() : byMove,
      unlockableIds.isEmpty() ? Collections.emptySet() : unlockableIds,
      predicateTMs.isEmpty() ? Collections.emptyMap() : predicateTMs,
      dynamicTMs.isEmpty() ? Collections.emptyList() : List.copyOf(dynamicTMs)
    );
  }

  /**
   * Clears the cached TM index on server shutdown to prevent stale references across restarts.
   */
  public static void clearIndex() {
    cachedIndex = null;
  }

  private static TMIndex getIndex() {
    TMIndex current = cachedIndex;
    int currentSize = TechnicalMachines.INSTANCE.getTmMap().size();
    if (current == null || current.sourceSize != currentSize) {
      rebuildIndex();
      current = cachedIndex;
    }
    return current;
  }

  /**
   * Resolves an online {@link ServerPlayer} from the running server instance.
   *
   * @param playerUuid the player's UUID
   * @return the online player, or {@code null} if offline or server is unavailable
   */
  public static ServerPlayer resolvePlayer(UUID playerUuid) {
    MinecraftServer server = CobblemonPatches.server;
    if (server == null || playerUuid == null) {
      return null;
    }
    return server.getPlayerList().getPlayer(playerUuid);
  }

  /**
   * Determines in O(1) whether a TM may be unlocked through gameplay triggers
   * (Pokemon catch, level-up, evolution, login sync, or Data Monitor).
   *
   * @param tm the technical machine definition
   * @return {@code true} only if the TM declares {@code cobblemon:unlockable} (or has no obtain methods)
   */
  public static boolean canUnlockFromGameplay(TechnicalMachine tm) {
    if (tm == null) {
      return false;
    }
    return getIndex().gameplayUnlockableIds.contains(tm.getId());
  }

  /**
   * Resolves all gameplay-unlockable TM IDs learnable from a {@link Pokemon} using O(1) move lookups
   * against the pre-filtered index instead of scanning every registered TM.
   *
   * @param pokemon the Pokemon whose accessible, active, and pre-evolution moves are inspected
   * @return collection of eligible TM identifiers to unlock
   */
  public static Collection<ResourceLocation> getGameplayUnlockableTMsForPokemon(Pokemon pokemon) {
    if (pokemon == null) {
      return Collections.emptyList();
    }
    Map<MoveTemplate, List<ResourceLocation>> byMove = getIndex().gameplayUnlockableByMove;
    if (byMove.isEmpty()) {
      return Collections.emptyList();
    }

    Set<ResourceLocation> result = null;
    for (MoveTemplate move : pokemon.getAllAccessibleMoves()) {
      result = collectMatchingTMs(move, byMove, result);
    }
    for (Move move : pokemon.getMoveSet()) {
      result = collectMatchingTMs(move.getTemplate(), byMove, result);
    }

    int level = pokemon.getLevel();
    PreEvolution preEvolution = pokemon.getPreEvolution();
    while (preEvolution != null) {
      Learnset learnset = preEvolution.getForm().getMoves();
      for (Map.Entry<Integer, List<MoveTemplate>> entry : learnset.getLevelUpMoves().entrySet()) {
        if (entry.getKey() <= level) {
          for (MoveTemplate move : entry.getValue()) {
            result = collectMatchingTMs(move, byMove, result);
          }
        }
      }
      for (MoveTemplate move : learnset.getEvolutionMoves()) {
        result = collectMatchingTMs(move, byMove, result);
      }
      preEvolution = preEvolution.getForm().getPreEvolution();
    }

    return result != null ? result : Collections.emptyList();
  }

  /**
   * Filters direct TM unlock requests (e.g. {@code /technicalmachine unlock}) against
   * predicate-only custom obtain methods with an O(1) fast-path when none apply.
   *
   * @param tmIds      the requested TM identifiers
   * @param playerUuid the target player's UUID
   * @return the original collection when all IDs pass, or a filtered list otherwise
   */
  public static Collection<ResourceLocation> filterEligibleDirectUnlocks(
    Collection<ResourceLocation> tmIds,
    UUID playerUuid
  ) {
    if (tmIds == null || tmIds.isEmpty()) {
      return tmIds;
    }
    Map<ResourceLocation, TechnicalMachine> predicateTMs = getIndex().directUnlockPredicateTMs;
    if (predicateTMs.isEmpty()) {
      return tmIds;
    }

    boolean hasAnyPredicateTarget = false;
    for (ResourceLocation tmId : tmIds) {
      if (predicateTMs.containsKey(tmId)) {
        hasAnyPredicateTarget = true;
        break;
      }
    }
    if (!hasAnyPredicateTarget) {
      return tmIds;
    }

    ServerPlayer player = resolvePlayer(playerUuid);
    if (player == null) {
      return tmIds;
    }

    List<ResourceLocation> allowed = new ArrayList<>(tmIds.size());
    for (ResourceLocation tmId : tmIds) {
      TechnicalMachine gated = predicateTMs.get(tmId);
      if (gated == null || matchesAnyObtainMethod(gated, player)) {
        allowed.add(tmId);
      }
    }
    return allowed.size() == tmIds.size() ? tmIds : allowed;
  }

  /**
   * Checks whether any non-default passive {@link ObtainMethod} on the TM currently matches the player.
   *
   * @param tm     the technical machine definition
   * @param player the player to evaluate against
   * @return {@code true} if a dynamic passive obtain method matches the player
   */
  public static boolean matchesDynamicPassive(TechnicalMachine tm, ServerPlayer player) {
    if (tm == null || player == null) {
      return false;
    }
    for (ObtainMethod method : tm.getObtainMethods()) {
      if (method.getPassive() && !(method instanceof DefaultObtainMethod) && safeMatches(method, player)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks in O(1) whether at least one registered TM uses a non-default passive {@link ObtainMethod}.
   *
   * @return {@code true} if dynamic passive TM evaluation is needed on this server
   */
  public static boolean hasAnyDynamicPassiveTM() {
    return !getIndex().dynamicPassiveTMs.isEmpty();
  }

  /**
   * Builds the effective set of TM identifiers visible to a player's client without
   * mutating their persistent {@code learnedTMs} set. Returns {@code baseTMs} directly
   * (zero allocations) when no additional dynamic passive TMs match.
   *
   * @param player  the target player
   * @param baseTMs the player's persistent learned TMs
   * @return {@code baseTMs} if unchanged, or a new mutable set including matched dynamic passive TMs
   */
  public static Set<ResourceLocation> buildEffectiveClientTMs(
    ServerPlayer player,
    Set<ResourceLocation> baseTMs
  ) {
    List<TechnicalMachine> dynamicTMs = getIndex().dynamicPassiveTMs;
    if (player == null || dynamicTMs.isEmpty()) {
      return baseTMs;
    }

    Set<ResourceLocation> effective = null;
    for (TechnicalMachine tm : dynamicTMs) {
      ResourceLocation tmId = tm.getId();
      if (!baseTMs.contains(tmId) && matchesDynamicPassive(tm, player)) {
        if (effective == null) {
          effective = new HashSet<>(baseTMs.size() + dynamicTMs.size());
          effective.addAll(baseTMs);
        }
        effective.add(tmId);
      }
    }
    return effective != null ? effective : baseTMs;
  }

  /**
   * Re-evaluates dynamic passive TMs and pushes a non-incremental sync to the player's client
   * only when the server has at least one dynamic passive TM registered.
   *
   * @param player the player opening the TM Machine
   */
  public static void syncDynamicTMsToPlayer(ServerPlayer player) {
    if (player == null || !hasAnyDynamicPassiveTM()) {
      return;
    }
    TMMoveManager tmData = Cobblemon.INSTANCE.getPlayerDataManager().getTMData(player);
    tmData.syncClient(tmData.getLearnedTMs(), false);
  }

  private static Set<ResourceLocation> collectMatchingTMs(
    MoveTemplate move,
    Map<MoveTemplate, List<ResourceLocation>> byMove,
    Set<ResourceLocation> result
  ) {
    List<ResourceLocation> ids = byMove.get(move);
    if (ids == null) {
      return result;
    }
    if (result == null) {
      result = new HashSet<>();
    }
    result.addAll(ids);
    return result;
  }

  private static boolean isGameplayUnlockable(TechnicalMachine tm) {
    List<ObtainMethod> obtainMethods = tm.getObtainMethods();
    if (obtainMethods.isEmpty()) {
      return true;
    }
    for (ObtainMethod method : obtainMethods) {
      if (method instanceof UnlockableObtainMethod) {
        return true;
      }
    }
    return false;
  }

  private static boolean requiresPredicateOnDirectUnlock(TechnicalMachine tm) {
    List<ObtainMethod> obtainMethods = tm.getObtainMethods();
    if (obtainMethods.isEmpty()) {
      return false;
    }
    for (ObtainMethod method : obtainMethods) {
      if (method instanceof DefaultObtainMethod
        || method instanceof UnlockableObtainMethod
        || method instanceof ImpossibleObtainMethod) {
        return false;
      }
    }
    return true;
  }

  private static boolean hasDynamicPassiveMethod(TechnicalMachine tm) {
    for (ObtainMethod method : tm.getObtainMethods()) {
      if (method.getPassive() && !(method instanceof DefaultObtainMethod)) {
        return true;
      }
    }
    return false;
  }

  private static boolean matchesAnyObtainMethod(TechnicalMachine tm, ServerPlayer player) {
    for (ObtainMethod method : tm.getObtainMethods()) {
      if (safeMatches(method, player)) {
        return true;
      }
    }
    return false;
  }

  private static boolean safeMatches(ObtainMethod method, ServerPlayer player) {
    try {
      return method.matches(player);
    } catch (Throwable t) {
      CobblemonPatches.LOGGER.error(
        "Failed to evaluate TM ObtainMethod {} for player {}",
        method.getClass().getSimpleName(),
        player.getScoreboardName(),
        t
      );
      return false;
    }
  }
}
