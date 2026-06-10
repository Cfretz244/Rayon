package dev.lazurite.rayon.impl.mixin.common.entity;

import dev.lazurite.rayon.api.EntityPhysicsElement;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Prevents movement/velocity packets from being sent for {@link EntityPhysicsElement}s
 * (their state is synced by Rayon itself).
 *
 * 26.1: sendChanges dispatches through the Synchronizer instead of a Consumer, so the old
 * version-fragile accept-ordinal redirects are replaced by a packet-type filter over every
 * sendToTrackingPlayers call in the method.
 */
@Mixin(ServerEntity.class)
public class EntityTrackerEntryMixin {
    @Shadow @Final private Entity entity;

    @Redirect(
            method = "sendChanges",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayers(Lnet/minecraft/network/protocol/Packet;)V"
            )
    )
    public void sendChanges$filter(ServerEntity.Synchronizer synchronizer, Packet<? super ClientGamePacketListener> packet) {
        if (EntityPhysicsElement.is(this.entity)
                && (packet instanceof ClientboundMoveEntityPacket || packet instanceof ClientboundSetEntityMotionPacket)) {
            return;
        }

        synchronizer.sendToTrackingPlayers(packet);
    }
}
