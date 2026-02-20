package org.kingpixel.cobblemonpatches;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatchesUtil {

  private static final Logger LOGGER =
    LoggerFactory.getLogger("AsyncCatcherFabric");

  public static void catchOp(String reason) {
    if (CobblemonPatches.server != null && !CobblemonPatches.server.isOnThread()) {
      LOGGER.error(
        "Thread {} failed main thread check: {}",
        Thread.currentThread().getName(),
        reason,
        new Throwable()
      );

      throw new IllegalStateException("Asynchronous " + reason + "!");
    }
  }
}
