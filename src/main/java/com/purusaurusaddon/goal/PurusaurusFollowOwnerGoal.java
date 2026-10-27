package com.purusaurusaddon.goal;

import com.purusaurusaddon.taming.TamingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Modo "Seguir": o Purusaurus domesticado segue o dono, como o lobo, inclusive
 * teletransportando-se quando fica longe demais. Baseado no FollowOwnerGoal vanilla.
 *
 * Velocidades (multiplicadores do atributo de movimento), proximas ao mob base
 * (andar 0.35 / correr 0.70), um pouco maiores para o pet nao ficar lento:
 *  - andar  = 0.40
 *  - correr = 0.78
 * Corre (e usa a animacao "run" via setAggressive) quando o dono esta correndo
 * OU muito longe; caso contrario anda normalmente.
 *
 * Distancias (maiores que as do lobo, por causa do tamanho do bicho):
 *  - comeca a seguir a 12 blocos (lobo: 10)
 *  - para de seguir a 6 blocos
 *  - passa a correr a partir de 15 blocos
 *  - teletransporta a 26 blocos (lobo: 12)
 */
public class PurusaurusFollowOwnerGoal extends Goal {

    private static final double WALK_SPEED = 0.40D;
    private static final double RUN_SPEED = 0.78D;

    private static final float START_DISTANCE = 12.0F;
    private static final float STOP_DISTANCE = 6.0F;
    private static final float RUN_DISTANCE = 15.0F;
    private static final float TELEPORT_DISTANCE = 26.0F;

    private final Mob mob;
    private final PathNavigation navigation;

    private Player owner;
    private int timeToRecalcPath;

    public PurusaurusFollowOwnerGoal(Mob mob) {
        this.mob = mob;
        this.navigation = mob.getNavigation();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private Player resolveOwner() {
        UUID id = TamingUtil.getOwner(mob);
        if (id == null) {
            return null;
        }
        return mob.level().getPlayerByUUID(id);
    }

    private boolean inFollowMode() {
        return TamingUtil.isTamed(mob) && TamingUtil.getMode(mob) == TamingUtil.MODE_FOLLOW;
    }

    @Override
    public boolean canUse() {
        if (!inFollowMode()) {
            return false;
        }
        Player p = resolveOwner();
        if (p == null || p.isSpectator() || p.isDeadOrDying()) {
            return false;
        }
        if (mob.distanceToSqr(p) < (double) (START_DISTANCE * START_DISTANCE)) {
            return false;
        }
        this.owner = p;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!inFollowMode() || owner == null || owner.isSpectator() || owner.isRemoved()) {
            return false;
        }
        return !navigation.isDone()
                && mob.distanceToSqr(owner) > (double) (STOP_DISTANCE * STOP_DISTANCE);
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.navigation.stop();
        this.mob.setAggressive(false); // volta para a animacao de andar/parado
    }

    @Override
    public void tick() {
        mob.getLookControl().setLookAt(owner, 10.0F, (float) mob.getMaxHeadXRot());

        // Correr se o dono estiver correndo ou muito longe; senao, andar.
        boolean shouldRun = owner.isSprinting()
                || mob.distanceToSqr(owner) > (double) (RUN_DISTANCE * RUN_DISTANCE);
        double speed = shouldRun ? RUN_SPEED : WALK_SPEED;
        mob.setAggressive(shouldRun);            // alterna animacao run/walk
        navigation.setSpeedModifier(speed);      // ajuste de velocidade responsivo

        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(10);
            if (mob.distanceToSqr(owner) >= (double) (TELEPORT_DISTANCE * TELEPORT_DISTANCE)) {
                teleportToOwner();
            } else {
                navigation.moveTo(owner, speed);
            }
        }
    }

    private void teleportToOwner() {
        BlockPos base = owner.blockPosition();
        for (int i = 0; i < 10; i++) {
            int x = base.getX() + randInt(-3, 3);
            int y = base.getY() + randInt(-1, 1);
            int z = base.getZ() + randInt(-3, 3);
            if (maybeTeleportTo(x, y, z)) {
                return;
            }
        }
    }

    private boolean maybeTeleportTo(int x, int y, int z) {
        if (Math.abs((double) x - owner.getX()) < 2.0D && Math.abs((double) z - owner.getZ()) < 2.0D) {
            return false;
        }
        if (!canTeleportTo(new BlockPos(x, y, z))) {
            return false;
        }
        mob.moveTo((double) x + 0.5D, (double) y, (double) z + 0.5D, mob.getYRot(), mob.getXRot());
        navigation.stop();
        return true;
    }

    private boolean canTeleportTo(BlockPos pos) {
        Level level = mob.level();
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }
        if (level.getBlockState(below).getBlock() instanceof LeavesBlock) {
            return false;
        }
        BlockPos delta = pos.subtract(mob.blockPosition());
        return level.noCollision(mob, mob.getBoundingBox().move(delta.getX(), delta.getY(), delta.getZ()));
    }

    private int randInt(int min, int max) {
        return mob.getRandom().nextInt(max - min + 1) + min;
    }
}
