package org.kingpixel.cobblemonpatches.mixins.cobblemon.blockentity;

import com.cobblemon.mod.common.block.multiblock.FossilMultiblockStructure;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import org.kingpixel.cobblemonpatches.OpsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FossilMultiblockStructure.class)
public abstract class FossilMultiblockStructureMixin {

  @Redirect(
    method = "writeToNbt",
    at = @At(
      value = "INVOKE",
      target = "Lcom/mojang/serialization/Codec;encodeStart(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"
    )
  )
  private <T> DataResult<NbtElement> cobblemonpatches$encodeItemStackSafe(
    Codec<T> codec,
    DynamicOps<?> ops,
    Object value
  ) {
    return ItemStack.CODEC.encodeStart(
      OpsUtil.getOps(),
      (ItemStack) value
    );
  }
}

