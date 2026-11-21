package org.kingpixel.cobblemonpatches;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kingpixel.cobblemonpatches.api.CobblemonPatchesAPI;
import org.kingpixel.cobblemonpatches.api.action.PatchActionResult;
import org.kingpixel.cobblemonpatches.api.model.PatchConfigView;
import org.kingpixel.cobblemonpatches.api.model.PatchStatusView;
import org.kingpixel.cobblemonpatches.api.model.PatchSummaryView;
import org.kingpixel.cobblemonpatches.application.PatchActionService;
import org.kingpixel.cobblemonpatches.application.PatchContextService;
import org.kingpixel.cobblemonpatches.config.ConfigManager;
import org.kingpixel.cobblemonpatches.config.ModConfig;
import org.kingpixel.cobblemonpatches.models.PatchCategory;
import org.kingpixel.cobblemonpatches.models.PatchInfo;
import org.kingpixel.cobblemonpatches.models.PatchRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestCobblemonPatchesApi {

  private Path tempConfigPath;

  @BeforeEach
  void setUp() throws IOException {
    tempConfigPath = Files.createTempFile("cobblemonpatches_test_", ".json");
    ConfigManager.setConfigPathForTesting(tempConfigPath);
    ModConfig freshConfig = new ModConfig();
    ConfigManager.setConfigForTesting(freshConfig);
  }

  @AfterEach
  void tearDown() throws IOException {
    if (tempConfigPath != null && Files.exists(tempConfigPath)) {
      Files.deleteIfExists(tempConfigPath);
    }
  }

  @Test
  void testActionResultStatusesAndFactories() {
    PatchActionResult successWithDetails = PatchActionResult.success("Enabled!", "showdown_caching", true);
    assertTrue(successWithDetails.isSuccess());
    assertEquals(PatchActionResult.Status.SUCCESS, successWithDetails.getStatus());
    assertEquals("Enabled!", successWithDetails.getMessage());
    assertEquals("showdown_caching", successWithDetails.getPatchId());
    assertTrue(successWithDetails.isEnabled());

    PatchActionResult successSimple = PatchActionResult.success("Success!");
    assertTrue(successSimple.isSuccess());
    assertEquals(PatchActionResult.Status.SUCCESS, successSimple.getStatus());
    assertEquals("Success!", successSimple.getMessage());
    assertEquals("", successSimple.getPatchId());
    assertFalse(successSimple.isEnabled());

    PatchActionResult notFound = PatchActionResult.patchNotFound("Not found", "unknown_patch");
    assertFalse(notFound.isSuccess());
    assertEquals(PatchActionResult.Status.PATCH_NOT_FOUND, notFound.getStatus());
    assertEquals("Not found", notFound.getMessage());
    assertEquals("unknown_patch", notFound.getPatchId());

    PatchActionResult invalidArg = PatchActionResult.invalidArgument("Invalid parameter");
    assertFalse(invalidArg.isSuccess());
    assertEquals(PatchActionResult.Status.INVALID_ARGUMENT, invalidArg.getStatus());
    assertEquals("Invalid parameter", invalidArg.getMessage());

    PatchActionResult alreadyState = PatchActionResult.alreadyInState("Already enabled", "battle_timeout", true);
    assertFalse(alreadyState.isSuccess());
    assertEquals(PatchActionResult.Status.ALREADY_IN_STATE, alreadyState.getStatus());
    assertEquals("battle_timeout", alreadyState.getPatchId());
    assertTrue(alreadyState.isEnabled());

    PatchActionResult notToggleable = PatchActionResult.notToggleable("Static patch", "core_mixin");
    assertFalse(notToggleable.isSuccess());
    assertEquals(PatchActionResult.Status.NOT_TOGGLEABLE, notToggleable.getStatus());

    PatchActionResult configError = PatchActionResult.configError("Failed to write");
    assertFalse(configError.isSuccess());
    assertEquals(PatchActionResult.Status.CONFIG_ERROR, configError.getStatus());

    PatchActionResult genericError = PatchActionResult.error("Crash");
    assertFalse(genericError.isSuccess());
    assertEquals(PatchActionResult.Status.ERROR, genericError.getStatus());
    assertEquals("Crash", genericError.getMessage());
  }

  @Test
  void testPatchViewModelsAndImmutability() {
    PatchStatusView statusView = new PatchStatusView(
      "test_patch",
      "Test Patch",
      PatchCategory.PERFORMANCE,
      "Test description",
      true,
      true
    );
    assertEquals("test_patch", statusView.getId());
    assertEquals("Test Patch", statusView.getName());
    assertEquals(PatchCategory.PERFORMANCE, statusView.getCategory());
    assertEquals("Test description", statusView.getDescription());
    assertTrue(statusView.isEnabled());
    assertTrue(statusView.isDynamicToggleable());

    Map<String, Boolean> toggles = new HashMap<>();
    toggles.put("showdown_caching", true);
    PatchConfigView configView = new PatchConfigView(
      true,
      90,
      "Timeout message",
      4,
      toggles
    );
    assertTrue(configView.isDebug());
    assertEquals(90, configView.getBattleInactivityTimeoutSeconds());
    assertEquals("Timeout message", configView.getBattleInactivityTimeoutMessage());
    assertEquals(4, configView.getAutosaveStoresPerTick());
    assertEquals(1, configView.getPatchToggles().size());
    assertTrue(configView.getPatchToggles().get("showdown_caching"));

    // Verify unmodifiable map defensively protects state
    Map<String, Boolean> retrievedToggles = configView.getPatchToggles();
    assertThrows(UnsupportedOperationException.class, () -> retrievedToggles.put("new_toggle", false));

    List<PatchStatusView> patches = new ArrayList<>();
    patches.add(statusView);
    PatchSummaryView summaryView = new PatchSummaryView(1, 1, patches, configView);
    assertEquals(1, summaryView.getTotalPatches());
    assertEquals(1, summaryView.getActivePatches());
    assertEquals(1, summaryView.getPatches().size());
    assertSame(configView, summaryView.getConfig());

    // Verify unmodifiable list defensively protects state
    List<PatchStatusView> retrievedPatches = summaryView.getPatches();
    assertThrows(UnsupportedOperationException.class, () -> retrievedPatches.clear());
  }

  @Test
  void testPatchRegistryAndCategories() {
    assertTrue(PatchRegistry.exists("showdown_caching"));
    assertTrue(PatchRegistry.exists("SHOWDOWN_CACHING"));
    assertTrue(PatchRegistry.exists("pc_box_iterator"));
    assertTrue(PatchRegistry.exists("battle_inactivity_timeout"));
    assertTrue(PatchRegistry.exists("autosave_spreading"));
    assertTrue(PatchRegistry.exists("snowball_culling"));
    assertTrue(PatchRegistry.exists("tm_machine_security"));
    assertTrue(PatchRegistry.exists("npc_texture_cache"));
    assertTrue(PatchRegistry.exists("aging_despawner"));
    assertTrue(PatchRegistry.exists("showdown_closed_stream"));

    Optional<PatchInfo> info = PatchRegistry.get("showdown_caching");
    assertTrue(info.isPresent());
    assertEquals(PatchCategory.PERFORMANCE, info.get().getCategory());
    assertEquals("Performance & Memory", info.get().getCategory().getDisplayName());

    assertFalse(PatchRegistry.exists("non_existent_patch_123"));
    assertTrue(PatchRegistry.get("non_existent_patch_123").isEmpty());
    assertTrue(PatchRegistry.get(null).isEmpty());

    // Test registering a new custom patch
    PatchInfo custom = new PatchInfo(
      "custom_anti_exploit",
      "Custom Exploit Patch",
      PatchCategory.EXPLOIT_FIX,
      "Custom mitigation",
      true,
      true
    );
    PatchRegistry.register(custom);
    assertTrue(PatchRegistry.exists("custom_anti_exploit"));
    assertEquals(PatchCategory.EXPLOIT_FIX, PatchRegistry.get("custom_anti_exploit").get().getCategory());
  }

  @Test
  void testPatchActionServiceGuards() {
    PatchActionService service = PatchActionService.getInstance();

    // Blank patchId returns INVALID_ARGUMENT
    PatchActionResult blankResult = service.togglePatch("", true).join();
    assertFalse(blankResult.isSuccess());
    assertEquals(PatchActionResult.Status.INVALID_ARGUMENT, blankResult.getStatus());

    PatchActionResult nullResult = service.togglePatch(null, true).join();
    assertFalse(nullResult.isSuccess());
    assertEquals(PatchActionResult.Status.INVALID_ARGUMENT, nullResult.getStatus());

    // Unknown patchId returns PATCH_NOT_FOUND
    PatchActionResult unknownResult = service.togglePatch("unknown_patch_xyz", true).join();
    assertFalse(unknownResult.isSuccess());
    assertEquals(PatchActionResult.Status.PATCH_NOT_FOUND, unknownResult.getStatus());

    // Toggling to the state it already is in returns ALREADY_IN_STATE
    // "showdown_caching" defaults to enabled (true)
    PatchActionResult alreadyEnabled = service.togglePatch("showdown_caching", true).join();
    assertFalse(alreadyEnabled.isSuccess());
    assertEquals(PatchActionResult.Status.ALREADY_IN_STATE, alreadyEnabled.getStatus());

    // Toggling to false succeeds
    PatchActionResult disableResult = service.disablePatch("showdown_caching").join();
    assertTrue(disableResult.isSuccess());
    assertEquals(PatchActionResult.Status.SUCCESS, disableResult.getStatus());
    assertFalse(disableResult.isEnabled());
    assertFalse(CobblemonPatches.getConfig().isPatchEnabled("showdown_caching", true));

    // Re-enabling succeeds
    PatchActionResult enableResult = service.enablePatch("showdown_caching").join();
    assertTrue(enableResult.isSuccess());
    assertEquals(PatchActionResult.Status.SUCCESS, enableResult.getStatus());
    assertTrue(enableResult.isEnabled());
    assertTrue(CobblemonPatches.getConfig().isPatchEnabled("showdown_caching", false));

    // Negative battle timeout returns INVALID_ARGUMENT
    PatchActionResult negativeTimeout = service.setBattleInactivityTimeout(-5).join();
    assertFalse(negativeTimeout.isSuccess());
    assertEquals(PatchActionResult.Status.INVALID_ARGUMENT, negativeTimeout.getStatus());

    // Valid battle timeout succeeds
    PatchActionResult validTimeout = service.setBattleInactivityTimeout(180).join();
    assertTrue(validTimeout.isSuccess());
    assertEquals(180, CobblemonPatches.getConfig().getBattleInactivityTimeoutSeconds());

    // Zero or negative autosave stores returns INVALID_ARGUMENT
    PatchActionResult zeroStores = service.setAutosaveStoresPerTick(0).join();
    assertFalse(zeroStores.isSuccess());
    assertEquals(PatchActionResult.Status.INVALID_ARGUMENT, zeroStores.getStatus());

    PatchActionResult negativeStores = service.setAutosaveStoresPerTick(-2).join();
    assertFalse(negativeStores.isSuccess());
    assertEquals(PatchActionResult.Status.INVALID_ARGUMENT, negativeStores.getStatus());

    // Valid autosave stores succeeds
    PatchActionResult validStores = service.setAutosaveStoresPerTick(5).join();
    assertTrue(validStores.isSuccess());
    assertEquals(5, CobblemonPatches.getConfig().getAutosaveStoresPerTick());

    // Debug toggle
    PatchActionResult debugResult = service.setDebug(true).join();
    assertTrue(debugResult.isSuccess());
    assertTrue(CobblemonPatches.getConfig().isDebug());

    // Reload configs
    PatchActionResult reloadResult = service.reloadConfigs().join();
    assertTrue(reloadResult.isSuccess());
  }

  @Test
  void testPatchContextService() {
    PatchContextService contextService = PatchContextService.getInstance();

    // Query invalid patchId
    Optional<PatchStatusView> empty = contextService.getPatchStatus("").join();
    assertTrue(empty.isEmpty());

    Optional<PatchStatusView> nullEmpty = contextService.getPatchStatus(null).join();
    assertTrue(nullEmpty.isEmpty());

    // Query valid patch
    Optional<PatchStatusView> status = contextService.getPatchStatus("pc_box_iterator").join();
    assertTrue(status.isPresent());
    assertEquals("pc_box_iterator", status.get().getId());
    assertEquals(PatchCategory.PERFORMANCE, status.get().getCategory());
    assertTrue(status.get().isEnabled());

    // Query all statuses
    List<PatchStatusView> all = contextService.getAllPatchStatuses().join();
    assertFalse(all.isEmpty());
    assertTrue(all.stream().anyMatch(p -> p.getId().equalsIgnoreCase("snowball_culling")));

    // Query summary
    PatchSummaryView summary = contextService.getSummary().join();
    assertNotNull(summary);
    assertTrue(summary.getTotalPatches() > 0);
    assertTrue(summary.getActivePatches() > 0);
    assertNotNull(summary.getConfig());

    // Query config view
    PatchConfigView configView = contextService.getConfigView().join();
    assertNotNull(configView);
    assertEquals(CobblemonPatches.getConfig().isDebug(), configView.isDebug());
  }

  @Test
  void testCobblemonPatchesAPIFacade() {
    // Synchronous queries
    assertNotNull(CobblemonPatchesAPI.getConfig());
    assertEquals(CobblemonPatches.getConfig().isDebug(), CobblemonPatchesAPI.isDebugEnabled());
    assertTrue(CobblemonPatchesAPI.isPatchEnabled("tm_machine_security"));

    Optional<PatchStatusView> statusSync = CobblemonPatchesAPI.getPatchStatusSync("tm_machine_security");
    assertTrue(statusSync.isPresent());
    assertEquals(PatchCategory.EXPLOIT_FIX, statusSync.get().getCategory());

    List<PatchStatusView> allSync = CobblemonPatchesAPI.getAllPatchStatusesSync();
    assertFalse(allSync.isEmpty());

    PatchSummaryView summarySync = CobblemonPatchesAPI.getSummarySync();
    assertNotNull(summarySync);

    // Asynchronous operations via facade
    PatchActionResult toggleResult = CobblemonPatchesAPI.togglePatch("tm_machine_security", false).join();
    assertTrue(toggleResult.isSuccess());
    assertFalse(CobblemonPatchesAPI.isPatchEnabled("tm_machine_security"));

    PatchActionResult enableResult = CobblemonPatchesAPI.enablePatch("tm_machine_security").join();
    assertTrue(enableResult.isSuccess());
    assertTrue(CobblemonPatchesAPI.isPatchEnabled("tm_machine_security"));

    PatchActionResult disableResult = CobblemonPatchesAPI.disablePatch("tm_machine_security").join();
    assertTrue(disableResult.isSuccess());
    assertFalse(CobblemonPatchesAPI.isPatchEnabled("tm_machine_security"));

    PatchActionResult setDebugResult = CobblemonPatchesAPI.setDebug(false).join();
    assertTrue(setDebugResult.isSuccess());
    assertFalse(CobblemonPatchesAPI.isDebugEnabled());

    PatchActionResult setTimeoutResult = CobblemonPatchesAPI.setBattleInactivityTimeout(60).join();
    assertTrue(setTimeoutResult.isSuccess());
    assertEquals(60, CobblemonPatchesAPI.getConfig().getBattleInactivityTimeoutSeconds());

    PatchActionResult setAutosaveResult = CobblemonPatchesAPI.setAutosaveStoresPerTick(3).join();
    assertTrue(setAutosaveResult.isSuccess());
    assertEquals(3, CobblemonPatchesAPI.getConfig().getAutosaveStoresPerTick());

    PatchActionResult reloadResult = CobblemonPatchesAPI.reloadConfigs().join();
    assertTrue(reloadResult.isSuccess());
  }
}
