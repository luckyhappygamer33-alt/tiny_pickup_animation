package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.minecraft.client.network.ClientPlayerInteractionManager;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    // pickFromInventory is called when the player middle-clicks a block whose item
    // exists in the main inventory but not directly in the hotbar. It only sends a
    // packet to the server — the client inventory isn't updated immediately, so
    // MinecraftClientMixin.onPickBlock (RETURN) sees no change and can't handle it.
    // Setting this flag here signals ClientPlayNetworkHandlerMixin to treat the
    // next
    // hotbar slot update packet (36-44) as a pick-block confirmation and animate
    // it.
    @Inject(method = "pickFromInventory", at = @At("HEAD"))
    private void onPickFromInventory(int slotIndex, CallbackInfo ci) {
        PickupTracker.setPickBlockFromInventory(slotIndex);
    }
}
