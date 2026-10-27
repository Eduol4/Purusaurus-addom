package com.purusaurusaddon.event;

import com.purusaurusaddon.PurusaurusAddon;
import com.purusaurusaddon.goal.PurusaurusFollowOwnerGoal;
import com.purusaurusaddon.goal.PurusaurusStayGoal;
import com.purusaurusaddon.taming.TamingUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Comportamento do Purusaurus adicionado por este addon, tudo via eventos do
 * Forge (sem mixins). O estado fica no persistentData da entidade.
 *
 *  - Bebes (selvagens e domesticados) e adultos domesticados sao NEUTROS: so
 *    revidam se forem atacados; o domesticado nunca revida contra o proprio dono.
 *    Adultos selvagens continuam hostis como no mod base.
 *  - Bebes tem metade do HP e do dano dos adultos.
 *  - Domesticacao so em bebes, com carnes/peixes, cada um com sua taxa.
 *  - Domesticado: os mesmos itens NAO re-domesticam; em vez disso curam (se ferido).
 *  - Shift + clique direito (qualquer item) alterna Vagar / Seguir / Ficar.
 */
@Mod.EventBusSubscriber(modid = PurusaurusAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PurusaurusEvents {

    /** Injeta as goals de Seguir/Ficar na entidade do mod base quando ela entra no mundo. */
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        if (!TamingUtil.isPurusaurus(event.getEntity()) || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        boolean already = mob.goalSelector.getAvailableGoals().stream()
                .anyMatch(w -> w.getGoal() instanceof PurusaurusStayGoal);
        if (already) {
            return;
        }
        mob.goalSelector.addGoal(0, new PurusaurusStayGoal(mob));
        mob.goalSelector.addGoal(1, new PurusaurusFollowOwnerGoal(mob));
        TamingUtil.updateBabyAttributes(mob); // bebe: metade do HP/dano
    }

    /** Mantem os atributos de bebe (metade de HP/dano) sincronizados conforme ele cresce. */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity self = event.getEntity();
        if (self.level().isClientSide || self.tickCount % 20 != 0) {
            return; // a cada 1s, so no servidor
        }
        if (TamingUtil.isPurusaurus(self) && self instanceof Mob mob) {
            TamingUtil.updateBabyAttributes(mob);
        }
    }

    /**
     * Torna o Purusaurus NEUTRO quando for bebe ou domesticado: bloqueia a mira
     * proativa no jogador, mas deixa revidar quem o atacou. O domesticado nunca
     * revida contra o proprio dono. Adulto selvagem continua hostil (sem interferencia).
     */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity self = event.getEntity();
        if (!TamingUtil.isPurusaurus(self) || !(self instanceof Mob mob)) {
            return;
        }
        if (!(event.getNewTarget() instanceof Player target)) {
            return; // so controlamos alvos que sao jogadores
        }

        boolean tamed = TamingUtil.isTamed(mob);
        boolean baby = mob.isBaby();

        // Adulto selvagem (nem bebe nem domesticado): hostil como no mod base.
        if (!tamed && !baby) {
            return;
        }

        // Domesticado nunca mira no proprio dono.
        if (tamed) {
            UUID owner = TamingUtil.getOwner(mob);
            if (owner != null && owner.equals(target.getUUID())) {
                event.setCanceled(true);
                return;
            }
        }

        // Neutro: so permite mirar no jogador se foi ele quem atacou (revide).
        boolean retaliation = mob.getLastHurtByMob() == target;
        if (!retaliation) {
            event.setCanceled(true);
        }
    }

    /** Clique direito: alterna modo (shift), cura (domesticado) ou domestica (bebe). */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!TamingUtil.isPurusaurus(event.getTarget()) || !(event.getTarget() instanceof Mob mob)) {
            return;
        }
        Player player = event.getEntity();
        boolean client = mob.level().isClientSide;

        // 1) Shift + clique (qualquer item) em domesticado -> alterna modo.
        if (player.isShiftKeyDown()) {
            if (TamingUtil.isTamed(mob)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(client));
                if (!client) {
                    TamingUtil.cycleMode(mob, player);
                }
            }
            return;
        }

        ItemStack stack = event.getItemStack();
        boolean isTamingItem = TamingUtil.tamingChance(stack.getItem()) > 0.0F;

        if (TamingUtil.isTamed(mob)) {
            // 2) Domesticado: itens de domesticacao curam (nunca re-domesticam).
            if (!isTamingItem) {
                return;
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(client));
            if (client) {
                return;
            }
            if (mob.getHealth() < mob.getMaxHealth()) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                mob.heal(TamingUtil.healAmount(stack.getItem()));
                mob.swing(InteractionHand.MAIN_HAND); // reusa a mordida como animacao de comer
            }
            return;
        }

        // 3) Nao domesticado: apenas bebes podem ser domesticados.
        if (!mob.isBaby() || !isTamingItem) {
            return;
        }
        float chance = TamingUtil.tamingChance(stack.getItem());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(client));
        if (client) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (mob.getRandom().nextFloat() < chance) {
            TamingUtil.setTamed(mob, player);
            mob.setTarget(null);
            TamingUtil.spawnTamingParticles(mob, true);   // coracoes
        } else {
            TamingUtil.spawnTamingParticles(mob, false);  // fumaca
        }
    }
}
