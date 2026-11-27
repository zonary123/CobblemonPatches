package org.kingpixel.cobblemonpatches.mixins.cobblemon.spawning;

import com.bedrockk.molang.runtime.MoLangRuntime;
import com.bedrockk.molang.runtime.MoParams;
import com.bedrockk.molang.runtime.struct.QueryStruct;
import com.cobblemon.mod.common.api.molang.MoLangFunctions;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.HashMap;
import java.util.function.Function;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Defers MoLangRuntime and QueryStruct initialization on SpawnablePosition until getRuntime()
 * is actually requested. Eliminates thousands of MoLangRuntime, MoLangEnvironment, and HashMap
 * allocations per second for candidate spawn positions that never evaluate MoLang expressions.
 */
@Mixin(value = SpawnablePosition.class, remap = false)
public abstract class SpawnablePositionMixin {

  @Unique
  private static final MoLangRuntime cobblemonPatches$SENTINEL_RUNTIME = new MoLangRuntime();

  @Unique
  private static final QueryStruct cobblemonPatches$SENTINEL_STRUCT = new QueryStruct(new HashMap<>());

  @Unique
  private MoLangRuntime cobblemonPatches$lazyRuntime;

  @Redirect(
      method = "<init>",
      at = @At(
          value = "NEW",
          target = "com/bedrockk/molang/runtime/MoLangRuntime"
      )
  )
  private MoLangRuntime cobblemonPatches$deferRuntimeCreation() {
    return cobblemonPatches$SENTINEL_RUNTIME;
  }

  @WrapOperation(
      method = "<init>",
      at = @At(
          value = "INVOKE",
          target = "Lcom/cobblemon/mod/common/api/molang/MoLangFunctions;setup(Lcom/bedrockk/molang/runtime/MoLangRuntime;)Lcom/bedrockk/molang/runtime/MoLangRuntime;"
      )
  )
  private MoLangRuntime cobblemonPatches$skipSentinelRuntimeSetup(
      MoLangFunctions instance,
      MoLangRuntime runtime,
      Operation<MoLangRuntime> original
  ) {
    if (runtime == cobblemonPatches$SENTINEL_RUNTIME) {
      return runtime;
    }
    return original.call(instance, runtime);
  }

  @WrapOperation(
      method = "<init>",
      at = @At(
          value = "INVOKE",
          target = "Lcom/bedrockk/molang/runtime/struct/QueryStruct;addFunction(Ljava/lang/String;Ljava/util/function/Function;)Lcom/bedrockk/molang/runtime/struct/QueryStruct;"
      )
  )
  private QueryStruct cobblemonPatches$skipSentinelAddFunction(
      QueryStruct instance,
      String name,
      Function<MoParams, Object> function,
      Operation<QueryStruct> original
  ) {
    if (instance == cobblemonPatches$SENTINEL_RUNTIME.getEnvironment().query) {
      return instance;
    }
    return original.call(instance, name, function);
  }

  @Redirect(
      method = "<init>",
      at = @At(
          value = "NEW",
          target = "java/util/HashMap"
      )
  )
  private HashMap<String, Function<MoParams, Object>> cobblemonPatches$skipInitialStructMap() {
    return null;
  }

  @Redirect(
      method = "<init>",
      at = @At(
          value = "NEW",
          target = "com/bedrockk/molang/runtime/struct/QueryStruct"
      )
  )
  private QueryStruct cobblemonPatches$deferStructCreation(HashMap<String, Function<MoParams, Object>> functions) {
    return cobblemonPatches$SENTINEL_STRUCT;
  }

  @Inject(method = "getRuntime", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$getLazyRuntime(CallbackInfoReturnable<MoLangRuntime> cir) {
    MoLangRuntime runtime = this.cobblemonPatches$lazyRuntime;
    if (runtime == null) {
      SpawnablePosition self = (SpawnablePosition) (Object) this;
      runtime = MoLangFunctions.INSTANCE.setup(new MoLangRuntime());
      runtime.getEnvironment().query.addFunction("spawnable_position", params -> self.getOrSetupStruct());
      this.cobblemonPatches$lazyRuntime = runtime;
    }
    cir.setReturnValue(runtime);
  }
}
