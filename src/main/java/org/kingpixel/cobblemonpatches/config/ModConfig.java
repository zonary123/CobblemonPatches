package org.kingpixel.cobblemonpatches.config;

/**
 * Configuration options for Cobblemon Patches.
 */
public class ModConfig {
  private boolean debug = false;

  private String battleInactivityTimeoutMessage = "&cBattle timed out due to inactivity.";

  public ModConfig() {
    // Default constructor for GSON deserialization
  }

  public boolean isDebug() {
    return this.debug;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  public String getBattleInactivityTimeoutMessage() {
    return this.battleInactivityTimeoutMessage;
  }

  public void setBattleInactivityTimeoutMessage(String battleInactivityTimeoutMessage) {
    this.battleInactivityTimeoutMessage = battleInactivityTimeoutMessage;
  }
}
