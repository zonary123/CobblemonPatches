package org.kingpixel.cobblemonpatches.mixins.cobblemon.storage;

import com.cobblemon.mod.common.datafixer.CobblemonSchemas;
import com.google.gson.JsonObject;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.kingpixel.cobblemonpatches.CobblemonPatches;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Mixin into {@code CobblemonSchemas$CobblemonDataFixerCodec} providing fast-path encoding
 * and decoding, bypassing DataFixerUpper overhead when data is already on the latest schema version.
 *
 * @param <R> the decoded record type
 */
@Mixin(targets = "com.cobblemon.mod.common.datafixer.CobblemonSchemas$CobblemonDataFixerCodec", remap = false)
public abstract class CobblemonDataFixerCodecMixin<R> implements Codec<R> {

  @Shadow @Final private Codec<R> baseCodec;
  @Shadow @Final private TypeReference typeReference;

  /**
   * Fast-path encode avoiding NBT map cloning and DataResult boxing when possible.
   *
   * @param input  the input object to encode
   * @param ops    serialization dynamic ops
   * @param prefix existing prefix element
   * @param <T>    serialized element type
   * @return encoded DataResult
   * @author Zonary123
   * @reason Fast-path encode avoiding NBT map cloning and DataResult boxing when possible.
   */
  @Overwrite
  @Override
  public <T> DataResult<T> encode(R input, DynamicOps<T> ops, T prefix) {
    DataResult<T> result = this.baseCodec.encode(input, ops, prefix);
    return result.flatMap(encoded -> {
      if (encoded instanceof NbtCompound nbtCompound) {
        nbtCompound.putInt(CobblemonSchemas.VERSION_KEY, CobblemonSchemas.DATA_VERSION);
        return DataResult.success(encoded);
      } else if (encoded instanceof JsonObject jsonObject) {
        jsonObject.addProperty(CobblemonSchemas.VERSION_KEY, CobblemonSchemas.DATA_VERSION);
        return DataResult.success(encoded);
      }
      return ops.mergeToMap(
        encoded,
        ops.createString(CobblemonSchemas.VERSION_KEY),
        ops.createInt(CobblemonSchemas.DATA_VERSION)
      );
    });
  }

  /**
   * Fast-path decode bypassing DFU when data is already on current DATA_VERSION.
   *
   * @param ops   serialization dynamic ops
   * @param input serialized input
   * @param <T>   serialized element type
   * @return decoded pair result
   * @author Zonary123
   * @reason Fast-path decode bypassing DFU when data is already on current DATA_VERSION.
   */
  @Overwrite
  @Override
  public <T> DataResult<Pair<R, T>> decode(DynamicOps<T> ops, T input) {
    int inputVersion = 0;
    if (input instanceof NbtCompound nbtCompound) {
      if (nbtCompound.contains(CobblemonSchemas.VERSION_KEY, NbtElement.NUMBER_TYPE)) {
        inputVersion = nbtCompound.getInt(CobblemonSchemas.VERSION_KEY);
      }
    } else if (input instanceof JsonObject jsonObject) {
      if (jsonObject.has(CobblemonSchemas.VERSION_KEY)) {
        inputVersion = jsonObject.get(CobblemonSchemas.VERSION_KEY).getAsInt();
      }
    } else {
      inputVersion = ops.get(input, CobblemonSchemas.VERSION_KEY)
        .flatMap(ops::getNumberValue)
        .map(Number::intValue)
        .result()
        .orElse(0);
    }

    if (inputVersion >= CobblemonSchemas.DATA_VERSION) {
      return this.baseCodec.decode(ops, input);
    }

    if (CobblemonPatches.getConfig().isDebug()) {
      CobblemonPatches.LOGGER.info(
        "Applying Cobblemon DataFixer version update for {}: schema version {} -> {}",
        this.typeReference.typeName(),
        inputVersion,
        CobblemonSchemas.DATA_VERSION
      );
    }

    Dynamic<T> dynamicWithoutVersion = new Dynamic<>(ops, ops.remove(input, CobblemonSchemas.VERSION_KEY));
    Dynamic<T> dataFixedDynamic = CobblemonSchemas.getDATA_FIXER().update(
      this.typeReference,
      dynamicWithoutVersion,
      inputVersion,
      CobblemonSchemas.DATA_VERSION
    );
    return this.baseCodec.decode(dataFixedDynamic);
  }
}
