package io.github.multiapples.mixin;

import io.github.multiapples.TeaForAllMobScaling;
import io.github.multiapples.MobScaling;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "Lnet/minecraft/entity/player/PlayerEntity;tick()V", at = @At("TAIL"))
    protected void tick(CallbackInfo ci) {
        PlayerEntity that = (PlayerEntity)(Object)this;
        if (that.getEntityWorld().getRegistryKey() == World.NETHER) {
            if (that.getPos().getY() >= TeaForAllMobScaling.config.netherDOTYlevel
                    && !MobScaling.getFlagDownedEnderDragon(that.getScoreboard())) {

                boolean success = that.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER,
                        60, 1, false, true, true));
                if (success) {
                    that.sendMessage(Text.of("Somewhere, a Dragon forbids your presence..."), true);
                }
            }
        }
    }
}
