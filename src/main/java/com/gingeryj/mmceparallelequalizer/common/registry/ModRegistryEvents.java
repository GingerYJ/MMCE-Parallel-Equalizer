package com.gingeryj.mmceparallelequalizer.common.registry;

import com.gingeryj.mmceparallelequalizer.Reference;
import com.gingeryj.mmceparallelequalizer.common.component.ParallelEqualizerComponents;
import com.gingeryj.mmceparallelequalizer.common.item.ItemBlockParallelEqualizerHatch;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class ModRegistryEvents {

    private ModRegistryEvents() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(ModBlocks.PARALLEL_EQUALIZER_HATCH);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemBlockParallelEqualizerHatch(ModBlocks.PARALLEL_EQUALIZER_HATCH));
    }

    @SubscribeEvent
    public static void registerComponentTypes(RegistryEvent.Register<ComponentType> event) {
        event.getRegistry().register(ParallelEqualizerComponents.TYPE);
    }
}
