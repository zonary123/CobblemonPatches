package org.kingpixel.cobblemonpatches.api.model;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable view of the current CobblemonPatches global configuration.
 */
@Getter
public final class PatchConfigView {
  private final boolean debug;
  private final int battleInactivityTimeoutSeconds;
  private final String battleInactivityTimeoutMessage;
  private final int autosaveStoresPerTick;
  private final Map<String, Boolean> patchToggles;

  public PatchConfigView(boolean debug,
                         int battleInactivityTimeoutSeconds,
                         String battleInactivityTimeoutMessage,
                         int autosaveStoresPerTick,
                         Map<String, Boolean> patchToggles) {
    this.debug = debug;
    this.battleInactivityTimeoutSeconds = battleInactivityTimeoutSeconds;
    this.battleInactivityTimeoutMessage = battleInactivityTimeoutMessage != null ? battleInactivityTimeoutMessage : "";
    this.autosaveStoresPerTick = autosaveStoresPerTick;
    this.patchToggles = patchToggles != null
      ? Collections.unmodifiableMap(new HashMap<>(patchToggles))
      : Collections.emptyMap();
  }
}
