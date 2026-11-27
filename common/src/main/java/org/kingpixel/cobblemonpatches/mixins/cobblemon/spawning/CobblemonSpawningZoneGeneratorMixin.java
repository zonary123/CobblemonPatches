package org.kingpixel.cobblemonpatches.mixins.cobblemon.spawning;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.spawning.CobblemonSpawningZoneGenerator;
import com.cobblemon.mod.common.api.spawning.SpawningZone;
import com.cobblemon.mod.common.api.spawning.influence.SpawningZoneInfluence;
import com.cobblemon.mod.common.api.spawning.influence.detector.HabitatBlockDetector;
import com.cobblemon.mod.common.api.spawning.influence.detector.SpawningInfluenceDetector;
import com.cobblemon.mod.common.api.spawning.prospecting.IncenseSweetDetector;
import com.cobblemon.mod.common.api.spawning.prospecting.SaccharineLogSlatheredDetector;
import com.cobblemon.mod.common.api.spawning.spawner.Spawner;
import com.cobblemon.mod.common.api.spawning.spawner.SpawningZoneInput;
import com.cobblemon.mod.common.api.tags.CobblemonBlockTags;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optimizes CobblemonSpawningZoneGenerator.generate() by:
 * 1. Eliminating per-voxel empty ArrayList allocations from Kotlin flatMap over block detectors.
 * 2. Avoiding Kotlin Pair and lambda allocations per (x, z) column via primitive chunk caching.
 * 3. Hoisting sky light layer lookups outside the 3D voxel loop and avoiding double-filling arrays.
 * 4. Reusing immutable BlockData instances for common air/stone/deepslate states and single-pass entity collection.
 */
@Mixin(value = CobblemonSpawningZoneGenerator.class, remap = false)
public abstract class CobblemonSpawningZoneGeneratorMixin {

  @Unique
  private static BlockState cobblemonPatches$airState;

  @Unique
  private static BlockState cobblemonPatches$stoneState;

  @Unique
  private static BlockState cobblemonPatches$deepslateState;

  @Unique
  private static SpawningZone.BlockData[] cobblemonPatches$airBlockData;

  @Unique
  private static SpawningZone.BlockData cobblemonPatches$defaultAirData;

  @Unique
  private static SpawningZone.BlockData cobblemonPatches$darkStoneData;

  @Unique
  private static SpawningZone.BlockData cobblemonPatches$darkDeepslateData;

  @Unique
  private static void cobblemonPatches$ensureBlockDataCache() {
    if (cobblemonPatches$airBlockData != null) {
      return;
    }
    BlockState air = Blocks.AIR.defaultBlockState();
    BlockState stone = Blocks.STONE.defaultBlockState();
    BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
    SpawningZone.BlockData[] table = new SpawningZone.BlockData[256];
    for (int light = 0; light < 16; light++) {
      for (int skyLight = 0; skyLight < 16; skyLight++) {
        table[(light << 4) | skyLight] = new SpawningZone.BlockData(air, light, skyLight);
      }
    }
    cobblemonPatches$airState = air;
    cobblemonPatches$stoneState = stone;
    cobblemonPatches$deepslateState = deepslate;
    cobblemonPatches$defaultAirData = table[0];
    cobblemonPatches$darkStoneData = new SpawningZone.BlockData(stone, 0, 0);
    cobblemonPatches$darkDeepslateData = new SpawningZone.BlockData(deepslate, 0, 0);
    cobblemonPatches$airBlockData = table;
  }

