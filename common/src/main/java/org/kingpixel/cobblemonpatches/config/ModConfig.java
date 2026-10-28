package org.kingpixel.cobblemonpatches.config;

/**
 * Configuration options for Cobblemon Patches.
 */
public class ModConfig {
  private boolean debug = false;

  /**
   * Seconds a battle may wait on player input before it is force-ended. Set to -1 to disable.
   */
  private int battleInactivityTimeoutSeconds = 120;

  private String battleInactivityTimeoutMessage = "&cBattle timed out due to inactivity.";

  private int autosaveStoresPerTick = 2;

  public ModConfig() {
    // Default constructor for GSON deserialization
  }

  public boolean isDebug() {
    return this.debug;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  public int getBattleInactivityTimeoutSeconds() {
    return this.battleInactivityTimeoutSeconds;
  }

  public void setBattleInactivityTimeoutSeconds(int battleInactivityTimeoutSeconds) {
    this.battleInactivityTimeoutSeconds = battleInactivityTimeoutSeconds;
  }

  public String getBattleInactivityTimeoutMessage() {
    return this.battleInactivityTimeoutMessage;
  }

  public void setBattleInactivityTimeoutMessage(String battleInactivityTimeoutMessage) {
    this.battleInactivityTimeoutMessage = battleInactivityTimeoutMessage;
  }

  public int getAutosaveStoresPerTick() {
    return this.autosaveStoresPerTick;
  }

  public void setAutosaveStoresPerTick(int autosaveStoresPerTick) {
    this.autosaveStoresPerTick = autosaveStoresPerTick;
  }
}
