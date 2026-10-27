package com.purusaurusaddon.goal;

import com.purusaurusaddon.taming.TamingUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Faz o Purusaurus domesticado atacar o alvo que o dono atacou (como o
 * OwnerHurtTargetGoal do lobo), com a excecao de outros animais domesticados.
 */
public class PurusaurusOwnerHurtTargetGoal extends TargetGoal {

    private final Mob purusaurus;
    private LivingEntity ownerLastHurt;
    private int timestamp;

    public PurusaurusOwnerHurtTargetGoal(Mob mob) {
        super(mob, false);
        this.purusaurus = mob;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!TamingUtil.isTamed(purusaurus)) {
            return false;
        }
        Player owner = TamingUtil.getOwnerPlayer(purusaurus);
        if (owner == null) {
            return false;
        }
        this.ownerLastHurt = owner.getLastHurtMob();
        int t = owner.getLastHurtMobTimestamp();
        return t != this.timestamp
                && this.ownerLastHurt != null
                && this.ownerLastHurt != owner
                && this.ownerLastHurt.isAlive()
                && !TamingUtil.isDomesticated(this.ownerLastHurt)      // nao ataca outros pets
                && this.canAttack(this.ownerLastHurt, TargetingConditions.DEFAULT);
    }

    @Override
    public void start() {
        this.mob.setTarget(this.ownerLastHurt);
        Player owner = TamingUtil.getOwnerPlayer(purusaurus);
        if (owner != null) {
            this.timestamp = owner.getLastHurtMobTimestamp();
        }
        super.start();
    }
}
