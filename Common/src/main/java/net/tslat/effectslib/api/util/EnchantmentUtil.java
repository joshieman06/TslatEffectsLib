package net.tslat.effectslib.api.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.tslat.effectslib.api.ExtendedEnchantment;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Helper class for performing miscellaneous work relating to {@link net.minecraft.world.item.enchantment.Enchantment Enchantments}
 */
public final class EnchantmentUtil {
	/**
	 * Gathers all the enchantment information from the given entity, keeping it all cached for efficient usage
	 * @param entity The entity to get the enchantment information from
	 * @param filterForExtendedEnchants Whether to skip any enchants that aren't {@link ExtendedEnchantment ExtendedEnchantments}
	 * @return The total enchantment level, and full list of enchantments and the stacks they are on
	 */
	public static Map<Enchantment, EntityEnchantmentData> collectAllEnchantments(LivingEntity entity, boolean filterForExtendedEnchants) {
		Map<Enchantment, EntityEnchantmentData> data = new Reference2ObjectOpenHashMap<>();

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = entity.getItemBySlot(slot);

			if (stack.isEmpty())
				continue;

			for (ObjectIntPair<Holder<Enchantment>> instance : getStackEnchantmentsForUse(entity, stack, slot, filterForExtendedEnchants)) {
				data.computeIfAbsent(instance.key().value(), EntityEnchantmentData::new).accountStack(stack, instance.valueInt());
			}
		}

		return data;
	}

	/**
	 * Get all enchantments on this ItemStack for the context of usage for handling
	 * @param entity The entity that this stack belongs to
	 * @param stack The ItemStack to get the enchantments from
	 * @param slot The slot the stack is equipped in
	 * @param filterForExtendedEnchants Whether to skip any enchantments that aren't {@link ExtendedEnchantment Extended Enchantments}
	 */
	public static <E extends Enchantment> List<ObjectIntPair<Holder<E>>> getStackEnchantmentsForUse(LivingEntity entity, ItemStack stack, EquipmentSlot slot, boolean filterForExtendedEnchants) {
		final List<ObjectIntPair<Holder<E>>> enchants = new ObjectArrayList<>();

		for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
			if (!filterForExtendedEnchants || (entry.getKey().value() instanceof ExtendedEnchantment extendedEnchant && extendedEnchant.isApplicable(stack, entry.getIntValue(), entity, slot)))
				enchants.add(ObjectIntPair.of((Holder<E>)entry.getKey(), entry.getIntValue()));
		}

		return enchants;
	}

	/**
	 * Get an enchantment and its associated level for the provided ItemStack, if applicable
	 *
	 * @param stack The stack to get the enchantment from
	 * @return The enchant data retrieved from the stack, or null if this enchantment is not on the provided stack
	 */
	@Nullable
	public static ObjectIntPair<Holder<Enchantment>> getEnchantDetailsForStack(Enchantment enchant, ItemStack stack) {
		ItemEnchantments enchants = stack.getEnchantments();

		for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
			if (entry.getKey() == enchant)
				return ObjectIntPair.of(entry.getKey(), entry.getIntValue());
		}

		return null;
	}

	/**
	 * Container object that holds collated enchantment data for an entity.
	 */
	public static class EntityEnchantmentData {
		private final Enchantment enchant;
		private final List<ObjectIntPair<ItemStack>> stacks = new ObjectArrayList<>();
		private int totalLevel = 0;

		public EntityEnchantmentData(Enchantment enchant) {
			this.enchant = enchant;
		}

		public Enchantment getEnchantment() {
			return this.enchant;
		}

		public List<ObjectIntPair<ItemStack>> getEnchantedStacks() {
			return this.stacks;
		}

		public int getTotalEnchantmentLevel() {
			return this.totalLevel;
		}

		/**
		 * Return what percentage (0-1 inclusive) of the totalLevel this stack represents for this enchantment
		 */
		public float fractionOfTotal(ItemStack stack) {
			for (ObjectIntPair<ItemStack> entry : this.stacks) {
				if (entry.first() == stack)
					return entry.valueInt() / (float)this.totalLevel;
			}

			return 0;
		}

		public void accountStack(ItemStack stack, int level) {
			this.stacks.add(ObjectIntPair.of(stack, level));
			this.totalLevel += level;
		}
	}
}
