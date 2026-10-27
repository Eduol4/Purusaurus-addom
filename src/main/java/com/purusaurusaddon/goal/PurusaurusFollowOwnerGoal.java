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
 * Modo "Seguir": o Purusaurus domesticado segue o dono, como o lobo,
 * inclusive teletransportando-se para perto quando fica longe demais.
 * Baseado no FollowOwnerGoal vanilla.
 */
public class PurusaurusFollowOwnerGoal extends Goal {

    private final Mob mob;
    private final double speedModifier;
    private final float startDistance;
    private final float stopDistance;
    private final float teleportDistance;
    private final PathNavigation navigation;

    private Player owner;
    private int timeToRecalcPath;

    public PurusaurusFollowOwnerGoal(Mob mob, double speedModifier,
                                     float startDistance, float stopDistance, float teleportDistance) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.teleportDistance = teleportDistance;
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
        if (mob.distanceToSqr(p) < (double) (startDistance * startDistance)) {
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
                && mob.distanceToSqr(owner) > (double) (stopDistance * stopDistance);
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.navigation.stop();
    }

    @Override
    public void tick() {
        mob.getLookControl().setLookAt(owner, 10.0F, (float) mob.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(10);
            if (mob.distanceToSqr(owner) >= (double) (teleportDistance * teleportDistance)) {
                teleportToOwner();
            } else {
                navigation.moveTo(owner, speedModifier);
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
