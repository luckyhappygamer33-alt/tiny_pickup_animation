package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cat_metalhead.tiny_pickup_animation.ModConfig;
import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.screen.slot.CrafterOutputSlot;
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

        if (!(client.currentScreen instanceof HandledScreen<?>))
            return;

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

        if (!(client.currentScreen instanceof HandledScreen<?>))
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
        boolean isStonecutterOutputSlot = handler instanceof StonecutterScreenHandler && slotId == 1;

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
        // - Default block container: wasEmpty only — ignores hopper/dispenser top-ups
        if (isStonecutterOutputSlot) {
            // System.out.println("stonec cutter case");
            // same as cartography table case
        } else if (isCartographyOutputSlot) {
            // System.out.println("cartography table case");
            // Cartography table output is computed client-side without a server packet,
            // so packet detection here would be unreliable. Handled instead via frame-diff
            // in HandledScreenMixin.drawSlot. This empty block exists solely to prevent
            // fallthrough to the default wasEmpty/countIncreased branch below.
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

                    boolean newRecipe = slotStackAfter.getItem() != PickupTracker.getLastCraftingOutputItem();

                    if (newScreen || newRecipe) {
                        PickupTracker.addSlot(syncId, slotId);
                    }
                    lastCraftingSyncId = syncId;
                    PickupTracker.setLastCraftingOutputItem(slotStackAfter.getItem());
                }
            }
        } else if (slot instanceof CrafterOutputSlot) {
            if (ModConfig.get().crafterAnimationEnabled) {
                // System.out.println("crafter case");

                if (wasEmpty || itemChanged) {
                    PickupTracker.addSlot(syncId, slotId);
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

    // Server confirms the new selected hotbar slot after a pick-block action via
    // this packet. If the pick-block flag is set, register the animation and clear
    // the flag. The slot value is already a direct hotbar index (0-8), no
    // conversion needed unlike the old ScreenHandlerSlotUpdateS2CPacket approach.
    @Inject(method = "onUpdateSelectedSlot", at = @At("RETURN"))
    private void onUpdateSelectedSlot(UpdateSelectedSlotS2CPacket packet, CallbackInfo ci) {
        if (!PickupTracker.isPickBlockPending())
            return;

        int slot = packet.slot();
        PickupTracker.addHotbarSlot(slot);
        PickupTracker.setPickBlockPending(null);
    }
}
