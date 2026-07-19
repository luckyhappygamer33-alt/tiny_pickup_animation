package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.injection.At;

import net.cat_metalhead.tiny_pickup_animation.ModConfig;
import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.cat_metalhead.tiny_pickup_animation.SlotKey;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    @Shadow
    protected ScreenHandler handler;

    private boolean anvilOutputWasEmpty = true;
    private ItemStack lastCartographyOutput = ItemStack.EMPTY;
    private boolean grindstoneOutputWasEmpty = true;
    private boolean loomOutputWasEmpty = true;
    private boolean smithingOutputWasEmpty = true;
    private boolean stonecutterOutputWasEmpty = true;

    private float lastTickDelta = 0f;

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void onDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        ItemStack stack = slot.getStack();

        int syncId = MinecraftClient.getInstance().player.currentScreenHandler.syncId;

        SlotKey key = new SlotKey(syncId, handler.slots.indexOf(slot));

        // These screens compute their output client-side without sending a server
        // packet,
        // so packet detection in ClientPlayNetworkHandlerMixin can't see the change.
        // Instead, track the output slot state frame-by-frame and register the
        // animation
        // the first time the slot transitions from empty to non-empty (or content
        // changes).
        if (handler instanceof AnvilScreenHandler) {
            Slot outputSlot = handler.slots.get(2);
            boolean isEmpty = outputSlot.getStack().isEmpty();
            if (anvilOutputWasEmpty && !isEmpty && ModConfig.get().anvilAnimationEnabled) {
                // System.out.println("anvil case (client side)");

                PickupTracker.addSlot(handler.syncId, 2);
            }
            anvilOutputWasEmpty = isEmpty;
        } else if (handler instanceof CartographyTableScreenHandler) {
            Slot outputSlot = handler.slots.get(2);
            ItemStack current = outputSlot.getStack();
            if (!current.isEmpty() && !ItemStack.areEqual(current, lastCartographyOutput) && ModConfig
                    .get().cartographyTableAnimationEnabled) {
                // System.out.println("cartography table case (client side)");

                PickupTracker.addSlot(handler.syncId, 2);
            }
            lastCartographyOutput = current.copy();
        } else if (handler instanceof GrindstoneScreenHandler) {

            Slot outputSlot = handler.slots.get(2);
            boolean isEmpty = outputSlot.getStack().isEmpty();
            if (grindstoneOutputWasEmpty && !isEmpty && ModConfig.get().grindstoneAnimationEnabled) {
                // System.out.println("grindstone case (client side)");

                PickupTracker.addSlot(handler.syncId, 2);
            }
            grindstoneOutputWasEmpty = isEmpty;
        } else if (handler instanceof LoomScreenHandler) {
            Slot outputSlot = handler.slots.get(3);
            boolean isEmpty = outputSlot.getStack().isEmpty();
            if (loomOutputWasEmpty && !isEmpty && ModConfig.get().loomAnimationEnabled) {
                // System.out.println("loom case (client side)");

                PickupTracker.addSlot(handler.syncId, 3);
            }
            loomOutputWasEmpty = isEmpty;
        } else if (handler instanceof SmithingScreenHandler) {
            Slot outputSlot = handler.slots.get(3);
            boolean isEmpty = outputSlot.getStack().isEmpty();
            if (smithingOutputWasEmpty && !isEmpty && ModConfig.get().smithingTableAnimationEnabled) {
                // System.out.println("smithing table case (client side)");

                PickupTracker.addSlot(handler.syncId, 3);
            }
            smithingOutputWasEmpty = isEmpty;
        } else if (handler instanceof StonecutterScreenHandler) {
            Slot outputSlot = handler.slots.get(1);
            boolean isEmpty = outputSlot.getStack().isEmpty();
            if (stonecutterOutputWasEmpty && !isEmpty && ModConfig.get().stonecutterAnimationEnabled) {
                // System.out.println("stonecutter case (client side)");

                PickupTracker.addSlot(handler.syncId, 1);
            }
            stonecutterOutputWasEmpty = isEmpty;
        }

        // Creative inventory hotbar slots (0-8) are not caught by the packet handler
        // detection in ClientPlayNetworkHandlerMixin when in creative mode — either due
        // to slot index mismatch or the creative screen handler behaving differently.
        // Instead we detect ground pickups here via vanilla bobbingAnimationTime > 0.
        // containsKey guard prevents resetting the timer on every frame while
        // animating.
        if (MinecraftClient.getInstance().currentScreen instanceof CreativeInventoryScreen
                && slot.inventory instanceof PlayerInventory && slot.getIndex() < 9) {

            if (stack.getBobbingAnimationTime() > 0 && !PickupTracker.getSlotsToAnimate().containsKey(key)
                    && ModConfig.get().inventoryAnimationEnabled) {
                PickupTracker.addSlot(handler.syncId, handler.slots.indexOf(slot));
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

                int x = slot.x;
                int y = slot.y;

                context.getMatrices().pushMatrix();
                context.getMatrices().translate((float) (x + 8), (float) (y + 8));
                context.getMatrices().scale(scale, scale);
                context.getMatrices().translate((float) (-(x + 8)), (float) (-(y + 8)));

                // Draw the animated item
                context.drawItem(stack, slot.x, slot.y);
                context.getMatrices().popMatrix();
                context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, stack, slot.x, slot.y);

                ci.cancel();
            } else if (f <= 0.0F) {

                // Animation finished - draw normally one last time
                context.drawItem(stack, slot.x, slot.y);
                context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, stack, slot.x, slot.y);
            }
        }
    }

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"))
    private void onSlotClickHead(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        // Clear suppression from previous click — all packets from that action have
        // been processed by now
        PickupTracker.setSuppressInventoryAnimation(false);

        if (slot instanceof CraftingResultSlot && actionType == SlotActionType.QUICK_MOVE) {
            // Shift-clicking crafting output sends inventory slot update packets as a side
            // effect.
            // Suppress those so only the output slot itself animates, not the destination
            // inventory slot.
            PickupTracker.setSuppressInventoryAnimation(true);
        } else if (slot != null && slot.inventory instanceof CraftingInventory) {
            // Player clicked a crafting ingredient slot. If the output is currently empty,
            // they are starting a fresh recipe — reset tracking so the next output fill
            // animates.
            Slot outputSlot = handler.slots.stream()
                    .filter(s -> s instanceof CraftingResultSlot)
                    .findFirst().orElse(null);
            if (outputSlot != null && outputSlot.getStack().isEmpty()) {
                PickupTracker.resetLastCraftingOutputItem();
            }

        }
    }

    @Inject(method = "render", at = @At("HEAD")) // tick method to tick pending animations
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Capture tickDelta for sub-tick interpolation used in drawSlot animation
        // smoothing
        lastTickDelta = delta;
        // Tick delayed animations (e.g. brewing stand cascade) and graduate them into
        // slotsToAnimate
        PickupTracker.tickPending();
    }
}
