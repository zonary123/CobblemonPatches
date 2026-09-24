package org.kingpixel.cobblemonpatches;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.kingpixel.cobblemonpatches.config.ConfigManager;
import org.kingpixel.cobblemonpatches.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Cobblemon Patches Fabric mod.
 * Initializes server lifecycle listeners and logs active runtime optimizations.
 */
public class CobblemonPatches implements ModInitializer {
  public static final Logger LOGGER = LoggerFactory.getLogger("Cobblemon Patches");
  public static MinecraftServer server = null;

  public static ModConfig getConfig() {
    return ConfigManager.getConfig();
  }

  /**
   * Initializes the mod during the Fabric loading phase.
   * Registers server lifecycle events to capture the server instance.
   */
  @Override
  public void onInitialize() {
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
      Optimization PatureBlocks.
      👤 Asynchronous loading and caching for NPC player textures are enabled.
      """);
    events();
  }

  /**
   * Registers lifecycle event listeners to maintain a global reference to the running MinecraftServer.
   */
  private void events() {
    ServerLifecycleEvents.SERVER_STARTING.register(minecraftServer -> CobblemonPatches.server = minecraftServer);
  }
}
