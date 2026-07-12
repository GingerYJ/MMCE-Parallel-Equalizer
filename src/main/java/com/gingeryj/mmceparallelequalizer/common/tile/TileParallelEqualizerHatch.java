package com.gingeryj.mmceparallelequalizer.common.tile;

import com.gingeryj.mmceparallelequalizer.common.component.ParallelEqualizerComponents;
import github.kasuminova.mmce.common.event.Phase;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.event.machine.MachineStructureFormedEvent;
import github.kasuminova.mmce.common.event.machine.MachineStructureUpdateEvent;
import github.kasuminova.mmce.common.event.machine.MachineTickEvent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class TileParallelEqualizerHatch extends TileColorableMachineComponent
    implements MachineComponentTileNotifiable {

    private static final int CHECK_INTERVAL_TICKS = 10;
    private static final Map<ActiveMachineRecipe, Integer> NATURAL_PARALLELISM = new WeakHashMap<>();
    private static final Map<Object, AllocationState> ALLOCATION_STATES = new WeakHashMap<>();

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
        if (event instanceof MachineTickEvent) {
            Object controller = event.getController();
            Integer ticksExisted = getOptionalInt(controller, "getTicksExisted");
            if (((MachineTickEvent) event).phase == Phase.END
                && ticksExisted != null && ticksExisted % CHECK_INTERVAL_TICKS == 0) {
                equalizeIfNeeded(controller);
            }
            return;
        }

        if (event instanceof MachineStructureFormedEvent || event instanceof MachineStructureUpdateEvent) {
            markDirty(event.getController());
            return;
        }

        if (event instanceof FactoryRecipeFinishEvent) {
            ActiveMachineRecipe activeRecipe = ((FactoryRecipeFinishEvent) event).getActiveRecipe();
            if (activeRecipe != null) {
                NATURAL_PARALLELISM.remove(activeRecipe);
            }
            markDirty(event.getController());
            return;
        }

        if (!(event instanceof FactoryRecipeStartEvent)) {
            return;
        }

        FactoryRecipeStartEvent startEvent = (FactoryRecipeStartEvent) event;
        RecipeCraftingContext context = startEvent.getContext();
        ActiveMachineRecipe activeRecipe = startEvent.getActiveRecipe();
        if (context == null || activeRecipe == null) {
            return;
        }

        markDirty(startEvent.getController());
        NATURAL_PARALLELISM.putIfAbsent(activeRecipe, Math.max(1, activeRecipe.getParallelism()));

        int quota = getStartupQuota(startEvent.getController());
        if (quota < 1) {
            return;
        }

        context.setParallelism(Math.min(Math.max(1, activeRecipe.getParallelism()), quota));
    }

    /**
     * Keep enough parallelism available for other threads while MMCE is starting
     * recipes one at a time. The final allocation is performed at tick END,
     * after all threads that could start this tick have been discovered.
     */
    private static int getStartupQuota(Object factory) {
        try {
            int maxThreads = getInt(factory, "getMaxThreads");
            Object coreThreads = invoke(factory, "getCoreRecipeThreads");
            int maxParallelism = getInt(factory, "getMaxParallelism");
            if (!(coreThreads instanceof Map)) {
                return -1;
            }

            int threadSlots = Math.max(1, maxThreads + ((Map<?, ?>) coreThreads).size());
            return Math.max(1, Math.max(0, maxParallelism) / threadSlots);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassCastException ignored) {
            return -1;
        }
    }

    /**
     * Reallocate the complete machine budget among threads with an active
     * recipe. This uses the active list instead of the configured thread cap,
     * so idle slots do not reduce the share of working threads. Integer
     * remainders are handed out one by one, and a thread's recipe limit is
     * respected before any remaining budget is offered to later threads.
     */
    private static void equalizeIfNeeded(Object factory) {
        Integer maxParallelism = getOptionalInt(factory, "getMaxParallelism");
        if (maxParallelism == null) {
            return;
        }

        boolean shouldEqualize;
        synchronized (ALLOCATION_STATES) {
            AllocationState state = ALLOCATION_STATES.computeIfAbsent(factory, ignored -> new AllocationState());
            shouldEqualize = state.dirty || state.lastMaxParallelism != maxParallelism;
            state.dirty = false;
            state.lastMaxParallelism = maxParallelism;
        }

        if (shouldEqualize) {
            equalizeActiveThreads(factory, Math.max(0, maxParallelism));
        }
    }

    private static void markDirty(Object factory) {
        synchronized (ALLOCATION_STATES) {
            ALLOCATION_STATES.computeIfAbsent(factory, ignored -> new AllocationState()).dirty = true;
        }
    }

    private static void equalizeActiveThreads(Object factory, int maxParallelism) {
        Object value = invoke(factory, "getRecipeThreadList");
        if (!(value instanceof Object[])) {
            return;
        }

        List<ActiveThread> activeThreads = new ArrayList<>();
        for (Object thread : (Object[]) value) {
            if (thread == null) {
                continue;
            }

            Object recipeValue = invoke(thread, "getActiveRecipe");
            if (!(recipeValue instanceof ActiveMachineRecipe)) {
                continue;
            }

            Object contextValue = invoke(thread, "getContext");
            RecipeCraftingContext context = contextValue instanceof RecipeCraftingContext
                ? (RecipeCraftingContext) contextValue : null;
            activeThreads.add(new ActiveThread((ActiveMachineRecipe) recipeValue, context));
        }

        if (activeThreads.isEmpty()) {
            return;
        }

        int remaining = maxParallelism;
        for (int i = 0; i < activeThreads.size(); i++) {
            ActiveThread activeThread = activeThreads.get(i);
            int threadsLeft = activeThreads.size() - i;
            int fairShare = Math.max(1, (remaining + threadsLeft - 1) / threadsLeft);
            int recipeLimit = NATURAL_PARALLELISM.getOrDefault(
                activeThread.recipe, Math.max(1, activeThread.recipe.getMaxParallelism()));
            int allocation = Math.min(fairShare, recipeLimit);

            if (activeThread.context != null) {
                activeThread.context.setParallelism(allocation);
            } else {
                activeThread.recipe.setParallelism(allocation);
            }
            remaining = Math.max(0, remaining - allocation);
        }
    }

    private static int getInt(Object target, String methodName)
        throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object value = invokeRequired(target, methodName);
        return value instanceof Integer ? (Integer) value : -1;
    }

    private static Integer getOptionalInt(Object target, String methodName) {
        try {
            Object value = invokeRequired(target, methodName);
            return value instanceof Integer ? (Integer) value : null;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Object invoke(Object target, String methodName) {
        try {
            return invokeRequired(target, methodName);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Object invokeRequired(Object target, String methodName)
        throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private static final class ActiveThread {
        private final ActiveMachineRecipe recipe;
        private final RecipeCraftingContext context;

        private ActiveThread(ActiveMachineRecipe recipe, RecipeCraftingContext context) {
            this.recipe = recipe;
            this.context = context;
        }
    }

    private static final class AllocationState {
        private boolean dirty = true;
        private int lastMaxParallelism = Integer.MIN_VALUE;
    }
}
