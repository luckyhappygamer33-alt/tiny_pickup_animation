package net.cat_metalhead.tiny_pickup_animation.mixin;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
    @Shadow
    public DefaultedList<ItemStack> main;

    private final Map<Integer, ItemStack> previousStacks = new HashMap<>();
    private boolean shouldTrack = false;

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"))
    private void onInsertStackHead(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        System.out.println("haed");
        if (!stack.isEmpty()) {
            // String itemName = stack.getItem().getName().getString();
            // System.out.println("[DEBUG] insertStack called with inventory open: " +
            // itemName);

            // PickupTracker.addPickedUp(stack.copy());

            /////
            // System.out.println("////////////////");
            // PlayerInventory self = (PlayerInventory) (Object) this;
            // for (int i = 0; i < self.main.size(); i++) {
            // ItemStack invStack = self.main.get(i);
            // String itemName = invStack.getItem().getName().getString();
            // System.out.println("[DEBUG] i: " + i + ",name: " + itemName);
            // if (!invStack.isEmpty() &&
            // invStack.getItem() == stack.getItem() &&
            // ItemStack.areNbtEqual(invStack, stack)) {

            // PickupTracker.addSlot(i); // Track by slot index
            // System.out.println("[DEBUG] Tracking slot " + i);
            // }
            System.out.println("haed1");

            PlayerInventory self = (PlayerInventory) (Object) this;
            // Save snapshot of current inventory state
            // Make sure this is the CLIENT's player inventory
            // only run on client side
            if (self.player.getWorld().isClient()) {
                System.out.println("haed2");

                previousStacks.clear();
                for (int i = 0; i < self.main.size(); i++) {
                    previousStacks.put(i, self.main.get(i).copy());
                }
                shouldTrack = true;
                System.out.println("[CLIENT] Tracking inventory changes");
            }
        }

        // String itemName = stack.getItem().getName().getString();
        // System.out.println("[DEBUG] insertStack called with: " + itemName);
        // // PickupTracker.cleanup();
        // PickupTracker.addPickedUp(stack.copy());
    }

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("RETURN"))
    private void onInsertStackReturn(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        System.out.println("return1");
        if (shouldTrack && cir.getReturnValue()) {
            shouldTrack = false;
            System.out.println("return2");
            System.out.println("return3");

            // String itemName = stack.getItem().getName().getString();
            // System.out.println("[DEBUG] insertStack called with inventory open: " +
            // itemName);

            // PickupTracker.addPickedUp(stack.copy());

            /////
            // System.out.println("////////////////");
            // PlayerInventory self = (PlayerInventory) (Object) this;
            // for (int i = 0; i < self.main.size(); i++) {
            // ItemStack invStack = self.main.get(i);
            // String itemName = invStack.getItem().getName().getString();
            // System.out.println("[DEBUG] i: " + i + ",name: " + itemName);
            // if (!invStack.isEmpty() &&
            // invStack.getItem() == stack.getItem() &&
            // ItemStack.areNbtEqual(invStack, stack)) {

            // PickupTracker.addSlot(i); // Track by slot index
            // System.out.println("[DEBUG] Tracking slot " + i);
            // }
            // }
            PlayerInventory self = (PlayerInventory) (Object) this;
            if (!self.player.getWorld().isClient())
                return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || !(client.currentScreen instanceof HandledScreen<?> screen)) {
                previousStacks.clear();
                return;
            }

            for (int i = 0; i < self.main.size(); i++) {
                ItemStack before = previousStacks.getOrDefault(i, ItemStack.EMPTY);
                ItemStack after = self.main.get(i);

                // Check if this slot changed (item added, count increased, etc.)
                if (!ItemStack.areEqual(before, after)) {
                    System.out.println("abab1 - Inventory index " + i + " changed");
                    System.out.println("self class: " + self.getClass().getName());
                    System.out.println("self hashcode: " + System.identityHashCode(self));
                    // Find the Slot object that corresponds to this inventory index
                    System.out.println("abab1");
                    for (Slot slot : screen.getScreenHandler().slots) {
                        System.out.println("  Checking slot " + slot.id +
                                ", inventory class: " + slot.inventory.getClass().getName() +
                                ", inventory hashcode: " + System.identityHashCode(slot.inventory) +
                                ", getIndex: " + slot.getIndex());
                        if (slot.inventory == self && slot.getIndex() == i) {
                            System.out.println("abab2");
                            PickupTracker.addSlot(slot);
                            System.out.println(
                                    "[DEBUG] Found and tracking slot " + slot.id + " for inventory index " + i);
                            break;
                        }
                    }
                    // PickupTracker.addSlot(i);
                    // System.out.println("[DEBUG] Slot " + i + " changed! Before: " +
                    // before.getItem().getName().getString() + " x" + before.getCount() +
                    // ", After: " + after.getItem().getName().getString() + " x" +
                    // after.getCount());
                }
            }

            previousStacks.clear();

            // String itemName = stack.getItem().getName().getString();
            // System.out.println("[DEBUG] insertStack called with: " + itemName);
            // // PickupTracker.cleanup();
            // PickupTracker.addPickedUp(stack.copy());
        }
    }
}
