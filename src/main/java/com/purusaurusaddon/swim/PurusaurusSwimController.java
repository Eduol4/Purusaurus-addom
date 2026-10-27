package com.purusaurusaddon.swim;

import com.purusaurusaddon.taming.TamingUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Controla o nado do Purusaurus. Enquanto ele esta na agua, este controlador
 * assume o movimento manualmente (definindo o deltaMovement), o que permite
 * mergulhar e emergir em 3D como um animal aquatico, em vez de so boiar.
 *
 * Nao mexe no codigo do mod base: o mob continua sendo terrestre; apenas
 * "pilotamos" ele enquanto submerso. Foco em perseguir alvos e seguir o dono
 * na agua (o vagar-na-agua acontece de brinde quando a navegacao entra nela).
 *
 * Velocidades: modificador * escala -> blocos por tick. Facilmente ajustaveis.
 */
public final class PurusaurusSwimController {

    private PurusaurusSwimController() {}

    public static final double SWIM_SLOW_MODIFIER = 0.50D; // nado lento (~andar)
    public static final double SWIM_FAST_MODIFIER = 1.00D;  // nado rapido (~correr)
    private static final double SWIM_SCALE = 0.22D;         // modificador -> blocos/tick

    /** Chamado todo tick no servidor para cada Purusaurus. Age somente na agua. */
    public static void tick(Mob mob) {
        if (!mob.isInWater()) {
            return;
        }

        // Faz o controlador de animacao do mob base tocar "swim" enquanto na agua.
        mob.setAggressive(true);

        Vec3 goal = swimGoal(mob);
        if (goal != null) {
            Vec3 toGoal = goal.subtract(mob.position());
            double dist = toGoal.length();
            if (dist > 0.4D) {
                Vec3 dir = toGoal.scale(1.0D / dist);
                double speed = (shouldSwimFast(mob) ? SWIM_FAST_MODIFIER : SWIM_SLOW_MODIFIER) * SWIM_SCALE;
                Vec3 desired = dir.scale(speed);
                // suaviza a mudanca de velocidade/direcao
                Vec3 blended = mob.getDeltaMovement().scale(0.6D).add(desired.scale(0.4D));
                mob.setDeltaMovement(blended);
                mob.hasImpulse = true;
            }
        } else {
            // Ocioso na agua: sobe suavemente para ficar perto da superficie.
            Vec3 dm = mob.getDeltaMovement();
            if (dm.y < 0.02D) {
                mob.setDeltaMovement(dm.x * 0.8D, dm.y + 0.02D, dm.z * 0.8D);
            }
        }
    }

    /** Para onde nadar: alvo de combate, caminho atual, ou o dono (modo seguir). */
    private static Vec3 swimGoal(Mob mob) {
        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive()) {
            return target.position();
        }
        Path path = mob.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            return path.getNextEntityPos(mob);
        }
        if (TamingUtil.isTamed(mob) && TamingUtil.getMode(mob) == TamingUtil.MODE_FOLLOW) {
            Player owner = TamingUtil.getOwnerPlayer(mob);
            if (owner != null && mob.distanceToSqr(owner) > 9.0D) {
                return owner.position();
            }
        }
        return null;
    }

    /** Nada rapido ao perseguir um alvo, ou ao seguir um dono correndo/distante. */
    private static boolean shouldSwimFast(Mob mob) {
        if (mob.getTarget() != null) {
            return true;
        }
        if (TamingUtil.isTamed(mob) && TamingUtil.getMode(mob) == TamingUtil.MODE_FOLLOW) {
            Player owner = TamingUtil.getOwnerPlayer(mob);
            return owner != null && (owner.isSprinting() || mob.distanceToSqr(owner) > 100.0D);
        }
        return false;
    }
}
