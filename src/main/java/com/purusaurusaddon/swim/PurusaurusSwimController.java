package com.purusaurusaddon.swim;

import com.purusaurusaddon.taming.TamingUtil;
import net.minecraft.util.Mth;
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
 * Nao mexe no codigo do mod base: o mob continua terrestre; apenas "pilotamos"
 * ele enquanto na agua.
 */
public final class PurusaurusSwimController {

    private PurusaurusSwimController() {}

    public static final double SWIM_SLOW_MODIFIER = 0.50D; // nado lento (~andar)
    public static final double SWIM_FAST_MODIFIER = 1.00D;  // nado rapido (~correr)
    private static final double SWIM_SCALE = 0.22D;         // modificador -> blocos/tick
    private static final float TURN_SPEED = 15.0F;          // graus/tick para virar o corpo

    /** Chamado todo tick no servidor para cada Purusaurus. Age somente na agua. */
    public static void tick(Mob mob) {
        if (!mob.isInWater()) {
            return;
        }

        // Faz o controlador de animacao do mob base tocar "swim" enquanto na agua.
        mob.setAggressive(true);

        // Modo "ficar" na agua: segura a profundidade atual em vez de boiar.
        if (TamingUtil.isTamed(mob) && TamingUtil.getMode(mob) == TamingUtil.MODE_STAY) {
            holdDepth(mob);
            return;
        }

        Vec3 goal = swimGoal(mob);
        if (goal != null) {
            Vec3 toGoal = goal.subtract(mob.position());
            double dist = toGoal.length();
            if (dist > 0.4D) {
                Vec3 dir = toGoal.scale(1.0D / dist);
                double speed = (shouldSwimFast(mob) ? SWIM_FAST_MODIFIER : SWIM_SLOW_MODIFIER) * SWIM_SCALE;
                Vec3 desired = dir.scale(speed);
                mob.setDeltaMovement(mob.getDeltaMovement().scale(0.6D).add(desired.scale(0.4D)));
                mob.hasImpulse = true;
                faceMovement(mob, dir); // vira o corpo para a direcao (corrige nadar de re)
            }
        } else {
            // Ocioso na agua: sobe suavemente para ficar perto da superficie.
            Vec3 dm = mob.getDeltaMovement();
            if (dm.y < 0.02D) {
                mob.setDeltaMovement(dm.x * 0.8D, dm.y + 0.02D, dm.z * 0.8D);
            }
        }
    }

    /** Segura a profundidade onde foi mandado ficar (nao sobe nem desce). */
    private static void holdDepth(Mob mob) {
        double targetY;
        if (mob.getPersistentData().contains(TamingUtil.STAY_Y_KEY)) {
            targetY = mob.getPersistentData().getDouble(TamingUtil.STAY_Y_KEY);
        } else {
            targetY = mob.getY();
            mob.getPersistentData().putDouble(TamingUtil.STAY_Y_KEY, targetY);
        }
        double dy = Mth.clamp(targetY - mob.getY(), -0.08D, 0.08D);
        mob.setDeltaMovement(0.0D, dy, 0.0D);
    }

    /** Gira o corpo gradualmente para a direcao horizontal do nado. */
    private static void faceMovement(Mob mob, Vec3 dir) {
        if (dir.x * dir.x + dir.z * dir.z < 1.0E-4D) {
            return;
        }
        float targetYaw = (float) (Mth.atan2(dir.z, dir.x) * (180.0D / Math.PI)) - 90.0F;
        float delta = Mth.clamp(Mth.wrapDegrees(targetYaw - mob.getYRot()), -TURN_SPEED, TURN_SPEED);
        float newYaw = mob.getYRot() + delta;
        mob.setYRot(newYaw);
        mob.yBodyRot = newYaw;
    }

    /**
     * Para onde nadar, em ordem de prioridade. Importante: perseguir/seguir usam a
     * posicao 3D real (com profundidade). O caminho da navegacao fica por ultimo,
     * pois a navegacao de chao so cria nos na superficie (nao serve para mergulhar).
     */
    private static Vec3 swimGoal(Mob mob) {
        // 1) Perseguir alvo (3D).
        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive()) {
            return target.position();
        }
        // 2) Seguir o dono (3D, com profundidade).
        if (TamingUtil.isTamed(mob) && TamingUtil.getMode(mob) == TamingUtil.MODE_FOLLOW) {
            Player owner = TamingUtil.getOwnerPlayer(mob);
            if (owner != null && mob.distanceToSqr(owner) > 9.0D) {
                return owner.position();
            }
        }
        // 3) Vagar nadando (destino gravado na entidade pelo PurusaurusSwimWanderGoal).
        Vec3 wander = TamingUtil.getSwimWander(mob);
        if (wander != null) {
            return wander;
        }
        // 4) Fallback: caminho atual da navegacao (nos de superficie).
        Path path = mob.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            return path.getNextEntityPos(mob);
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
