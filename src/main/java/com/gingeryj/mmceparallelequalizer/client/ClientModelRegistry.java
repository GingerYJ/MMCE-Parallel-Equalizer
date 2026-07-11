package com.gingeryj.mmceparallelequalizer.client;

import com.gingeryj.mmceparallelequalizer.Reference;
import com.gingeryj.mmceparallelequalizer.common.block.BlockParallelEqualizerHatch;
import com.gingeryj.mmceparallelequalizer.common.registry.ModBlocks;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Side.CLIENT)
public final class ClientModelRegistry {

    private ClientModelRegistry() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(
            Item.getItemFromBlock(ModBlocks.PARALLEL_EQUALIZER_HATCH),
            0,
            new ModelResourceLocation(Reference.MOD_ID + ":" + BlockParallelEqualizerHatch.NAME, "inventory")
        );
    }
}
