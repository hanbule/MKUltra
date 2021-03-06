package com.chaosbuffalo.mkultra.effects.spells;

import com.chaosbuffalo.mkultra.MKUltra;
import com.chaosbuffalo.mkultra.core.IPlayerData;
import com.chaosbuffalo.mkultra.core.MKUPlayerData;
import com.chaosbuffalo.mkultra.core.PlayerFormulas;
import com.chaosbuffalo.mkultra.effects.SpellCast;
import com.chaosbuffalo.mkultra.effects.SpellTriggers;
import com.chaosbuffalo.mkultra.effects.passives.PassiveAbilityPotionBase;
import com.chaosbuffalo.mkultra.init.ModSounds;
import com.chaosbuffalo.mkultra.utils.AbilityUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = MKUltra.MODID)
public class LifeSiphonPotion extends PassiveAbilityPotionBase {

    public static final LifeSiphonPotion INSTANCE = new LifeSiphonPotion();

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Potion> event) {
        event.getRegistry().register(INSTANCE.finish());
    }

    public static SpellCast Create(Entity source) {
        return INSTANCE.newSpellCast(source);
    }

    private LifeSiphonPotion() {
        setPotionName("effect.life_siphon");
        SpellTriggers.PLAYER_KILL_ENTITY.register(this, this::onPlayerKillEntity);
    }

    public void onPlayerKillEntity(LivingDeathEvent event, DamageSource source, EntityPlayer player) {
        IPlayerData pData = MKUPlayerData.get(player);
        if (pData != null) {
            if (SpellTriggers.isMeleeDamage(source)) {
                AbilityUtils.playSoundAtServerEntity(player, ModSounds.spell_dark_5, SoundCategory.PLAYERS);
                float healAmount = PlayerFormulas.applyHealBonus(pData, 4.0f);
                player.heal(healAmount);
            }
        }
    }

    @Override
    public ResourceLocation getIconTexture() {
        return new ResourceLocation(MKUltra.MODID, "textures/class/abilities/life_siphon.png");
    }
}