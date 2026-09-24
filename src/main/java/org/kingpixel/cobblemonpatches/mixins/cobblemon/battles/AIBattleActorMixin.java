package org.kingpixel.cobblemonpatches.mixins.cobblemon.battles;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.battles.model.ai.BattleAI;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.PassActionResponse;
import com.cobblemon.mod.common.battles.ShowdownActionRequest;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.exception.IllegalActionChoiceException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mixin into {@link AIBattleActor} fixing the unexecuted Kotlin lambda bug
 * when {@code request == null} or during {@link IllegalActionChoiceException},
 * which previously caused AI/wild Pokémon battles to freeze indefinitely.
 *
 * @author Carlos Varas Alonso
 */
@Mixin(value = AIBattleActor.class, remap = false)
public abstract class AIBattleActorMixin extends BattleActor {

  @Shadow public abstract BattleAI getBattleAI();

  protected AIBattleActorMixin(UUID uuid, List<BattlePokemon> pokemonList) {
    super(uuid, pokemonList);
  }

  /**
   * Called when the AI is requested to make a choice. Fixes the uninvoked fallback block
   * so that pass actions are always set when no request is present, preventing battle softlocks.
   *
   * @author Carlos Varas Alonso
   * @reason Fixes Kotlin uninvoked lambda bug when request == null which freezes battles
   */
  @Overwrite
  public void onChoiceRequested() {
    ShowdownActionRequest req = this.getRequest();
    try {
      if (req != null) {
        List<ActiveBattlePokemon> actives = this.getActivePokemon();
        List<ShowdownActionResponse> choices = req.iterate(actives, (battleMon, moveset, forceSwitch) ->
          getBattleAI().choose(battleMon, this.getBattle(), this.getSide(), moveset, forceSwitch)
        );
        this.setActionResponses(choices);
        for (BattlePokemon pokemon : this.getPokemonList()) {
          pokemon.setWillBeSwitchedIn(false);
        }
      } else {
        fallbackPass();
        Cobblemon.LOGGER.warn("AI requested choice, but no request was set. Returning PassActionResponses.");
      }
    } catch (IllegalActionChoiceException exception) {
      Cobblemon.LOGGER.error("AI was unable to choose an action, we're going to need to pass!", exception);
      if (req != null) {
        List<ActiveBattlePokemon> actives = this.getActivePokemon();
        List<ShowdownActionResponse> choices = req.iterate(actives, (battleMon, moveset, forceSwitch) ->
          PassActionResponse.INSTANCE
        );
        this.setActionResponses(choices);
      } else {
        fallbackPass();
      }
    } catch (Throwable t) {
      Cobblemon.LOGGER.error("Unexpected error in AI choice request, passing turn to prevent softlock", t);
      fallbackPass();
    }
  }

  private void fallbackPass() {
    List<ShowdownActionResponse> response = new ArrayList<>();
    int activeCount = this.getActivePokemon().size();
    for (int i = 0; i < activeCount; i++) {
      response.add(PassActionResponse.INSTANCE);
    }
    this.setActionResponses(response);
  }
}
