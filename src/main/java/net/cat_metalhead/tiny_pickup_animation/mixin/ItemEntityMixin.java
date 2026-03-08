package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cat_metalhead.tiny_pickup_animation.PickupState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;

import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
    public boolean pickedUp = false;

    // @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    // private void onPickup(PlayerEntity player, CallbackInfo ci) {
    // MinecraftClient client = MinecraftClient.getInstance();
    // if (client == null || client.player == null)
    // return;
    // // if (player != client.player)
    // // return;

    // // This only fires when a player walks over a ground item
    // System.out.println("piiiick");
    // PickupState.pendingPickup = true;
    // }

    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    private void onPickup(PlayerEntity player, CallbackInfo ci) {
        // Only run on client side
        if (!((ItemEntity) (Object) this).getWorld().isClient())
            return;

        PickupState.pendingPickup = true;
    }

    // @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    // private void onPickup(PlayerEntity player, CallbackInfo ci) {
    // if (!((ItemEntity) (Object) this).getWorld().isClient) {
    // ItemStack stack = ((ItemEntity) (Object) this).getStack();
    // Item item = stack.getItem();

    // int countBefore = countItem(player.getInventory(), item);
    // ((ItemEntity) (Object) this).getWorld().getServer().execute(() -> {
    // int countAfter = countItem(player.getInventory(), item);
    // if (countAfter > countBefore) {
    // System.out.println("[DEBUG] Player picked up: " +
    // item.getName().getString());
    // }
    // });

    // // PickupTracker.markPickedUp(stack.copy());
    // }
    // }

}
