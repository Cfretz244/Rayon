package dev.lazurite.rayon.impl.mixin.client;

import com.jme3.bullet.objects.PhysicsRigidBody;
import dev.lazurite.rayon.impl.util.debug.CollisionObjectDebugger;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds an F3 key combination (F3 + R). It toggles
 * renders for all relevant {@link PhysicsRigidBody} objects.
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardMixin {
    // 26.1: back to varargs.
    @Shadow protected abstract void debugFeedbackTranslated(String string, Object... args);

    // 26.1: handleDebugKeys takes a KeyEvent.
    @Inject(method = "handleDebugKeys", at = @At("HEAD"), cancellable = true)
    private void processF3(net.minecraft.client.input.KeyEvent event, CallbackInfoReturnable<Boolean> info) {
        if (event.key() == 82) { // 'r' key
            boolean enabled = CollisionObjectDebugger.toggle();

            if (enabled) {
                debugFeedbackTranslated("debug.rayon.on");
            } else {
                debugFeedbackTranslated("debug.rayon.off");
            }

            info.setReturnValue(true);
        }
    }
}
