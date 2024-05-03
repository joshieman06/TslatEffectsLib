package net.tslat.effectslib.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.tslat.effectslib.api.ExtendedEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(GrindstoneMenu.class)
public class GrindstoneMenuMixin {
	@Unique
	private static ItemStack tel$capturedStack;

	@Inject(method = "removeNonCursesFrom", at = @At("HEAD"))
	public void tel$captureStack(ItemStack stack, CallbackInfoReturnable<ItemStack> callback) {
		tel$capturedStack = stack;
	}

	@WrapOperation(method = "removeNonCursesFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;updateEnchantments(Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/world/item/enchantment/ItemEnchantments;"))
	private ItemEnchantments tel$checkRemoveCurses(ItemStack stack, Consumer<ItemEnchantments.Mutable> consumer, Operation<ItemEnchantments> original) {
		return original.call(stack, (Consumer<ItemEnchantments.Mutable>)enchants -> {
			enchants.removeIf(enchant -> {
				if (enchant instanceof ExtendedEnchantment extendedEnchantment) {
					Boolean shouldRemove = extendedEnchantment.shouldGrindstoneRemove(tel$capturedStack);

					if (shouldRemove != null)
						return shouldRemove;
				}

				return !enchant.value().isCurse();
			});
		});
	}

	// Fix once lambda capturing is fixed
	/*@WrapOperation(method = {"lambda$removeNonCursesFrom$2", "method_16694"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/ItemEnchantments$Mutable;removeIf(Ljava/util/function/Predicate;)V"))
	private static void tel$checkRemoveCurses(ItemEnchantments.Mutable enchantments, Predicate<Holder<Enchantment>> basePredicate, Operation<Void> original) {
		System.out.println("poopy");
		original.call(enchantments, (Predicate<Holder<Enchantment>>)enchant -> {
			System.out.println("nah");
			if (enchant instanceof ExtendedEnchantment extendedEnchantment) {
				Boolean shouldRemove = extendedEnchantment.shouldGrindstoneRemove(tel$capturedStack);

				if (shouldRemove != null)
					return shouldRemove;
			}

			return basePredicate.test(enchant);
		});
	}*/
}