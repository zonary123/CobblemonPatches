package org.kingpixel.cobblemonpatches;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobblemonPatches implements ModInitializer {
  public static final Logger LOGGER = LoggerFactory.getLogger("Cobblemon Patches");
  public static MinecraftServer server = null;

  @Override
  public void onInitialize() {
    LOGGER.info("""
      🛠️ Cobblemon Patches mod initialized.
      ⚡ All optimizations and patches are now active.
      🔑 Caching for showdownId() is enabled.
      📦 PCBox and PCStore iterator optimizations are active.
      ⚔️ Optimizations for PokemonBattle's isPvN(), isPvP(), and isPvW() are enabled.
      """);
    events();

    
  }

  private void events() {
    ServerLifecycleEvents.SERVER_STARTING.register(server -> CobblemonPatches.server = server);
  }

}
