package com.purusaurusaddon.registry;

import com.purusaurusaddon.PurusaurusAddon;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PurusaurusAddon.MODID);

    public static final RegistryObject<CreativeModeTab> PURUSAURUS_TAB = TABS.register("purusaurus_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + PurusaurusAddon.MODID + ".purusaurus_tab"))
                    .icon(() -> new ItemStack(ModItems.PURUSAURUS_EGG.get()))
                    .displayItems((params, output) -> output.accept(ModItems.PURUSAURUS_EGG.get()))
                    .build());
}
