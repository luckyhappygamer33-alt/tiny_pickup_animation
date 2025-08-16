package net.cat_metalhead.tiny_pickup_animation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderHotbarItem", at = @At("HEAD"), cancellable = true)
    private void onRenderHotbarItem(DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {

        if (!stack.isEmpty()) {
			float f = (float)stack.getBobbingAnimationTime() - tickCounter.getTickProgress(false);
			// System.out.println(f);
			if (f > 0.0F) {
				float progress = f / 5.0F;
				float scale = 1.0F + 0.25F * (float)Math.sin(progress * Math.PI); // Fancy bounce scale

				context.getMatrices().pushMatrix();
				context.getMatrices().translate((float)(x + 8), (float)(y + 12));
				context.getMatrices().scale(scale*1.1F, scale*1.1F);
				context.getMatrices().translate((float)(-(x + 8)), (float)(-(y + 12)));					
			}

			context.drawItem(player, stack, x, y, seed);
			if (f > 0.0F) {
				context.getMatrices().popMatrix();
			}

			context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, stack, x, y);

			ci.cancel();
		}
    }
}
