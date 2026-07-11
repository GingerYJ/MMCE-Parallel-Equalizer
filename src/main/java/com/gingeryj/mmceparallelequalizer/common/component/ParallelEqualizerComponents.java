package com.gingeryj.mmceparallelequalizer.common.component;

import com.gingeryj.mmceparallelequalizer.Reference;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import net.minecraft.util.ResourceLocation;

public final class ParallelEqualizerComponents {

    public static final ResourceLocation TYPE_ID = new ResourceLocation(Reference.MOD_ID, "parallel_equalizer");
    public static final ComponentType TYPE = new ParallelEqualizerComponentType().setRegistryName(TYPE_ID);

    private ParallelEqualizerComponents() {
    }
}
