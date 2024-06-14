package net.tslat.effectslib.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.tslat.effectslib.api.ExtendedMobEffect;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Shadow
	@Final
	private Map<MobEffect, MobEffectInstance> activeEffects;
	@Shadow
	protected abstract void onEffectRemoved(MobEffectInstance pEffectInstance);
	@Shadow
	public abstract Collection<MobEffectInstance> getActiveEffects();

	@WrapOperation(
			method = "actuallyHurt",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")
	)
	private float tel$hookDamageReduction(LivingEntity entity, DamageSource damageSource, float damage, Operation<Float> original) {
		if (!damageSource.is(DamageTypeTags.BYPASSES_EFFECTS))
			damage = tel$handleDamageReduction(entity, damageSource, damage);

		return original.call(entity, damageSource, damage);
	}

    @Unique
	private float tel$handleDamageReduction(LivingEntity victim, DamageSource damageSource, float damage) {
		final List<Consumer<Float>> attackerCallbacks = new ObjectArrayList<>();
		final List<Consumer<Float>> victimCallbacks = new ObjectArrayList<>();
		final boolean bypassesEnchants = damageSource.is(DamageTypeTags.BYPASSES_ENCHANTMENTS);

		if (damageSource.getEntity() instanceof LivingEntity attacker) {
			for (MobEffectInstance instance : attacker.getActiveEffects()) {
				if (instance.getEffect().value() instanceof ExtendedMobEffect extendedMobEffect) {
					damage = extendedMobEffect.modifyOutgoingAttackDamage(attacker, victim, instance, damageSource, damage);

					attackerCallbacks.add(dmg -> extendedMobEffect.afterOutgoingAttack(attacker, victim, instance, damageSource, dmg));
				}
			}
		}

		for (MobEffectInstance instance : victim.getActiveEffects()) {
			if (instance.getEffect().value() instanceof ExtendedMobEffect extendedMobEffect) {
				damage = extendedMobEffect.modifyIncomingAttackDamage(victim, instance, damageSource, damage);

				victimCallbacks.add(dmg -> extendedMobEffect.afterIncomingAttack(victim, instance, damageSource, dmg));
			}
		}

		if (damage > 0) {
			for (Consumer<Float> consumer : attackerCallbacks) {
				consumer.accept(damage);
			}

			for (Consumer<Float> consumer : victimCallbacks) {
				consumer.accept(damage);
			}
		}

		return damage;
	}

	@Inject(
			method = "hurt",
			at = @At(
					value = "HEAD"
			),
			cancellable = true
	)
	private void tel$checkCancellation(DamageSource damageSource, float damage, CallbackInfoReturnable<Boolean> callback) {
		if (tel$checkEffectAttackCancellation((LivingEntity)(Object)this, damageSource, damage))
			callback.setReturnValue(false);
	}

	@Unique
	private boolean tel$checkEffectAttackCancellation(LivingEntity victim, DamageSource damageSource, float damage) {
		for (MobEffectInstance instance : victim.getActiveEffects()) {
			if (instance.getEffect().value() instanceof ExtendedMobEffect extendedMobEffect)
				if (!extendedMobEffect.beforeIncomingAttack(victim, instance, damageSource, damage))
					return true;
		}

		return false;
	}

	@Inject(
			method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;onEffectAdded(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)V"
			)
	)
	private void tel$onEffectAdded(MobEffectInstance effectInstance, @Nullable Entity source, CallbackInfoReturnable<Boolean> callback) {
		if (effectInstance.getEffect().value() instanceof ExtendedMobEffect extendedEffect)
			extendedEffect.onApplication(effectInstance, source, (LivingEntity)(Object)this, effectInstance.getAmplifier());
	}

	@WrapOperation(
			method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/effect/MobEffectInstance;update(Lnet/minecraft/world/effect/MobEffectInstance;)Z"
			)
	)
	private boolean tel$onEffectUpdated(MobEffectInstance existingEffect, MobEffectInstance newEffect, Operation<Boolean> original) {
		if (existingEffect.getEffect().value() instanceof ExtendedMobEffect extendedEffect)
			newEffect = extendedEffect.onReapplication(existingEffect, newEffect, (LivingEntity)(Object)this);

		return original.call(existingEffect, newEffect);
	}

	@WrapOperation(
			method = "checkTotemDeathProtection",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;removeAllEffects()Z"
			)
	)
	private boolean tel$checkTotemUndyingClear(LivingEntity entity, Operation<Boolean> original) {
		if (entity.level().isClientSide())
			return false;

		boolean hasRemoved = false;

		for (Iterator<MobEffectInstance> iterator = this.activeEffects.values().iterator(); iterator.hasNext();) {
			MobEffectInstance instance = iterator.next();

			if (!(instance.getEffect().value() instanceof ExtendedMobEffect extendedEffect) || extendedEffect.shouldBeRemovedByTotemOfDeath(instance, entity)) {
				onEffectRemoved(instance);
				iterator.remove();

				hasRemoved = true;
			}
		}

		return hasRemoved;
	}

	@Inject(
			method = "removeEffect",
			at = @At(value = "HEAD"),
			cancellable = true
	)
	private void tel$onEffectRemoved(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> callback) {
		if (effect.value() instanceof ExtendedMobEffect extendedEffect) {
			final MobEffectInstance effectInstance = this.activeEffects.get(extendedEffect);

			if (effectInstance instanceof MobEffectInstanceAccessor instance && instance.hasTicksRemaining() && !extendedEffect.onRemove(effectInstance, (LivingEntity)(Object)this))
				callback.setReturnValue(false);
		}
	}

    @WrapOperation(
			method = "removeAllEffects",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/Collection;iterator()Ljava/util/Iterator;"
			)
	)
	private Iterator<MobEffectInstance> tel$wrapRemoveAllEffects(Collection<MobEffectInstance> collection, Operation<Iterator<MobEffectInstance>> original) {
		final LivingEntity entity = (LivingEntity)(Object)this;

		return new Iterator<>() {
			final Iterator<MobEffectInstance> iterator = collection.iterator();
			MobEffectInstance next = null;

			@Override
			public boolean hasNext() {
				if (!this.iterator.hasNext())
					return false;

				if (this.next != null)
					return true;

				this.next = this.iterator.next();

				if (this.next.getEffect().value() instanceof ExtendedMobEffect extendedEffect && !extendedEffect.onRemove(this.next, entity)) {
					this.next = null;

					return hasNext();
				}

				return true;
			}

			@Override
			public MobEffectInstance next() {
				if (this.next != null) {
					MobEffectInstance nextPrev = this.next;
					this.next = null;

					return nextPrev;
				}

				return this.iterator.next();
			}

			@Override
			public void remove() {
				this.iterator.remove();
			}
		};
	}

	@WrapOperation(
			method = "tickEffects",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;onEffectRemoved(Lnet/minecraft/world/effect/MobEffectInstance;)V"
			)
	)
	private void tel$onEffectExpired(LivingEntity instance, MobEffectInstance effectInstance, Operation<Void> original) {
		original.call(instance, effectInstance);

		if (effectInstance.getEffect().value() instanceof ExtendedMobEffect extendedEffect)
			extendedEffect.onExpiry(effectInstance, instance);
	}

	@Inject(
			method = "canBeAffected",
			at = @At(
					value = "HEAD"
			),
			cancellable = true
	)
	private void tel$checkEffectApplicability(MobEffectInstance effectInstance, CallbackInfoReturnable<Boolean> callback) {
		final LivingEntity self = (LivingEntity)(Object)this;

		if (effectInstance.getEffect().value() instanceof ExtendedMobEffect extendedEffect && !extendedEffect.canApply(self, effectInstance))
			callback.setReturnValue(false);

		if (!getActiveEffects().isEmpty()) {
			for (MobEffectInstance otherInstance : getActiveEffects()) {
				if (otherInstance.getEffect().value() instanceof ExtendedMobEffect extendedEffect && !extendedEffect.canApplyOther(self, otherInstance))
					callback.setReturnValue(false);
			}
		}
	}

	@WrapOperation(
			method = "tickEffects",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/network/syncher/SynchedEntityData;get(Lnet/minecraft/network/syncher/EntityDataAccessor;)Ljava/lang/Object;",
					ordinal = 0
			)
	)
	private <T extends List<ParticleOptions>> Object tel$doEffectParticles(SynchedEntityData entityData, EntityDataAccessor<T> dataAcessor, Operation<T> original) {
		final LivingEntity self = (LivingEntity)(Object)this;

		if (self.level().isClientSide() && !tel$checkCustomEffectParticles(self))
			return List.of();

		return original.call(entityData, dataAcessor);
	}

	@Unique
	private boolean tel$checkCustomEffectParticles(LivingEntity entity) {
		boolean continueVanilla = false;

		for (MobEffectInstance effect : this.activeEffects.values()) {
			if (!(effect.getEffect().value() instanceof ExtendedMobEffect extendedEffect) || !extendedEffect.doClientSideEffectTick(effect, entity))
				continueVanilla = true;
		}

		return continueVanilla;
	}

	@WrapOperation(method = "addAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;save()Lnet/minecraft/nbt/Tag;"))
	public Tag tel$wrapEffectSave(MobEffectInstance instance, Operation<Tag> original) {
		final Tag data = original.call(instance);

		if (data instanceof CompoundTag compoundTag && instance.getEffect().value() instanceof ExtendedMobEffect extendedEffect)
			extendedEffect.write(compoundTag, instance);

		return data;
	}

	@WrapOperation(method = "readAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;load(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/effect/MobEffectInstance;"))
	public MobEffectInstance tel$wrapEffectLoad(CompoundTag tag, Operation<MobEffectInstance> original) {
		final MobEffectInstance instance = original.call(tag);

		if (instance != null && instance.getEffect().value() instanceof ExtendedMobEffect extendedEffect)
			extendedEffect.read(tag, instance);

		return instance;
	}
}
