package org.kingpixel.cobblemonpatches.mixins.cobblemon.entity;

import com.bedrockk.molang.runtime.value.StringValue;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.entity.npc.NPCPlayerModelType;
import com.cobblemon.mod.common.entity.npc.NPCPlayerTexture;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Mixin(NPCEntity.class)
public abstract class NPCEntityMixin extends PassiveEntity {

  @Shadow(remap = false)
  public abstract Set<String> getAppliedAspects();

  @Shadow(remap = false)
  public abstract void updateAspects();

  @Shadow(remap = false)
  public abstract com.bedrockk.molang.runtime.struct.VariableStruct getData();

  @Unique
  private static final Cache<String, NPCPlayerTexture> cobblemonpatches$PROFILE_TEXTURE_CACHE = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterAccess(1, TimeUnit.DAYS)
    .build();

  @Unique
  private static final Cache<String, byte[]> cobblemonpatches$URI_CACHE = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterAccess(1, TimeUnit.DAYS)
    .build();

  @Unique
  private static final ConcurrentHashMap<String, CompletableFuture<NPCPlayerTexture>> cobblemonpatches$PENDING_LOOKUPS = new ConcurrentHashMap<>();

  protected NPCEntityMixin(EntityType<? extends PassiveEntity> entityType, World world) {
    super(entityType, world);
  }

  @Inject(method = "loadTextureFromGameProfileName", at = @At("HEAD"), cancellable = true, remap = false)
  private void cobblemonpatches$loadTextureFromGameProfileName(String username, CallbackInfo ci) {
    ci.cancel();

    if (username == null || username.isBlank()) {
      return;
    }

    String trimmedName = username.trim();
    String cacheKey = trimmedName.toLowerCase(Locale.ROOT);

    NPCPlayerTexture cached = cobblemonpatches$PROFILE_TEXTURE_CACHE.getIfPresent(cacheKey);
    if (cached != null) {
      cobblemonpatches$applyTexture(cached, trimmedName);
      return;
    }

    MinecraftServer currentServer = this.getServer();
    if (currentServer == null) {
      return;
    }

    cobblemonpatches$PENDING_LOOKUPS.computeIfAbsent(cacheKey, key -> cobblemonpatches$fetchPlayerTextureAsync(currentServer, trimmedName, key))
      .thenAccept(npcTexture -> {
        if (npcTexture != null) {
          currentServer.execute(() -> {
            if (this.isAlive() && !this.isRemoved()) {
              cobblemonpatches$applyTexture(npcTexture, trimmedName);
            }
          });
        }
      });
  }

  @Inject(method = "loadTexture", at = @At("HEAD"), cancellable = true, remap = false)
  private void cobblemonpatches$loadTexture(URI uri, NPCPlayerModelType model, CallbackInfo ci) {
    ci.cancel();

    if (uri == null) {
      return;
    }

    String urlStr = uri.toString();
    byte[] cachedBytes = cobblemonpatches$URI_CACHE.getIfPresent(urlStr);
    if (cachedBytes != null) {
      cobblemonpatches$applyModelAndAspects(cachedBytes, model);
      return;
    }

    MinecraftServer currentServer = this.getServer();
    CompletableFuture.runAsync(() -> {
      byte[] bytes = cobblemonpatches$getOrDownloadUriBytes(uri, urlStr);
      if (bytes != null && currentServer != null) {
        currentServer.execute(() -> {
          if (this.isAlive() && !this.isRemoved()) {
            cobblemonpatches$applyModelAndAspects(bytes, model);
          }
        });
      }
    }, Util.getIoWorkerExecutor());
  }

  @Unique
  private static CompletableFuture<NPCPlayerTexture> cobblemonpatches$fetchPlayerTextureAsync(
    MinecraftServer server,
    String username,
    String cacheKey
  ) {
    CompletableFuture<NPCPlayerTexture> future = new CompletableFuture<>();

    CompletableFuture.runAsync(() -> {
      try {
        Optional<GameProfile> cachedProfile = server.getUserCache() != null
          ? server.getUserCache().findByName(username)
          : Optional.empty();

        if (cachedProfile.isPresent()) {
          cobblemonpatches$resolveProfileTextures(server, cachedProfile.get(), username, cacheKey, future);
        } else {
          server.getGameProfileRepo().findProfilesByNames(new String[]{username}, new ProfileLookupCallback() {
            @Override
            public void onProfileLookupSucceeded(GameProfile profile) {
              cobblemonpatches$resolveProfileTextures(server, profile, username, cacheKey, future);
            }

            @Override
            public void onProfileLookupFailed(String profileName, Exception exception) {
              Cobblemon.LOGGER.error("Unable to load texture for game profile name: {}", username, exception);
              cobblemonpatches$failPendingLookup(cacheKey, future);
            }
          });
        }
      } catch (Exception e) {
        Cobblemon.LOGGER.error("Exception during profile lookup for game profile name: {}", username, e);
        cobblemonpatches$failPendingLookup(cacheKey, future);
      }
    }, Util.getIoWorkerExecutor());

    return future;
  }

