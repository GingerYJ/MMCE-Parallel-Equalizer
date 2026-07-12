package com.gingeryj.mmceparallelequalizer.common.tile;

import com.gingeryj.mmceparallelequalizer.common.component.ParallelEqualizerComponents;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeStartEvent;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTileNotifiable;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public final class TileParallelEqualizerHatch extends TileColorableMachineComponent
    implements MachineComponentTileNotifiable {

    private final MachineComponent<TileParallelEqualizerHatch> component =
        new MachineComponent<TileParallelEqualizerHatch>(IOType.INPUT) {
            @Override
            public ComponentType getComponentType() {
                return ParallelEqualizerComponents.TYPE;
            }

            @Override
            public TileParallelEqualizerHatch getContainerProvider() {
                return TileParallelEqualizerHatch.this;
            }
        };

    @Override
    public MachineComponent<?> provideComponent() {
        return component;
    }

    @Override
    public void onMachineEvent(MachineEvent event) {
        if (!(event instanceof FactoryRecipeStartEvent)) {
            return;
        }

        FactoryRecipeStartEvent startEvent = (FactoryRecipeStartEvent) event;
        RecipeCraftingContext context = startEvent.getContext();
        ActiveMachineRecipe activeRecipe = startEvent.getActiveRecipe();
        if (context == null || activeRecipe == null) {
            return;
        }

        int quota = getParallelismQuota(startEvent.getController());
        if (quota < 1) {
            return;
        }

        context.setParallelism(Math.min(activeRecipe.getParallelism(), quota));
    }

    private static int getParallelismQuota(Object factory) {
        try {
            int maxThreads = (Integer) factory.getClass().getMethod("getMaxThreads").invoke(factory);
            Object coreThreads = factory.getClass().getMethod("getCoreRecipeThreads").invoke(factory);
            int maxParallelism = (Integer) factory.getClass().getMethod("getMaxParallelism").invoke(factory);
            if (!(coreThreads instanceof Map)) {
                return -1;
            }

            int threadSlots = Math.max(1, maxThreads + ((Map<?, ?>) coreThreads).size());
            return Math.max(1, Math.max(0, maxParallelism) / threadSlots);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassCastException ignored) {
            return -1;
        }
    }
}
