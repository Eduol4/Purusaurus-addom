package com.purusaurusaddon.registry;

import com.purusaurusaddon.PurusaurusAddon;
import com.purusaurusaddon.block.entity.PurusaurusEggBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PurusaurusAddon.MODID);

    public static final RegistryObject<BlockEntityType<PurusaurusEggBlockEntity>> PURUSAURUS_EGG =
            BLOCK_ENTITIES.register("purusaurus_egg",
                    () -> BlockEntityType.Builder
                            .of(PurusaurusEggBlockEntity::new, ModBlocks.PURUSAURUS_EGG.get())
                            .build(null));
}
