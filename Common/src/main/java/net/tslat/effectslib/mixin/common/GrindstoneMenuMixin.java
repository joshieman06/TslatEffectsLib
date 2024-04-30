package net.tslat.effectslib.mixin.common;

import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.tslat.effectslib.api.ExtendedEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GrindstoneMenu.class)
public class GrindstoneMenuMixin {
	@Unique
	private static ItemStack tel$capturedStack;

	@Inject(method = "removeNonCursesFrom", at = @At("HEAD"))
	public void tel$captureStack(ItemStack stack, CallbackInfoReturnable<ItemStack> callback) {
		tel$capturedStack = stack;
	}

	@Inject(method = {"lambda$removeNonCursesFrom$2", "method_16694"}, at = @At(value = "HEAD", remap = false), cancellable = true)
	private static void tel$checkRemoveCurses(ItemEnchantments.Mutable enchantments, CallbackInfo callback) {
		enchantments.removeIf(enchant -> {
			if (enchant instanceof ExtendedEnchantment extendedEnchantment) {
				Boolean shouldRemove = extendedEnchantment.shouldGrindstoneRemove(tel$capturedStack);

				if (shouldRemove != null)
					return shouldRemove;
			}

			return !enchant.value().isCurse();
		});

		callback.cancel();
	}
}