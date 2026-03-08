package net.cat_metalhead.tiny_pickup_animation.mixin;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cat_metalhead.tiny_pickup_animation.PickupState;
import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    private final Map<Integer, ItemStack> previousStacks = new HashMap<>();
    private ItemStack slotStackBefore = ItemStack.EMPTY;

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("HEAD"))
    private void onScreenHandlerSlotUpdateHead(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null)
            return;
        if (!(client.currentScreen instanceof HandledScreen<?> screen))
            return;

        System.out.println("check1-head");

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

        System.out.println("check1-return");

        int syncId = packet.getSyncId();
        int slotId = packet.getSlot();

        if (syncId != client.player.playerScreenHandler.syncId)
            return;

        ScreenHandler handler = screen.getScreenHandler();
        if (slotId < 0 || slotId >= handler.slots.size())
            return;

        Slot slot = handler.slots.get(slotId);
        ItemStack after = slot.getStack();

        boolean wasEmpty = slotStackBefore.isEmpty() && !after.isEmpty();
        boolean countIncreased = !slotStackBefore.isEmpty() && !after.isEmpty()
                && slotStackBefore.getItem() == after.getItem()
                && after.getCount() > slotStackBefore.getCount();

        System.out.println("pendingPickup = " + PickupState.pendingPickup);
        System.out.println("wasEmpty = " + wasEmpty);
        System.out.println("countIncreased = " + countIncreased);

        // trigger if pickup from world OR if slot genuinely gained items
        if (wasEmpty || countIncreased) {
            PickupTracker.addSlot(slot);
            System.out.println("piiick");
            PickupState.pendingPickup = false;
        }
    }

    @Inject(method = "onInventory", at = @At("HEAD"))
    private void onInventoryHead(InventoryS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null || client.player == null)
            return;
        if (!(client.currentScreen instanceof HandledScreen<?>))
            return;

        System.out.println("check2");

        // snapshot before
        previousStacks.clear();
        PlayerInventory inv = client.player.getInventory();
        for (int i = 0; i < inv.main.size(); i++) {
            previousStacks.put(i, inv.main.get(i).copy());
        }
        PickupState.pendingPickup = true;

        System.out.println("heas");

        // /give sends a full inventory sync, not a slot update
        // PickupState.pendingPickup = true;
        // // schedule reset on next tick so slot updates have time to consume it
        // MinecraftClient.getInstance().execute(() -> PickupState.pendingPickup =
        // false);
    }

    @Inject(method = "onInventory", at = @At("RETURN"))
    private void onInventoryReturn(InventoryS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null)
            return;
        if (!(client.currentScreen instanceof HandledScreen<?> screen))
            return;
        if (!PickupState.pendingPickup)
            return;

        PickupState.pendingPickup = false;
        System.out.println("returns");

        PlayerInventory inv = client.player.getInventory();
        ScreenHandler handler = screen.getScreenHandler();

        for (int i = 0; i < inv.main.size(); i++) {
            ItemStack before = previousStacks.getOrDefault(i, ItemStack.EMPTY);
            ItemStack after = inv.main.get(i);

            boolean wasEmpty = before.isEmpty() && !after.isEmpty();
            boolean countIncreased = !before.isEmpty() && !after.isEmpty() &&
                    before.getItem() == after.getItem() &&
                    after.getCount() > before.getCount();

            if (wasEmpty || countIncreased) {
                // find the matching slot in the screen handler
                for (Slot slot : handler.slots) {
                    if (slot.inventory == inv && slot.getIndex() == i) {
                        PickupTracker.addSlot(slot);
                        break;
                    }
                }
            }
        }

        previousStacks.clear();
    }

    // @Inject(method = "onScreenHandlerSlotUpdate", at = @At("RETURN"))
    // private void onSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet,
    // CallbackInfo ci) {
    // MinecraftClient client = MinecraftClient.getInstance();
    // if (client == null || client.player == null)
    // return;
    // if (!(client.currentScreen instanceof HandledScreen<?> screen))
    // return;

    // int syncId = packet.getSyncId();
    // int slotId = packet.getSlot();

    // if (syncId != client.player.playerScreenHandler.syncId)
    // return;

    // ScreenHandler handler = screen.getScreenHandler();
    // if (slotId < 0 || slotId >= handler.slots.size())
    // return;

    // Slot slot = handler.slots.get(slotId);

    // // This fires on the client after the slot has been updated
    // // Only set if not already pending, prevents repeated firing
    // if (!PickupState.pendingPickup) {
    // PickupState.pendingPickup = true;
    // System.out.println("slot id:" + slotId);
    // PickupTracker.addSlot(slot);
    // }
    // }
}
