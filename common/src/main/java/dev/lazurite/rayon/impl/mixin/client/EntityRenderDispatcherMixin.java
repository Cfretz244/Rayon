package dev.lazurite.rayon.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.lazurite.rayon.api.EntityPhysicsElement;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides debug hitboxes for physics elements (their collision shape is drawn by
 * {@link dev.lazurite.rayon.impl.util.debug.CollisionObjectDebugger} instead).
 * Shadow positioning is handled by {@link EntityRendererMixin} since 1.21.2
 * (renderShadow reads world coordinates from the EntityRenderState).
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    // 1.21: renderHitbox gained red/green/blue colour params.
    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private static void renderHitbox(PoseStack matrices, VertexConsumer vertices, Entity entity, float tickDelta, float red, float green, float blue, CallbackInfo info) {
        if (EntityPhysicsElement.is(entity)) {
            info.cancel();
        }
    }
}
