package dev.lazurite.rayon.impl.util.debug;


import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.lazurite.rayon.api.event.render.DebugRenderEvents;
import dev.lazurite.rayon.impl.bullet.collision.body.MinecraftRigidBody;
import dev.lazurite.rayon.impl.bullet.collision.body.shape.MinecraftShape;
import dev.lazurite.rayon.impl.bullet.math.Convert;
import dev.lazurite.rayon.impl.bullet.collision.body.ElementRigidBody;
import dev.lazurite.rayon.impl.bullet.collision.space.MinecraftSpace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CoreShaders;

/**
 * This class handles debug rendering on the client. Press F3+r to render
 * all {@link ElementRigidBody} objects present in the {@link MinecraftSpace}.
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

    public static void renderSpace(MinecraftSpace space, PoseStack stack, float tickDelta) {
        final var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        // 1.21.2: core shader programs are registry constants; setShader takes the ShaderProgram directly.
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);

        // 1.21: Tesselator.begin(mode, format) now both allocates and begins the BufferBuilder
        // (there is no getBuilder()/builder.begin()); the draw is BufferUploader.drawWithShader(MeshData).
        final var builder = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        DebugRenderEvents.BEFORE_RENDER.invoke(new DebugRenderEvents.Context(space, builder, stack, cameraPos, tickDelta));

        space.getTerrainMap().values().forEach(terrain -> CollisionObjectDebugger.renderBody(terrain, builder, stack, tickDelta));
        space.getRigidBodiesByClass(ElementRigidBody.class).forEach(elementRigidBody -> CollisionObjectDebugger.renderBody(elementRigidBody, builder, stack, tickDelta));

        final var mesh = builder.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
    }

    public static void renderBody(MinecraftRigidBody rigidBody, BufferBuilder builder, PoseStack stack, float tickDelta) {
        final var position = rigidBody.isStatic() ?
                rigidBody.getPhysicsLocation(new Vector3f()) :
                ((ElementRigidBody) rigidBody).getFrame().getLocation(new Vector3f(), tickDelta);

        final var rotation = rigidBody.isStatic() ?
                rigidBody.getPhysicsRotation(new Quaternion()) :
                ((ElementRigidBody) rigidBody).getFrame().getRotation(new Quaternion(), tickDelta);

        renderShape(rigidBody.getMinecraftShape(), position, rotation, builder, stack, rigidBody.getOutlineColor(), 1.0f);
    }

    public static void renderShape(MinecraftShape shape, Vector3f position, Quaternion rotation, BufferBuilder builder, PoseStack stack, Vector3f color, float alpha) {
        final var triangles = shape.getTriangles(Quaternion.IDENTITY);
        final var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        for (var triangle : triangles) {
            final var vertices = triangle.getVertices();

            stack.pushPose();
            stack.translate(position.x - cameraPos.x, position.y - cameraPos.y, position.z - cameraPos.z);
            stack.mulPose(Convert.toMinecraft(rotation));
            final var p1 = vertices[0];
            final var p2 = vertices[1];
            final var p3 = vertices[2];

            builder.addVertex(stack.last().pose(), p1.x, p1.y, p1.z).setColor(color.x, color.y, color.z, alpha);
            builder.addVertex(stack.last().pose(), p2.x, p2.y, p2.z).setColor(color.x, color.y, color.z, alpha);
            builder.addVertex(stack.last().pose(), p3.x, p3.y, p3.z).setColor(color.x, color.y, color.z, alpha);
            builder.addVertex(stack.last().pose(), p1.x, p1.y, p1.z).setColor(color.x, color.y, color.z, alpha);
            stack.popPose();
        }
    }
}