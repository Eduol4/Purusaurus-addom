package com.purusaurusaddon.registry;

import com.purusaurusaddon.PurusaurusAddon;
import com.purusaurusaddon.block.PurusaurusEggBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, PurusaurusAddon.MODID);

    public static final RegistryObject<Block> PURUSAURUS_EGG = BLOCKS.register("purusaurus_egg",
            () -> new PurusaurusEggBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
}
