package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cat_metalhead.tiny_pickup_animation.ModConfig;
import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    private ItemStack slotStackBefore = ItemStack.EMPTY;
    private int lastCraftingSyncId = -1;

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("HEAD"))
    private void onScreenHandlerSlotUpdateHead(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        // Snapshot the slot's item stack before the packet is applied.
        // Used in the RETURN inject to compare before/after state and determine
        // whether the slot gained items (wasEmpty, countIncreased, itemChanged).
        // Only runs when a HandledScreen is open since ground pickups with no
        // open screen are handled separately via vanilla bobbingAnimationTime.
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null)
            return;

        if (!(client.currentScreen instanceof HandledScreen<?> screen))
            return;
        ;

        int slotId = packet.getSlot();
        ScreenHandler handler = client.player.currentScreenHandler;
        if (slotId < 0 || slotId >= handler.slots.size())
            return;

        slotStackBefore = handler.slots.get(slotId).getStack().copy();
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("RETURN"))
    private void onScreenHandlerSlotUpdateReturn(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null)
            return;

        int syncId = packet.getSyncId();
        int slotId = packet.getSlot();

        // Pick-block where the target item was in the main inventory (not already in
        // hotbar).
        // The client sends pickFromInventory to the server, which moves the item and
        // confirms
        // via this packet. The flag was set in ClientPlayerInteractionManagerMixin when
        // pickFromInventory fired — we can't detect the destination slot any earlier
        // since
        // the client inventory isn't updated until this server confirmation arrives.
        if (PickupTracker.isPickBlockFromInventory() && slotId >= 36 && slotId <= 44) {
            PickupTracker.addHotbarSlot(slotId - 36);
            PickupTracker.setPickBlockFromInventory(null);
        }

        if (!(client.currentScreen instanceof HandledScreen<?> screen))
            return;

        if (syncId != client.player.currentScreenHandler.syncId) {
            return;
        }

        ScreenHandler handler = client.player.currentScreenHandler;
        if (slotId < 0 || slotId >= handler.slots.size()) {
            return;
        }

        Slot slot = handler.slots.get(slotId);
        ItemStack slotStackAfter = slot.getStack();

        boolean wasEmpty = slotStackBefore.isEmpty() && !slotStackAfter.isEmpty();
        boolean countIncreased = !slotStackBefore.isEmpty() && !slotStackAfter.isEmpty()
                && slotStackBefore.getItem() == slotStackAfter.getItem()
                && slotStackAfter.getCount() > slotStackBefore.getCount();
        boolean itemChanged = !slotStackBefore.isEmpty() && !slotStackAfter.isEmpty()
                && slotStackBefore.getItem() != slotStackAfter.getItem();

        boolean isBrewingOutputSlot = handler instanceof BrewingStandScreenHandler
                && slotId >= 0 && slotId <= 2;
        boolean isEnchantingOutputSlot = handler instanceof EnchantmentScreenHandler && slotId == 0;
        boolean isCartographyOutputSlot = handler instanceof CartographyTableScreenHandler && slotId == 2;

        // Route the slot update to the appropriate animation logic based on screen/slot
        // type.
        // Each case handles a different detection strategy:
        // - Cartography: suppressed here, handled by frame diff in
        // HandledScreenMixin.drawSlot
        // - Enchanting: uses !areEqual since enchanting modifies NBT without changing
        // item type
        // - Brewing: uses !areEqual + staggered delay for the cascade effect across 3
        // slots
        // - CraftingResultSlot: suppresses spam-click repetition via
        // lastCraftingOutputItem tracking
        // - FurnaceOutputSlot: wasEmpty only — animates once when first item finishes
        // smelting
        // - Default player inventory: full conditions (wasEmpty, countIncreased,
        // itemChanged)
        // Also registers hotbar key when the updated slot is in the hotbar range
        // (36-44)
        // - Default block container: wasEmpty only — ignores hopper/dispenser top-ups
        if (isCartographyOutputSlot) {
            // System.out.println("cartography table case");
            // handled in drawSlot via frame comparison — suppress default case
        } else if (isEnchantingOutputSlot) {
            // System.out.println("enchanting table case");
            boolean enchantingCompleted = !slotStackBefore.isEmpty() && !slotStackAfter.isEmpty()
                    && !ItemStack.areEqual(slotStackBefore, slotStackAfter);
            if (enchantingCompleted && ModConfig.get().enchantingTableAnimationEnabled) {

                PickupTracker.addSlot(syncId, slotId);
            }
        } else if (isBrewingOutputSlot) {
            // System.out.println("brewing stand case");
            boolean brewingCompleted = !slotStackBefore.isEmpty() && !slotStackAfter.isEmpty()
                    && !ItemStack.areEqual(slotStackBefore, slotStackAfter);
            if (brewingCompleted && ModConfig.get().brewingStandAnimationEnabled) {

                float delay = slotId * ModConfig.get().brewingStandCascadeDelay;
                PickupTracker.addSlotDelayed(syncId, slotId, delay);
            }
        } else if (slot instanceof CraftingResultSlot) {

            if (ModConfig.get().craftingAnimationEnabled) {
                // System.out.println("crafting table case");
                if (wasEmpty || itemChanged) {

                    boolean newScreen = syncId != lastCraftingSyncId;

                    // System.out
                    // .println("lastCraftingOutputItem current value = " +
                    // PickupTracker.getLastCraftingOutputItem());
                    // System.out.println("slotStackAfter item = " + slotStackAfter.getItem());
                    boolean newRecipe = slotStackAfter.getItem() != PickupTracker.getLastCraftingOutputItem();
                    // System.out.println("newScreen " + newScreen);
                    // System.out.println("newRecipe " + newRecipe);

                    if (newScreen || newRecipe) {
                        PickupTracker.addSlot(syncId, slotId);
                    }
                    lastCraftingSyncId = syncId;
                    // System.out.println("lastCraftingOutputItem set to " +
                    // slotStackAfter.getItem().getName());
                    PickupTracker.setLastCraftingOutputItem(slotStackAfter.getItem());
                }
            }
        } else if (slot instanceof FurnaceOutputSlot) {
            // System.out.println("furnace case");
            if (wasEmpty && ModConfig.get().furnaceAnimationEnabled) {

                PickupTracker.addSlot(syncId, slotId);
            }
        } else if (wasEmpty || countIncreased) {
            if (slot.inventory instanceof PlayerInventory) {
                // System.out.println("default case");

                // player inventory slot — full animation logic
                if (!PickupTracker.isSuppressInventoryAnimation() && ModConfig.get().inventoryAnimationEnabled) {
                    PickupTracker.addSlot(syncId, slotId);
                    // if (slotId >= 36 && slotId <= 44) { //PROBABLY REDUNDANT
                    // System.out.println("villager said huh");
                    // PickupTracker.addHotbarSlot(slotId - 36);
                    // } //PROBABLY REDUNDANT
                }
            } else if (wasEmpty) {
                // System.out.println("block container case");

                // block container slot — only animate on empty-->filled
                if (ModConfig.get().containersAnimationEnabled) {
                    PickupTracker.addSlot(syncId, slotId);
                }
            }
        }
    }
}
