package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.injection.At;

import net.cat_metalhead.tiny_pickup_animation.ModConfig;
import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.cat_metalhead.tiny_pickup_animation.SlotKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Shadow
    protected AbstractContainerMenu menu;

    private boolean anvilOutputWasEmpty = true;
    private ItemStack lastCartographyOutput = ItemStack.EMPTY;
    private boolean grindstoneOutputWasEmpty = true;
    private boolean loomOutputWasEmpty = true;
    private boolean smithingOutputWasEmpty = true;

    private float lastTickDelta = 0f;

    @Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
    private void onExtractSlot(GuiGraphicsExtractor context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null)
            return;

        ItemStack stack = slot.getItem();

        int x = slot.x;
        int y = slot.y;

        int syncId = client.player.containerMenu.containerId;

        SlotKey key = new SlotKey(syncId, menu.slots.indexOf(slot));

        // These screens compute their output client-side without sending a server
        // packet,
        // so packet detection in ClientPlayNetworkHandlerMixin can't see the change.
        // Instead, track the output slot state frame-by-frame and register the
        // animation
        // the first time the slot transitions from empty to non-empty (or content
        // changes).
        if (menu instanceof AnvilMenu) {
            Slot outputSlot = menu.slots.get(2);
            boolean isEmpty = outputSlot.getItem().isEmpty();
            if (anvilOutputWasEmpty && !isEmpty && ModConfig.get().anvilAnimationEnabled) {
                // System.out.println("anvil case (client side)");

                PickupTracker.addSlot(menu.containerId, 2);
            }
            anvilOutputWasEmpty = isEmpty;
        } else if (menu instanceof CartographyTableMenu) {
            Slot outputSlot = menu.slots.get(2);
            ItemStack current = outputSlot.getItem();
            if (!current.isEmpty() && !ItemStack.matches(current, lastCartographyOutput) && ModConfig
                    .get().cartographyTableAnimationEnabled) {
                // System.out.println("cartography table case (client side)");

                PickupTracker.addSlot(menu.containerId, 2);
            }
            lastCartographyOutput = current.copy();
        } else if (menu instanceof GrindstoneMenu) {

            Slot outputSlot = menu.slots.get(2);
            boolean isEmpty = outputSlot.getItem().isEmpty();
            if (grindstoneOutputWasEmpty && !isEmpty && ModConfig.get().grindstoneAnimationEnabled) {
                // System.out.println("grindstone case (client side)");

                PickupTracker.addSlot(menu.containerId, 2);
            }
            grindstoneOutputWasEmpty = isEmpty;
        } else if (menu instanceof LoomMenu) {
            Slot outputSlot = menu.slots.get(3);
            boolean isEmpty = outputSlot.getItem().isEmpty();
            if (loomOutputWasEmpty && !isEmpty && ModConfig.get().loomAnimationEnabled) {
                // System.out.println("loom case (client side)");

                PickupTracker.addSlot(menu.containerId, 3);
            }
            loomOutputWasEmpty = isEmpty;
        } else if (menu instanceof SmithingMenu) {
            Slot outputSlot = menu.slots.get(3);
            boolean isEmpty = outputSlot.getItem().isEmpty();
            if (smithingOutputWasEmpty && !isEmpty && ModConfig.get().smithingTableAnimationEnabled) {
                // System.out.println("smithing table case (client side)");

                PickupTracker.addSlot(menu.containerId, 3);
            }
            smithingOutputWasEmpty = isEmpty;
        }

        // Creative inventory hotbar slots (45-54) are not caught by the packet handler
        // detection in ClientPlayNetworkHandlerMixin when in creative mode — either due
        // to slot index mismatch or the creative screen handler behaving differently.
        // Instead we detect ground pickups here via vanilla bobbingAnimationTime > 0.
        // containsKey guard prevents resetting the timer on every frame while
        // animating.
        if (Minecraft.getInstance().gui.screen() instanceof CreativeModeInventoryScreen
                && slot.container instanceof Inventory && slot.index > 44) {

            if (stack.getPopTime() > 0 && !PickupTracker.getSlotsToAnimate().containsKey(key)
                    && ModConfig.get().inventoryAnimationEnabled) {
                PickupTracker.addSlot(menu.containerId, menu.slots.indexOf(slot));
            }
        }

        if (PickupTracker.getSlotsToAnimate().containsKey(key)) {
            float f = PickupTracker.getBobbingAnimationTimeCustom(key) - lastTickDelta;
            if (f > 0.0F) {
                float progress = f / ModConfig.get().animationDuration;
                // float scale = 1.0F + ModConfig.get().bounceScale * (float) Math.sin(progress
                // * Math.PI); // 0.25F is
                // bounce scale

                float scale = 1.0F + ModConfig.get().bounceScale * (float) Math.pow(progress, 2.0F);

                context.pose().pushMatrix();
                context.pose().translate((float) (x + 8), (float) (y + 8));
                context.pose().scale(scale, scale);
                context.pose().translate((float) (-(x + 8)), (float) (-(y + 8)));

                context.item(stack, x, y);
                context.pose().popMatrix();
                context.itemDecorations(Minecraft.getInstance().font, stack, x, y);

                ci.cancel();
            } else if (f <= 0.0F) {

                // Animation finished - draw normally one last time
                context.item(stack, slot.x, slot.y);
                context.itemDecorations(Minecraft.getInstance().font, stack, slot.x, slot.y);
            }
        }
    }

    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("HEAD"))
    private void onSlotClick(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
        // Clear suppression from previous click — all packets from that action have
        // been processed by now
        PickupTracker.setSuppressInventoryAnimation(false);

        if (slot instanceof ResultSlot && actionType == ContainerInput.QUICK_MOVE) {
            // Primary-clicking crafting output sends inventory slot update packets as a
            // side
            // effect.
            // Suppress those so only the output slot itself animates, not the destination
            // inventory slot.
            PickupTracker.setSuppressInventoryAnimation(true);
        } else if (slot != null && slot.container instanceof CraftingContainer) {
            // Player clicked a crafting ingredient slot. If the output is currently empty,
            // they are starting a fresh recipe — reset tracking so the next output fill
            // animates.
            Slot outputSlot = menu.slots.stream()
                    .filter(s -> s instanceof ResultSlot)
                    .findFirst().orElse(null);
            if (outputSlot != null && outputSlot.getItem().isEmpty()) {
                PickupTracker.resetLastCraftingOutputItem();
            }

        }

        // Player clicked the stonecutter input slot — they may be changing the input
        // item,
        // so reset the flag to allow animation on the next recipe selection.
        if (slot != null && menu instanceof StonecutterMenu
                && menu.slots.indexOf(slot) == 0) {
            PickupTracker.setStonecutterHadOutput(false);
        }
    }

    @Inject(method = "extractRenderState", at = @At("HEAD")) // tick method to tick pending animations
    private void onExtractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta,
            CallbackInfo ci) {
        // Capture tickDelta for sub-tick interpolation used in drawSlot animation
        // smoothing
        lastTickDelta = delta;
        // Tick delayed animations (e.g. brewing stand cascade) and graduate them into
        // slotsToAnimate
        PickupTracker.tickPending();
    }
}