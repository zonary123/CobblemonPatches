package org.kingpixel.cobblemonpatches;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for asserting thread safety and logging async access violations.
 */
public class PatchesUtil {

  public static final Logger LOGGER =
    LoggerFactory.getLogger("AsyncCatcherFabric");

  private PatchesUtil() {
  }

  /**
   * Checks whether the current operation is executing on the Minecraft main server thread.
   * Logs a stack trace if invoked from an off-thread worker.
   *
   * @param reason the descriptive context or operation identifier being checked
   */
  public static void catchOp(String reason) {
    if (CobblemonPatches.server != null && !CobblemonPatches.server.isOnThread()) {
      LOGGER.error(
        "Thread {} failed main thread check: {}",
        Thread.currentThread().getName(),
        reason,
        new Throwable()
      );
    }
  }
}
