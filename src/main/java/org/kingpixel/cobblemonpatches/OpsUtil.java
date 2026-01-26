package org.kingpixel.cobblemonpatches;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryOps;

public class OpsUtil {

  private static RegistryOps<JsonElement> ops;

  public static RegistryOps<JsonElement> getOps() {
    if (ops == null) {
      try {
        DynamicRegistryManager registryManager = CobblemonPatches.server.getRegistryManager();
        ops = RegistryOps.of(JsonOps.INSTANCE, registryManager);
      } catch (Exception e) {
        CobblemonPatches.LOGGER.error("Failed to create RegistryOps for FossilMultiblockStructureMixin redirect", e);
        e.printStackTrace();
      }
    }

    return ops;
  }
}
