package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import net.cat_metalhead.tiny_pickup_animation.ModConfig;
import net.cat_metalhead.tiny_pickup_animation.PickupTracker;
import net.cat_metalhead.tiny_pickup_animation.SlotKey;
import net.cat_metalhead.tiny_pickup_animation.ModConfig.AnimationMode;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@Mixin(Gui.class)
public class GuiMixin {

	private final Item[] prevFrameItems = new Item[10];
	private final Item[] currentFrameItems = new Item[10];
	private final float[] prevBobbingTime = new float[10];

	@Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
	private void onExtractSlot(GuiGraphicsExtractor context, int x, int y, DeltaTracker deltaTracker,
			Player player,
			ItemStack stack, int seed, CallbackInfo ci) {

		if (!ModConfig.get().enabled)
			return;

		AnimationMode hotbarMode = ModConfig.get().hotbarAnimationMode;
		AnimationMode pickBlockMode = ModConfig.get().pickBlockAnimationMode;
		AnimationMode itemStateChangedMode = ModConfig.get().itemStateChangedAnimationMode;

		int slotIndex = seed - 1;
		if (slotIndex < 0 || slotIndex > 9) {
			return; // not a regular hotbar slot — skip our logic entirely
		}

		// Detects two hotbar-slot scenarios that don't produce a bobbingAnimationTime
		// jump:
		// 1. Swap — two slots exchange items simultaneously (e.g. hotbar - offhand).
		// Identified by finding another slot where prevFrameItems[i] == curr AND
		// that slot also changed this frame. Registered as ground pickup so hotbarMode
		// applies.
		// 2. Item-state-change — item type changed in place with no corresponding swap
		// (e.g. empty bucket -> water bucket, glass bottle -> water bottle).
		// Registered separately so itemStateChangedMode applies independently.
		// Uses prevFrameItems/currentFrameItems snapshots taken in onRenderHotbar so
		// all slots are compared against the same frame boundary simultaneously.
		Item prev = prevFrameItems[slotIndex];
		Item curr = currentFrameItems[slotIndex];

		if (curr != prev && curr != null && prev != null) {
			// check if curr appears as prev in any other slot --> swap
			boolean isSwap = false;
			for (int i = 0; i < 10; i++) {
				if (i != slotIndex
						&& prevFrameItems[i] == curr
						&& currentFrameItems[i] != prevFrameItems[i]) { // other slot also changed
					isSwap = true;
					break;
				}
			}
			if (isSwap) {
				PickupTracker.addGroundPickupSlot(slotIndex);
			}
			if (!isSwap) {
				PickupTracker.addItemStateChangedSlot(slotIndex);
			}
		}

		float currentBobbing = stack.getPopTime();
		if (currentBobbing > prevBobbingTime[slotIndex]) {
			// Ground pickup just started — register our own timer instead of using
			// vanilla'
			SlotKey pickBlockKey = new SlotKey(-1, slotIndex);
			if (!PickupTracker.getSlotsToAnimate().containsKey(pickBlockKey)) {
				PickupTracker.addGroundPickupSlot(slotIndex); // new method, new key e.g. SlotKey(-3, slot)
			}
		}
		prevBobbingTime[slotIndex] = currentBobbing;

		if (!stack.isEmpty()) {
			float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
			SlotKey groundKey = new SlotKey(-3, slotIndex);
			float f = PickupTracker.getBobbingAnimationTimeCustom(groundKey) - partialTick;
			// float f = stack.getBobbingAnimationTime() - tickDelta; //vanilla way
			boolean isPickBlock = false;
			boolean isItemStateChanged = false;
			boolean hasCustomAnimation = f > 0.0F;

			if (!hasCustomAnimation) {
				// pick-block!!!
				// Vanilla isn't animating — check our custom tracker (e.g. pick-block)
				SlotKey key = new SlotKey(-1, seed - 1);
				float pickBlockF = PickupTracker.getBobbingAnimationTimeCustom(key)
						- partialTick;
				if (pickBlockF > 0.0F) {
					f = pickBlockF;
					isPickBlock = true;
				}

				SlotKey itemStateKey = new SlotKey(-2, seed - 1);
				float itemStateF = PickupTracker.getBobbingAnimationTimeCustom(itemStateKey) - partialTick;
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

			if (mode == AnimationMode.VANILLA) { // VANILLA MODE
				// let vanilla handle everything
				if (!isPickBlock && !isItemStateChanged && !hasCustomAnimation) {
					return;
				}

				if (f > 0.0F) {
					float h = 1.0F + f / 5.0F;
					context.pose().pushMatrix();
					context.pose().translate((float) (x + 8), (float) (y + 12));
					context.pose().scale(1.0F / h, (h + 1.0F) / 2.0F);
					context.pose().translate((float) (-(x + 8)), (float) (-(y + 12)));
				}

				context.item(player, stack, x, y, seed);
				if (f > 0.0F) {
					context.pose().popMatrix();
				}

				context.itemDecorations(Minecraft.getInstance().font, stack, x, y);

				ci.cancel();
				return;
			} else if (mode == AnimationMode.CUSTOM) { // CUSTOM (MOD) MODE
				if (f > 0.0F) {
					float progress = f / ModConfig.get().animationDuration;
					// float scale = 1.0F + ModConfig.get().bounceScale * (float) Math.sin(progress
					// * Math.PI); // 0.25F is
					// bounce
					// scale

					float scale = 1.0F + ModConfig.get().bounceScale * (float) Math.pow(progress, 2.0F);

					context.pose().pushMatrix();
					context.pose().translate((float) (x + 8), (float) (y + 8));
					context.pose().scale(scale, scale);
					context.pose().translate((float) (-(x + 8)), (float) (-(y + 8)));
				}

				context.item(player, stack, x, y, seed);
				if (f > 0.0F) {
					context.pose().popMatrix();
				}

				context.itemDecorations(Minecraft.getInstance().font, stack, x, y);

				ci.cancel();
				return;
			}
			// DISABLED MODE
			context.item(player, stack, x, y, seed);
			context.itemDecorations(Minecraft.getInstance().font, stack, x, y);
			ci.cancel();

		}
	}

	// Snapshots all 10 hotbar slots (0-8 + offhand) once per frame before
	// renderHotbarItem fires for individual slots. This ensures prevFrameItems and
	// currentFrameItems are fully populated for all slots simultaneously, making
	// cross-slot swap detection in renderHotbarItem reliable — if snapshotted
	// per-slot inside renderHotbarItem instead, earlier slots wouldn't yet have
	// their updated state when later slots are processed.
	// Skipped when a screen is open — packet handler covers slot changes then,
	// and running here would cause double animations on hotbar slots.
	@Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"))
	private void onExtractHotbarAndDecorations(GuiGraphicsExtractor context, DeltaTracker deltaTracker,
			CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();

		if (client == null || client.screen != null)
			return;

		System.arraycopy(currentFrameItems, 0, prevFrameItems, 0, 10);

		for (int i = 0; i < 9; i++) {
			ItemStack s = client.player.getInventory().getItem(i);
			currentFrameItems[i] = s.isEmpty() ? null : s.getItem();
		}

		// offhand
		ItemStack offhand = client.player.getOffhandItem();
		currentFrameItems[9] = offhand.isEmpty() ? null : offhand.getItem();
	}

}