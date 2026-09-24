package org.kingpixel.cobblemonpatches.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.kingpixel.cobblemonpatches.CobblemonPatches;

/**
 * Handles loading, saving, and managing configuration for Cobblemon Patches.
 */
public class ConfigManager {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("cobblemonpatches.json");
  private static ModConfig config = load();

  private ConfigManager() {
  }

  public static ModConfig getConfig() {
    if (config == null) {
      config = load();
    }
    return config;
  }

  public static ModConfig load() {
    if (Files.exists(CONFIG_PATH)) {
      try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
        ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
        if (loaded != null) {
          config = loaded;
          save();
          return config;
        }
      } catch (Exception e) {
        CobblemonPatches.LOGGER.error("Failed to load cobblemonpatches.json config, falling back to default.", e);
      }
    }

    config = new ModConfig();
    save();
    return config;
  }

  public static void save() {
    try {
      Path parent = CONFIG_PATH.getParent();
      if (parent != null && !Files.exists(parent)) {
        Files.createDirectories(parent);
      }
      try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
        GSON.toJson(config, writer);
      }
    } catch (IOException e) {
      CobblemonPatches.LOGGER.error("Failed to save cobblemonpatches.json config.", e);
    }
  }
}
