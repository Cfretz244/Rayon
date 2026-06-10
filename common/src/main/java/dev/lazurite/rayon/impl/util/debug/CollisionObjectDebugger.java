package dev.lazurite.rayon.impl.util.debug;


import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.lazurite.rayon.api.event.render.DebugRenderEvents;
import dev.lazurite.rayon.impl.bullet.collision.body.MinecraftRigidBody;
import dev.lazurite.rayon.impl.bullet.collision.body.shape.MinecraftShape;
import dev.lazurite.rayon.impl.bullet.math.Convert;
import dev.lazurite.rayon.impl.bullet.collision.body.ElementRigidBody;
import dev.lazurite.rayon.impl.bullet.collision.space.MinecraftSpace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

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

    // 1.21.5: immediate-mode drawing (Tesselator.begin + BufferUploader.drawWithShader) is gone;
    // draw line geometry through the debug pass's BufferSource with RenderType.lines().
    public static void renderSpace(MinecraftSpace space, PoseStack stack, MultiBufferSource.BufferSource bufferSource, float tickDelta) {
        final var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        final var consumer = bufferSource.getBuffer(RenderType.lines());

        DebugRenderEvents.BEFORE_RENDER.invoke(new DebugRenderEvents.Context(space, consumer, stack, cameraPos, tickDelta));

        space.getTerrainMap().values().forEach(terrain -> CollisionObjectDebugger.renderBody(terrain, consumer, stack, tickDelta));
        space.getRigidBodiesByClass(ElementRigidBody.class).forEach(elementRigidBody -> CollisionObjectDebugger.renderBody(elementRigidBody, consumer, stack, tickDelta));

        bufferSource.endBatch(RenderType.lines());
    }

    public static void renderBody(MinecraftRigidBody rigidBody, VertexConsumer consumer, PoseStack stack, float tickDelta) {
        final var position = rigidBody.isStatic() ?
                rigidBody.getPhysicsLocation(new Vector3f()) :
                ((ElementRigidBody) rigidBody).getFrame().getLocation(new Vector3f(), tickDelta);

        final var rotation = rigidBody.isStatic() ?
                rigidBody.getPhysicsRotation(new Quaternion()) :
                ((ElementRigidBody) rigidBody).getFrame().getRotation(new Quaternion(), tickDelta);

        renderShape(rigidBody.getMinecraftShape(), position, rotation, consumer, stack, rigidBody.getOutlineColor(), 1.0f);
    }

    public static void renderShape(MinecraftShape shape, Vector3f position, Quaternion rotation, VertexConsumer consumer, PoseStack stack, Vector3f color, float alpha) {
        final var triangles = shape.getTriangles(Quaternion.IDENTITY);
        final var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        for (var triangle : triangles) {
            final var vertices = triangle.getVertices();

            stack.pushPose();
            stack.translate(position.x - cameraPos.x, position.y - cameraPos.y, position.z - cameraPos.z);
            stack.mulPose(Convert.toMinecraft(rotation));

            line(consumer, stack.last(), vertices[0], vertices[1], color, alpha);
            line(consumer, stack.last(), vertices[1], vertices[2], color, alpha);
            line(consumer, stack.last(), vertices[2], vertices[0], color, alpha);

            stack.popPose();
        }
    }

    private static void line(VertexConsumer consumer, PoseStack.Pose pose, Vector3f p1, Vector3f p2, Vector3f color, float alpha) {
        final var normal = p2.subtract(p1).normalize();
        consumer.addVertex(pose.pose(), p1.x, p1.y, p1.z).setColor(color.x, color.y, color.z, alpha).setNormal(pose, normal.x, normal.y, normal.z);
        consumer.addVertex(pose.pose(), p2.x, p2.y, p2.z).setColor(color.x, color.y, color.z, alpha).setNormal(pose, normal.x, normal.y, normal.z);
    }
}
