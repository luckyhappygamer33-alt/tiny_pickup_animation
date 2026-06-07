package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.injection.At;

import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Mixin(AbstractContainerScreen.class)
public class HandledScreenMixin {

    @Shadow
    protected AbstractContainerMenu menu;

    @Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
    private void onDrawSlot(GuiGraphicsExtractor context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        ItemStack stack = slot.getItem();

        int x = slot.x;
        int y = slot.y;

        if (PickupTracker.getSlotsToAnimate().containsKey(slot)) {
            float f = PickupTracker.getBobbingAnimationTimeCustom(slot);

            if (f > 0.0F) {
                float progress = f / 5.0F;
                float scale = 1.0F + 0.25F * (float) Math.sin(progress * Math.PI);

                context.pose().pushMatrix();
                context.pose().translate(x + 8, y + 8);
                context.pose().scale(scale * 1.1F, scale * 1.1F);
                context.pose().translate(-(x + 8), -(y + 8));

                // Použi player verziu drawItem!
                context.item(stack, x, y);
                context.pose().popMatrix();
                context.itemDecorations(Minecraft.getInstance().font, stack, x, y);

                ci.cancel();
            } else if (f <= 0.0F) {
                context.item(stack, x, y);
                context.itemDecorations(Minecraft.getInstance().font, stack, x, y);
                PickupTracker.removeSlot(slot);
            }
        }
    }
}