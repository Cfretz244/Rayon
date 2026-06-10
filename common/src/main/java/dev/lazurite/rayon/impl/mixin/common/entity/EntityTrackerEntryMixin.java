package dev.lazurite.rayon.impl.mixin.common.entity;

import dev.lazurite.rayon.api.EntityPhysicsElement;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

/**
 * Prevents certain packets from being sent for {@link EntityPhysicsElement}s.
 *
 * 1.21.4 sendChanges accept ordinals: 0 = SetPassengers, 1 = passenger Rot,
 * 2 = projectile motion bundle, 3 = SetEntityMotion, 4 = consolidated move packet,
 * 5 = RotateHead. Re-verify on every Minecraft bump.
 */
@Mixin(ServerEntity.class)
public class EntityTrackerEntryMixin {
    @Shadow @Final private Entity entity;

    @Redirect(
            method = "sendChanges",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V",
                    ordinal = 1
            )
    )
    public void rotate(Consumer consumer, Object object) {
        if (!EntityPhysicsElement.is(entity)) {
            consumer.accept(object);
        }
    }

    @Redirect(
            method = "sendChanges",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V",
                    ordinal = 3
            )
    )
    public void velocity(Consumer consumer, Object object) {
        if (!EntityPhysicsElement.is(entity)) {
            consumer.accept(object);
        }
    }

    @Redirect(
            method = "sendChanges",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V",
                    ordinal = 4
            )
    )
    public void multiple(Consumer consumer, Object object) {
        if (!EntityPhysicsElement.is(entity)) {
            consumer.accept(object);
        }
    }
}
