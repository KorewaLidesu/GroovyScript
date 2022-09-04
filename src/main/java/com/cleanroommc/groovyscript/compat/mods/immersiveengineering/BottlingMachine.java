package com.cleanroommc.groovyscript.compat.mods.immersiveengineering;

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.crafting.BottlingMachineRecipe;
import com.cleanroommc.groovyscript.api.IIngredient;
import com.cleanroommc.groovyscript.compat.mods.ModSupport;
import com.cleanroommc.groovyscript.helper.RecipeStream;
import com.cleanroommc.groovyscript.helper.recipe.AbstractRecipeBuilder;
import com.cleanroommc.groovyscript.registry.VirtualizedRegistry;
import com.cleanroommc.groovyscript.sandbox.GroovyLog;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class BottlingMachine extends VirtualizedRegistry<BottlingMachineRecipe> {

    public BottlingMachine() {
        super("Bottling", "bottling");
    }

    public static RecipeBuilder recipeBuilder() {
        return new RecipeBuilder();
    }

    @Override
    public void onReload() {
        removeScripted().forEach(recipe -> BottlingMachineRecipe.recipeList.removeIf(r -> r == recipe));
        BottlingMachineRecipe.recipeList.addAll(restoreFromBackup());
    }

    public void add(BottlingMachineRecipe recipe) {
        if (recipe != null) {
            addScripted(recipe);
            BottlingMachineRecipe.recipeList.add(recipe);
        }
    }

    public BottlingMachineRecipe add(ItemStack output, Object input, FluidStack fluidInput) {
        BottlingMachineRecipe recipe = create(output, input, fluidInput);
        add(recipe);
        return recipe;
    }

    public void remove(BottlingMachineRecipe recipe) {
        if (BottlingMachineRecipe.recipeList.removeIf(r -> r == recipe)) addBackup(recipe);
    }

    public void removeByOutput(ItemStack output) {
        for (int i = 0; i < BottlingMachineRecipe.recipeList.size(); i++) {
            BottlingMachineRecipe recipe = BottlingMachineRecipe.recipeList.get(i);
            if (ApiUtils.stackMatchesObject(output, recipe.output, true)) {
                addBackup(recipe);
                BottlingMachineRecipe.recipeList.remove(i);
                break;
            }
        }
    }

    public void removeByInput(ItemStack input, FluidStack inputFluid) {
        BottlingMachineRecipe recipe = BottlingMachineRecipe.findRecipe(input, inputFluid);
        if (recipe != null) {
            addBackup(recipe);
            BottlingMachineRecipe.recipeList.remove(recipe);
        }
    }

    public void removeByInput(ItemStack input) {
        for (BottlingMachineRecipe recipe : BottlingMachineRecipe.recipeList) {
            if (recipe.input.matches(input)) {
                addBackup(recipe);
                BottlingMachineRecipe.recipeList.remove(recipe);
            }
        }
    }

    public RecipeStream<BottlingMachineRecipe> stream() {
        return new RecipeStream<>(BottlingMachineRecipe.recipeList).setRemover(recipe -> {
            BottlingMachineRecipe recipe1 = BottlingMachineRecipe.findRecipe(recipe.input.stack, recipe.fluidInput);
            if (recipe1 != null) {
                remove(recipe1);
                return true;
            }
            return false;
        });
    }

    public void removeAll() {
        BottlingMachineRecipe.recipeList.forEach(this::addBackup);
        BottlingMachineRecipe.recipeList.clear();
    }

    private static BottlingMachineRecipe create(ItemStack output, Object input, FluidStack fluidInput) {
        if (input instanceof IIngredient) input = ((IIngredient) input).getMatchingStacks();
        return new BottlingMachineRecipe(output, input, fluidInput);
    }

    public static class RecipeBuilder extends AbstractRecipeBuilder<BottlingMachineRecipe> {


        @Override
        public String getErrorMsg() {
            return "Error adding Immersive Engineering Bottling recipe";
        }

        @Override
        public void validate(GroovyLog.Msg msg) {
            validateItems(msg, 1, 1, 1, 1);
            validateFluids(msg, 1, 1, 0, 0);
        }

        @Override
        public @Nullable BottlingMachineRecipe register() {
            if (!validate()) return null;
            return ModSupport.IMMERSIVE_ENGINEERING.get().bottlingMachine.add(output.get(0), input.get(0), fluidInput.get(0));
        }
    }
}