  @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$fastGenerate(
      Spawner spawner,
      SpawningZoneInput input,
      CallbackInfoReturnable<SpawningZone> cir
  ) {
    cobblemonPatches$ensureBlockDataCache();
    ServerLevel world = input.getWorld();
    int baseY = input.getBaseY();
    int height = input.getHeight();
    int minBuildHeight = world.getMinBuildHeight();
    int maxBuildHeight = world.getMaxBuildHeight();

    if (baseY < minBuildHeight) {
      int diff = minBuildHeight - baseY;
      baseY += diff;
      height -= diff;
      if (height < 1) {
        throw new IllegalStateException("World slice was attempted with totally awful base and dimensions");
      }
    }

    if (baseY + height >= maxBuildHeight) {
      int diff = (baseY + height - 1) - maxBuildHeight;
      height -= diff;
      if (height < 1) {
        throw new IllegalStateException("World slice was attempted with totally awful base and dimensions");
      }
    }

    int baseX = input.getBaseX();
    int baseZ = input.getBaseZ();
    int length = input.getLength();
    int width = input.getWidth();

    List<Vec3> nearbyEntityPositions = cobblemonPatches$collectNearbyEntityPositions(
        world, input, baseX, baseY, baseZ, length, height, width
    );

    SpawningZone.BlockData[][][] blocks = new SpawningZone.BlockData[length][height][width];
    Integer[][] skyLevel = new Integer[length][width];
    Integer maxBuildHeightBoxed = maxBuildHeight;

    Set<SpawningInfluenceDetector> allDetectors = SpawningInfluenceDetector.Companion.getDetectors();
    List<SpawningInfluenceDetector> activeBlockDetectors = cobblemonPatches$resolveActiveBlockDetectors(allDetectors);

    List<SpawningZoneInfluence> influences = new ArrayList<>();
    Long2ObjectOpenHashMap<ChunkAccess> chunkCache = new Long2ObjectOpenHashMap<>(16);
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    LayerLightEventListener skyLightLayer = world.getLightEngine().getLayerListener(LightLayer.SKY);

    int topY = baseY + height - 1;
    int lastChunkX = Integer.MIN_VALUE;
    int lastChunkZ = Integer.MIN_VALUE;
    ChunkAccess lastChunk = null;

    for (int lx = 0; lx < length; lx++) {
      int worldX = baseX + lx;
      int chunkX = SectionPos.blockToSectionCoord(worldX);
      SpawningZone.BlockData[][] blocksX = blocks[lx];
      Integer[] skyLevelX = skyLevel[lx];

      for (int lz = 0; lz < width; lz++) {
        int worldZ = baseZ + lz;
        int chunkZ = SectionPos.blockToSectionCoord(worldZ);

        ChunkAccess chunk;
        if (chunkX == lastChunkX && chunkZ == lastChunkZ) {
          chunk = lastChunk;
        } else {
          long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
          if (chunkCache.containsKey(chunkKey)) {
            chunk = chunkCache.get(chunkKey);
          } else {
            chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
            chunkCache.put(chunkKey, chunk);
          }
          lastChunkX = chunkX;
          lastChunkZ = chunkZ;
          lastChunk = chunk;
        }

        if (chunk == null) {
          cobblemonPatches$fillMissingColumn(blocksX, height, lz);
          skyLevelX[lz] = maxBuildHeightBoxed;
          continue;
        }

        int colSkyLevel = cobblemonPatches$populateColumn(
            world,
            chunk,
            skyLightLayer,
            pos,
            blocksX,
            activeBlockDetectors,
            influences,
            worldX,
            worldZ,
            baseY,
            topY,
            lz,
            maxBuildHeight
        );
        skyLevelX[lz] = (colSkyLevel == maxBuildHeight) ? maxBuildHeightBoxed : colSkyLevel;
      }
    }

    for (SpawningInfluenceDetector detector : allDetectors) {
      List<SpawningZoneInfluence> detected = detector.detectFromInput(spawner, input);
      if (!detected.isEmpty()) {
        influences.addAll(detected);
      }
    }

    cir.setReturnValue(new SpawningZone(
        input.getCause(),
        world,
        baseX,
        baseY,
        baseZ,
        blocks,
        skyLevel,
        nearbyEntityPositions,
        influences
    ));
  }

