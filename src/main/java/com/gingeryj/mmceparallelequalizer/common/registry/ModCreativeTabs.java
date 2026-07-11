package com.gingeryj.mmceparallelequalizer.common.registry;

import com.gingeryj.mmceparallelequalizer.Reference;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public final class ModCreativeTabs {

    public static final CreativeTabs PARALLEL_EQUALIZER = new CreativeTabs(Reference.MOD_ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ModBlocks.PARALLEL_EQUALIZER_HATCH);
        }
    };

    private ModCreativeTabs() {
    }
}
