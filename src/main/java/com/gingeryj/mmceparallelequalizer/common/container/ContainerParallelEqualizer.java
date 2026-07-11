package com.gingeryj.mmceparallelequalizer.common.container;

import com.gingeryj.mmceparallelequalizer.common.tile.TileParallelEqualizerHatch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public final class ContainerParallelEqualizer extends Container {

    private static final int PROPERTY_BOUND = 0;
    private static final int PROPERTY_THREADS_LOW = 1;
    private static final int PROPERTY_THREADS_HIGH = 2;
    private static final int PROPERTY_PARALLELISM_LOW = 3;
    private static final int PROPERTY_PARALLELISM_HIGH = 4;
    private static final int PROPERTY_QUOTA_LOW = 5;
    private static final int PROPERTY_QUOTA_HIGH = 6;
    private static final int PROPERTY_MACHINE_NAME_LENGTH = 7;
    private static final int PROPERTY_MACHINE_NAME_START = 8;
    private static final int MAX_MACHINE_NAME_LENGTH = 128;

    private final TileParallelEqualizerHatch equalizer;

    private boolean bound;
    private int threads;
    private int parallelism;
    private int equalizedParallelism;
    private String machineName = "";
    private char[] pendingMachineName = new char[0];

    private boolean sentInitialData;
    private boolean lastBound;
    private int lastThreads;
    private int lastParallelism;
    private int lastEqualizedParallelism;
    private String lastMachineName = "";

    public ContainerParallelEqualizer(TileParallelEqualizerHatch equalizer, EntityPlayer player) {
        this.equalizer = equalizer;
        addPlayerSlots(player);
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        sendSnapshot(listener, equalizer.getMachineStats(), equalizer.getBoundMachineName());
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        TileParallelEqualizerHatch.MachineStats stats = equalizer.getMachineStats();
        String currentMachineName = equalizer.getBoundMachineName();
        if (!sentInitialData
            || lastBound != stats.isBound()
            || lastThreads != stats.getThreads()
            || lastParallelism != stats.getParallelism()
            || lastEqualizedParallelism != stats.getEqualizedParallelism()
            || !lastMachineName.equals(currentMachineName)) {
            for (IContainerListener listener : listeners) {
                sendSnapshot(listener, stats, currentMachineName);
            }
            sentInitialData = true;
            lastBound = stats.isBound();
            lastThreads = stats.getThreads();
            lastParallelism = stats.getParallelism();
            lastEqualizedParallelism = stats.getEqualizedParallelism();
            lastMachineName = currentMachineName;
        }
    }

    private void sendSnapshot(IContainerListener listener, TileParallelEqualizerHatch.MachineStats stats,
                              String currentMachineName) {
        sendMachineStats(listener, stats);
        int length = Math.min(currentMachineName.length(), MAX_MACHINE_NAME_LENGTH);
        listener.sendWindowProperty(this, PROPERTY_MACHINE_NAME_LENGTH, length);
        for (int index = 0; index < length; index++) {
            listener.sendWindowProperty(this, PROPERTY_MACHINE_NAME_START + index, currentMachineName.charAt(index));
        }
    }

    private void sendMachineStats(IContainerListener listener, TileParallelEqualizerHatch.MachineStats stats) {
        listener.sendWindowProperty(this, PROPERTY_BOUND, stats.isBound() ? 1 : 0);
        sendInt(listener, PROPERTY_THREADS_LOW, stats.getThreads());
        sendInt(listener, PROPERTY_PARALLELISM_LOW, stats.getParallelism());
        sendInt(listener, PROPERTY_QUOTA_LOW, stats.getEqualizedParallelism());
    }

    private void sendInt(IContainerListener listener, int lowProperty, int value) {
        listener.sendWindowProperty(this, lowProperty, value & 0xFFFF);
        listener.sendWindowProperty(this, lowProperty + 1, value >>> 16 & 0xFFFF);
    }

    @Override
    public void updateProgressBar(int id, int data) {
        if (id == PROPERTY_MACHINE_NAME_LENGTH) {
            int length = Math.max(0, Math.min(data, MAX_MACHINE_NAME_LENGTH));
            pendingMachineName = new char[length];
            if (length == 0) {
                machineName = "";
            }
            return;
        }
        if (id >= PROPERTY_MACHINE_NAME_START
            && id < PROPERTY_MACHINE_NAME_START + pendingMachineName.length) {
            int index = id - PROPERTY_MACHINE_NAME_START;
            pendingMachineName[index] = (char) (data & 0xFFFF);
            if (index == pendingMachineName.length - 1) {
                machineName = new String(pendingMachineName);
            }
            return;
        }

        switch (id) {
            case PROPERTY_BOUND:
                bound = data != 0;
                break;
            case PROPERTY_THREADS_LOW:
            case PROPERTY_THREADS_HIGH:
                threads = updateInt(threads, id == PROPERTY_THREADS_LOW, data);
                break;
            case PROPERTY_PARALLELISM_LOW:
            case PROPERTY_PARALLELISM_HIGH:
                parallelism = updateInt(parallelism, id == PROPERTY_PARALLELISM_LOW, data);
                break;
            case PROPERTY_QUOTA_LOW:
            case PROPERTY_QUOTA_HIGH:
                equalizedParallelism = updateInt(equalizedParallelism, id == PROPERTY_QUOTA_LOW, data);
                break;
            default:
                break;
        }
    }

    private static int updateInt(int currentValue, boolean lowPart, int update) {
        int value = update & 0xFFFF;
        return lowPart ? currentValue & 0xFFFF0000 | value : currentValue & 0xFFFF | value << 16;
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

    public String getMachineName() {
        return machineName;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return !equalizer.isInvalid() && player.getDistanceSq(equalizer.getPos()) <= 64.0D;
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
        return ItemStack.EMPTY;
    }

    private void addPlayerSlots(EntityPlayer player) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(player.inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(player.inventory, column, 8 + column * 18, 142));
        }
    }
}
