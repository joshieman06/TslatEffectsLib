package net.tslat.effectslib.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffectInstance;
import net.tslat.effectslib.api.ExtendedMobEffect;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Shadow
	@Final
	Minecraft minecraft;

	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/Options;screenEffectScale()Lnet/minecraft/client/OptionInstance;"
			)
	)
	private void tel$renderEffectOverlays(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo callback) {
		tel$doExtendedEffectRenders(this.minecraft.player, partialTicks);
	}

	@Unique
	private void tel$doExtendedEffectRenders(LocalPlayer player, float partialTicks) {
		PoseStack poseStack = new PoseStack();

		for (MobEffectInstance instance : player.getActiveEffects()) {
			if (instance.getEffect().value() instanceof ExtendedMobEffect extendedMobEffect && extendedMobEffect.getOverlayRenderer() != null)
				extendedMobEffect.getOverlayRenderer().render(poseStack, partialTicks, instance);
		}
	}
}
