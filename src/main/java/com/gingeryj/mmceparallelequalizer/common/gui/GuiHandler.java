package com.gingeryj.mmceparallelequalizer.common.gui;

import com.gingeryj.mmceparallelequalizer.client.gui.GuiParallelEqualizer;
import com.gingeryj.mmceparallelequalizer.common.container.ContainerParallelEqualizer;
import com.gingeryj.mmceparallelequalizer.common.tile.TileParallelEqualizerHatch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

public final class GuiHandler implements IGuiHandler {

    public static final int PARALLEL_EQUALIZER_GUI_ID = 0;

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));
        if (id == PARALLEL_EQUALIZER_GUI_ID && tileEntity instanceof TileParallelEqualizerHatch) {
            return new ContainerParallelEqualizer((TileParallelEqualizerHatch) tileEntity, player);
        }
        return null;
    }

    @Nullable
    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));
        if (id == PARALLEL_EQUALIZER_GUI_ID && tileEntity instanceof TileParallelEqualizerHatch) {
            TileParallelEqualizerHatch equalizer = (TileParallelEqualizerHatch) tileEntity;
            return new GuiParallelEqualizer(new ContainerParallelEqualizer(equalizer, player));
        }
        return null;
    }
}
