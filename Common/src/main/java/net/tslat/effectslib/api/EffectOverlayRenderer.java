package net.tslat.effectslib.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.effect.MobEffectInstance;

@FunctionalInterface
public interface EffectOverlayRenderer {
	void render(PoseStack poseStack, DeltaTracker deltaTracker, MobEffectInstance effectInstance);
}