  @Unique
  private static List<Vec3> cobblemonPatches$collectNearbyEntityPositions(
      ServerLevel world,
      SpawningZoneInput input,
      int baseX,
      int baseY,
      int baseZ,
      int length,
      int height,
      int width
  ) {
    double minDist = Cobblemon.INSTANCE.getConfig().getMinimumDistanceBetweenEntities();
    AABB box = AABB.ofSize(
        new Vec3(baseX + length / 2.0, baseY + height / 2.0, baseZ + width / 2.0),
        length + minDist,
        height + minDist,
        width + minDist
    );
    List<Entity> rawEntities = world.getEntities(input.getCause().getEntity(), box);
    List<Vec3> positions = new ArrayList<>(rawEntities.size());
    for (int i = 0; i < rawEntities.size(); i++) {
      Entity entity = rawEntities.get(i);
      if (entity instanceof LivingEntity) {
        positions.add(entity.position());
      }
    }
    return positions;
  }

  @Unique
  private static void cobblemonPatches$fillMissingColumn(
      SpawningZone.BlockData[][] blocksX,
      int height,
      int lz
  ) {
    SpawningZone.BlockData defaultAir = cobblemonPatches$defaultAirData;
    for (int ly = 0; ly < height; ly++) {
      blocksX[ly][lz] = defaultAir;
    }
  }

  @Unique
  private static int cobblemonPatches$populateColumn(
      ServerLevel world,
      ChunkAccess chunk,
      LayerLightEventListener skyLightLayer,
      BlockPos.MutableBlockPos pos,
      SpawningZone.BlockData[][] blocksX,
      List<SpawningInfluenceDetector> activeBlockDetectors,
      List<SpawningZoneInfluence> influences,
      int worldX,
      int worldZ,
      int baseY,
      int topY,
      int lz,
      int maxBuildHeight
  ) {
    pos.set(worldX, topY, worldZ);
    boolean canSeeSky = world.canSeeSky(pos);
    int colSkyLevel = maxBuildHeight;

    for (int y = topY; y >= baseY; y--) {
      pos.set(worldX, y, worldZ);
      int skyLight = skyLightLayer.getLightValue(pos);
      BlockState state = chunk.getBlockState(pos);
      int light = world.getMaxLocalRawBrightness(pos);
      blocksX[y - baseY][lz] = cobblemonPatches$getOrCreateBlockData(state, light, skyLight);

      if (activeBlockDetectors != null) {
        for (int i = 0; i < activeBlockDetectors.size(); i++) {
          List<SpawningZoneInfluence> detected = activeBlockDetectors.get(i).detectFromBlock(world, pos, state);
          if (!detected.isEmpty()) {
            influences.addAll(detected);
          }
        }
      }

      if (canSeeSky) {
        colSkyLevel = y;
        if (state.getFluidState().isEmpty() && !state.is(CobblemonBlockTags.SEES_SKY)) {
          canSeeSky = false;
        }
      }
    }

    return colSkyLevel;
  }

  @Unique
  private static SpawningZone.BlockData cobblemonPatches$getOrCreateBlockData(
      BlockState state,
      int light,
      int skyLight
  ) {
    if (state == cobblemonPatches$airState && (light & ~0xF) == 0 && (skyLight & ~0xF) == 0) {
      return cobblemonPatches$airBlockData[(light << 4) | skyLight];
    }
    if (light == 0 && skyLight == 0) {
      if (state == cobblemonPatches$stoneState) {
        return cobblemonPatches$darkStoneData;
      }
      if (state == cobblemonPatches$deepslateState) {
        return cobblemonPatches$darkDeepslateData;
      }
    }
    return new SpawningZone.BlockData(state, light, skyLight);
  }

  @Unique
  private static List<SpawningInfluenceDetector> cobblemonPatches$resolveActiveBlockDetectors(
      Set<SpawningInfluenceDetector> allDetectors
  ) {
    List<SpawningInfluenceDetector> active = null;
    for (SpawningInfluenceDetector detector : allDetectors) {
      if (!cobblemonPatches$isKnownNoOpBlockDetector(detector)) {
        if (active == null) {
          active = new ArrayList<>(2);
        }
        active.add(detector);
      }
    }
    return active;
  }

  @Unique
  private static boolean cobblemonPatches$isKnownNoOpBlockDetector(SpawningInfluenceDetector detector) {
    return detector instanceof HabitatBlockDetector
        || detector instanceof SaccharineLogSlatheredDetector
        || detector instanceof IncenseSweetDetector;
  }
}
