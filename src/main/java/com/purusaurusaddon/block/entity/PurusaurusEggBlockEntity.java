package com.purusaurusaddon.block.entity;

import com.purusaurusaddon.PurusaurusAddon;
import com.purusaurusaddon.block.PurusaurusEggBlock;
import com.purusaurusaddon.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Guarda o progresso de choco do ovo e, ao atingir o tempo, faz nascer
 * um Purusaurus bebe. Usar uma block entity (em vez de random tick como o
 * ovo de tartaruga) deixa o tempo de choco preciso: ~1 dia/noite completo,
 * independente da sorte do random tick.
 */
public class PurusaurusEggBlockEntity extends BlockEntity {

    /** Tempo total de choco em ticks. 24000 ticks = 1 dia/noite completo no Minecraft. */
    public static final int HATCH_TIME = 24000;

    private int hatchProgress;

    public PurusaurusEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PURUSAURUS_EGG.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PurusaurusEggBlockEntity be) {
        be.hatchProgress++;

        // Atualiza o estagio visual da rachadura (0 -> 1 em 1/3, 1 -> 2 em 2/3).
        int stage = be.hatchProgress >= (HATCH_TIME * 2 / 3) ? 2
                : (be.hatchProgress >= (HATCH_TIME / 3) ? 1 : 0);
        if (state.getValue(PurusaurusEggBlock.HATCH) != stage) {
            level.setBlock(pos, state.setValue(PurusaurusEggBlock.HATCH, stage), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS,
                    0.7F, 0.9F + level.random.nextFloat() * 0.2F);
            be.setChanged();
        }

        if (be.hatchProgress >= HATCH_TIME && level instanceof ServerLevel server) {
            be.hatch(server, pos);
        }
    }

    private void hatch(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS,
                0.7F, 0.9F + level.random.nextFloat() * 0.2F);
        level.removeBlock(pos, false);

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(PurusaurusAddon.PURUSAURUS_ID);
        if (type == null) {
            PurusaurusAddon.LOGGER.warn(
                    "Entidade {} nao encontrada - o Unearthed Journey esta instalado? O ovo nao chocou.",
                    PurusaurusAddon.PURUSAURUS_ID);
            return;
        }

        Entity entity = type.create(level);
        if (entity == null) {
            return;
        }

        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;

        if (entity instanceof Mob mob) {
            mob.moveTo(x, y, z, Mth.wrapDegrees(level.random.nextFloat() * 360F), 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.BREEDING, null, null);
            if (mob instanceof AgeableMob ageable) {
                ageable.setBaby(true);
            }
            // O filhote chocado nao desaparece com o tempo.
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
        } else {
            entity.moveTo(x, y, z, 0.0F, 0.0F);
            level.addFreshEntity(entity);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("HatchProgress", this.hatchProgress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.hatchProgress = tag.getInt("HatchProgress");
    }
}
