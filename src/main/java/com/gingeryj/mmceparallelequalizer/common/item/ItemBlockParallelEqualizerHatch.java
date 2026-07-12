package com.gingeryj.mmceparallelequalizer.common.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public final class ItemBlockParallelEqualizerHatch extends ItemBlock {

    public ItemBlockParallelEqualizerHatch(Block block) {
        super(block);
        setRegistryName(block.getRegistryName());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, net.minecraft.client.util.ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(I18n.translateToLocal("tooltip.mmceparallelequalizer.parallel_equalizer_hatch"));
    }
}
