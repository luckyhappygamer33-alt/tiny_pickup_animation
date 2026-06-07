package net.cat_metalhead.tiny_pickup_animation;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;

public class PickupTracker {
    // private static final Map<Slot, Boolean> slotsToAnimate = new HashMap<>();
    private static final Map<Slot, Float> slotsToAnimate = new HashMap<>();

    public static void addSlot(Slot slot) {
        slotsToAnimate.put(slot, 5.0F);
        // printPickedUpItems();
    }

    public static void cleanup() {
        slotsToAnimate.clear();
    }

    public static Map<Slot, Float> getSlotsToAnimate() {
        return slotsToAnimate;
    }

    public static void removeSlot(Slot slot) {
        slotsToAnimate.remove(slot);
    }

    public static float getBobbingAnimationTimeCustom(Slot slot) {
        Float f = slotsToAnimate.get(slot);

        if (f == null)
            return 0.0F;

        f -= Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks();

        if (f <= 0.0F) {
            return 0.0F;
        }

        slotsToAnimate.put(slot, f);
        return f;
    }

    public static void printSlotsToAnimate() {
        System.out.println("=================");
        for (Map.Entry<Slot, Float> entry : slotsToAnimate.entrySet()) {
            System.out.println("Slot: " + entry.getKey() + ", Value: " +
                    entry.getValue());
        }
    }
}