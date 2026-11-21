package org.kingpixel.cobblemonpatches.config;

import lombok.Getter;
import lombok.Setter;

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

  public ModConfig() {
    // Default constructor for GSON deserialization
  }
}
