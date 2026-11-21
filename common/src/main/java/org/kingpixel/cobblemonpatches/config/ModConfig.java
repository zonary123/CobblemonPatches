package org.kingpixel.cobblemonpatches.config;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration options for Cobblemon Patches.
 */
@Getter
@Setter
public class ModConfig {
  private boolean debug = false;

  private int battleInactivityTimeoutSeconds = 120;

  private String battleInactivityTimeoutMessage = "&cBattle timed out due to inactivity.";

  private int autosaveStoresPerTick = 2;

  private Map<String, Boolean> patchToggles = new HashMap<>();

  public ModConfig() {
    // Default constructor for GSON deserialization
  }

  public boolean isPatchEnabled(String patchId, boolean defaultValue) {
    if (patchId == null || patchToggles == null || !patchToggles.containsKey(patchId.toLowerCase())) {
      return defaultValue;
    }
    return Boolean.TRUE.equals(patchToggles.get(patchId.toLowerCase()));
  }

  public void setPatchEnabled(String patchId, boolean enabled) {
    if (patchId == null || patchId.isBlank()) {
      return;
    }
    if (patchToggles == null) {
      patchToggles = new HashMap<>();
    }
    patchToggles.put(patchId.toLowerCase(), enabled);
  }
}
