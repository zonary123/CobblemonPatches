package org.kingpixel.cobblemonpatches.mixins.blockentity;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.PokemonStoreManager;
import com.cobblemon.mod.common.api.storage.pc.ConstantsKt;
import com.cobblemon.mod.common.api.storage.pc.PCBox;
import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.DistributionUtilsKt;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.util.UUID;

@Mixin(targets = "com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity$Tethering")
public class PokemonPastureBlockEntityTetheringMixin {

  @Shadow
  @Final
  private UUID pcId;

  @Shadow
  @Final
  private UUID pokemonId;

  @Unique
  private PCPosition cachedPCPosition;

  /**
   * @author MemencioPerez
   * @reason Cache the PCPosition for faster retrieval
   */
  @SuppressWarnings("resource")
  @Overwrite
  public final @Nullable Pokemon getPokemon() {
    MinecraftServer server = DistributionUtilsKt.server();
    if (server == null) return null;

    PokemonStoreManager storage = Cobblemon.INSTANCE.getStorage();
    PCStore pc = storage.getPC(this.pcId, server.getRegistryManager());
    Pokemon pokemon;

    if (cachedPCPosition != null && (pokemon = pc.get(cachedPCPosition)) != null && pokemon.getUuid().equals(pokemonId)) {
      return pokemon;
    }

    var boxes = pc.getBoxes();
    var maxBoxes = boxes.size();
    PCBox currentBox;
    for (int box = 0; box < maxBoxes; box++) {
      if ((currentBox = boxes.get(box)) == null) continue;
      for (int slot = 0; slot < ConstantsKt.POKEMON_PER_BOX; slot++) {
        if ((pokemon = currentBox.get(slot)) == null || !pokemon.getUuid().equals(pokemonId)) continue;
        cachedPCPosition = new PCPosition(box, slot);
        return pokemon;
      }
    }

    return null;
  }
}
