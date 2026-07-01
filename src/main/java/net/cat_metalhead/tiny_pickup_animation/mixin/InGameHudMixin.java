package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import net.cat_metalhead.tiny_pickup_animation.ModConfig;
import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.cat_metalhead.tiny_pickup_animation.SlotKey;
import net.cat_metalhead.tiny_pickup_animation.ModConfig.AnimationMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Mixin(InGameHud.class)
public class InGameHudMixin {

	private final Item[] lastHotbarItems = new Item[10];
	private final boolean[] wasGroundPickupActive = new boolean[10];

	@Inject(method = "renderHotbarItem", at = @At("HEAD"), cancellable = true)
	private void onRenderHotbarItem(DrawContext context, int x, int y, float tickDelta, PlayerEntity player,
			ItemStack stack, int seed, CallbackInfo ci) {

		if (!ModConfig.get().enabled)
			return;

		AnimationMode hotbarMode = ModConfig.get().hotbarAnimationMode;
		AnimationMode pickBlockMode = ModConfig.get().pickBlockAnimationMode;
		AnimationMode itemStateChangedMode = ModConfig.get().itemStateChangedAnimationMode;

		//// item changed state like fill bucket into water bucket, fill bottles into
		//// water bottles
		int slotIndex = seed - 1;
		// System.out.println("slotIndex: " + slotIndex);
		if (slotIndex < 0 || slotIndex > 9) {
			return; // not a regular hotbar slot — skip our logic entirely
		}

		Item currentItem = stack.isEmpty() ? null : stack.getItem();
		Item lastItem = lastHotbarItems[slotIndex];

		if (currentItem != lastItem) {
			if (currentItem != null && lastItem != null) {
				// item type changed — animate
				PickupTracker.addItemStateChangedSlot(slotIndex);
			}
			lastHotbarItems[slotIndex] = currentItem;
		}
		////
		///

		boolean vanillaAnimating = stack.getBobbingAnimationTime() > 0;
		if (vanillaAnimating && !wasGroundPickupActive[slotIndex]) {
			// Ground pickup just started — register our own timer instead of using
			// vanilla's
			PickupTracker.addGroundPickupSlot(slotIndex); // new method, new key e.g. SlotKey(-3, slot)
		}
		wasGroundPickupActive[slotIndex] = vanillaAnimating;

		if (!stack.isEmpty()) {
			SlotKey groundKey = new SlotKey(-3, slotIndex);
			float f = PickupTracker.getBobbingAnimationTimeCustom(groundKey) - tickDelta;
			// float f = stack.getBobbingAnimationTime() - tickDelta;
			boolean isPickBlock = false;
			boolean isItemStateChanged = false;

			if (f <= 0.0F) {
				// pick-block!!!
				// Vanilla isn't animating — check our custom tracker (e.g. pick-block)
				SlotKey key = new SlotKey(-1, seed - 1);
				float customF = PickupTracker.getBobbingAnimationTimeCustom(key) - tickDelta;
				if (customF > 0.0F) {
					f = customF;
					isPickBlock = true;
				}

				SlotKey itemStateKey = new SlotKey(-2, seed - 1);
				float itemStateF = PickupTracker.getBobbingAnimationTimeCustom(itemStateKey) - tickDelta;
				if (itemStateF > 0.0F) {
					f = itemStateF;
					isItemStateChanged = true;
				}
			}

			AnimationMode mode = hotbarMode;
			if (isPickBlock) {
				mode = pickBlockMode;
			} else if (isItemStateChanged) {
				mode = itemStateChangedMode;
			}

			// AnimationMode mode = isPickBlock ? pickBlockMode : hotbarMode;

			if (mode == AnimationMode.VANILLA) { // VANILLA MODE
				// let vanilla handle everything
				if (!isPickBlock && !isItemStateChanged)
					return;

				if (f > 0.0F) {
					float h = 1.0F + f / 5.0F;
					context.getMatrices().push();
					context.getMatrices().translate((float) (x + 8), (float) (y + 12), 0.0F);
					context.getMatrices().scale(1.0F / h, (h + 1.0F) / 2.0F, 1.0F);
					context.getMatrices().translate((float) (-(x + 8)), (float) (-(y + 12)), 0.0F);
				}

				context.drawItem(player, stack, x, y, seed);
				if (f > 0.0F) {
					context.getMatrices().pop();
				}

				context.drawItemInSlot(MinecraftClient.getInstance().textRenderer, stack, x, y);

				ci.cancel();
				return;
			} else if (mode == AnimationMode.CUSTOM) { // CUSTOM (MOD) MODE
				// draw item but suppress all animation
				if (f > 0.0F) {
					float progress = f / ModConfig.get().animationDuration;
					float scale = 1.0F + ModConfig.get().bounceScale * (float) Math.sin(progress * Math.PI); // 0.25F is
																												// bounce
																												// scale

					context.getMatrices().push();
					context.getMatrices().translate((float) (x + 8), (float) (y + 12), 0.0F);
					context.getMatrices().scale(scale * 1.1F, scale * 1.1F, 1.0F);
					context.getMatrices().translate((float) (-(x + 8)), (float) (-(y + 12)), 0.0F);
				}

				context.drawItem(player, stack, x, y, seed);
				if (f > 0.0F) {
					context.getMatrices().pop();
				}

				context.drawItemInSlot(MinecraftClient.getInstance().textRenderer, stack, x, y);

				ci.cancel();
				return;
			}

			// DISABLED MODE
			context.drawItem(player, stack, x, y, seed);
			context.drawItemInSlot(MinecraftClient.getInstance().textRenderer, stack, x, y);
			ci.cancel();

		}
	}
}
