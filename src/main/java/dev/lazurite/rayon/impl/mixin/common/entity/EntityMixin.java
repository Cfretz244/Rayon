package dev.lazurite.rayon.impl.mixin.common.entity;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import dev.lazurite.rayon.api.EntityPhysicsElement;
import dev.lazurite.rayon.impl.bullet.math.Convert;
import dev.lazurite.toolbox.api.math.QuaternionHelper;
import dev.lazurite.toolbox.api.math.VectorHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Basic changes for {@link EntityPhysicsElement}s. ({@link CallbackInfo#cancel()} go brrr)
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    public void pushAwayFrom(Entity entity, CallbackInfo info) {
        if (EntityPhysicsElement.is((Entity) (Object) this) && EntityPhysicsElement.is(entity)) {
            info.cancel();
        }
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    public void move(CallbackInfo info) {
        if (EntityPhysicsElement.is((Entity) (Object) this)) {
            info.cancel();
        }
    }

    // 1.21.6: entity save data flows through ValueInput/ValueOutput; compound sub-tags are
    // written via codecs to keep the on-disk format unchanged.
    @Inject(method = "saveWithoutId", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V"))
    public void saveWithoutId(ValueOutput output, CallbackInfo info) {
        if (EntityPhysicsElement.is((Entity) (Object) this)) {
            var rigidBody = EntityPhysicsElement.get((Entity) (Object) this).getRigidBody();
            output.store("orientation", CompoundTag.CODEC, QuaternionHelper.toTag(Convert.toMinecraft(rigidBody.getPhysicsRotation(new Quaternion()))));
            output.store("linearVelocity", CompoundTag.CODEC, VectorHelper.toTag(Convert.toMinecraft(rigidBody.getLinearVelocity(new Vector3f()))));
            output.store("angularVelocity", CompoundTag.CODEC, VectorHelper.toTag(Convert.toMinecraft(rigidBody.getAngularVelocity(new Vector3f()))));
            output.putFloat("mass", rigidBody.getMass());
            output.putFloat("dragCoefficient", rigidBody.getDragCoefficient());
            output.putFloat("friction", rigidBody.getFriction());
            output.putFloat("restitution", rigidBody.getRestitution());
            output.putBoolean("terrainLoadingEnabled", rigidBody.terrainLoadingEnabled());
            output.putInt("buoyancyType", rigidBody.getBuoyancyType().ordinal());
            output.putInt("dragType", rigidBody.getDragType().ordinal());
        }
    }

    @Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V"))
    public void load(ValueInput input, CallbackInfo info) {
        if (EntityPhysicsElement.is((Entity) (Object) this)) {
            EntityPhysicsElement.get((Entity) (Object) this).getRigidBody().readTagInfo(input);
        }
    }
}
