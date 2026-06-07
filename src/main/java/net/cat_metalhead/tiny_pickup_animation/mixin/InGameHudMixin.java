package net.cat_metalhead.tiny_pickup_animation.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public class InGameHudMixin {
	@Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
	private void onRenderHotbarItem(
			GuiGraphicsExtractor context, int x, int y, DeltaTracker tickCounter, Player player,
			ItemStack stack, int seed, CallbackInfo ci) {

		if (!stack.isEmpty()) {
			float f = (float) stack.getPopTime() - tickCounter.getGameTimeDeltaPartialTick(false);
			// System.out.println(f);
			if (f > 0.0F) {
				float progress = f / 5.0F;
				float scale = 1.0F + 0.25F * (float) Math.sin(progress * Math.PI); // Fancy bounce scale

				context.pose().pushMatrix();
				context.pose().translate((float) (x + 8), (float) (y + 12));
				context.pose().scale(scale * 1.1F, scale * 1.1F);
				context.pose().translate((float) (-(x + 8)), (float) (-(y + 12)));
			}

			context.item(player, stack, x, y, seed);
			if (f > 0.0F) {
				context.pose().popMatrix();
			}

			context.itemDecorations(Minecraft.getInstance().font, stack, x, y);

			ci.cancel();
		}
	}
}
