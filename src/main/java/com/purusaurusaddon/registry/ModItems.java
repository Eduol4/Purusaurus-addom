package com.purusaurusaddon.registry;

import com.purusaurusaddon.PurusaurusAddon;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, PurusaurusAddon.MODID);

    // Item do bloco: colocado no mundo vira o ovo. Nao empilha com outros ovos
    // no mesmo bloco porque cada ovo e um bloco inteiro proprio.
    public static final RegistryObject<Item> PURUSAURUS_EGG = ITEMS.register("purusaurus_egg",
            () -> new BlockItem(ModBlocks.PURUSAURUS_EGG.get(), new Item.Properties()));
}
