package org.kingpixel.cobblemonpatches.api.model;

import lombok.Getter;
import org.kingpixel.cobblemonpatches.models.PatchCategory;

/**
 * Immutable view of a specific patch's runtime configuration and status.
 */
@Getter
public final class PatchStatusView {
  private final String id;
  private final String name;
  private final PatchCategory category;
  private final String description;
  private final boolean enabled;
  private final boolean dynamicToggleable;

  public PatchStatusView(String id,
                         String name,
                         PatchCategory category,
                         String description,
                         boolean enabled,
                         boolean dynamicToggleable) {
    this.id = id != null ? id : "";
    this.name = name != null ? name : "";
    this.category = category != null ? category : PatchCategory.STABILITY;
    this.description = description != null ? description : "";
    this.enabled = enabled;
    this.dynamicToggleable = dynamicToggleable;
  }
}
