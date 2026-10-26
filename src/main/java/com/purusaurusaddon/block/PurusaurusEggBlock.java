package com.purusaurusaddon.block;

import com.purusaurusaddon.PurusaurusAddon;
import com.purusaurusaddon.block.entity.PurusaurusEggBlockEntity;
import com.purusaurusaddon.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Ovo de Purusaurus. Espelha o comportamento do ovo de tartaruga vanilla,
 * com as diferencas pedidas:
 *  - 1 ovo por bloco (sem a propriedade "EGGS" de 1 a 4);
 *  - choca em ~1 dia/noite (ver {@link PurusaurusEggBlockEntity#HATCH_TIME});
 *  - ao chocar, nasce 1 Purusaurus bebe.
 * A propriedade HATCH (0-2) controla apenas os estagios visuais de rachadura.
 */
public class PurusaurusEggBlock extends BaseEntityBlock {

    /** Estagio visual da rachadura: 0 = inteiro, 1 = rachado, 2 = muito rachado. */
    public static final IntegerProperty HATCH = IntegerProperty.create("hatch", 0, 2);

    /** Permitir que o ovo seja pisoteado/destruido (como o de tartaruga). */
    public static final boolean ALLOW_TRAMPLE = true;

    // Formato um pouco maior que o ovo unico de tartaruga (que e 3,0,3 -> 12,7,12).
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 11.0D, 14.0D);

    public PurusaurusEggBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(HATCH, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HATCH);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // ---- Suporte / sobrevivencia (precisa de bloco solido embaixo) ----

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    // ---- Block entity + ticker (conta o tempo de choco) ----

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PurusaurusEggBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.PURUSAURUS_EGG.get(),
                PurusaurusEggBlockEntity::serverTick);
    }

    // ---- Pisoteio (opcional, igual ao ovo de tartaruga) ----

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (ALLOW_TRAMPLE && canTrample(level, entity)) {
            this.destroyEgg(level, pos, entity, 100);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (ALLOW_TRAMPLE && !(entity instanceof net.minecraft.world.entity.animal.Turtle) && canTrample(level, entity)) {
            this.destroyEgg(level, pos, entity, 3);
        }
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    private void destroyEgg(Level level, BlockPos pos, Entity entity, int chanceDivisor) {
        if (!level.isClientSide && level.random.nextInt(chanceDivisor) == 0) {
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS,
                    0.7F, 0.9F + level.random.nextFloat() * 0.2F);
            level.destroyBlock(pos, false, entity);
        }
    }

    private static boolean canTrample(Level level, Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        if (entity instanceof Bat) {
            return false;
        }
        // O proprio Purusaurus nao pisoteia seus ovos.
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (PurusaurusAddon.PURUSAURUS_ID.equals(id)) {
            return false;
        }
        if (entity instanceof Player) {
            return true;
        }
        return ForgeEventFactory.getMobGriefingEvent(level, entity);
    }
}
