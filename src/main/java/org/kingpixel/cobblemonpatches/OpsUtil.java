package org.kingpixel.cobblemonpatches;

import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryOps;

public class OpsUtil {

  private static DynamicOps<NbtElement> ops;

  public static DynamicOps<NbtElement> getOps() {
    if (ops == null) {
      try {
        DynamicRegistryManager registryManager = CobblemonPatches.server.getRegistryManager();
        ops = RegistryOps.of(NbtOps.INSTANCE, registryManager);
      } catch (Exception e) {
        CobblemonPatches.LOGGER.error("Failed to create RegistryOps", e);
      }
    }
    return ops;
  }
}
