package com.cleanroommc.groovyscript.mixin.thermalexpansion;

import cofh.core.inventory.ComparableItemStackValidatedNBT;
import cofh.thermalexpansion.util.managers.machine.CentrifugeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = CentrifugeManager.class, remap = false)
public interface CentrifugeManagerAccessor {

    @Accessor
    static Map<ComparableItemStackValidatedNBT, CentrifugeManager.CentrifugeRecipe> getRecipeMap() {
        throw new AssertionError();
    }

    @Accessor
    static Map<ComparableItemStackValidatedNBT, CentrifugeManager.CentrifugeRecipe> getRecipeMapMobs() {
        throw new AssertionError();
    }
}
