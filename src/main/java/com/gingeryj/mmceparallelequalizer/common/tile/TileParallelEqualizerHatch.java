package com.gingeryj.mmceparallelequalizer.common.tile;

import com.gingeryj.mmceparallelequalizer.common.component.ParallelEqualizerComponents;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeFinishEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeStartEvent;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTileNotifiable;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

public final class TileParallelEqualizerHatch extends TileColorableMachineComponent
    implements MachineComponentTileNotifiable {

    private static final Map<ActiveMachineRecipe, Boolean> INITIALIZED_RECIPES = new WeakHashMap<>();
    private static final ClassValue<FactoryAccess> FACTORY_ACCESS = new ClassValue<FactoryAccess>() {
        @Override
        protected FactoryAccess computeValue(Class<?> type) {
            return FactoryAccess.create(type);
        }
    };

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
        if (event instanceof FactoryRecipeFinishEvent) {
            ActiveMachineRecipe activeRecipe = ((FactoryRecipeFinishEvent) event).getActiveRecipe();
            if (activeRecipe != null) {
                forgetRecipe(activeRecipe);
            }
            return;
        }

        if (!(event instanceof FactoryRecipeStartEvent)) {
            return;
        }

        FactoryRecipeStartEvent startEvent = (FactoryRecipeStartEvent) event;
        RecipeCraftingContext context = startEvent.getContext();
        ActiveMachineRecipe activeRecipe = startEvent.getActiveRecipe();
        Object factory = startEvent.getController();
        if (context == null || activeRecipe == null || factory == null) {
            return;
        }

        int quota = getStaticQuota(factory);
        if (quota < 1 || !rememberRecipe(activeRecipe)) {
            return;
        }

        context.setParallelism(Math.min(Math.max(1, activeRecipe.getParallelism()), quota));
    }

    /**
     * Reserve a fixed share for every configured factory thread. This deliberately
     * does not reclaim idle threads' shares, avoiding periodic reallocation work.
     */
    private static int getStaticQuota(Object factory) {
        FactoryAccess access = FACTORY_ACCESS.get(factory.getClass());
        Integer maxThreads = access.getInt(factory, access.maxThreads);
        Integer maxParallelism = access.getInt(factory, access.maxParallelism);
        Object coreThreads = access.invoke(factory, access.coreRecipeThreads);
        if (maxThreads == null || maxParallelism == null || !(coreThreads instanceof Map)) {
            return -1;
        }

        int threadSlots = Math.max(1, maxThreads + ((Map<?, ?>) coreThreads).size());
        return Math.max(1, maxParallelism / threadSlots);
    }

    private static boolean rememberRecipe(ActiveMachineRecipe activeRecipe) {
        synchronized (INITIALIZED_RECIPES) {
            return INITIALIZED_RECIPES.put(activeRecipe, Boolean.TRUE) == null;
        }
    }

    private static void forgetRecipe(ActiveMachineRecipe activeRecipe) {
        synchronized (INITIALIZED_RECIPES) {
            INITIALIZED_RECIPES.remove(activeRecipe);
        }
    }

    private static final class FactoryAccess {
        private static final FactoryAccess UNSUPPORTED = new FactoryAccess(null, null, null);

        private final Method maxThreads;
        private final Method coreRecipeThreads;
        private final Method maxParallelism;

        private FactoryAccess(Method maxThreads, Method coreRecipeThreads, Method maxParallelism) {
            this.maxThreads = maxThreads;
            this.coreRecipeThreads = coreRecipeThreads;
            this.maxParallelism = maxParallelism;
        }

        private static FactoryAccess create(Class<?> type) {
            try {
                return new FactoryAccess(
                    type.getMethod("getMaxThreads"),
                    type.getMethod("getCoreRecipeThreads"),
                    type.getMethod("getMaxParallelism"));
            } catch (NoSuchMethodException | SecurityException ignored) {
                return UNSUPPORTED;
            }
        }

        private Integer getInt(Object target, Method method) {
            Object value = invoke(target, method);
            return value instanceof Integer ? (Integer) value : null;
        }

        private Object invoke(Object target, Method method) {
            if (method == null) {
                return null;
            }
            try {
                return method.invoke(target);
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                return null;
            }
        }
    }
}
