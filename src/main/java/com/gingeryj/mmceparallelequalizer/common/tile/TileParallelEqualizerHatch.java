package com.gingeryj.mmceparallelequalizer.common.tile;

import com.gingeryj.mmceparallelequalizer.common.component.ParallelEqualizerComponents;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.event.machine.MachineStructureFormedEvent;
import github.kasuminova.mmce.common.event.machine.MachineStructureUpdateEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeStartEvent;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTileNotifiable;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public final class TileParallelEqualizerHatch extends TileColorableMachineComponent
    implements MachineComponentTileNotifiable, ITickable {

    private static final String NBT_BOUND_CONTROLLER = "boundController";
    private static final String NBT_BOUND_MACHINE = "boundMachine";

    private boolean bound;
    private long boundController;
    private String boundMachine = "";
    private int bindingCheckTicks;

    private final MachineComponent<TileParallelEqualizerHatch> component = new MachineComponent<TileParallelEqualizerHatch>(IOType.INPUT) {
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
        if (event instanceof MachineStructureFormedEvent || event instanceof MachineStructureUpdateEvent) {
            bindMachine(event.getController());
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

        int quota = getParallelismQuota(startEvent.getController());
        if (quota < 1) {
            return;
        }
        context.setParallelism(Math.min(activeRecipe.getParallelism(), quota));
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        if (++bindingCheckTicks >= 20) {
            bindingCheckTicks = 0;
            getBoundController();
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        bound = compound.hasKey(NBT_BOUND_CONTROLLER);
        boundController = bound ? compound.getLong(NBT_BOUND_CONTROLLER) : 0L;
        boundMachine = compound.getString(NBT_BOUND_MACHINE);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        if (bound) {
            compound.setLong(NBT_BOUND_CONTROLLER, boundController);
            compound.setString(NBT_BOUND_MACHINE, boundMachine);
        }
    }

    public String getBoundMachineName() {
        return boundMachine;
    }

    public MachineStats getMachineStats() {
        Object controller = getBoundController();
        if (controller == null) {
            return MachineStats.UNBOUND;
        }

        FactoryStats factoryStats = getFactoryStats(controller);
        if (factoryStats == null) {
            return new MachineStats(true, 0, getMaxParallelism(controller), 0);
        }
        return new MachineStats(true, factoryStats.threadSlots, factoryStats.maxParallelism, factoryStats.quota);
    }

    private void bindMachine(Object controller) {
        if (world == null || world.isRemote || !isStructureFormed(controller)) {
            return;
        }

        BlockPos controllerPos = getControllerPosition(controller);
        String machineName = getMachineName(controller);
        if (controllerPos == null || machineName.isEmpty()) {
            return;
        }

        long packedControllerPos = controllerPos.toLong();
        if (bound && boundController == packedControllerPos && boundMachine.equals(machineName)) {
            return;
        }

        bound = true;
        boundController = packedControllerPos;
        boundMachine = machineName;
        markForUpdateSync();
    }

    @Nullable
    private Object getBoundController() {
        if (!bound || world == null || !world.isBlockLoaded(BlockPos.fromLong(boundController))) {
            return null;
        }

        TileEntity tileEntity = world.getTileEntity(BlockPos.fromLong(boundController));
        if (tileEntity != null && isStructureFormed(tileEntity)) {
            return tileEntity;
        }

        if (!world.isRemote) {
            clearBinding();
        }
        return null;
    }

    private void clearBinding() {
        if (!bound) {
            return;
        }
        bound = false;
        boundController = 0L;
        boundMachine = "";
        markForUpdateSync();
    }

    private static boolean isStructureFormed(Object controller) {
        return Boolean.TRUE.equals(invoke(controller, "isStructureFormed"));
    }

    @Nullable
    private static BlockPos getControllerPosition(Object controller) {
        Object position = invoke(controller, "getPos");
        return position instanceof BlockPos ? (BlockPos) position : null;
    }

    private static String getMachineName(Object controller) {
        Object machine = invoke(controller, "getFoundMachine");
        Object registryName = machine == null ? null : invoke(machine, "getRegistryName");
        return registryName == null ? "" : registryName.toString();
    }

    private static int getMaxParallelism(Object controller) {
        Object value = invoke(controller, "getMaxParallelism");
        return value instanceof Integer ? Math.max(0, (Integer) value) : 0;
    }

    @Nullable
    private static Object invoke(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
            return null;
        }
    }

    private static int getParallelismQuota(Object factory) {
        FactoryStats stats = getFactoryStats(factory);
        return stats == null ? -1 : stats.quota;
    }

    @Nullable
    private static FactoryStats getFactoryStats(Object factory) {
        try {
            // MMCE's factory hierarchy references GeckoLib, which is supplied by the target modpack.
            int maxThreads = (Integer) factory.getClass().getMethod("getMaxThreads").invoke(factory);
            Object coreThreads = factory.getClass().getMethod("getCoreRecipeThreads").invoke(factory);
            int maxParallelism = (Integer) factory.getClass().getMethod("getMaxParallelism").invoke(factory);
            if (!(coreThreads instanceof Map)) {
                return null;
            }

            int threadSlots = Math.max(1, maxThreads + ((Map<?, ?>) coreThreads).size());
            return new FactoryStats(threadSlots, Math.max(0, maxParallelism), Math.max(1, maxParallelism / threadSlots));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
            return null;
        }
    }

    public static final class MachineStats {
        public static final MachineStats UNBOUND = new MachineStats(false, 0, 0, 0);

        private final boolean bound;
        private final int threads;
        private final int parallelism;
        private final int equalizedParallelism;

        private MachineStats(boolean bound, int threads, int parallelism, int equalizedParallelism) {
            this.bound = bound;
            this.threads = threads;
            this.parallelism = parallelism;
            this.equalizedParallelism = equalizedParallelism;
        }

        public boolean isBound() {
            return bound;
        }

        public int getThreads() {
            return threads;
        }

        public int getParallelism() {
            return parallelism;
        }

        public int getEqualizedParallelism() {
            return equalizedParallelism;
        }
    }

    private static final class FactoryStats {
        private final int threadSlots;
        private final int maxParallelism;
        private final int quota;

        private FactoryStats(int threadSlots, int maxParallelism, int quota) {
            this.threadSlots = threadSlots;
            this.maxParallelism = maxParallelism;
            this.quota = quota;
        }
    }
}
