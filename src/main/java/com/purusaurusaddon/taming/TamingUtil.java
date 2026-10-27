package com.purusaurusaddon.taming;

import com.purusaurusaddon.PurusaurusAddon;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.UUID;

/**
 * Regras e dados da domesticacao do Purusaurus.
 *
 * O "estado domesticado" e guardado no {@link Entity#getPersistentData()} da
 * propria entidade do Unearthed Journey (tag ForgeData), entao nao precisamos
 * modificar a classe compilada do mod base nem registrar capabilities: o dado
 * e salvo/carregado junto com a entidade automaticamente.
 */
public final class TamingUtil {

    private TamingUtil() {}

    public static final String TAMED_KEY = "purusaurusaddon:Tamed";
    public static final String OWNER_KEY = "purusaurusaddon:Owner";

    /** Item de domesticacao -> taxa de sucesso (0..1). */
    public static final Map<Item, Float> TAMING_ITEMS = Map.ofEntries(
            Map.entry(Items.BEEF, 0.30F),           // carne de vaca
            Map.entry(Items.PORKCHOP, 0.25F),       // carne de porco
            Map.entry(Items.MUTTON, 0.20F),         // carne de ovelha
            Map.entry(Items.RABBIT, 0.15F),         // carne de coelho
            Map.entry(Items.CHICKEN, 0.10F),        // carne de frango
            Map.entry(Items.SALMON, 0.05F),         // salmao
            Map.entry(Items.COD, 0.025F),           // bacalhau
            Map.entry(Items.TROPICAL_FISH, 0.015F)  // peixe tropical
    );

    /** Chance de domesticar com este item, ou 0 se o item nao serve. */
    public static float tamingChance(Item item) {
        return TAMING_ITEMS.getOrDefault(item, 0.0F);
    }

    /** True se a entidade for o Purusaurus do Unearthed Journey. */
    public static boolean isPurusaurus(Entity entity) {
        if (entity == null) {
            return false;
        }
        ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return PurusaurusAddon.PURUSAURUS_ID.equals(id);
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
