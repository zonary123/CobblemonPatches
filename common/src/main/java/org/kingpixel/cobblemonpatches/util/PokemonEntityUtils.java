package org.kingpixel.cobblemonpatches.util;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.kingpixel.cobblemonpatches.CobblemonPatches;

import java.util.UUID;

/**
 * Utility methods for Pokémon entity lifecycle, ownership validation, and state evaluation.
 *
 * @author Carlos Varas Alonso
 */
public final class PokemonEntityUtils {

  private PokemonEntityUtils() {}

  /**
   * Evaluates whether a Pokémon entity is an orphaned player-owned Pokémon whose owner
   * has disconnected, switched servers, or is removed.
   *
   * @param entity the Pokémon entity to check
   * @return {@code true} if the entity belongs to an offline/disconnected player; {@code false} otherwise
   */
  public static boolean isOrphanedPlayerPokemon(PokemonEntity entity) {
    if (entity == null || entity.isRemoved() || entity.getTethering() != null) {
      return false;
    }

    Pokemon pokemon = entity.getPokemon();
    if (pokemon == null || !pokemon.isPlayerOwned()) {
      return false;
    }

    UUID ownerUuid = entity.getOwnerUUID();
    if (ownerUuid == null) {
      return false;
    }

    MinecraftServer server = CobblemonPatches.server != null ? CobblemonPatches.server : entity.getServer();
    if (server == null || !server.isRunning()) {
      return false;
    }

    ServerPlayer player = server.getPlayerList().getPlayer(ownerUuid);
    return player == null || player.hasDisconnected() || player.isRemoved();
  }
}
