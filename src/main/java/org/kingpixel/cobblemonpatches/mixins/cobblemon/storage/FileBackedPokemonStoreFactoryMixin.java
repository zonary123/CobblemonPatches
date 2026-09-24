package org.kingpixel.cobblemonpatches.mixins.cobblemon.storage;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.api.storage.factory.FileBackedPokemonStoreFactory;
import com.cobblemon.mod.common.platform.events.ServerTickEvent;
import kotlin.Unit;
import net.minecraft.registry.DynamicRegistryManager;
import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Mixin into {@link FileBackedPokemonStoreFactory} implementing tick-spread autosave routines
 * for dirty Pokémon stores to mitigate server tick stalls.
 */
@Mixin(value = FileBackedPokemonStoreFactory.class, remap = false)
public abstract class FileBackedPokemonStoreFactoryMixin {

  @Shadow private Set<PokemonStore<?>> dirtyStores;
  @Shadow private int passedTicks;

  @Shadow public abstract void save(PokemonStore<?> store, DynamicRegistryManager registryAccess);

  @Unique private static final int MAX_SAVES_PER_TICK = 1;

  /**
   * Spreads dirty Pokemon store saving incrementally across game ticks rather than in a single synchronous burst.
   *
   * @param factory  the store factory instance
   * @param it       tick event payload containing the server
   * @param cir      callback returnable
   * @author Zonary123
   * @reason Implements Paper-style tick-spreading autosave to eliminate tick freeze spikes.
   */
  @Inject(method = "saveSubscription$lambda$0", at = @At("HEAD"), cancellable = true)
  private static void cobblemonpatches$tickSpreadingAutosave(
    FileBackedPokemonStoreFactory<?> factory,
    ServerTickEvent.Pre it,
    CallbackInfoReturnable<Unit> cir
  ) {
    FileBackedPokemonStoreFactoryMixin self = (FileBackedPokemonStoreFactoryMixin) (Object) factory;
    int saveIntervalSeconds = Cobblemon.INSTANCE.getConfig().getPokemonSaveIntervalSeconds();
    if (saveIntervalSeconds <= 0) {
      cir.setReturnValue(Unit.INSTANCE);
      return;
    }

    self.passedTicks++;
    int targetTicks = 20 * saveIntervalSeconds;

    if (self.passedTicks >= targetTicks) {
      if (!self.dirtyStores.isEmpty()) {
        DynamicRegistryManager registryManager = it.getServer().getRegistryManager();
        for (int i = 0; i < MAX_SAVES_PER_TICK && !self.dirtyStores.isEmpty(); i++) {
          PokemonStore<?> store = self.dirtyStores.iterator().next();
          if (CobblemonPatches.getConfig().isDebug()) {
            CobblemonPatches.LOGGER.info("Autosaving dirty PokemonStore {} (remaining: {})", store.getUuid(), self.dirtyStores.size() - 1);
          }
          self.save(store, registryManager);
        }
      }

      if (self.dirtyStores.isEmpty()) {
        self.passedTicks = 0;
      }
    }

    cir.setReturnValue(Unit.INSTANCE);
  }
}
