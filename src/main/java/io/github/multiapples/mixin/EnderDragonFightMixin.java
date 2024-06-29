package io.github.multiapples.mixin;

import io.github.multiapples.MobScaling;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonFight.class)
public abstract class EnderDragonFightMixin {
    @Shadow
    protected final ServerWorld world;

    protected EnderDragonFightMixin(ServerWorld world) {
        this.world = world; // dummy constructor
    }

    @Inject(method = "Lnet/minecraft/entity/boss/dragon/EnderDragonFight;dragonKilled(Lnet/minecraft/entity/boss/dragon/EnderDragonEntity;)V",
        at = @At("TAIL"))
    protected void dragonKilled(CallbackInfo ci) {
        MobScaling.flagDownedEnderDragon(this.world.getScoreboard());
    }
}
