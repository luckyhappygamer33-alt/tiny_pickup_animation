package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cat_metalhead.tiny_pickup_animation.ModConfig;
import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.inventory.CrafterSlot;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.NonInteractiveResultSlot;
import net.minecraft.world.inventory.Slot;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    // slotStackBefore is initialized to ItemStack.EMPTY and is always set before
    // use.
    private ItemStack slotStackBefore = ItemStack.EMPTY;
    private int lastCraftingSyncId = -1;

    @Inject(method = "handleContainerSetSlot", at = @At("HEAD"))
    private void onHandleContainerSetSlotHead(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        // Snapshot the slot's item stack before the packet is applied.
        // Used in the RETURN inject to compare before/after state and determine
        // whether the slot gained items (wasEmpty, countIncreased, itemChanged).
        // Only runs when a HandledScreen is open since ground pickups with no
        // open screen are handled separately via vanilla bobbingAnimationTime.
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null)
            return;

        if (!(client.screen instanceof AbstractContainerScreen<?>))
            return;

        int slotId = packet.getSlot();
        AbstractContainerMenu handler = client.player.containerMenu;
        if (slotId < 0 || slotId >= handler.slots.size())
            return;

        slotStackBefore = handler.slots.get(slotId).getItem().copy();
    }

    @Inject(method = "handleContainerSetSlot", at = @At("RETURN"))
    private void onHandleContainerSetSlotReturn(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null)
            return;
        int syncId = packet.getContainerId();
        int slotId = packet.getSlot();

        if (!(client.screen instanceof AbstractContainerScreen<?>))
            return;
        if (syncId != client.player.containerMenu.containerId) {
            return;
        }
        AbstractContainerMenu handler = client.player.containerMenu;
        if (slotId < 0 || slotId >= handler.slots.size()) {
            return;
        }

        Slot slot = handler.slots.get(slotId);
        ItemStack slotStackAfter = slot.getItem();

        boolean wasEmpty = slotStackBefore.isEmpty() && !slotStackAfter.isEmpty();
        boolean countIncreased = !slotStackBefore.isEmpty() && !slotStackAfter.isEmpty()
                && slotStackBefore.getItem() == slotStackAfter.getItem()
                && slotStackAfter.getCount() > slotStackBefore.getCount();
        boolean itemChanged = !slotStackBefore.isEmpty() && !slotStackAfter.isEmpty()
                && slotStackBefore.getItem() != slotStackAfter.getItem();

        boolean isBrewingOutputSlot = handler instanceof BrewingStandMenu
                && slotId >= 0 && slotId <= 2;
        boolean isEnchantingOutputSlot = handler instanceof EnchantmentMenu && slotId == 0;
        boolean isCartographyOutputSlot = handler instanceof CartographyTableMenu && slotId == 2;
        boolean isStonecutterOutputSlot = handler instanceof StonecutterMenu && slotId == 1;

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
            if (wasEmpty && !PickupTracker.isStonecutterHadOutput() && ModConfig.get().stonecutterAnimationEnabled) {
                PickupTracker.addSlot(syncId, slotId);
            }
            PickupTracker.setStonecutterHadOutput(!slotStackAfter.isEmpty());
        } else if (isCartographyOutputSlot) {
            // System.out.println("cartography table case");
            // Cartography table output is computed client-side without a server packet,
            // so packet detection here would be unreliable. Handled instead via frame-diff
            // in HandledScreenMixin.drawSlot. This empty block exists solely to prevent
            // fallthrough to the default wasEmpty/countIncreased branch below.
        } else if (isEnchantingOutputSlot) {
            // System.out.println("enchanting table case");
            boolean enchantingCompleted = !slotStackBefore.isEmpty() && !slotStackAfter.isEmpty()
                    && !ItemStack.matches(slotStackBefore, slotStackAfter);
            if (enchantingCompleted && ModConfig.get().enchantingTableAnimationEnabled) {

                PickupTracker.addSlot(syncId, slotId);
            }
        } else if (isBrewingOutputSlot) {
            // System.out.println("brewing stand case");
            boolean brewingCompleted = !slotStackBefore.isEmpty() && !slotStackAfter.isEmpty()
                    && !ItemStack.matches(slotStackBefore, slotStackAfter);
            if (brewingCompleted && ModConfig.get().brewingStandAnimationEnabled) {

                float delay = slotId * ModConfig.get().brewingStandCascadeDelay;
                PickupTracker.addSlotDelayed(syncId, slotId, delay);
            }
        } else if (slot instanceof ResultSlot) {

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
        } else if (slot instanceof CrafterSlot || slot instanceof NonInteractiveResultSlot) {
            if (ModConfig.get().crafterAnimationEnabled) {
                // System.out.println("crafter case");

                if (wasEmpty || itemChanged) {
                    PickupTracker.addSlot(syncId, slotId);
                }
            }

        } else if (slot instanceof FurnaceResultSlot) {
            // System.out.println("furnace case");
            if (wasEmpty && ModConfig.get().furnaceAnimationEnabled) {

                PickupTracker.addSlot(syncId, slotId);
            }
        } else if (wasEmpty || countIncreased) {
            if (slot.container instanceof Inventory) {
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
    @Inject(method = "handleSetHeldSlot", at = @At("RETURN"))
    private void onHandleSetHeldSlot(ClientboundSetHeldSlotPacket packet, CallbackInfo ci) {
        if (!PickupTracker.isPickBlockPending())
            return;

        int slot = packet.slot();
        PickupTracker.addHotbarSlot(slot);
        PickupTracker.setPickBlockPending(null);
    }
}