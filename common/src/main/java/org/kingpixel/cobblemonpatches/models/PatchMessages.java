package org.kingpixel.cobblemonpatches.models;

/**
 * Externalized message templates for CobblemonPatches user-facing API feedback.
 * Enforces zero hardcoded user-facing strings across services and API facades.
 */
public final class PatchMessages {
  public static final String SUCCESS_PATCH_ENABLED = "Patch '%s' has been enabled.";
  public static final String SUCCESS_PATCH_DISABLED = "Patch '%s' has been disabled.";
  public static final String SUCCESS_CONFIG_RELOADED = "CobblemonPatches configuration reloaded successfully.";
  public static final String SUCCESS_DEBUG_UPDATED = "Debug logging mode updated to %s.";
  public static final String SUCCESS_TIMEOUT_UPDATED = "Battle inactivity timeout updated to %d seconds.";
  public static final String SUCCESS_AUTOSAVE_UPDATED = "Autosave stores per tick updated to %d.";

  public static final String ERROR_PATCH_NOT_FOUND = "Patch '%s' was not found.";
  public static final String ERROR_INVALID_ARGUMENT = "Invalid argument: %s.";
  public static final String ERROR_ALREADY_IN_STATE = "Patch '%s' is already %s.";
  public static final String ERROR_NOT_TOGGLEABLE = "Patch '%s' cannot be dynamically toggled at runtime.";
  public static final String ERROR_GENERIC = "An unexpected error occurred: %s.";
  public static final String ERROR_CONFIG_SAVE = "Failed to persist configuration changes.";

  private PatchMessages() {
  }
}
