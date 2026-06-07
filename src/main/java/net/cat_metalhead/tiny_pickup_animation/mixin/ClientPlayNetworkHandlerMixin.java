package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
    private ItemStack slotStackBefore = ItemStack.EMPTY;

    @Inject(method = "handleContainerSetSlot", at = @At("HEAD"))
    private void onScreenHandlerSlotUpdateHead(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null)
            return;
        if (!(client.screen instanceof AbstractContainerScreen<?> screen))
            return;

        // System.out.println("check1-head");

        int slotId = packet.getSlot();
        AbstractContainerMenu handler = screen.getMenu();
        if (slotId < 0 || slotId >= handler.slots.size())
            return;

        slotStackBefore = handler.slots.get(slotId).getItem().copy();
    }

    @Inject(method = "handleContainerSetSlot", at = @At("RETURN"))
    private void onScreenHandlerSlotUpdateReturn(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null)
            return;
        if (!(client.screen instanceof AbstractContainerScreen<?> screen))
            return;

        // System.out.println("check1-return");

        int syncId = packet.getContainerId();
        int slotId = packet.getSlot();

        if (syncId != client.player.inventoryMenu.containerId)
            return;

        AbstractContainerMenu handler = screen.getMenu();
        if (slotId < 0 || slotId >= handler.slots.size())
            return;

        Slot slot = handler.slots.get(slotId);
        ItemStack slotStackAfter = slot.getItem();

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