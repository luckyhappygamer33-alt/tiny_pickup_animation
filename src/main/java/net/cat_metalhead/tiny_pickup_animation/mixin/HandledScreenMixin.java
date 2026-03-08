package net.cat_metalhead.tiny_pickup_animation.mixin;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.lib.apache.commons.ObjectUtils.Null;

import org.spongepowered.asm.mixin.injection.At;

import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.cat_metalhead.tiny_pickup_animation.StackKey;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
    private final Map<Integer, ItemStack> previousStacks = new HashMap<>();

    @Shadow
    protected ScreenHandler handler;

    // @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    // private void onRenderHotbarItem(DrawContext context, int mouseX, int mouseY,
    // float delta, CallbackInfo ci) {
    // if (previousStacks.isEmpty()) {
    // for (int k = 0; k < this.handler.slots.size(); ++k) {
    // Slot slot = this.handler.slots.get(k);
    // ItemStack stack = slot.getStack();
    // previousStacks.put(k, slot.getStack().copy());

    // // if (!stack.isEmpty()) {
    // // System.out.println("Slot " + k + ": " + stack.getName().getString());
    // // } else {
    // // System.out.println("Slot " + k + ": [Empty]");
    // // }
    // }

    // // return; // Skip animation logic on first frame
    // }
    // System.out.println("check1");

    // for (int k = 0; k < this.handler.slots.size(); ++k) {
    // Slot slot = this.handler.slots.get(k);
    // ItemStack current = slot.getStack();
    // ItemStack previous = previousStacks.getOrDefault(k, ItemStack.EMPTY);

    // // if (!ItemStack.areEqual(current, previous) && !current.isEmpty()) {
    // // // New item appeared or changed
    // // if (PickupTracker.shouldAnimate(current)) {
    // // System.out.println("check");
    // // animatedSlots.put(k, delta); // System.currentTimeMillis());

    // // PickupTracker.cleanup();
    // // }
    // // }
    // System.out.println("check2");

    // if (!ItemStack.areEqual(current, previous) && !current.isEmpty()) {
    // // New item appeared or changed
    // if (PickupTracker.getPickedUpItems().get(StackKey.from(current)) != null) {

    // System.out.println("check3");
    // // animatedSlots.put(k, delta); // System.currentTimeMillis());
    // PickupTracker.printPickedUpItems();
    // PickupTracker.cleanup();
    // }
    // }

    // previousStacks.put(k, current.copy());
    // }

    // }

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void onDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        // int slotIndex = handler.slots.indexOf(slot);
        ItemStack stack = slot.getStack();

        if (PickupTracker.getSlotsToAnimate().containsKey(slot)) { // (!stack.isEmpty()
            // &&
            // animatedSlots.containsKey(slotIndex))
            // if (stack.isEmpty()) {
            // System.out.println("this is empty, i:" + slotIndex);
            // return;
            // }

            // System.out.println("check1");
            // PickupTracker.printSlotsToAnimate();

            if (PickupTracker.getSlotsToAnimate().get(slot) == true) {
                stack.setBobbingAnimationTime(5);
                PickupTracker.getSlotsToAnimate().put(slot, false);
            }

            // long startTime = animatedSlots.get(slotIndex);
            // long elapsed = System.currentTimeMillis() - startTime;
            float f = stack.getBobbingAnimationTime() -
                    MinecraftClient.getInstance().getTickDelta();

            // System.out.println("f: " + f);
            // System.out.println("bobbing time:" + stack.getBobbingAnimationTime());

            if (f > 0.0F) {
                float progress = f / 5.0F;
                float scale = 1.0F + 0.25F * (float) Math.sin(progress * Math.PI);

                int x = slot.x;
                int y = slot.y;

                context.getMatrices().push();
                context.getMatrices().translate(x + 8, y + 8, 0.0F);
                context.getMatrices().scale(scale * 1.3F, scale * 1.3F, 1.0F);
                context.getMatrices().translate(-(x + 8), -(y + 8), 0.0F);

                // Draw the animated item
                context.drawItem(stack, slot.x, slot.y);
                context.getMatrices().pop();
                context.drawItemInSlot(MinecraftClient.getInstance().textRenderer, stack, slot.x, slot.y);

                // System.out.println("in");

                ci.cancel();
            } else if (f <= 0.0F) {
                // animatedSlots.remove(slotIndex); // Animation done
                // PickupTracker.cleanup(); // animation done --> clean

                // Animation finished - draw normally one last time
                context.drawItem(stack, slot.x, slot.y);
                context.drawItemInSlot(MinecraftClient.getInstance().textRenderer, stack, slot.x, slot.y);

                PickupTracker.removeSlot(slot);
                // PickupTracker.printSlotsToAnimate();

                // System.out.println("out");
            }

            // Draw the item normally
            // context.drawItem(stack, slot.x, slot.y);
            // if (f > 0.0F) {
            // context.getMatrices().pop();
            // }

            // if (PickupTracker.getPickedUpItems().get(StackKey.from(stack)) != null) {
            // context.getMatrices().pop();

            // }
            // context.drawItemInSlot(MinecraftClient.getInstance().textRenderer, stack,
            // slot.x, slot.y);

            // if (animatedSlots.containsKey(slotIndex)) {
            // context.getMatrices().pop();
            // }

        }
    }

    @Redirect(method = "drawSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawItem(Lnet/minecraft/item/ItemStack;III)V"))
    private void skipDrawItem(DrawContext context, ItemStack stack, int x, int y,
            int seed) {
        int slotIndex = handler.slots.indexOf(stackSlotLookup(stack));
        if (!stack.isEmpty() &&
                PickupTracker.getSlotsToAnimate().get(StackKey.from(stack)) != null) {
            // Skip drawing — handled in @Inject
            return;
        }

        // Fallback: draw normally
        context.drawItem(stack, x, y, seed);
    }

    @Redirect(method = "drawSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V"))
    private void skipDrawItemInSlot(DrawContext context, TextRenderer textRenderer, ItemStack stack, int x, int y,
            @Nullable String countOverride) {
        int slotIndex = handler.slots.indexOf(stackSlotLookup(stack));
        if (!stack.isEmpty() &&
                PickupTracker.getSlotsToAnimate().get(StackKey.from(stack)) != null) {
            // Skip drawing — handled in @Inject
            return;
        }

        // Fallback: draw normally
        context.drawItemInSlot(textRenderer, stack, x, y, countOverride);
    }

    private Slot stackSlotLookup(ItemStack stack) {
        for (Slot slot : handler.slots) {
            if (slot.getStack() == stack) {
                return slot;
            }
        }
        return null;
    }
}
