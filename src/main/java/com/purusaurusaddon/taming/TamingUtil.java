package com.purusaurusaddon.taming;

import com.purusaurusaddon.PurusaurusAddon;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;

/**
 * Regras e dados da domesticacao do Purusaurus.
 *
 * O estado (domesticado, dono e modo) e guardado no
 * {@link Entity#getPersistentData()} da propria entidade do Unearthed Journey
 * (tag ForgeData), entao nao precisamos modificar a classe compilada do mod
 * base: o dado e salvo/carregado junto com a entidade automaticamente.
 */
public final class TamingUtil {

    private TamingUtil() {}

    public static final String TAMED_KEY = "purusaurusaddon:Tamed";
    public static final String OWNER_KEY = "purusaurusaddon:Owner";
    public static final String MODE_KEY = "purusaurusaddon:Mode";
    public static final String STAY_Y_KEY = "purusaurusaddon:StayY"; // profundidade travada no modo ficar

    // Modos de comportamento do Purusaurus domesticado.
    public static final int MODE_WANDER = 0; // vaga como os selvagens
    public static final int MODE_FOLLOW = 1; // segue o dono (com teleporte)
    public static final int MODE_STAY = 2;   // fica parado

    /** Item de domesticacao -> taxa de sucesso (0..1). */
    public static final Map<Item, Float> TAMING_ITEMS = Map.ofEntries(
            Map.entry(Items.BEEF, 0.30F),           // carne de vaca
            Map.entry(Items.PORKCHOP, 0.25F),       // carne de porco
            Map.entry(Items.MUTTON, 0.20F),         // carne de ovelha
            Map.entry(Items.RABBIT, 0.15F),         // carne de coelho
            Map.entry(Items.CHICKEN, 0.10F),        // carne de frango
            Map.entry(Items.SALMON, 0.05F),         // salmao
            Map.entry(Items.COD, 0.025F),           // bacalhau
            Map.entry(Items.TROPICAL_FISH, 0.015F), // peixe tropical
            Map.entry(Items.ROTTEN_FLESH, 0.01F)    // carne podre (so domestica, nao cura)
    );

    /** Item -> pontos de cura (ao alimentar um Purusaurus domesticado ferido). */
    public static final Map<Item, Float> HEAL_AMOUNTS = Map.ofEntries(
            Map.entry(Items.BEEF, 25.0F),
            Map.entry(Items.PORKCHOP, 24.0F),
            Map.entry(Items.MUTTON, 23.0F),
            Map.entry(Items.RABBIT, 18.0F),
            Map.entry(Items.CHICKEN, 17.0F),
            Map.entry(Items.SALMON, 8.0F),
            Map.entry(Items.COD, 7.0F),
            Map.entry(Items.TROPICAL_FISH, 6.0F)
    );

    /** Chance de domesticar com este item, ou 0 se o item nao serve. */
    public static float tamingChance(Item item) {
        return TAMING_ITEMS.getOrDefault(item, 0.0F);
    }

    /** Pontos de cura deste item, ou 0 se o item nao cura. */
    public static float healAmount(Item item) {
        return HEAL_AMOUNTS.getOrDefault(item, 0.0F);
    }

    // Tipo da entidade do Purusaurus, resolvido uma vez (comparacao por referencia e barata).
    private static EntityType<?> purusaurusType;

    /** True se a entidade for o Purusaurus do Unearthed Journey. */
    public static boolean isPurusaurus(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (purusaurusType == null) {
            purusaurusType = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(PurusaurusAddon.PURUSAURUS_ID);
        }
        return entity.getType() == purusaurusType;
    }

    public static boolean isTamed(Entity entity) {
        return entity != null && entity.getPersistentData().getBoolean(TAMED_KEY);
    }

    public static void setTamed(Entity entity, Player owner) {
        CompoundTag data = entity.getPersistentData();
        data.putBoolean(TAMED_KEY, true);
        if (owner != null) {
            data.putUUID(OWNER_KEY, owner.getUUID());
        }
    }

    public static UUID getOwner(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.hasUUID(OWNER_KEY) ? data.getUUID(OWNER_KEY) : null;
    }

    /** O dono online desta entidade, ou null se nao houver dono ou ele estiver offline. */
    public static Player getOwnerPlayer(Mob mob) {
        UUID id = getOwner(mob);
        if (id == null) {
            return null;
        }
        return mob.level().getPlayerByUUID(id);
    }

    /** True se a entidade for um animal domesticado (nosso Purusaurus, ou pets vanilla). */
    public static boolean isDomesticated(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (isTamed(entity)) {
            return true; // Purusaurus domesticado por este addon
        }
        if (entity instanceof TamableAnimal tamable && tamable.isTame()) {
            return true; // lobo, gato, papagaio...
        }
        if (entity instanceof AbstractHorse horse && horse.isTamed()) {
            return true; // cavalo, burro, mula, lhama...
        }
        return false;
    }

    public static int getMode(Entity entity) {
        return entity.getPersistentData().getInt(MODE_KEY);
    }

    // ---- Destino do "vagar nadando" (lido pelo PurusaurusSwimController) ----

