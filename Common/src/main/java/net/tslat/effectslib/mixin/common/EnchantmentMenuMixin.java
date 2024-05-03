package net.tslat.effectslib.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.tslat.effectslib.api.ExtendedEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(EnchantmentMenu.class)
public class EnchantmentMenuMixin {
	@WrapOperation(method = "getEnchantmentList", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;selectEnchantment(Lnet/minecraft/world/flag/FeatureFlagSet;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/item/ItemStack;IZ)Ljava/util/List;"))
	private List<EnchantmentInstance> filterExtendedEnchantments(FeatureFlagSet featureFlags, RandomSource random, ItemStack stack, int enchantLevel, boolean includeTreasure, Operation<List<EnchantmentInstance>> original) {
		final List<EnchantmentInstance> enchants = original.call(featureFlags, random, stack, enchantLevel, includeTreasure);

		enchants.removeIf(instance -> instance.enchantment instanceof ExtendedEnchantment extendedEnchant && !extendedEnchant.canEnchant(stack, ExtendedEnchantment.ENCHANTING_TABLE));

		return enchants;
	}
}
