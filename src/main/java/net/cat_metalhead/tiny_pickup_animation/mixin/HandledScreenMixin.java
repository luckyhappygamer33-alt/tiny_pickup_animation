package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.injection.At;

import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    @Shadow
    protected ScreenHandler handler;

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void onDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        ItemStack stack = slot.getStack();

        if (PickupTracker.getSlotsToAnimate().containsKey(slot)) {
            // if (PickupTracker.getSlotsToAnimate().get(slot) > 0.0F) {
            // stack.setBobbingAnimationTime(5);
            // PickupTracker.getSlotsToAnimate().put(slot, false);
            // }

            float f = PickupTracker.getBobbingAnimationTimeCustom(slot);

            // System.out.println("f: " + f);
            // System.out.println("bobbing time:" + stack.getBobbingAnimationTime());

            if (f > 0.0F) {
                float progress = f / 5.0F;
                float scale = 1.0F + 0.25F * (float) Math.sin(progress * Math.PI);

                int x = slot.x;
                int y = slot.y;

                context.getMatrices().pushMatrix();
                context.getMatrices().translate(x + 8, y + 8);
                context.getMatrices().scale(scale * 1.1F, scale * 1.1F);
                context.getMatrices().translate(-(x + 8), -(y + 8));

                // Draw the animated item
                context.drawItem(stack, slot.x, slot.y);
                context.getMatrices().popMatrix();
                context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, stack, slot.x, slot.y);

                // System.out.println("drawwing");
                ci.cancel();
            } else if (f <= 0.0F) {

                // Animation finished - draw normally one last time
                context.drawItem(stack, slot.x, slot.y);
                context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, stack, slot.x, slot.y);

                PickupTracker.removeSlot(slot);
                // PickupTracker.printSlotsToAnimate();

                // System.out.println("drawwing_end");
            }
        }
    }
}