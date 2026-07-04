package dev.lazurite.rayon.impl.bullet.collision.body;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Quaternion;
import dev.lazurite.rayon.api.PhysicsElement;
import dev.lazurite.rayon.impl.bullet.collision.body.shape.MinecraftShape;
import dev.lazurite.rayon.impl.bullet.collision.space.MinecraftSpace;
import dev.lazurite.rayon.impl.bullet.math.Convert;
import dev.lazurite.rayon.impl.bullet.thread.util.Clock;
import dev.lazurite.rayon.impl.util.Frame;
import com.jme3.math.Vector3f;
import dev.lazurite.toolbox.api.math.QuaternionHelper;
import dev.lazurite.toolbox.api.math.VectorHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;

import java.security.InvalidParameterException;

public abstract class ElementRigidBody extends MinecraftRigidBody {
    public static final float SLEEP_TIME_IN_SECONDS = 2.0f;

    protected final PhysicsElement element;

    private final Frame frame;
    private final Clock sleepTimer;
    private boolean terrainLoading;
    private float dragCoefficient;
    // Multiplier on the water-drag force applied to submerged triangles (see PressureGenerator).
    // 1.0 = normal water drag; 0.0 = none. Lets a body (e.g. a submersible) move through water with
    // reduced resistance without disabling its other drag. Defaults to 1.0 so existing bodies are
    // unaffected.
    private float waterDragScale = 1.0f;
    // Reference area used by the SIMPLE central air drag (see PressureGenerator). When < 0 (the
    // default) the drag area is derived from the collision box, as it always was. Setting an
    // explicit value DECOUPLES aerodynamic drag from the collision box, so a body can keep a tiny
    // collision box (e.g. for fitting through gaps) without losing drag / gaining top speed. The
    // value is Σ(half-extent²) of the desired aero box, matching the box-derived formula.
    private float dragArea = -1.0f;
    private BuoyancyType buoyancyType;
    private DragType dragType;
    private BoundingBox currentBoundingBox = new BoundingBox();
    private AABB currentMinecraftBoundingBox = new AABB(0, 0, 0, 0, 0 ,0);

    public ElementRigidBody(PhysicsElement element, MinecraftSpace space, MinecraftShape shape, float mass, float dragCoefficient, float friction, float restitution) {
        super(space, shape, mass);

        if (shape instanceof MinecraftShape.Concave) {
            throw new InvalidParameterException("Only massless rigid bodies can use concave shapes.");
        }

        this.element = element;
        this.frame = new Frame();
        this.sleepTimer = new Clock();

        this.setTerrainLoadingEnabled(!this.isStatic());
        this.setDragCoefficient(dragCoefficient);
        this.setFriction(friction);
        this.setRestitution(restitution);
        this.setBuoyancyType(BuoyancyType.WATER);
        this.setDragType(DragType.SIMPLE);
    }

    public PhysicsElement getElement() {
        return this.element;
    }

    // 1.21.6: reads from a ValueInput (same keys/format as the old CompoundTag).
    public void readTagInfo(ValueInput input) {
        input.read("orientation", CompoundTag.CODEC).ifPresent(tag -> this.setPhysicsRotation(Convert.toBullet(QuaternionHelper.fromTag(tag))));
        input.read("linearVelocity", CompoundTag.CODEC).ifPresent(tag -> this.setLinearVelocity(Convert.toBullet(VectorHelper.fromTag(tag))));
        input.read("angularVelocity", CompoundTag.CODEC).ifPresent(tag -> this.setAngularVelocity(Convert.toBullet(VectorHelper.fromTag(tag))));
//        this.setMass(tag.getFloat("mass"));
//        this.setDragCoefficient(tag.getFloat("dragCoefficient"));
//        this.setFriction(tag.getFloat("friction"));
//        this.setRestitution(tag.getFloat("restitution"));
//        this.setBuoyancyType(ElementRigidBody.BuoyancyType.values()[tag.getInt("buoyancyType")]);
//        this.setDragType(ElementRigidBody.DragType.values()[tag.getInt("dragType")]);
    }

    public boolean terrainLoadingEnabled() {
        return this.terrainLoading && !this.isStatic();
    }

    public void setTerrainLoadingEnabled(boolean terrainLoading) {
        this.terrainLoading = terrainLoading;
    }

    public float getDragCoefficient() {
        return dragCoefficient;
    }

    public void setDragCoefficient(float dragCoefficient) {
        this.dragCoefficient = dragCoefficient;
    }

    public float getWaterDragScale() {
        return this.waterDragScale;
    }

    public void setWaterDragScale(float waterDragScale) {
        this.waterDragScale = waterDragScale;
    }

    public float getDragArea() {
        return this.dragArea;
    }

    public void setDragArea(float dragArea) {
        this.dragArea = dragArea;
    }

    public BuoyancyType getBuoyancyType() {
        return this.buoyancyType;
    }

    public void setBuoyancyType(BuoyancyType buoyancyType) {
        this.buoyancyType = buoyancyType;
    }

    public DragType getDragType() {
        return this.dragType;
    }

    public void setDragType(DragType dragType) {
        this.dragType = dragType;
    }

    public Frame getFrame() {
        return this.frame;
    }

    public Clock getSleepTimer() {
        return this.sleepTimer;
    }

    @Override
    public Vector3f getOutlineColor() {
        return this.isActive() ? new Vector3f(1.0f, 1.0f, 1.0f) : new Vector3f(1.0f, 0.0f, 0.0f);
    }

    public void updateFrame() {
        getFrame().from(getFrame(), getPhysicsLocation(new Vector3f()), getPhysicsRotation(new Quaternion()));
        this.updateBoundingBox();
    }

    public boolean isNear(BlockPos blockPos) {
        return this.currentMinecraftBoundingBox.intersects(new AABB(blockPos).inflate(0.5f));
    }

    public boolean isNear(SectionPos blockPos) {
        return this.currentMinecraftBoundingBox.intersects(new AABB(blockPos.center()).inflate(8.5f));
    }

    public boolean isWaterBuoyancyEnabled() {
        return buoyancyType == BuoyancyType.WATER || buoyancyType == BuoyancyType.ALL;
    }

    public boolean isAirBuoyancyEnabled() {
        return buoyancyType == BuoyancyType.AIR || buoyancyType == BuoyancyType.ALL;
    }

    public boolean isWaterDragEnabled() {
        // We check for simple drag here, but complex drag is always used for water buoyancy.
        return dragType == DragType.WATER || dragType == DragType.ALL || dragType == DragType.SIMPLE;
    }

    public boolean isAirDragEnabled() {
        return dragType == DragType.AIR || dragType == DragType.ALL;
    }

    public void updateBoundingBox() {
        this.currentBoundingBox = this.boundingBox(this.currentBoundingBox);
        this.currentMinecraftBoundingBox = Convert.toMinecraft(this.currentBoundingBox);
    }

    public AABB getCurrentMinecraftBoundingBox() {
        return currentMinecraftBoundingBox;
    }

    public BoundingBox getCurrentBoundingBox() {
        return currentBoundingBox;
    }


    public enum BuoyancyType {
        NONE,
        AIR,
        WATER,
        ALL
    }

    public enum DragType {
        NONE,
        AIR,
        WATER,
        SIMPLE,
        ALL
    }
}