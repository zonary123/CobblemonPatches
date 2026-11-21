package org.kingpixel.cobblemonpatches.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of known optimization, security, and stability patches.
 */
public final class PatchRegistry {
  private static final Map<String, PatchInfo> PATCHES = new LinkedHashMap<>();

  static {
    register(new PatchInfo(
      "showdown_caching",
      "Showdown ID Caching",
      PatchCategory.PERFORMANCE,
      "Caches showdownId and formOnlyShowdownId for species and forms",
      true,
      true
    ));
    register(new PatchInfo(
      "pc_box_iterator",
      "PC Box Non-Allocating Iterator",
      PatchCategory.PERFORMANCE,
      "Optimizes PC box traversals without allocating intermediate array lists",
      true,
      true
    ));
    register(new PatchInfo(
      "battle_inactivity_timeout",
      "Battle Inactivity Timeout",
      PatchCategory.STABILITY,
      "Terminates abandoned or stuck battles to avoid player/entity freezes",
      true,
      true
    ));
    register(new PatchInfo(
      "autosave_spreading",
      "Tick-Spreading Autosave",
      PatchCategory.PERFORMANCE,
      "Spreads dirty player store saves across multiple server ticks",
      true,
      true
    ));
    register(new PatchInfo(
      "snowball_culling",
      "Snowball Projectile Culling",
      PatchCategory.SECURITY,
      "Purges lingering and stationary snowballs to prevent TPS drop and lag machines",
      true,
      true
    ));
    register(new PatchInfo(
      "tm_machine_security",
      "TM Machine Distance Validation",
      PatchCategory.EXPLOIT_FIX,
      "Enforces player reach and block entity validation to stop duplication exploits",
      true,
      true
    ));
    register(new PatchInfo(
      "npc_texture_cache",
      "NPC Player Texture Async Cache",
      PatchCategory.PERFORMANCE,
      "Caffeine async caching for NPC player skins to eliminate main thread stalls",
      true,
      true
    ));
    register(new PatchInfo(
      "aging_despawner",
      "Aging Despawner Optimization",
      PatchCategory.PERFORMANCE,
      "Throttles despawner checks and uses squared distance math",
      true,
      true
    ));
    register(new PatchInfo(
      "showdown_closed_stream",
      "Showdown Closed Stream Guard",
      PatchCategory.STABILITY,
      "Guards Showdown JS stream writes against TypeError exceptions on closed battles",
      true,
      true
    ));
  }

  private PatchRegistry() {
  }

  public static synchronized void register(PatchInfo info) {
    if (info != null && !info.getId().isBlank()) {
      PATCHES.put(info.getId().toLowerCase(), info);
    }
  }

  public static synchronized Optional<PatchInfo> get(String id) {
    if (id == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(PATCHES.get(id.toLowerCase()));
  }

  public static synchronized List<PatchInfo> getAll() {
    return Collections.unmodifiableList(new ArrayList<>(PATCHES.values()));
  }

  public static synchronized boolean exists(String id) {
    return id != null && PATCHES.containsKey(id.toLowerCase());
  }
}
