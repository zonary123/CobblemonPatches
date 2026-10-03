package org.kingpixel.cobblemonpatches.mixins.cobblemon.storage;

import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for {@link PlayerPartyStore} to guard against server crashes during shoulder entity validation
 * when a player's shoulder entity NBT is missing or corrupt.
 */
@Mixin(value = PlayerPartyStore.class, remap = false)
public abstract class PlayerPartyStoreMixin {

  /**
   * Validates that the shoulder entity NBT contains a valid Pokémon compound tag with a valid UUID
   * before Cobblemon queries it. If corrupted or missing, immediately purges the shoulder entity
   * to heal the player's state and prevents a server-crashing NullPointerException.
   *
   * @param player player being evaluated
   * @param isLeft whether checking left or right shoulder
   * @param cir    callback returnable
   */
  @Inject(method = "validateShoulder", at = @At("HEAD"), cancellable = true)
  private void cobblemonpatches$safeValidateShoulder(
      ServerPlayer player,
      boolean isLeft,
      CallbackInfoReturnable<Boolean> cir
  ) {
    CompoundTag shoulderTag = isLeft ? player.getShoulderEntityLeft() : player.getShoulderEntityRight();
    if (shoulderTag == null || shoulderTag.isEmpty()) {
      cir.setReturnValue(true);
      return;
    }

    String id = shoulderTag.getString("id");
    if (!id.equals("cobblemon:pokemon")) {
      return;
    }

    if (!shoulderTag.contains("Pokemon", Tag.TAG_COMPOUND)) {
      purgeShoulder(player, isLeft);
      cir.setReturnValue(true);
      return;
    }

    CompoundTag pokemonTag = shoulderTag.getCompound("Pokemon");
    if (!pokemonTag.hasUUID("UUID")) {
      purgeShoulder(player, isLeft);
      cir.setReturnValue(true);
    }
  }

  private void purgeShoulder(ServerPlayer player, boolean isLeft) {
    if (isLeft) {
      player.setShoulderEntityLeft(new CompoundTag());
    } else {
      player.setShoulderEntityRight(new CompoundTag());
    }
  }
}
