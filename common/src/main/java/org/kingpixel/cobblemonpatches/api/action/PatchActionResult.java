package org.kingpixel.cobblemonpatches.api.action;

import lombok.Getter;

/**
 * Immutable result representation for CobblemonPatches headless actions.
 */
@Getter
public final class PatchActionResult {

  public enum Status {
    SUCCESS,
    PATCH_NOT_FOUND,
    INVALID_ARGUMENT,
    ALREADY_IN_STATE,
    NOT_TOGGLEABLE,
    DISABLED,
    CONFIG_ERROR,
    NO_PERMISSION,
    ERROR
  }

  private final boolean success;
  private final Status status;
  private final String message;
  private final String patchId;
  private final boolean enabled;

  private PatchActionResult(boolean success,
                            Status status,
                            String message,
                            String patchId,
                            boolean enabled) {
    this.success = success;
    this.status = status != null ? status : Status.ERROR;
    this.message = message != null ? message : "";
    this.patchId = patchId != null ? patchId : "";
    this.enabled = enabled;
  }

  public static PatchActionResult success(String message, String patchId, boolean enabled) {
    return new PatchActionResult(true, Status.SUCCESS, message, patchId, enabled);
  }

  public static PatchActionResult success(String message) {
    return new PatchActionResult(true, Status.SUCCESS, message, "", false);
  }

  public static PatchActionResult failure(Status status, String message) {
    return new PatchActionResult(false, status, message, "", false);
  }

  public static PatchActionResult failure(Status status, String message, String patchId, boolean enabled) {
    return new PatchActionResult(false, status, message, patchId, enabled);
  }

  public static PatchActionResult patchNotFound(String message, String patchId) {
    return failure(Status.PATCH_NOT_FOUND, message, patchId, false);
  }

  public static PatchActionResult invalidArgument(String message) {
    return failure(Status.INVALID_ARGUMENT, message);
  }

  public static PatchActionResult alreadyInState(String message, String patchId, boolean enabled) {
    return failure(Status.ALREADY_IN_STATE, message, patchId, enabled);
  }

  public static PatchActionResult notToggleable(String message, String patchId) {
    return failure(Status.NOT_TOGGLEABLE, message, patchId, true);
  }

  public static PatchActionResult configError(String message) {
    return failure(Status.CONFIG_ERROR, message);
  }

  public static PatchActionResult error(String message) {
    return failure(Status.ERROR, message);
  }
}
