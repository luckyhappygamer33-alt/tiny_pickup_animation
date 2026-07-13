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
	private final Item[] itemJustLeft = new Item[10];
	private final boolean[] pendingItemStateChanged = new boolean[10];
	private final Item[] prevFrameItems = new Item[10];
	private final Item[] currentFrameItems = new Item[10];
	private boolean tmp = false;
	private final float[] prevBobbingTime = new float[10];

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

		Item prev = prevFrameItems[slotIndex];
		Item curr = currentFrameItems[slotIndex];

		if (curr != prev && curr != null && prev != null) {

			// System.out.println("RENDER slotIndex=" + slotIndex + " seed=" + seed + "
			// item="
			// + (stack.isEmpty() ? "null" : stack.getItem()));
			// check if curr appears as prev in any other slot --> swap
			boolean isSwap = false;
			// System.out.println("CHANGE slot=" + slotIndex
			// + " prev=" + prev
			// + " curr=" + curr);
			for (int i = 0; i < 10; i++) {
				if (i != slotIndex
						&& prevFrameItems[i] == curr
						&& currentFrameItems[i] != prevFrameItems[i]) { // other slot also changed
					isSwap = true;
					tmp = true;
					// System.out.println(" SWAP MATCH found at slot=" + i
					// + " prevFrameItems[i]=" + prevFrameItems[i]
					// + " currentFrameItems[i]=" + currentFrameItems[i]);
					break;
				}
			}
			if (isSwap) {
				PickupTracker.addGroundPickupSlot(slotIndex);
			}
			if (!isSwap) {
				PickupTracker.addItemStateChangedSlot(slotIndex);
			}
			// System.out.println(" isSwap=" + isSwap);
			// System.out.println(" registered as: " + (isSwap ? "groundPickup" :
			// "itemStateChanged"));
		}

		float currentBobbing = stack.getBobbingAnimationTime();
		if (currentBobbing > prevBobbingTime[slotIndex]) {
			// Ground pickup just started — register our own timer instead of using
			// vanilla's
			PickupTracker.addGroundPickupSlot(slotIndex); // new method, new key e.g. SlotKey(-3, slot)
		}
		prevBobbingTime[slotIndex] = currentBobbing;

		if (!stack.isEmpty()) {
			SlotKey groundKey = new SlotKey(-3, slotIndex);
			float f = PickupTracker.getBobbingAnimationTimeCustom(groundKey) - tickDelta;
			// if (tmp) {
			// if (slotIndex == 0 || slotIndex == 1 || slotIndex == 9) {
			// System.out.println("READ groundKey=" + groundKey + " f=" + f);
			// }
			// tmp = false;
			// }
			// float f = stack.getBobbingAnimationTime() - tickDelta;
			boolean isPickBlock = false;
			boolean isItemStateChanged = false;
			boolean hasCustomAnimation = f > 0.0F;

			if (!hasCustomAnimation) {
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

			if (mode == AnimationMode.VANILLA) { // VANILLA MODE
				// let vanilla handle everything
				if (!isPickBlock && !isItemStateChanged && !hasCustomAnimation) {
					return;
				}

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
					// float scale = 1.0F + ModConfig.get().bounceScale * (float) Math.sin(progress
					// * Math.PI); // 0.25F is
					// bounce
					// scale

					float scale = 1.0F + ModConfig.get().bounceScale * (float) Math.pow(progress, 2.0F);

					context.getMatrices().push();
					context.getMatrices().translate((float) (x + 8), (float) (y + 8), 0.0F);
					context.getMatrices().scale(scale, scale, 1.0F);
					context.getMatrices().translate((float) (-(x + 8)), (float) (-(y + 8)), 0.0F);
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

	@Inject(method = "renderHotbar", at = @At("HEAD"))
	private void onRenderHotbar(float tickDelta, DrawContext context, CallbackInfo ci) {
		// System.out.println("onRenderHotbar fired");
		MinecraftClient client = MinecraftClient.getInstance();

		if (client == null || client.currentScreen != null)
			return;

		System.arraycopy(currentFrameItems, 0, prevFrameItems, 0, 10);

		for (int i = 0; i < 9; i++) {
			ItemStack s = client.player.getInventory().getStack(i);
			currentFrameItems[i] = s.isEmpty() ? null : s.getItem();
		}

		// offhand
		ItemStack offhand = client.player.getOffHandStack();
		currentFrameItems[9] = offhand.isEmpty() ? null : offhand.getItem();

		// In onRenderHotbar, after building currentFrameItems:
		// for (int i = 0; i < 10; i++) {
		// if (currentFrameItems[i] != prevFrameItems[i]) {
		// System.out.println("FRAME DIFF slot=" + i
		// + " prev=" + prevFrameItems[i]
		// + " curr=" + currentFrameItems[i]);
		// for (int j = 0; j < 9; j++) {
		// ItemStack s = client.player.getInventory().getStack(j);
		// System.out.println("SNAPSHOT slot=" + j + " item=" + (s.isEmpty() ? "null" :
		// s.getItem()));
		// }
		// }
		// }
	}

}
