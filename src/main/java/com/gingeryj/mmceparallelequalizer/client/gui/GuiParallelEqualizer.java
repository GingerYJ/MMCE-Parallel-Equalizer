package com.gingeryj.mmceparallelequalizer.client.gui;

import com.gingeryj.mmceparallelequalizer.common.container.ContainerParallelEqualizer;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public final class GuiParallelEqualizer extends GuiContainer {

    private static final ResourceLocation BACKGROUND =
        new ResourceLocation("modularmachinery", "textures/gui/guismartinterface.png");

    private final ContainerParallelEqualizer container;

    public GuiParallelEqualizer(ContainerParallelEqualizer container) {
        super(container);
        this.container = container;
        xSize = 176;
        ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.mmceparallelequalizer.parallel_equalizer.title"), 8, 7, 0x404040);

        String machine = container.isBound()
            ? getLocalizedMachineName(container.getMachineName())
            : I18n.format("gui.mmceparallelequalizer.parallel_equalizer.unbound");
        drawLabel("gui.mmceparallelequalizer.parallel_equalizer.machine", machine, 8, 22);
        drawLabel("gui.mmceparallelequalizer.parallel_equalizer.threads", formatFactoryNumber(container.getThreads()), 8, 35);
        drawLabel("gui.mmceparallelequalizer.parallel_equalizer.parallelism", formatNumber(container.getParallelism()), 8, 48);
        drawLabel("gui.mmceparallelequalizer.parallel_equalizer.quota", formatFactoryNumber(container.getEqualizedParallelism()), 8, 61);

        fontRenderer.drawString(I18n.format("container.inventory"), 8, 73, 0x404040);
    }

    private void drawLabel(String key, String value, int x, int y) {
        String label = I18n.format(key, value);
        fontRenderer.drawString(trimToWidth(label, xSize - x - 8), x, y, 0x404040);
    }

    private String formatNumber(int value) {
        return container.isBound() ? Integer.toString(value) : "-";
    }

    private String formatFactoryNumber(int value) {
        return container.isBound() && value > 0 ? Integer.toString(value) : "-";
    }

    private String getLocalizedMachineName(String registryName) {
        if (registryName.isEmpty()) {
            return I18n.format("gui.mmceparallelequalizer.parallel_equalizer.unbound");
        }
        try {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(registryName));
            return machine == null ? registryName : machine.getLocalizedName();
        } catch (RuntimeException ignored) {
            return registryName;
        }
    }

    private String trimToWidth(String text, int maxWidth) {
        if (fontRenderer.getStringWidth(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        return fontRenderer.trimStringToWidth(text, maxWidth - fontRenderer.getStringWidth(suffix)) + suffix;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int left = (width - xSize) / 2;
        int top = (height - ySize) / 2;
        mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(left, top, 0, 0, xSize, ySize);
    }
}
