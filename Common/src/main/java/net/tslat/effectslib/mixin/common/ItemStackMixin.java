package net.tslat.effectslib.mixin.common;

import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tslat.effectslib.api.ExtendedEnchantment;
import net.tslat.effectslib.api.ExtendedMobEffect;
import net.tslat.effectslib.api.util.EnchantmentUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "inventoryTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;inventoryTick(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;IZ)V"))
    public void tel$onInventoryTick(Level level, Entity entity, int slot, boolean isSelected, CallbackInfo ci) {
        if (entity instanceof LivingEntity livingEntity)
            tel$doEnchantmentTicks(livingEntity, (ItemStack)(Object)this);
    }

    @Unique
    private static void tel$doEnchantmentTicks(LivingEntity entity, ItemStack stack) {
        for (ObjectIntPair<Holder<ExtendedEnchantment>> enchant : EnchantmentUtil.<ExtendedEnchantment>getStackEnchantmentsForUse(entity, stack, null, true)) {
            enchant.key().value().tick(entity, stack);
        }
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    public void tel$onItemUseFinish(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (!level.isClientSide && ((DataComponentHolder)this).has(DataComponents.FOOD) && !entity.getActiveEffectsMap().isEmpty())
            tel$checkCustomCuring(entity, (ItemStack)(Object)this);
    }

    @Unique
    private static void tel$checkCustomCuring(LivingEntity entity, ItemStack stack) {
        final Set<Holder<MobEffect>> removingEffects = new ObjectOpenHashSet<>();

        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (effect.getEffect().value() instanceof ExtendedMobEffect extendedEffect && extendedEffect.shouldCureEffect(effect, stack, entity))
                removingEffects.add(effect.getEffect());
        }

        for (Holder<MobEffect> effect : removingEffects) {
            entity.removeEffect(effect);
        }
    }
}