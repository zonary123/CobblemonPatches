package org.kingpixel.cobblemonpatches.mixins.cobblemon.blockentity;

import com.cobblemon.mod.common.block.multiblock.FossilMultiblockStructure;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.kingpixel.cobblemonpatches.CobblemonPatches;
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
    if (!(value instanceof ItemStack stack)) {
      CobblemonPatches.LOGGER.warn("FossilMultiblockStructure received non-ItemStack value during writeToNbt: {}", value);
      return DataResult.success(new NbtCompound());
    }


    if (stack.isEmpty() || stack.getCount() <= 0) {
      CobblemonPatches.LOGGER.warn("Skipping invalid fossil machine stack during writeToNbt (item={}, count={})", stack.getItem(), stack.getCount());
      return DataResult.success(new NbtCompound());
    }

    stack.remove(DataComponentTypes.ENCHANTMENTS);
    stack.remove(DataComponentTypes.STORED_ENCHANTMENTS);

    return ItemStack.CODEC.encodeStart(
      OpsUtil.getOps(),
      stack
    );
  }
}
