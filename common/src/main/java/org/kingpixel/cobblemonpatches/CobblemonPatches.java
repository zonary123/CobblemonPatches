package org.kingpixel.cobblemonpatches;

import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.server.MinecraftServer;
import org.kingpixel.cobblemonpatches.config.ConfigManager;
import org.kingpixel.cobblemonpatches.config.ModConfig;
import org.kingpixel.cobblemonpatches.util.TMObtainMethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entry point for Cobblemon Patches.
 * Initializes server lifecycle listeners and logs active runtime optimizations.
 */
public class CobblemonPatches {
  public static final String MOD_ID = "cobblemonpatches";
  public static final Logger LOGGER = LoggerFactory.getLogger("Cobblemon Patches");
  public static MinecraftServer server = null;

  private CobblemonPatches() {
  }

  public static ModConfig getConfig() {
    return ConfigManager.getConfig();
  }

  /**
   * Initializes the mod during platform startup.
   */
  public static void init() {
    ConfigManager.load();
    if (getConfig().isDebug()) {
      LOGGER.info("🔍 Cobblemon Patches debug logging is ENABLED.");
    }
    LOGGER.info("""
      🛠️ Cobblemon Patches mod initialized.
      ⚡ All optimizations and patches are now active.
      🔑 Caching for showdownId() is enabled.
      📦 PCBox and PCStore iterator optimizations are active.
      ⚔️ Optimizations for PokemonBattle's isPvN(), isPvP(), and isPvW() are enabled.
      Optimization PastureBlocks.
      👤 Asynchronous loading and caching for NPC player textures are enabled.
      """);
    events();
  }

  /**
   * Registers lifecycle event listeners to maintain a global reference to the running MinecraftServer.
   */
  private static void events() {
    LifecycleEvent.SERVER_STARTING.register(minecraftServer -> CobblemonPatches.server = minecraftServer);
    LifecycleEvent.SERVER_STOPPED.register(minecraftServer -> {
      CobblemonPatches.server = null;
      OpsUtil.clear();
      TMObtainMethodUtils.clearIndex();
    });
  }
}
