package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    private ItemStack slotStackBefore = ItemStack.EMPTY;

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("HEAD"))
    private void onScreenHandlerSlotUpdateHead(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null)
            return;
        if (!(client.currentScreen instanceof HandledScreen<?> screen))
            return;

        // System.out.println("check1-head");

        int slotId = packet.getSlot();
        ScreenHandler handler = screen.getScreenHandler();
        if (slotId < 0 || slotId >= handler.slots.size())
            return;

        slotStackBefore = handler.slots.get(slotId).getStack().copy();
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("RETURN"))
    private void onScreenHandlerSlotUpdateReturn(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null)
            return;
        if (!(client.currentScreen instanceof HandledScreen<?> screen))
            return;

        // System.out.println("check1-return");

        int syncId = packet.getSyncId();
        int slotId = packet.getSlot();

        if (syncId != client.player.playerScreenHandler.syncId)
            return;

        ScreenHandler handler = screen.getScreenHandler();
        if (slotId < 0 || slotId >= handler.slots.size())
            return;

        Slot slot = handler.slots.get(slotId);
        ItemStack slotStackAfter = slot.getStack();

        boolean wasEmpty = slotStackBefore.isEmpty() && !slotStackAfter.isEmpty();
        boolean countIncreased = !slotStackBefore.isEmpty() && !slotStackAfter.isEmpty()
                && slotStackBefore.getItem() == slotStackAfter.getItem()
                && slotStackAfter.getCount() > slotStackBefore.getCount();

        // System.out.println("pendingPickup = " + PickupState.pendingPickup);
        // System.out.println("wasEmpty = " + wasEmpty);
        // System.out.println("countIncreased = " + countIncreased);

        // trigger if pickup from world OR if slot genuinely gained items
        if (wasEmpty || countIncreased) {
            PickupTracker.addSlot(slot);
            // System.out.println("piiick");
        }
    }
}
