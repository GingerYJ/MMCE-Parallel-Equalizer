package com.gingeryj.mmceparallelequalizer.common.block;

import com.gingeryj.mmceparallelequalizer.MMCEParallelEqualizer;
import com.gingeryj.mmceparallelequalizer.Reference;
import com.gingeryj.mmceparallelequalizer.common.gui.GuiHandler;
import com.gingeryj.mmceparallelequalizer.common.registry.ModCreativeTabs;
import com.gingeryj.mmceparallelequalizer.common.tile.TileParallelEqualizerHatch;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class BlockParallelEqualizerHatch extends BlockContainer {

    public static final String NAME = "parallel_equalizer_hatch";

    public BlockParallelEqualizerHatch() {
        super(Material.IRON);
        setHardness(5.0F);
        setResistance(10.0F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 1);
        setCreativeTab(ModCreativeTabs.PARALLEL_EQUALIZER);
        setRegistryName(Reference.MOD_ID, NAME);
        setTranslationKey(Reference.MOD_ID + "." + NAME);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return true;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return true;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(MMCEParallelEqualizer.INSTANCE, GuiHandler.PARALLEL_EQUALIZER_GUI_ID,
                world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileParallelEqualizerHatch();
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileParallelEqualizerHatch();
    }
}
