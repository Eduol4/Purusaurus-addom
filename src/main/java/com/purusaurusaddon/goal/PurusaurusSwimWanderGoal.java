package com.purusaurusaddon.goal;

import com.purusaurusaddon.taming.TamingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Vagar nadando: quando esta na agua e sem nada melhor a fazer (sem alvo, sem
 * seguir/ficar), escolhe um ponto de agua proximo e raso e nada ate la. Vale
 * para selvagens e domesticados. Propositalmente raso, perto da terra e ocasional
 * (geralmente nao vai fundo nem fica muito tempo na agua).
 */
public class PurusaurusSwimWanderGoal extends Goal {

    private final Mob mob;
    private double tx;
    private double ty;
    private double tz;
    private boolean hasTarget;

    public PurusaurusSwimWanderGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!mob.isInWater() || mob.getTarget() != null) {
            return false;
        }
        if (TamingUtil.isTamed(mob)) {
            int mode = TamingUtil.getMode(mob);
            if (mode == TamingUtil.MODE_STAY || mode == TamingUtil.MODE_FOLLOW) {
                return false;
            }
        }
        if (mob.getRandom().nextInt(80) != 0) {
            return false; // ocasional
        }
        Vec3 pos = findWanderPos();
        if (pos == null) {
            return false;
        }
        this.tx = pos.x;
        this.ty = pos.y;
        this.tz = pos.z;
        this.hasTarget = true;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return hasTarget
                && mob.isInWater()
                && mob.getTarget() == null
                && mob.distanceToSqr(tx, ty, tz) > 1.5D;
    }

    @Override
    public void start() {
        TamingUtil.setSwimWander(mob, tx, ty, tz); // o controlador de nado le isto
    }

    @Override
    public void stop() {
        this.hasTarget = false;
        TamingUtil.clearSwimWander(mob);
    }

    /** Ponto de agua proximo, raso (tende a nao ir fundo). */
    private Vec3 findWanderPos() {
        for (int i = 0; i < 8; i++) {
            int dx = mob.getRandom().nextInt(13) - 6; // -6..6
            int dz = mob.getRandom().nextInt(13) - 6;
            int dy = mob.getRandom().nextInt(4) - 3;   // -3..0 (nao muito fundo)
            BlockPos pos = mob.blockPosition().offset(dx, dy, dz);
            if (mob.level().getFluidState(pos).is(Fluids.WATER)) {
                return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            }
        }
        return null;
    }
}
