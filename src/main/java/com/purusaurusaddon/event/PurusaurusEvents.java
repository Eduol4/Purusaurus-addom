package com.purusaurusaddon.event;

import com.purusaurusaddon.PurusaurusAddon;
import com.purusaurusaddon.goal.PurusaurusFollowOwnerGoal;
import com.purusaurusaddon.goal.PurusaurusOwnerHurtTargetGoal;
import com.purusaurusaddon.goal.PurusaurusStayGoal;
import com.purusaurusaddon.registry.ModBlocks;
import com.purusaurusaddon.registry.ModItems;
import com.purusaurusaddon.swim.PurusaurusSwimController;
import com.purusaurusaddon.taming.TamingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
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
        // Domesticado ataca o que o dono ataca (menos outros pets).
        mob.targetSelector.addGoal(1, new PurusaurusOwnerHurtTargetGoal(mob));
        TamingUtil.updateBabyAttributes(mob); // bebe: metade do HP/dano

        // --- Preparo para a natacao ---
        // Deixa a navegacao entrar na agua (sem custo) e remove o "boiar" padrao,
        // para que o PurusaurusSwimController possa faze-lo mergulhar/emergir.
        mob.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
        if (mob.getNavigation() instanceof GroundPathNavigation groundNav) {
            groundNav.setCanFloat(true);
        }
        mob.goalSelector.getAvailableGoals().removeIf(w -> w.getGoal() instanceof FloatGoal);
    }

    /** Nado (todo tick) + atributos de bebe, oxigenio e regeneracao (a cada 1s). */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity self = event.getEntity();
        if (self.level().isClientSide) {
            return;
        }
        if (!TamingUtil.isPurusaurus(self) || !(self instanceof Mob mob)) {
            return;
        }

        // Todo tick: controle de nado na agua.
        PurusaurusSwimController.tick(mob);
        // Em terra, garante que a flag de animacao (usada para correr/nadar) nao
        // fique presa quando ninguem a estiver gerenciando (ex.: ao sair da agua).
        if (!mob.isInWater()) {
            boolean followRunning = mob.goalSelector.getRunningGoals()
                    .anyMatch(w -> w.getGoal() instanceof PurusaurusFollowOwnerGoal);
            if (!followRunning && mob.isAggressive()) {
                mob.setAggressive(false);
            }
        }

        // A cada 1s (no servidor):
        if (self.tickCount % 20 != 0) {
            return;
        }
        TamingUtil.updateBabyAttributes(mob); // bebe: metade do HP/dano

        // Sem oxigenio/afogamento: mantem o ar cheio (o dano tambem e cancelado).
        if (mob.getAirSupply() < mob.getMaxAirSupply()) {
            mob.setAirSupply(mob.getMaxAirSupply());
        }

        // Regeneracao natural: domesticado recupera 1 HP a cada 5 segundos.
        if (self.tickCount % 100 == 0 && TamingUtil.isTamed(mob)
                && mob.getHealth() < mob.getMaxHealth()) {
            mob.heal(1.0F);
        }
    }

    /** Sem afogamento + variacao de dano do ataque. */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // (a) Purusaurus nao se afoga.
        LivingEntity victim = event.getEntity();
        if (TamingUtil.isPurusaurus(victim) && event.getSource().is(DamageTypes.DROWN)) {
            event.setCanceled(true);
            return;
        }

        // (b) Ataque do Purusaurus adulto: 10% causa 15 de dano, 5% causa 18 (senao 12).
        Entity attacker = event.getSource().getEntity();
        if (TamingUtil.isPurusaurus(attacker) && attacker instanceof Mob mob && !mob.isBaby()) {
            float r = mob.getRandom().nextFloat();
            if (r < 0.05F) {
                event.setAmount(18.0F);
            } else if (r < 0.15F) {
                event.setAmount(15.0F);
            }
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

        // Neutro: permite mirar no jogador se (a) foi ele quem atacou o Purusaurus
        // (revide) ou (b) foi o dono quem atacou esse jogador (ataque assistido).
        boolean retaliation = mob.getLastHurtByMob() == target;
        boolean ownerAttacked = false;
        if (tamed) {
            Player owner = TamingUtil.getOwnerPlayer(mob);
            if (owner != null && owner.getLastHurtMob() == target) {
                ownerAttacked = true;
            }
        }
        if (!retaliation && !ownerAttacked) {
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
        boolean isHealItem = TamingUtil.healAmount(stack.getItem()) > 0.0F;

        if (TamingUtil.isTamed(mob)) {
            // 2) Domesticado: so as carnes que curam tem efeito (carne podre nao cura).
            if (!isHealItem) {
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
                TamingUtil.playFeedEffects(mob); // som de comer (grave) + mordida
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
        TamingUtil.playFeedEffects(mob); // som de comer (grave) + mordida
        if (mob.getRandom().nextFloat() < chance) {
            TamingUtil.setTamed(mob, player);
            mob.setTarget(null);
            TamingUtil.spawnTamingParticles(mob, true);   // coracoes
        } else {
            TamingUtil.spawnTamingParticles(mob, false);  // fumaca
        }
    }

    /**
     * Reproducao: em vez do bebe nascer direto, um Ovo de Purusaurus e posto no chao.
     * (A reproducao continua sendo acionada alimentando dois adultos com o item "meat"
     * do mod base, que ja e o alimento de acasalamento da entidade.)
     */
    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        Mob parent = event.getParentA();
        if (!TamingUtil.isPurusaurus(parent)) {
            return;
        }
        event.setCanceled(true); // nao nasce bebe; o Forge zera o "amor" dos pais
        layEgg(parent);
    }

    /** Chance de 1.5% de um Purusaurus adulto SELVAGEM dropar o ovo como item pegavel. */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity self = event.getEntity();
        if (!TamingUtil.isPurusaurus(self) || !(self instanceof Mob mob)) {
            return;
        }
        if (mob.isBaby() || TamingUtil.isTamed(mob) || !event.isRecentlyHit()) {
            return; // so adulto selvagem, morto por jogador
        }
        if (mob.getRandom().nextFloat() < 0.015F) {
            ItemStack egg = new ItemStack(ModItems.PURUSAURUS_EGG.get());
            ItemEntity drop = new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), egg);
            drop.setDefaultPickUpDelay();
            event.getDrops().add(drop);
        }
    }

    /** Coloca um Ovo de Purusaurus no chao proximo ao pai. */
    private static void layEgg(Mob parent) {
        Level level = parent.level();
        if (level.isClientSide) {
            return;
        }
        BlockState egg = ModBlocks.PURUSAURUS_EGG.get().defaultBlockState();
        BlockPos origin = parent.blockPosition();
        BlockPos[] candidates = {
                origin,
                origin.north(), origin.south(), origin.east(), origin.west()
        };
        for (BlockPos pos : candidates) {
            if (level.getBlockState(pos).isAir() && egg.canSurvive(level, pos)) {
                level.setBlock(pos, egg, 3);
                level.playSound(null, pos, SoundEvents.TURTLE_LAY_EGG, SoundSource.BLOCKS,
                        0.3F, 0.9F + parent.getRandom().nextFloat() * 0.2F);
                return;
            }
        }
    }
}
