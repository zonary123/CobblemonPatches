package org.kingpixel.cobblemonpatches.mixins.cobblemon.blockentity;

import org.kingpixel.cobblemonpatches.OpsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.cobblemon.mod.common.block.multiblock.FossilMultiblockStructure;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;

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
    // FIXING BUG: This new encoder is causing server desync issues. Strip enchantment components in-place.
    ItemStack stack = (ItemStack) value;

    // Remove NBT enchantments from the item stored in the machine
    stack.remove(DataComponentTypes.ENCHANTMENTS);
    stack.remove(DataComponentTypes.STORED_ENCHANTMENTS);

    return ItemStack.CODEC.encodeStart(
      OpsUtil.getOps(),
      stack
    );
  }
}