    private static final String WANDER_X = "purusaurusaddon:WanderX";
    private static final String WANDER_Y = "purusaurusaddon:WanderY";
    private static final String WANDER_Z = "purusaurusaddon:WanderZ";

    public static void setSwimWander(Mob mob, double x, double y, double z) {
        CompoundTag data = mob.getPersistentData();
        data.putDouble(WANDER_X, x);
        data.putDouble(WANDER_Y, y);
        data.putDouble(WANDER_Z, z);
    }

    public static void clearSwimWander(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        data.remove(WANDER_X);
        data.remove(WANDER_Y);
        data.remove(WANDER_Z);
    }

    public static Vec3 getSwimWander(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(WANDER_X)) {
            return null;
        }
        return new Vec3(data.getDouble(WANDER_X), data.getDouble(WANDER_Y), data.getDouble(WANDER_Z));
    }

    public static void setMode(Entity entity, int mode) {
        entity.getPersistentData().putInt(MODE_KEY, mode);
    }

    /** Alterna Vagar -> Seguir -> Ficar -> Vagar e avisa o jogador na action bar. */
    public static void cycleMode(Mob mob, Player player) {
        int mode = (getMode(mob) + 1) % 3;
        setMode(mob, mode);
        if (mode == MODE_STAY) {
            mob.getNavigation().stop();
            // Se mandar ficar na agua, trava a profundidade atual; senao, limpa.
            if (mob.isInWater()) {
                mob.getPersistentData().putDouble(STAY_Y_KEY, mob.getY());
            } else {
                mob.getPersistentData().remove(STAY_Y_KEY);
            }
        } else {
            mob.getPersistentData().remove(STAY_Y_KEY);
        }
        String message = switch (mode) {
            case MODE_FOLLOW -> "Purusaurus is following.";
            case MODE_STAY -> "Purusaurus is staying.";
            default -> "Purusaurus is wandering.";
        };
        // Enviado como texto da action bar (acima da hotbar, centralizado). Esse pacote
        // nao e suprimido pelas configuracoes de visibilidade de chat, ao contrario de
        // displayClientMessage(..., true).
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(message)));
        }
    }

    // ---- Atributos do bebe (metade do HP e do dano do adulto) ----

    private static final UUID BABY_HP_MODIFIER_ID = UUID.fromString("b1b0a111-0000-4000-8000-000000000001");
    private static final UUID BABY_DMG_MODIFIER_ID = UUID.fromString("b1b0a111-0000-4000-8000-000000000002");

    /**
     * Mantem os atributos do bebe em metade dos do adulto (HP e dano), aplicando/
     * removendo um modificador conforme a entidade e ou deixa de ser bebe.
     * Vale para selvagens e domesticados.
     */
    public static void updateBabyAttributes(Mob mob) {
        applyHalfWhileBaby(mob, Attributes.MAX_HEALTH, BABY_HP_MODIFIER_ID, "purusaurus_baby_hp");
        applyHalfWhileBaby(mob, Attributes.ATTACK_DAMAGE, BABY_DMG_MODIFIER_ID, "purusaurus_baby_damage");
        // Nao deixa a vida atual ultrapassar o novo maximo (ex.: bebe recem-chocado).
        if (mob.getHealth() > mob.getMaxHealth()) {
            mob.setHealth(mob.getMaxHealth());
        }
    }

    private static void applyHalfWhileBaby(Mob mob, Attribute attribute, UUID id, String name) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        boolean baby = mob.isBaby();
        boolean hasModifier = instance.getModifier(id) != null;
        if (baby && !hasModifier) {
            // MULTIPLY_TOTAL com -0.5 => valor final x0.5 (metade).
            instance.addPermanentModifier(
                    new AttributeModifier(id, name, -0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (!baby && hasModifier) {
            instance.removeModifier(id);
        }
    }

    /**
     * Efeito de alimentacao: toca o som de comer em tom grave e aciona a animacao
     * de mordida (reusada como "comer"). Chamar no lado servidor.
     */
    public static void playFeedEffects(Mob mob) {
        mob.swing(InteractionHand.MAIN_HAND);
        mob.level().playSound(
                null,
                mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL,
                1.2F,                                        // volume
                0.5F + mob.getRandom().nextFloat() * 0.15F); // tom grave (deeper)
    }

    /**
     * Mesmas particulas do lobo: coracoes no sucesso, fumaca na falha.
     * Enviadas pelo servidor para todos os clientes proximos.
     */
    public static void spawnTamingParticles(Mob mob, boolean success) {
        if (!(mob.level() instanceof ServerLevel server)) {
            return;
        }
        ParticleOptions particle = success ? ParticleTypes.HEART : ParticleTypes.SMOKE;
        server.sendParticles(
                particle,
                mob.getX(),
                mob.getY() + mob.getBbHeight() * 0.5D,
                mob.getZ(),
                7,                              // quantidade
                mob.getBbWidth() * 0.5D,        // espalhamento X
                mob.getBbHeight() * 0.5D,       // espalhamento Y
                mob.getBbWidth() * 0.5D,        // espalhamento Z
                0.02D                           // velocidade
        );
    }
}
