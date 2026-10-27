package com.purusaurusaddon.event;

import com.purusaurusaddon.PurusaurusAddon;
import com.purusaurusaddon.taming.TamingUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Comportamento do Purusaurus adicionado por este addon, tudo via eventos do
 * Forge (sem mixins / sem mexer na classe compilada do mod base):
 *
 *  - Bebe sempre passivo ao jogador; domesticado continua passivo mesmo adulto.
 *  - Domesticacao so em bebes, clicando com o botao direito usando carnes/peixes,
 *    cada item com sua taxa de sucesso, com particulas iguais as do lobo.
 */
@Mod.EventBusSubscriber(modid = PurusaurusAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PurusaurusEvents {

    /** Impede que o Purusaurus mire no jogador quando for bebe ou domesticado. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity self = event.getEntity();
        if (!TamingUtil.isPurusaurus(self)) {
            return;
        }
        if (!(event.getNewAboutToBeSetTarget() instanceof Player)) {
            return;
        }
        boolean baby = self instanceof Mob mob && mob.isBaby();
        if (baby || TamingUtil.isTamed(self)) {
            // Cancelar mantem o alvo atual (nenhum), entao ele nao persegue o jogador.
            event.setCanceled(true);
        }
    }

    /** Domesticacao ao clicar com o botao direito num bebe usando um item valido. */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!TamingUtil.isPurusaurus(event.getTarget())) {
            return;
        }
        if (!(event.getTarget() instanceof Mob mob)) {
            return;
        }
        // Somente bebes, e que ainda nao foram domesticados.
        if (!mob.isBaby() || TamingUtil.isTamed(mob)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        float chance = TamingUtil.tamingChance(stack.getItem());
        if (chance <= 0.0F) {
            return; // item nao serve para domesticar: deixa a interacao normal seguir
        }

        Player player = event.getEntity();

        // A partir daqui a interacao e nossa (evita comer/criar com o item base).
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(mob.level().isClientSide));

        if (mob.level().isClientSide) {
            return; // logica apenas no servidor
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        boolean success = mob.getRandom().nextFloat() < chance;
        if (success) {
            TamingUtil.setTamed(mob, player);
            mob.setTarget(null);
            TamingUtil.spawnTamingParticles(mob, true);   // coracoes
        } else {
            TamingUtil.spawnTamingParticles(mob, false);  // fumaca
        }
    }
}
