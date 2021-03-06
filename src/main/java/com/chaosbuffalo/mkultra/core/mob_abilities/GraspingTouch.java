package com.chaosbuffalo.mkultra.core.mob_abilities;

import com.chaosbuffalo.mkultra.GameConstants;
import com.chaosbuffalo.mkultra.MKUltra;
import com.chaosbuffalo.mkultra.core.IMobData;
import com.chaosbuffalo.mkultra.core.MKDamageSource;
import com.chaosbuffalo.mkultra.core.MobAbility;
import com.chaosbuffalo.mkultra.effects.spells.GraspingRootsPotion;
import com.chaosbuffalo.mkultra.fx.ParticleEffects;
import com.chaosbuffalo.mkultra.init.ModSounds;
import com.chaosbuffalo.mkultra.network.packets.ParticleEffectSpawnPacket;
import com.chaosbuffalo.mkultra.utils.AbilityUtils;
import com.chaosbuffalo.targeting_api.Targeting;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class GraspingTouch extends MobAbility {

    float BASE_DAMAGE = 1.0f;
    float DAMAGE_SCALE = .25f;

    public GraspingTouch() {
        super(MKUltra.MODID, "mob_ability.grasping_touch");
    }

    @Override
    public int getCooldown() {
        return 20 * GameConstants.TICKS_PER_SECOND;
    }

    @Override
    public MobAbility.AbilityType getAbilityType() {
        return MobAbility.AbilityType.ATTACK;
    }

    @Override
    public float getDistance() {
        return 4.0f;
    }

    @Override
    public int getCastTime() {
        return GameConstants.TICKS_PER_SECOND;
    }

    @Override
    public SoundEvent getCastingSoundEvent() {
        return ModSounds.hostile_casting_shadow;
    }

    @Nullable
    @Override
    public SoundEvent getCastingCompleteEvent() {
        return ModSounds.spell_earth_1;
    }

    @Override
    public Targeting.TargetType getTargetType() {
        return Targeting.TargetType.ENEMY;
    }

    @Override
    public void execute(EntityLivingBase entity, IMobData data, EntityLivingBase target, World theWorld) {
        if (target != null) {
            int level = data.getMobLevel();
            target.attackEntityFrom(MKDamageSource.fromMeleeSkill(getAbilityId(), entity, entity),
                    BASE_DAMAGE + DAMAGE_SCALE * level);
            target.addPotionEffect(GraspingRootsPotion
                    .Create(entity)
                    .setTarget(target)
                    .toPotionEffect(GameConstants.TICKS_PER_SECOND * level / 2, 1));
            AbilityUtils.playSoundAtServerEntity(target, ModSounds.spell_earth_6, SoundCategory.HOSTILE);
            Vec3d lookVec = entity.getLookVec();
            MKUltra.packetHandler.sendToAllAround(
                    new ParticleEffectSpawnPacket(
                            EnumParticleTypes.SPELL_MOB_AMBIENT.getParticleID(),
                            ParticleEffects.SPHERE_MOTION, 40, 4,
                            entity.posX, entity.posY + 1.0,
                            entity.posZ, 1.0, 1.0, 1.0, .25,
                            lookVec),
                    entity.dimension, target.posX,
                    target.posY, target.posZ, 50.0f);
        }
    }
}

