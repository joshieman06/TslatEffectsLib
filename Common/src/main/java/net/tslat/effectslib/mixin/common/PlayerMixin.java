package net.tslat.effectslib.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.tslat.effectslib.api.ExtendedEnchantment;
import net.tslat.effectslib.api.ExtendedMobEffect;
import net.tslat.effectslib.api.util.EnchantmentUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(Player.class)
public class PlayerMixin {
	@WrapOperation(
			method = "actuallyHurt",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")
	)
	private float tel$onIncomingAttack(Player entity, DamageSource damageSource, float damage, Operation<Float> original) {
		if (!damageSource.is(DamageTypeTags.BYPASSES_EFFECTS))
			damage = tel$handlePlayerDamage(entity, damageSource, damage);

		return original.call(entity, damageSource, damage);
	}

    @Unique
	private float tel$handlePlayerDamage(Player victim, DamageSource damageSource, float damage) {
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
						damage = enchant.modifyOutgoingAttackDamage(attacker, victim, damageSource, damage, stack.key(), stack.valueInt(), totalLevel);
						final boolean isLastStack = i == enchantedStacks - 1;

						attackerCallbacks.add(dmg -> enchant.afterOutgoingAttack(attacker, victim, damageSource, dmg, stack.key(), stack.valueInt(), totalLevel, isLastStack));
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
					damage = enchant.modifyIncomingAttackDamage(victim, damageSource, damage, stack.key(), stack.valueInt(), totalLevel);
					final boolean isLastStack = i == enchantedStacks - 1;

					victimCallbacks.add(dmg -> enchant.afterIncomingAttack(victim, damageSource, dmg, stack.key(), stack.valueInt(), totalLevel, isLastStack));
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
	private void tel$checkIncomingAttack(DamageSource damageSource, float damage, CallbackInfoReturnable<Boolean> callback) {
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
}
