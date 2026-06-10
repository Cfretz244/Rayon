package dev.lazurite.rayon.impl.mixin.common.entity;

import dev.lazurite.rayon.api.EntityPhysicsElement;
import dev.lazurite.rayon.api.PhysicsElement;
import dev.lazurite.rayon.impl.bullet.math.Convert;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Allows {@link PhysicsElement} objects to be affected by explosions.
 *
 * 1.21.2: Explosion became an interface; the server-side logic lives in
 * {@link ServerExplosion}, and knockback is applied via Entity.push(Vec3).
 */
@Mixin(ServerExplosion.class)
public class ExplosionMixin {
    @Redirect(
            method = "hurtEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;push(Lnet/minecraft/world/phys/Vec3;)V"
            )
    )
    public void hurtEntities$push(Entity entity, Vec3 velocity) {
        if (EntityPhysicsElement.is(entity)) {
            var element = EntityPhysicsElement.get(entity);
            element.getRigidBody().applyCentralImpulse(Convert.toBullet(velocity).multLocal(element.getRigidBody().getMass() * 100f));
        } else {
            entity.push(velocity);
        }
    }
}
