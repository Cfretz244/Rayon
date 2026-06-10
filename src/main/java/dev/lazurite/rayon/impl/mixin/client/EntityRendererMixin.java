package dev.lazurite.rayon.impl.mixin.client;

import com.jme3.math.Vector3f;
import dev.lazurite.rayon.api.EntityPhysicsElement;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Corrects the world position recorded in the {@link EntityRenderState} (1.21.2+),
 * which shadow rendering samples blocks from. Replaces the old renderShadow Y patch.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void extractRenderState(Entity entity, EntityRenderState state, float tickDelta, CallbackInfo info) {
        if (EntityPhysicsElement.is(entity)) {
            final var location = EntityPhysicsElement.get(entity).getPhysicsLocation(new Vector3f(), tickDelta);
            state.x = location.x;
            state.y = location.y;
            state.z = location.z;

        }
    }
}
