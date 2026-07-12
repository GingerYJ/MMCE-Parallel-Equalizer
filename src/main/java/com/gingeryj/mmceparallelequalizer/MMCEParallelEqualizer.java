package com.gingeryj.mmceparallelequalizer;

import com.gingeryj.mmceparallelequalizer.common.tile.TileParallelEqualizerHatch;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(
    modid = Reference.MOD_ID,
    name = Reference.MOD_NAME,
    version = Reference.VERSION,
    dependencies = "required-after:modularmachinery@[2.3.2,)",
    acceptedMinecraftVersions = "[1.12.2]"
)
public final class MMCEParallelEqualizer {

    @Mod.Instance(Reference.MOD_ID)
    public static MMCEParallelEqualizer INSTANCE;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerTileEntity(
            TileParallelEqualizerHatch.class,
            new ResourceLocation(Reference.MOD_ID, "parallel_equalizer_hatch")
        );
    }
}
