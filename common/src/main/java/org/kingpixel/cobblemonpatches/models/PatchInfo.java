package org.kingpixel.cobblemonpatches.models;

import lombok.Getter;

/**
 * Internal metadata model representing a registered patch.
 */
@Getter
public final class PatchInfo {
  private final String id;
  private final String name;
  private final PatchCategory category;
  private final String description;
  private final boolean defaultEnabled;
  private final boolean dynamicToggleable;

  public PatchInfo(String id,
                   String name,
                   PatchCategory category,
                   String description,
                   boolean defaultEnabled,
                   boolean dynamicToggleable) {
    this.id = id != null ? id : "";
    this.name = name != null ? name : "";
    this.category = category != null ? category : PatchCategory.STABILITY;
    this.description = description != null ? description : "";
    this.defaultEnabled = defaultEnabled;
    this.dynamicToggleable = dynamicToggleable;
  }
}
