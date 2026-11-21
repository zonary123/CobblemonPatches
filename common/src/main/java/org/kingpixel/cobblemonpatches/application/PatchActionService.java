package org.kingpixel.cobblemonpatches.application;

import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.kingpixel.cobblemonpatches.api.action.PatchActionResult;
import org.kingpixel.cobblemonpatches.config.ConfigManager;
import org.kingpixel.cobblemonpatches.config.ModConfig;
import org.kingpixel.cobblemonpatches.models.PatchInfo;
import org.kingpixel.cobblemonpatches.models.PatchMessages;
import org.kingpixel.cobblemonpatches.models.PatchRegistry;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Application service providing headless asynchronous operations to toggle patches and update configurations.
 */
public class PatchActionService {
  private static final PatchActionService INSTANCE = new PatchActionService();

  public static PatchActionService getInstance() {
    return INSTANCE;
  }

  private PatchActionService() {
  }

  public CompletableFuture<PatchActionResult> togglePatch(String patchId, boolean enabled) {
    if (patchId == null || patchId.isBlank()) {
      return CompletableFuture.completedFuture(
        PatchActionResult.invalidArgument(String.format(PatchMessages.ERROR_INVALID_ARGUMENT, "patchId cannot be blank"))
      );
    }

    return CompletableFuture.supplyAsync(() -> {
      Optional<PatchInfo> infoOpt = PatchRegistry.get(patchId);
      if (infoOpt.isEmpty()) {
        return PatchActionResult.patchNotFound(
          String.format(PatchMessages.ERROR_PATCH_NOT_FOUND, patchId),
          patchId
        );
      }

      PatchInfo info = infoOpt.get();
      if (!info.isDynamicToggleable()) {
        return PatchActionResult.notToggleable(
          String.format(PatchMessages.ERROR_NOT_TOGGLEABLE, info.getName()),
          info.getId()
        );
      }

      ModConfig config = CobblemonPatches.getConfig();
      boolean current = config.isPatchEnabled(info.getId(), info.isDefaultEnabled());
      if (current == enabled) {
        String stateStr = enabled ? "enabled" : "disabled";
        return PatchActionResult.alreadyInState(
          String.format(PatchMessages.ERROR_ALREADY_IN_STATE, info.getName(), stateStr),
          info.getId(),
          enabled
        );
      }

      config.setPatchEnabled(info.getId(), enabled);
      ConfigManager.save();

      String msg = enabled
        ? String.format(PatchMessages.SUCCESS_PATCH_ENABLED, info.getName())
        : String.format(PatchMessages.SUCCESS_PATCH_DISABLED, info.getName());

      return PatchActionResult.success(msg, info.getId(), enabled);
    }).exceptionally(ex -> PatchActionResult.error(String.format(PatchMessages.ERROR_GENERIC, ex.getMessage())));
  }

  public CompletableFuture<PatchActionResult> enablePatch(String patchId) {
    return togglePatch(patchId, true);
  }

  public CompletableFuture<PatchActionResult> disablePatch(String patchId) {
    return togglePatch(patchId, false);
  }

  public CompletableFuture<PatchActionResult> reloadConfigs() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        ConfigManager.reload();
        return PatchActionResult.success(PatchMessages.SUCCESS_CONFIG_RELOADED);
      } catch (Exception ex) {
        return PatchActionResult.configError(String.format(PatchMessages.ERROR_GENERIC, ex.getMessage()));
      }
    });
  }

  public CompletableFuture<PatchActionResult> setDebug(boolean debug) {
    return CompletableFuture.supplyAsync(() -> {
      ModConfig config = CobblemonPatches.getConfig();
      config.setDebug(debug);
      ConfigManager.save();
      String modeStr = debug ? "ENABLED" : "DISABLED";
      return PatchActionResult.success(String.format(PatchMessages.SUCCESS_DEBUG_UPDATED, modeStr));
    }).exceptionally(ex -> PatchActionResult.error(String.format(PatchMessages.ERROR_GENERIC, ex.getMessage())));
  }

  public CompletableFuture<PatchActionResult> setBattleInactivityTimeout(int seconds) {
    if (seconds < 0) {
      return CompletableFuture.completedFuture(
        PatchActionResult.invalidArgument(String.format(PatchMessages.ERROR_INVALID_ARGUMENT, "seconds must be >= 0"))
      );
    }
    return CompletableFuture.supplyAsync(() -> {
      ModConfig config = CobblemonPatches.getConfig();
      config.setBattleInactivityTimeoutSeconds(seconds);
      ConfigManager.save();
      return PatchActionResult.success(String.format(PatchMessages.SUCCESS_TIMEOUT_UPDATED, seconds));
    }).exceptionally(ex -> PatchActionResult.error(String.format(PatchMessages.ERROR_GENERIC, ex.getMessage())));
  }

  public CompletableFuture<PatchActionResult> setAutosaveStoresPerTick(int count) {
    if (count <= 0) {
      return CompletableFuture.completedFuture(
        PatchActionResult.invalidArgument(String.format(PatchMessages.ERROR_INVALID_ARGUMENT, "count must be > 0"))
      );
    }
    return CompletableFuture.supplyAsync(() -> {
      ModConfig config = CobblemonPatches.getConfig();
      config.setAutosaveStoresPerTick(count);
      ConfigManager.save();
      return PatchActionResult.success(String.format(PatchMessages.SUCCESS_AUTOSAVE_UPDATED, count));
    }).exceptionally(ex -> PatchActionResult.error(String.format(PatchMessages.ERROR_GENERIC, ex.getMessage())));
  }
}
