package com.purusaurusaddon;

import com.mojang.logging.LogUtils;
import com.purusaurusaddon.registry.ModBlockEntities;
import com.purusaurusaddon.registry.ModBlocks;
import com.purusaurusaddon.registry.ModCreativeTabs;
import com.purusaurusaddon.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Ponto de entrada do addon.
 *
 * Este mod NAO redistribui nenhum codigo nem asset do Unearthed Journey.
 * Ele apenas registra conteudo proprio (o Ovo de Purusaurus) e, na hora de
 * chocar, procura a entidade do Purusaurus pelo seu ID de registro
 * ({@link #PURUSAURUS_ID}). Se o Unearthed Journey nao estiver instalado,
 * o ovo simplesmente nao choca (e um aviso e registrado no log).
 */
@Mod(PurusaurusAddon.MODID)
public class PurusaurusAddon {

    public static final String MODID = "purusaurusaddon";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** ID de registro da entidade Purusaurus dentro do mod Unearthed Journey. */
    public static final ResourceLocation PURUSAURUS_ID =
            new ResourceLocation("unearthed_journey", "purussaurus_brasiliensis");

    public PurusaurusAddon() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);

        LOGGER.info("Purusaurus Addon carregado.");
    }
}
