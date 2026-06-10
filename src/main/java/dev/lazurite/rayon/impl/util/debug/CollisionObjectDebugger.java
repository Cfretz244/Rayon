package dev.lazurite.rayon.impl.util.debug;


import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import dev.lazurite.rayon.api.event.render.DebugRenderEvents;
import dev.lazurite.rayon.impl.bullet.collision.body.MinecraftRigidBody;
import dev.lazurite.rayon.impl.bullet.collision.body.shape.MinecraftShape;
import dev.lazurite.rayon.impl.bullet.collision.body.ElementRigidBody;
import dev.lazurite.rayon.impl.bullet.collision.space.MinecraftSpace;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

/**
 * This class handles debug rendering on the client. Press F3+r to render
 * all {@link ElementRigidBody} objects present in the {@link MinecraftSpace}.
 *
 * 26.1: drawn through the vanilla gizmo system (world-space lines).
 */
public final class CollisionObjectDebugger {
    private static boolean enabled;

    private CollisionObjectDebugger() {}

    public static boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void renderSpace(MinecraftSpace space, float tickDelta) {
        DebugRenderEvents.BEFORE_RENDER.invoke(new DebugRenderEvents.Context(space, tickDelta));

        space.getTerrainMap().values().forEach(terrain -> CollisionObjectDebugger.renderBody(terrain, tickDelta));
        space.getRigidBodiesByClass(ElementRigidBody.class).forEach(elementRigidBody -> CollisionObjectDebugger.renderBody(elementRigidBody, tickDelta));
    }

    public static void renderBody(MinecraftRigidBody rigidBody, float tickDelta) {
        final var position = rigidBody.isStatic() ?
                rigidBody.getPhysicsLocation(new Vector3f()) :
                ((ElementRigidBody) rigidBody).getFrame().getLocation(new Vector3f(), tickDelta);

        final var rotation = rigidBody.isStatic() ?
                rigidBody.getPhysicsRotation(new Quaternion()) :
                ((ElementRigidBody) rigidBody).getFrame().getRotation(new Quaternion(), tickDelta);

        final var color = rigidBody.getOutlineColor();
        final var argb = 0xFF000000
                | ((int) (color.x * 255.0f) << 16)
                | ((int) (color.y * 255.0f) << 8)
                | (int) (color.z * 255.0f);

        renderShape(rigidBody.getMinecraftShape(), position, rotation, argb);
    }

    public static void renderShape(MinecraftShape shape, Vector3f position, Quaternion rotation, int color) {
        for (final var triangle : shape.getTriangles(Quaternion.IDENTITY)) {
            final var vertices = triangle.getVertices();
            final var p0 = toWorld(vertices[0], position, rotation);
            final var p1 = toWorld(vertices[1], position, rotation);
            final var p2 = toWorld(vertices[2], position, rotation);

            Gizmos.line(p0, p1, color);
            Gizmos.line(p1, p2, color);
            Gizmos.line(p2, p0, color);
        }
    }

    private static Vec3 toWorld(Vector3f vertex, Vector3f position, Quaternion rotation) {
        final var rotated = rotation.mult(vertex, null).addLocal(position);
        return new Vec3(rotated.x, rotated.y, rotated.z);
    }
}
