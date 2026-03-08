package net.cat_metalhead.tiny_pickup_animation;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public record StackKey(Item item, NbtCompound nbt) {
    public static StackKey from(ItemStack stack) {
        return new StackKey(stack.getItem(), stack.getNbt() == null ? new NbtCompound() : stack.getNbt().copy());
    }
}
