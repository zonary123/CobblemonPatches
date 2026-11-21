package org.kingpixel.cobblemonpatches.models;

import lombok.Getter;

/**
 * Categorization for CobblemonPatches fixes and enhancements.
 */
@Getter
public enum PatchCategory {
  PERFORMANCE("Performance & Memory"),
  STABILITY("Stability & Crash Fixes"),
  SECURITY("Security & Anti-Exploit"),
  EXPLOIT_FIX("Anti-Dupe & Exploits");

  private final String displayName;

  PatchCategory(String displayName) {
    this.displayName = displayName;
  }
}
