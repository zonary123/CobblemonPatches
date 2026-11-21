package org.kingpixel.cobblemonpatches.api.model;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Immutable summary view aggregating all registered patches and active settings.
 */
@Getter
public final class PatchSummaryView {
  private final int totalPatches;
  private final int activePatches;
  private final List<PatchStatusView> patches;
  private final PatchConfigView config;

  public PatchSummaryView(int totalPatches,
                          int activePatches,
                          List<PatchStatusView> patches,
                          PatchConfigView config) {
    this.totalPatches = totalPatches;
    this.activePatches = activePatches;
    this.patches = patches != null ? Collections.unmodifiableList(patches) : Collections.emptyList();
    this.config = config;
  }
}
