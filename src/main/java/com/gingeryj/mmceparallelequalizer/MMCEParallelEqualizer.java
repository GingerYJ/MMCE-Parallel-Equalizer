package com.gingeryj.mmceparallelequalizer;

import com.gingeryj.mmceparallelequalizer.common.gui.GuiHandler;
import com.gingeryj.mmceparallelequalizer.common.tile.TileParallelEqualizerHatch;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = Reference.MOD_ID,
    name = Reference.MOD_NAME,
    version = Reference.VERSION,
    dependencies = "required-after:modularmachinery@[2.3.2,)",
    acceptedMinecraftVersions = "[1.12.2]"
)
public final class MMCEParallelEqualizer {

    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_NAME);

    @Mod.Instance(Reference.MOD_ID)
    public static MMCEParallelEqualizer INSTANCE;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerTileEntity(
            TileParallelEqualizerHatch.class,
            new ResourceLocation(Reference.MOD_ID, "parallel_equalizer_hatch")
        );
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
    }
}
