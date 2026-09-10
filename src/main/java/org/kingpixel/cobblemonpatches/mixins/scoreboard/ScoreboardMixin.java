package org.kingpixel.cobblemonpatches.mixins.scoreboard;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents client and server crashes caused by IllegalStateException when
 * removing a score holder that is not registered to the target team.
 */
@Mixin(Scoreboard.class)
public abstract class ScoreboardMixin {

  @Shadow
  @Nullable
  public abstract Team getScoreHolderTeam(String scoreHolderName);

  @Inject(
      method = "removeScoreHolderFromTeam",
      at = @At("HEAD"),
      cancellable = true
  )
  private void cobblemonPatchesGuardRemoveScoreHolderFromTeam(
      String scoreHolderName,
      Team team,
      CallbackInfo ci
  ) {
    if (this.getScoreHolderTeam(scoreHolderName) != team) {
      ci.cancel();
    }
  }
}
