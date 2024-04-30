package net.tslat.effectslib;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public interface TELCommon {
    AABB getRandomEntityBoundingBox(Entity entity, RandomSource random);
}