package io.github.multiapples.mixin;

import io.github.multiapples.MobScaling;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {
    @Inject(method = "Lnet/minecraft/entity/mob/MobEntity;initialize(Lnet/minecraft/world/ServerWorldAccess;Lnet/minecraft/world/LocalDifficulty;Lnet/minecraft/entity/SpawnReason;Lnet/minecraft/entity/EntityData;)Lnet/minecraft/entity/EntityData;",
            at = @At("RETURN"))
    protected void initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                              @Nullable EntityData entityData, CallbackInfoReturnable<EntityData> cir) {
        // This method gets injected into MobEntity#initialize() after it initializes the mob and right before
        // it returns entityData.
        MobEntity that = (MobEntity)(Object)this;
        MobScaling.assignMobScaling(that);
    }
}
