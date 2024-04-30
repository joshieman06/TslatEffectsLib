package net.tslat.effectslib.mixin.common;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.tslat.effectslib.api.ExtendedEnchantment;
import net.tslat.effectslib.api.ExtendedMobEffect;
import net.tslat.effectslib.api.util.EnchantmentUtil;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

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

	@ModifyArg(
			method = "actuallyHurt",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"
			),
			index = 1
	)
	private float tel$hookDamageReduction(DamageSource damageSource, float damage) {
		if (!damageSource.is(DamageTypeTags.BYPASSES_EFFECTS))
			damage = tel$handleDamageReduction(damageSource, damage);

		return damage;
	}

    @Unique
	private float tel$handleDamageReduction(DamageSource damageSource, float damage) {
		final LivingEntity victim = (LivingEntity)(Object)this;
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

			if (!bypassesEnchants) {
				Map<Enchantment, EnchantmentUtil.EntityEnchantmentData> attackerEnchants = EnchantmentUtil.collectAllEnchantments(attacker, true);

				for (Map.Entry<Enchantment, EnchantmentUtil.EntityEnchantmentData> entry : attackerEnchants.entrySet()) {
					final EnchantmentUtil.EntityEnchantmentData data = entry.getValue();
					final ExtendedEnchantment enchant = (ExtendedEnchantment)data.getEnchantment();
					final int totalLevel = data.getTotalEnchantmentLevel();
					final int enchantedStacks = data.getEnchantedStacks().size();

					for (int i = 0; i < enchantedStacks; i++) {
						final ObjectIntPair<ItemStack> stack = data.getEnchantedStacks().get(i);
						damage = enchant.modifyOutgoingAttackDamage(attacker, victim, damageSource, damage, stack.first(), stack.valueInt(), totalLevel);
						final boolean isLastStack = i == enchantedStacks - 1;

						attackerCallbacks.add(dmg -> enchant.afterOutgoingAttack(attacker, victim, damageSource, dmg, stack.first(), stack.valueInt(), totalLevel, isLastStack));
					}
				}
			}
		}

		for (MobEffectInstance instance : victim.getActiveEffects()) {
			if (instance.getEffect().value() instanceof ExtendedMobEffect extendedMobEffect) {
				damage = extendedMobEffect.modifyIncomingAttackDamage(victim, instance, damageSource, damage);

				victimCallbacks.add(dmg -> extendedMobEffect.afterIncomingAttack(victim, instance, damageSource, dmg));
			}
		}

		if (!bypassesEnchants) {
			Map<Enchantment, EnchantmentUtil.EntityEnchantmentData> victimEnchants = EnchantmentUtil.collectAllEnchantments(victim, true);

			for (Map.Entry<Enchantment, EnchantmentUtil.EntityEnchantmentData> entry : victimEnchants.entrySet()) {
				final EnchantmentUtil.EntityEnchantmentData data = entry.getValue();
				final ExtendedEnchantment enchant = (ExtendedEnchantment)data.getEnchantment();
				final int totalLevel = data.getTotalEnchantmentLevel();
				final int enchantedStacks = data.getEnchantedStacks().size();

				for (int i = 0; i < enchantedStacks; i++) {
					final ObjectIntPair<ItemStack> stack = data.getEnchantedStacks().get(i);
					damage = enchant.modifyIncomingAttackDamage(victim, damageSource, damage, stack.first(), stack.valueInt(), totalLevel);
					final boolean isLastStack = i == enchantedStacks - 1;

					victimCallbacks.add(dmg -> enchant.afterIncomingAttack(victim, damageSource, dmg, stack.first(), stack.valueInt(), totalLevel, isLastStack));
				}
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

	@Redirect(
			method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/effect/MobEffectInstance;update(Lnet/minecraft/world/effect/MobEffectInstance;)Z"
			)
	)
	private boolean tel$onEffectUpdated(MobEffectInstance existingEffect, MobEffectInstance newEffect) {
		if (existingEffect.getEffect().value() instanceof ExtendedMobEffect extendedEffect)
			newEffect = extendedEffect.onReapplication(existingEffect, newEffect, (LivingEntity)(Object)this);

		return existingEffect.update(newEffect);
	}

	@Redirect(
			method = "checkTotemDeathProtection",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;removeAllEffects()Z"
			)
	)
	private boolean tel$checkTotemUndyingClear(LivingEntity entity) {
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

    @Redirect(
			method = "removeAllEffects",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/Collection;iterator()Ljava/util/Iterator;"
			)
	)
	private Iterator<MobEffectInstance> tel$wrapRemoveAllEffects(Collection<MobEffectInstance> collection) {
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

	@Redirect(
			method = "tickEffects",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;onEffectRemoved(Lnet/minecraft/world/effect/MobEffectInstance;)V"
			)
	)
	private void tel$onEffectExpired(LivingEntity instance, MobEffectInstance effectInstance) {
		onEffectRemoved(effectInstance);

		if (effectInstance.getEffect().value() instanceof ExtendedMobEffect extendedEffect)
			extendedEffect.onExpiry(effectInstance, instance);
	}

	@Inject(
			method = "canBeAffected",
			at = @At(
					value = "TAIL"
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

	@Inject(
			method = "tickEffects",
			at = @At(
					value = "INVOKE_ASSIGN",
					target = "Lnet/minecraft/network/syncher/SynchedEntityData;get(Lnet/minecraft/network/syncher/EntityDataAccessor;)Ljava/lang/Object;",
					ordinal = 0
			),
			cancellable = true
	)
	private void tel$doEffectParticles(CallbackInfo callback) {
		final LivingEntity self = (LivingEntity)(Object)this;

		if (self.level().isClientSide() && !tel$checkCustomEffectParticles(self))
			callback.cancel();
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

	@Inject(
			method = "collectEquipmentChanges",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
			),
			locals = LocalCapture.CAPTURE_FAILSOFT
	)
	private void tel$onEquipmentChange(CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> callback, Map<EquipmentSlot, ItemStack> changeMap, EquipmentSlot[] slots, int slotsSize, int slotIndex, EquipmentSlot slot, ItemStack oldStack, ItemStack newStack) {
		tel$handleEquipmentChange((LivingEntity)(Object)this, oldStack, newStack, slot);
	}

	@Unique
	private void tel$handleEquipmentChange(LivingEntity entity, ItemStack from, ItemStack to, EquipmentSlot slot) {
		if (from == to)
			return;

		for (ObjectIntPair<Holder<ExtendedEnchantment>> instance : EnchantmentUtil.<ExtendedEnchantment>getStackEnchantmentsForUse(entity, from, slot, true)) {
			instance.key().value().onUnequip(entity, slot, from, to, instance.valueInt());
		}

		for (ObjectIntPair<Holder<ExtendedEnchantment>> instance : EnchantmentUtil.<ExtendedEnchantment>getStackEnchantmentsForUse(entity, to, slot, true)) {
			instance.key().value().onEquip(entity, slot, from, to, instance.valueInt());
		}
	}

	@Inject(method = "aiStep", at = @At("HEAD"))
	public void tel$onEntityTick(CallbackInfo ci) {
		final LivingEntity self = (LivingEntity)(Object)this;

		if (!(self instanceof Player)) {
			self.getArmorSlots().forEach(stack -> tel$tickEnchantmentsForEquipment(self, stack));
			tel$tickEnchantmentsForEquipment(self, self.getMainHandItem());
			tel$tickEnchantmentsForEquipment(self, self.getOffhandItem());
		}
	}

	@Unique
	private static void tel$tickEnchantmentsForEquipment(LivingEntity entity, ItemStack stack) {
		for (ObjectIntPair<Holder<ExtendedEnchantment>> enchant : EnchantmentUtil.<ExtendedEnchantment>getStackEnchantmentsForUse(entity, stack, null, true)) {
			enchant.key().value().tick(entity, stack);
		}
	}

	@Redirect(method = "addAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;save()Lnet/minecraft/nbt/Tag;"))
	public Tag tel$wrapEffectSave(MobEffectInstance instance) {
		final CompoundTag data = (CompoundTag)instance.save();

		if (instance.getEffect().value() instanceof ExtendedMobEffect extendedEffect)
			extendedEffect.write(data, instance);

		return data;
	}

	@Redirect(method = "readAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;load(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/effect/MobEffectInstance;"))
	public MobEffectInstance tel$wrapEffectLoad(CompoundTag tag) {
		final MobEffectInstance instance = MobEffectInstance.load(tag);

		if (instance != null && instance.getEffect().value() instanceof ExtendedMobEffect extendedEffect)
			extendedEffect.read(tag, instance);

		return instance;
	}
}