  @Unique
  private static void cobblemonpatches$resolveProfileTextures(
    MinecraftServer server,
    GameProfile profile,
    String username,
    String cacheKey,
    CompletableFuture<NPCPlayerTexture> future
  ) {
    try {
      ProfileResult profileResult = server.getSessionService().fetchProfile(profile.getId(), false);
      if (profileResult == null || profileResult.profile() == null) {
        Cobblemon.LOGGER.error("Failed to fetch profile for game profile name: {}", username);
        cobblemonpatches$failPendingLookup(cacheKey, future);
        return;
      }

      MinecraftProfileTextures textures = server.getSessionService().getTextures(profileResult.profile());
      MinecraftProfileTexture skin = textures.skin();
      if (skin == null || skin.getUrl() == null) {
        Cobblemon.LOGGER.warn("No skin texture found for game profile name: {}", username);
        cobblemonpatches$failPendingLookup(cacheKey, future);
        return;
      }

      NPCPlayerModelType model = cobblemonpatches$parseModelType(skin.getMetadata("model"));
      byte[] textureBytes = cobblemonpatches$getOrDownloadUriBytes(new URI(skin.getUrl()), skin.getUrl());

      if (textureBytes != null && textureBytes.length > 0) {
        NPCPlayerTexture npcTexture = new NPCPlayerTexture(textureBytes, model);
        cobblemonpatches$PROFILE_TEXTURE_CACHE.put(cacheKey, npcTexture);
        cobblemonpatches$PENDING_LOOKUPS.remove(cacheKey);
        future.complete(npcTexture);
      } else {
        cobblemonpatches$failPendingLookup(cacheKey, future);
      }
    } catch (Exception e) {
      Cobblemon.LOGGER.error("Error resolving profile textures for game profile name: {}", username, e);
      cobblemonpatches$failPendingLookup(cacheKey, future);
    }
  }

  @Unique
  private static NPCPlayerModelType cobblemonpatches$parseModelType(String modelMetadata) {
    String modelStr = (modelMetadata != null ? modelMetadata : "default").toUpperCase(Locale.ROOT);
    try {
      return NPCPlayerModelType.valueOf(modelStr);
    } catch (IllegalArgumentException ignored) {
      return NPCPlayerModelType.DEFAULT;
    }
  }

  @Unique
  private static byte[] cobblemonpatches$getOrDownloadUriBytes(URI uri, String urlStr) {
    byte[] cachedBytes = cobblemonpatches$URI_CACHE.getIfPresent(urlStr);
    if (cachedBytes != null) {
      return cachedBytes;
    }

    byte[] downloaded = cobblemonpatches$downloadTextureBytes(uri);
    if (downloaded != null && downloaded.length > 0) {
      cobblemonpatches$URI_CACHE.put(urlStr, downloaded);
      return downloaded;
    }
    return null;
  }

  @Unique
  private static void cobblemonpatches$failPendingLookup(String cacheKey, CompletableFuture<NPCPlayerTexture> future) {
    cobblemonpatches$PENDING_LOOKUPS.remove(cacheKey);
    future.complete(null);
  }

  @Unique
  private static byte[] cobblemonpatches$downloadTextureBytes(URI uri) {
    try {
      URLConnection connection = uri.toURL().openConnection();
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      try (InputStream in = connection.getInputStream()) {
        return in.readAllBytes();
      }
    } catch (Exception e) {
      Cobblemon.LOGGER.error("Failed to download texture bytes from URI: {}", uri, e);
      return null;
    }
  }

  @Unique
  private void cobblemonpatches$applyTexture(NPCPlayerTexture playerTexture, String username) {
    cobblemonpatches$applyModelAndAspects(playerTexture.getTexture(), playerTexture.getModel());
    this.getData().setDirectly("player_texture_username", new StringValue(username));
  }

  @Unique
  private void cobblemonpatches$applyModelAndAspects(byte[] bytes, NPCPlayerModelType model) {
    this.getAppliedAspects().remove("model-default");
    this.getAppliedAspects().remove("model-slim");
    this.getAppliedAspects().add("model-" + model.name().toLowerCase(Locale.ROOT));
    this.getDataTracker().set(NPCEntity.Companion.getNPC_PLAYER_TEXTURE(), new NPCPlayerTexture(bytes, model));
    this.updateAspects();
  }
}
