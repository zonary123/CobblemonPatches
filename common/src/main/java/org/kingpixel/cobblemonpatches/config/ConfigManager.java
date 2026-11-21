package org.kingpixel.cobblemonpatches.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import org.kingpixel.cobblemonpatches.CobblemonPatches;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Handles loading, saving, and managing configuration for Cobblemon Patches.
 */
public class ConfigManager {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  private static Path configPath = null;
  private static ModConfig config = null;

  private ConfigManager() {
  }

  public static synchronized Path getConfigPath() {
    if (configPath == null) {
      try {
        configPath = Platform.getConfigFolder().resolve("cobblemonpatches.json");
      } catch (Throwable ignored) {
        configPath = Path.of("config", "cobblemonpatches.json");
      }
    }
    return configPath;
  }

  public static synchronized void setConfigPathForTesting(Path path) {
    configPath = path;
  }

  public static synchronized void setConfigForTesting(ModConfig testConfig) {
    config = testConfig;
  }

  public static synchronized ModConfig getConfig() {
    if (config == null) {
      config = load();
    }
    return config;
  }

  public static synchronized ModConfig load() {
    extractWiki();
    Path path = getConfigPath();
    if (Files.exists(path)) {
      try (BufferedReader reader = Files.newBufferedReader(path)) {
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

  public static synchronized ModConfig reload() {
    config = null;
    return getConfig();
  }

  public static void extractWiki() {
    try {
      Path wikiDir;
      try {
        wikiDir = Platform.getConfigFolder().resolve("cobblemonpatches");
      } catch (Throwable ignored) {
        wikiDir = Path.of("config", "cobblemonpatches");
      }
      if (!Files.exists(wikiDir)) {
        Files.createDirectories(wikiDir);
      }
      Path wikiFile = wikiDir.resolve("WIKI.md");
      try (var in = CobblemonPatches.class.getClassLoader().getResourceAsStream("WIKI.md")) {
        if (in != null) {
          Files.copy(in, wikiFile, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    } catch (Exception e) {
      CobblemonPatches.LOGGER.error("Failed to extract WIKI.md", e);
    }
  }

  public static synchronized void save() {
    try {
      Path path = getConfigPath();
      Path parent = path.getParent();
      if (parent != null && !Files.exists(parent)) {
        Files.createDirectories(parent);
      }
      try (BufferedWriter writer = Files.newBufferedWriter(path)) {
        GSON.toJson(getConfig(), writer);
      }
    } catch (IOException e) {
      CobblemonPatches.LOGGER.error("Failed to save cobblemonpatches.json config.", e);
    }
  }
}
