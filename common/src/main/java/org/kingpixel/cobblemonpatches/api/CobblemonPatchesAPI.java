package org.kingpixel.cobblemonpatches.api;

import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.kingpixel.cobblemonpatches.api.action.PatchActionResult;
import org.kingpixel.cobblemonpatches.api.model.PatchConfigView;
import org.kingpixel.cobblemonpatches.api.model.PatchStatusView;
import org.kingpixel.cobblemonpatches.api.model.PatchSummaryView;
import org.kingpixel.cobblemonpatches.application.PatchActionService;
import org.kingpixel.cobblemonpatches.application.PatchContextService;
import org.kingpixel.cobblemonpatches.config.ModConfig;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Unified public API facade for CobblemonPatches.
 * Provides entry points for headless actions, asynchronous views, and synchronous helpers.
 */
public final class CobblemonPatchesAPI {

  private CobblemonPatchesAPI() {
  }

  // --- Headless Action API ---

  public static CompletableFuture<PatchActionResult> togglePatch(String patchId, boolean enabled) {
    return PatchActionService.getInstance().togglePatch(patchId, enabled);
  }

  public static CompletableFuture<PatchActionResult> enablePatch(String patchId) {
    return PatchActionService.getInstance().enablePatch(patchId);
  }

  public static CompletableFuture<PatchActionResult> disablePatch(String patchId) {
    return PatchActionService.getInstance().disablePatch(patchId);
  }

  public static CompletableFuture<PatchActionResult> reloadConfigs() {
    return PatchActionService.getInstance().reloadConfigs();
  }

  public static CompletableFuture<PatchActionResult> setDebug(boolean debug) {
    return PatchActionService.getInstance().setDebug(debug);
  }

  public static CompletableFuture<PatchActionResult> setBattleInactivityTimeout(int seconds) {
    return PatchActionService.getInstance().setBattleInactivityTimeout(seconds);
  }

  public static CompletableFuture<PatchActionResult> setAutosaveStoresPerTick(int count) {
    return PatchActionService.getInstance().setAutosaveStoresPerTick(count);
  }

  // --- Context & Query API ---

  public static CompletableFuture<PatchConfigView> getConfigView() {
    return PatchContextService.getInstance().getConfigView();
  }

  public static CompletableFuture<Optional<PatchStatusView>> getPatchStatus(String patchId) {
    return PatchContextService.getInstance().getPatchStatus(patchId);
  }

  public static CompletableFuture<List<PatchStatusView>> getAllPatchStatuses() {
    return PatchContextService.getInstance().getAllPatchStatuses();
  }

  public static CompletableFuture<PatchSummaryView> getSummary() {
    return PatchContextService.getInstance().getSummary();
  }

  // --- Direct Synchronous Convenience Helpers ---

  public static boolean isPatchEnabled(String patchId) {
    return PatchContextService.getInstance().isPatchActive(patchId);
  }

  public static boolean isDebugEnabled() {
    return CobblemonPatches.getConfig().isDebug();
  }

  public static ModConfig getConfig() {
    return CobblemonPatches.getConfig();
  }

  public static Optional<PatchStatusView> getPatchStatusSync(String patchId) {
    return PatchContextService.getInstance().getPatchStatus(patchId).join();
  }

  public static List<PatchStatusView> getAllPatchStatusesSync() {
    return PatchContextService.getInstance().getAllPatchStatuses().join();
  }

  public static PatchSummaryView getSummarySync() {
    return PatchContextService.getInstance().getSummary().join();
  }
}
