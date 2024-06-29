package io.github.multiapples.mixin;

import io.github.multiapples.TeaForAllMobScaling;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ConcentricRingsStructurePlacement.class)
public abstract class ConcentricRingsStructurePlacementMixin {
    @Inject(method = "Lnet/minecraft/world/gen/chunk/placement/ConcentricRingsStructurePlacement;isStartChunk(Lnet/minecraft/world/gen/chunk/placement/StructurePlacementCalculator;II)Z",
    at = @At("HEAD"), cancellable = true)
    protected void isStartChunk(StructurePlacementCalculator calculator, int chunkX, int chunkZ, CallbackInfoReturnable<Boolean> cir) {
        double chunkDistSqr = new Vec2f(chunkX, chunkZ).lengthSquared();
        if (chunkDistSqr * 16 * 16 <= Math.pow(TeaForAllMobScaling.config.strongholdDeadZone, 2)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
