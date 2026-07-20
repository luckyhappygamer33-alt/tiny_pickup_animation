package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    // pickItemFromBlock is called when the player middle-clicks a block. It only
    // sends a packet to the server — the client inventory and selected slot aren't
    // updated immediately. Setting this flag here signals onUpdateSelectedSlot in
    // ClientPlayNetworkHandlerMixin to treat the next selected slot confirmation
    // from the server as a pick-block and animate it.
    @Inject(method = "handlePickItemFromBlock", at = @At("HEAD"))
    private void onHandlePickItemFromBlock(BlockPos pos, boolean includeData, CallbackInfo ci) {
        PickupTracker.setPickBlockPending(0); // slot doesn't matter, just sets the flag
    }
}