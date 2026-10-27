package com.purusaurusaddon.goal;

import com.purusaurusaddon.taming.TamingUtil;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Modo "Ficar": o Purusaurus domesticado permanece parado no lugar.
 *
 * Ocupa o slot de movimento (flag MOVE) com prioridade alta, entao ele
 * sobrepoe o RandomStrollGoal e demais goals de andar do mob base.
 */
public class PurusaurusStayGoal extends Goal {

    private final Mob mob;

    public PurusaurusStayGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return TamingUtil.isTamed(mob) && TamingUtil.getMode(mob) == TamingUtil.MODE_STAY;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
        mob.setJumping(false);
    }

    @Override
    public void tick() {
        mob.getNavigation().stop();
        Vec3 dm = mob.getDeltaMovement();
        mob.setDeltaMovement(0.0D, dm.y, 0.0D); // mantem parado, mas respeita gravidade
    }
}
