package org.kingpixel.cobblemonpatches.application;

import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.kingpixel.cobblemonpatches.api.model.PatchConfigView;
import org.kingpixel.cobblemonpatches.api.model.PatchStatusView;
import org.kingpixel.cobblemonpatches.api.model.PatchSummaryView;
import org.kingpixel.cobblemonpatches.config.ModConfig;
import org.kingpixel.cobblemonpatches.models.PatchInfo;
import org.kingpixel.cobblemonpatches.models.PatchRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Application service providing read-only queries and immutable views for CobblemonPatches.
 */
public class PatchContextService {
  private static final PatchContextService INSTANCE = new PatchContextService();

  public static PatchContextService getInstance() {
    return INSTANCE;
  }

  private PatchContextService() {
  }

  public CompletableFuture<PatchConfigView> getConfigView() {
    return CompletableFuture.supplyAsync(() -> toView(CobblemonPatches.getConfig()));
  }

  public CompletableFuture<Optional<PatchStatusView>> getPatchStatus(String patchId) {
    if (patchId == null || patchId.isBlank()) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    return CompletableFuture.supplyAsync(() -> {
      Optional<PatchInfo> infoOpt = PatchRegistry.get(patchId);
      if (infoOpt.isEmpty()) {
        return Optional.empty();
      }
      PatchInfo info = infoOpt.get();
      boolean enabled = isPatchActive(info);
      return Optional.of(toView(info, enabled));
    });
  }

  public CompletableFuture<List<PatchStatusView>> getAllPatchStatuses() {
    return CompletableFuture.supplyAsync(() -> {
      List<PatchInfo> all = PatchRegistry.getAll();
      List<PatchStatusView> views = new ArrayList<>();
      for (PatchInfo info : all) {
        views.add(toView(info, isPatchActive(info)));
      }
      return Collections.unmodifiableList(views);
    });
  }

  public CompletableFuture<PatchSummaryView> getSummary() {
    return CompletableFuture.supplyAsync(() -> {
      ModConfig config = CobblemonPatches.getConfig();
      List<PatchInfo> all = PatchRegistry.getAll();
      List<PatchStatusView> views = new ArrayList<>();
      int active = 0;
      for (PatchInfo info : all) {
        boolean isEnabled = isPatchActive(info);
        if (isEnabled) {
          active++;
        }
        views.add(toView(info, isEnabled));
      }
      return new PatchSummaryView(all.size(), active, views, toView(config));
    });
  }

  public boolean isPatchActive(PatchInfo info) {
    if (info == null) {
      return false;
    }
    ModConfig config = CobblemonPatches.getConfig();
    return config.isPatchEnabled(info.getId(), info.isDefaultEnabled());
  }

  public boolean isPatchActive(String patchId) {
    if (patchId == null || patchId.isBlank()) {
      return false;
    }
    Optional<PatchInfo> infoOpt = PatchRegistry.get(patchId);
    if (infoOpt.isEmpty()) {
      return false;
    }
    return isPatchActive(infoOpt.get());
  }

  public PatchStatusView toView(PatchInfo info, boolean enabled) {
    if (info == null) {
      return null;
    }
    return new PatchStatusView(
      info.getId(),
      info.getName(),
      info.getCategory(),
      info.getDescription(),
      enabled,
      info.isDynamicToggleable()
    );
  }

  public PatchConfigView toView(ModConfig config) {
    if (config == null) {
      config = new ModConfig();
    }
    return new PatchConfigView(
      config.isDebug(),
      config.getBattleInactivityTimeoutSeconds(),
      config.getBattleInactivityTimeoutMessage(),
      config.getAutosaveStoresPerTick(),
      config.getPatchToggles()
    );
  }
}
