package net.tslat.effectslib.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.tslat.effectslib.api.ExtendedEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantRandomlyFunction.class)
public class EnchantRandomlyFunctionMixin {
	@WrapOperation(method = {"lambda$run$6", "method_53327"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"), remap = false)
	private static boolean canEnchantExtended(Enchantment instance, ItemStack stack, Operation<Boolean> original) {
		if (instance instanceof ExtendedEnchantment extendedEnchantment)
			return extendedEnchantment.canEnchant(stack, ExtendedEnchantment.LOOT);

		return original.call(instance, stack);
	}
}
