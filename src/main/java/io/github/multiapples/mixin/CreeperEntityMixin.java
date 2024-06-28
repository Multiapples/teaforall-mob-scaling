package io.github.multiapples.mixin;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Mixin(CreeperEntity.class)
public abstract class CreeperEntityMixin {

    @Inject(method = "Lnet/minecraft/entity/mob/CreeperEntity;spawnEffectsCloud()V", at = @At("HEAD"))
    protected void spawnEffectsCloud(CallbackInfo ci) {
        // This method gets injected at the start of the creeper's method responsible for spawning a potion effect cloud
        // upon exploding. This mixin strips the creeper of infinite duration potion effects before they get added to
        // the potion effect cloud.
        CreeperEntity that = (CreeperEntity)(Object)this;
        if (that.getActiveStatusEffects() == null) {
            return; // This is Minecraft's job to deal with.
        }
        Map<RegistryEntry<StatusEffect>, StatusEffectInstance> finiteEffects = that.getActiveStatusEffects()
                .entrySet()
                .stream()
                .filter(e -> !e.getValue().isInfinite())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        that.getActiveStatusEffects().clear();
        that.getActiveStatusEffects().putAll(finiteEffects);
    }
}
