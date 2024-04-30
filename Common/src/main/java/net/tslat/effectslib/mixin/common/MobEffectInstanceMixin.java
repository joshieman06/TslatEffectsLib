package net.tslat.effectslib.mixin.common;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.tslat.effectslib.api.ExtendedMobEffect;
import net.tslat.effectslib.api.ExtendedMobEffectHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Handle the various {@link MobEffectInstance callbacks}
 */
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin implements ExtendedMobEffectHolder {
	@Unique
	Object data;

	@Shadow
	private int duration;
	@Shadow
	private int amplifier;

	@Shadow protected abstract boolean hasRemainingDuration();

	@Redirect(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/effect/MobEffect;applyEffectTick(Lnet/minecraft/world/entity/LivingEntity;I)Z"
			)
	)
	private boolean tel$onEffectTick(MobEffect effect, LivingEntity entity, int amplifier) {
		if (effect instanceof ExtendedMobEffect extendedEffect)
			return extendedEffect.tick(entity, (MobEffectInstance)(Object)this, amplifier);

		return effect.applyEffectTick(entity, amplifier);
	}

	@Redirect(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/effect/MobEffect;shouldApplyEffectTickThisTick(II)Z"
			)
	)
	private boolean tel$checkEffectTick(MobEffect effect, int duration, int amplifier, LivingEntity entity) {
		if (!(effect instanceof ExtendedMobEffect extendedEffect))
			return effect.shouldApplyEffectTickThisTick(duration, amplifier);

		return extendedEffect.shouldTickEffect((MobEffectInstance)(Object)this, entity, this.duration, this.amplifier);
	}

	@Override
	public Object getExtendedMobEffectData() {
		return this.data;
	}

	@Override
	public void setExtendedMobEffectData(Object data) {
		this.data = data;
	}
}
