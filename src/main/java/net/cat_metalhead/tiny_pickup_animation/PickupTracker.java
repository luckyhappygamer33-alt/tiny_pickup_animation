package net.cat_metalhead.tiny_pickup_animation;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class PickupTracker {
    private static final Map<Slot, Boolean> slotsToAnimate = new HashMap<>();

    public static void addSlot(Slot slot) {
        slotsToAnimate.put(slot, true);

        // printPickedUpItems();
    }

    // public static boolean shouldAnimate(ItemStack stack) {
    // Long time = pickedUpItems.get(StackKey.from(stack));
    // if (time == null)
    // return false;

    // long elapsed = System.currentTimeMillis() - time;
    // return elapsed < 500; // Animate for 500ms
    // }

    public static void cleanup() {
        // long now = System.currentTimeMillis();
        // pickedUpItems.entrySet().removeIf(entry -> now - entry.getValue() > 500);
        slotsToAnimate.clear();
    }

    public static Map<Slot, Boolean> getSlotsToAnimate() {
        return slotsToAnimate;
    }

    public static void removeSlot(Slot slot) {
        slotsToAnimate.remove(slot);
    }

    public static void printSlotsToAnimate() {
        System.out.println("=================");
        for (Map.Entry<Slot, Boolean> entry : slotsToAnimate.entrySet()) {
            System.out.println("Slot: " + entry.getKey() + ", Value: " +
                    entry.getValue());
        }
    }
}
