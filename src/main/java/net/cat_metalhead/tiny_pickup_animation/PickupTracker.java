package net.cat_metalhead.tiny_pickup_animation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;

public class PickupTracker {
    // Active animations: maps a slot key to its remaining animation time (starts at
    // 5.0F, counts down per frame)
    private static final Map<SlotKey, Float> slotsToAnimate = new HashMap<>();

    // Slots waiting to animate after a delay (e.g. brewing stand cascade).
    // Countdown in ticks; graduates into slotsToAnimate once it reaches 0.
    private static final Map<SlotKey, Float> pendingSlots = new HashMap<>();

    // Set by ClientPlayerInteractionManagerMixin when pickFromInventory fires.
    // Signals that the next hotbar slot update packet (36-44) belongs to a
    // pick-block
    // from inventory action, since the destination slot isn't known until the
    // server confirms.
    private static Integer pickBlockPending = null;

    // Tracks the last item type that appeared in the crafting output slot.
    // Used to suppress animations when spam-clicking the same recipe output.
    private static Item lastCraftingOutputItem = null; // crafting table

    // Tracks whether the stonecutter output slot currently has an item.
    // Used to suppress repeated animations when cycling through recipes —
    // animation only fires on the first selection (empty --> filled transition).
    private static boolean stonecutterHadOutput = true; // crafting table

    // Set when the player shift-clicks a crafting output slot (QUICK_MOVE).
    // Suppresses animations on the inventory slots that receive the crafted items
    // as a side effect, since only the output slot itself should animate.
    private static Boolean suppressInventoryAnimation = false; // crafting table

    static SlotKey hotbarSlotKey(int slot) {
        return new SlotKey(-1, slot);
    }

    static SlotKey itemStateChangedSlotKey(int slot) {
        return new SlotKey(-2, slot);
    }

    static SlotKey groundPickupSlotKey(int slot) {
        return new SlotKey(-3, slot);
    }

    public static void addSlot(int syncId, int slotId) {
        if (!ModConfig.get().enabled)
            return;
        System.out.println("addSlot syncId=" + syncId + " slotId=" + slotId + " from="
                + Thread.currentThread().getStackTrace()[2]);
        slotsToAnimate.put(new SlotKey(syncId, slotId), ModConfig.get().animationDuration);
        // printPickedUpItems();
    }

    public static void addHotbarSlot(int slotId) {
        if (!ModConfig.get().enabled)
            return;
        slotsToAnimate.put(hotbarSlotKey(slotId), ModConfig.get().animationDuration);
    }

    public static void addItemStateChangedSlot(int slotId) {
        if (!ModConfig.get().enabled)
            return;
        slotsToAnimate.put(itemStateChangedSlotKey(slotId), ModConfig.get().animationDuration);
    }

    public static void addGroundPickupSlot(int slotId) {
        if (!ModConfig.get().enabled)
            return;
        // slotsToAnimate.put(groundPickupSlotKey(slotId),
        // ModConfig.get().animationDuration);
        SlotKey key = groundPickupSlotKey(slotId);
        slotsToAnimate.put(key, ModConfig.get().animationDuration);
    }

    public static void addSlotDelayed(int syncId, int slotId, float delaySeconds) {
        if (!ModConfig.get().enabled)
            return;
        pendingSlots.put(new SlotKey(syncId, slotId), delaySeconds);
    }

    /// ===============///

    public static void tickPending() {
        float delta = MinecraftClient.getInstance().getRenderTickCounter().getFixedDeltaTicks();
        Iterator<Map.Entry<SlotKey, Float>> it = pendingSlots.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<SlotKey, Float> entry = it.next();
            float remaining = entry.getValue() - delta;
            if (remaining <= 0) {
                slotsToAnimate.put(entry.getKey(), ModConfig.get().animationDuration);
                it.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    public static Map<SlotKey, Float> getSlotsToAnimate() {
        return slotsToAnimate;
    }

    public static void removeSlot(SlotKey slotKey) {
        slotsToAnimate.remove(slotKey);
    }

    public static float getBobbingAnimationTimeCustom(SlotKey slotKey) {
        Float f = slotsToAnimate.get(slotKey);

        if (f == null)
            return 0.0F;

        f -= MinecraftClient.getInstance().getRenderTickCounter().getFixedDeltaTicks();

        if (f <= 0.0F) {
            removeSlot(slotKey);
            return 0.0F;
        }

        slotsToAnimate.put(slotKey, f);
        return f;
    }

    public static boolean isPickBlockPending() {
        return pickBlockPending != null;
    }

    public static void setPickBlockPending(Integer slotIndex) {
        PickupTracker.pickBlockPending = slotIndex;
    }

    public static Item getLastCraftingOutputItem() {
        return lastCraftingOutputItem;
    }

    public static void setLastCraftingOutputItem(Item item) {
        lastCraftingOutputItem = item;
    }

    public static void resetLastCraftingOutputItem() {
        lastCraftingOutputItem = null;
    }

    public static boolean isStonecutterHadOutput() {
        return stonecutterHadOutput;
    }

    public static void setStonecutterHadOutput(boolean value) {
        stonecutterHadOutput = value;
    }

    public static void setSuppressInventoryAnimation(Boolean value) {
        PickupTracker.suppressInventoryAnimation = value;
    }

    public static Boolean isSuppressInventoryAnimation() {
        return PickupTracker.suppressInventoryAnimation;
    }

    public static void printSlotsToAnimate() {
        System.out.println("=================");
        for (Map.Entry<SlotKey, Float> entry : slotsToAnimate.entrySet()) {
            System.out.println("Slot: " + entry.getKey() + ", Value: " +
                    entry.getValue());
        }
    }

}
