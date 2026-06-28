package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.injection.At;

import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.cat_metalhead.tiny_pickup_animation.SlotKey;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
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
    private final Item[] lastHotbarItems = new Item[9];

    private float lastTickDelta = 0f;

    // @Inject(method = "init", at = @At("HEAD"))
    // private void onInit(CallbackInfo ci) {
    // // Snapshot current hotbar items so frame-diff detection in drawSlot doesn't
    // // animate existing items as "new" the first time the screen renders.
    // // Covers the creative inventory screen where ground pickups bypass packet
    // // detection.
    // for (Slot slot : handler.slots) {
    // if (slot.inventory instanceof PlayerInventory && slot.getIndex() < 9) {
    // lastHotbarItems[slot.getIndex()] = slot.getStack().isEmpty() ? null :
    // slot.getStack().getItem();
    // }
    // }
    // }

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
            if (anvilOutputWasEmpty && !isEmpty) {
                System.out.println("anvil case (client side)");

                PickupTracker.addSlot(handler.syncId, 2);
            }
            anvilOutputWasEmpty = isEmpty;
        } else if (handler instanceof CartographyTableScreenHandler) {
            Slot outputSlot = handler.slots.get(2);
            ItemStack current = outputSlot.getStack();
            if (!current.isEmpty() && !ItemStack.areEqual(current, lastCartographyOutput)) {
                System.out.println("cartography table case (client side)");

                PickupTracker.addSlot(handler.syncId, 2);
            }
            lastCartographyOutput = current.copy();
        } else if (handler instanceof GrindstoneScreenHandler) {
            Slot outputSlot = handler.slots.get(2);
            boolean isEmpty = outputSlot.getStack().isEmpty();
            if (grindstoneOutputWasEmpty && !isEmpty) {
                System.out.println("grindstone case (client side)");

                PickupTracker.addSlot(handler.syncId, 2);
            }
            grindstoneOutputWasEmpty = isEmpty;
        } else if (handler instanceof LoomScreenHandler) {
            Slot outputSlot = handler.slots.get(3);
            boolean isEmpty = outputSlot.getStack().isEmpty();
            if (loomOutputWasEmpty && !isEmpty) {
                System.out.println("loom case (client side)");

                PickupTracker.addSlot(handler.syncId, 3);
            }
            loomOutputWasEmpty = isEmpty;
        } else if (handler instanceof SmithingScreenHandler) {
            Slot outputSlot = handler.slots.get(3);
            boolean isEmpty = outputSlot.getStack().isEmpty();
            if (smithingOutputWasEmpty && !isEmpty) {
                System.out.println("smithing table case (client side)");

                PickupTracker.addSlot(handler.syncId, 3);
            }
            smithingOutputWasEmpty = isEmpty;
        } else if (handler instanceof StonecutterScreenHandler) {
            Slot outputSlot = handler.slots.get(1);
            boolean isEmpty = outputSlot.getStack().isEmpty();
            if (stonecutterOutputWasEmpty && !isEmpty) {
                System.out.println("stonecutter case (client side)");

                PickupTracker.addSlot(handler.syncId, 1);
            }
            stonecutterOutputWasEmpty = isEmpty;
        }

        // Creative inventory renders its hotbar via drawSlot rather than
        // renderHotbarItem,
        // so InGameHudMixin never fires for it. Ground pickups set vanilla
        // bobbingAnimationTime,
        // which we detect here to bridge the gap. Guard with containsKey to prevent
        // resetting
        // the timer to 5.0F on every frame while the animation is already running.
        if (MinecraftClient.getInstance().currentScreen instanceof CreativeInventoryScreen
                && slot.inventory instanceof PlayerInventory && slot.getIndex() < 9) {
            if (stack.getBobbingAnimationTime() > 0 && !PickupTracker.getSlotsToAnimate().containsKey(key)) {
                PickupTracker.addSlot(handler.syncId, handler.slots.indexOf(slot));
            }
        }

        if (PickupTracker.getSlotsToAnimate().containsKey(key)) {
            float f = PickupTracker.getBobbingAnimationTimeCustom(key) - lastTickDelta;

            if (f > 0.0F) {
                float progress = f / 5.0F;
                float scale = 1.0F + 0.25F * (float) Math.sin(progress * Math.PI);

                int x = slot.x;
                int y = slot.y;

                context.getMatrices().push();
                context.getMatrices().translate(x + 8, y + 8, 0.0F);
                context.getMatrices().scale(scale * 1.1F, scale * 1.1F, 1.0F);
                context.getMatrices().translate(-(x + 8), -(y + 8), 0.0F);

                // Draw the animated item
                context.drawItem(stack, slot.x, slot.y);
                context.getMatrices().pop();
                context.drawItemInSlot(MinecraftClient.getInstance().textRenderer, stack, slot.x, slot.y);

                ci.cancel();
            } else if (f <= 0.0F) {

                // Animation finished - draw normally one last time
                context.drawItem(stack, slot.x, slot.y);
                context.drawItemInSlot(MinecraftClient.getInstance().textRenderer, stack, slot.x, slot.y);
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
                System.out.println("lastCraftingOutputItem reset to null");
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
