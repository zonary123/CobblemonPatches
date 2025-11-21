package org.kingpixel.cobblemonpatches;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CobblemonPatches implements ModInitializer {
  public static final Logger LOGGER = Logger.getLogger("Cobblemon Patches");
  public static MinecraftServer server = null;

  static {
    ConsoleHandler consoleHandler = new ConsoleHandler();
    consoleHandler.setLevel(Level.INFO);
    LOGGER.addHandler(consoleHandler);
    consoleHandler.setFormatter(new Formatter() {
      @Override
      public String format(java.util.logging.LogRecord record) {
        return record.getMessage() + "\n";
      }
    });
  }

  @Override
  public void onInitialize() {
    LOGGER.info("🛠️ Cobblemon Patches mod initialized.");
    LOGGER.info("⚡ All optimizations and patches are now active.");
    LOGGER.info("🔑 Caching for showdownId() is enabled.");
    LOGGER.info("📦 PCBox and PCStore iterator optimizations are active.");
    LOGGER.info("⚔️ Optimizations for PokemonBattle's isPvN(), isPvP(), and isPvW() are enabled.");
    events();
  }

  private void events() {
    ServerLifecycleEvents.SERVER_STARTING.register(server -> CobblemonPatches.server = server);
  }

}
