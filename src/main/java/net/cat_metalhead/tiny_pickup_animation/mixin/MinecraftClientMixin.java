package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.cat_metalhead.tiny_pickup_animation.PickupTracker;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    private int pickSlotBefore;
    private ItemStack pickStackBefore = ItemStack.EMPTY;

    // Snapshot selected slot and its stack before doItemPick runs.
    // doItemPick may switch selectedSlot or fill an empty slot client-side,
    // so we need the pre-action state to detect what actually changed.
    @Inject(method = "doItemPick", at = @At("HEAD"))
    private void onPickBlockHead(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        pickSlotBefore = client.player.getInventory().selectedSlot;
        pickStackBefore = client.player.getInventory().getStack(pickSlotBefore).copy();
    }

    // After doItemPick, compare against the snapshot to detect what changed.
    // Covers two cases:
    // - Item already in hotbar: selectedSlot switches to it (slotChanged)
    // - Empty hotbar slot gets item in creative: stack fills (stackChanged)
    // The third case — item in main inventory jumping to hotbar — is NOT detectable
    // here since the client inventory hasn't updated yet. That case is handled by
    // ClientPlayerInteractionManagerMixin + ClientPlayNetworkHandlerMixin via a
    // flag.
    @Inject(method = "doItemPick", at = @At("RETURN"))
    private void onPickBlockReturn(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        int pickSlotAfter = client.player.getInventory().selectedSlot;
        ItemStack pickStackAfter = client.player.getInventory().getStack(pickSlotAfter);

        boolean slotChanged = pickSlotAfter != pickSlotBefore;
        boolean stackChanged = !ItemStack.areEqual(pickStackBefore, pickStackAfter);

        if (slotChanged || stackChanged) {
            // System.out.println("PICK BLOCK slot = " + slotAfter);
            PickupTracker.addHotbarSlot(pickSlotAfter);
        }
    }
}
