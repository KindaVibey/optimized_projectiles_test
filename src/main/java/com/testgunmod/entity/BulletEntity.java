package com.testgunmod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import java.util.List;

/**
 * Optimized bullet entity with no distance-based despawning
 */
public class BulletEntity extends Entity {

    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.FLOAT);

    private int ticksAlive = 0;
    private boolean hasHit = false;

    // Physics constants
    private static final double AIR_DRAG = 0.99;
    private static final double GRAVITY = 0.015;
    private static final double COLLISION_INFLATE = 0.8;
    private static final double TARGET_INFLATE = 0.1;

    public BulletEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true; // CHANGED: Prevent culling-based removal
    }

    public BulletEntity(EntityType<?> type, Level level, Vec3 position, Vec3 velocity, float damage) {
        this(type, level);
        this.setPos(position.x, position.y, position.z);
        this.setDeltaMovement(velocity);
        this.updateRotation();
        this.entityData.set(DATA_DAMAGE, damage);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_DAMAGE, 10.0f);
    }

    @Override
    public void tick() {
        super.tick();

        ticksAlive++;

        // Only despawn after 30 seconds or on hit
        if (ticksAlive > 600 || hasHit) {
            this.discard();
            return;
        }

        Vec3 motion = this.getDeltaMovement();

        if (!this.level().isClientSide) {
            checkCollisions();
        }

        // Move with current velocity
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        // Apply physics for next tick
        this.setDeltaMovement(motion.x * AIR_DRAG, motion.y - GRAVITY, motion.z * AIR_DRAG);

        this.updateRotation();
    }

    private void checkCollisions() {
        Vec3 currentPos = this.position();
        Vec3 motion = this.getDeltaMovement();

        double motionLength = motion.length();
        int steps = Math.max(1, (int) Math.ceil(motionLength * 2));

        for (int i = 0; i < steps; i++) {
            double stepProgress = (double) i / steps;
            double nextStepProgress = (double) (i + 1) / steps;

            Vec3 stepStart = currentPos.add(motion.scale(stepProgress));
            Vec3 stepEnd = currentPos.add(motion.scale(nextStepProgress));

            BlockHitResult blockHit = this.level().clip(new ClipContext(
                    stepStart, stepEnd,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    this
            ));

            if (blockHit.getType() != HitResult.Type.MISS) {
                hasHit = true;
                return;
            }

            AABB searchBox = new AABB(stepStart, stepEnd).inflate(COLLISION_INFLATE);

            List<Entity> entities = this.level().getEntities(this, searchBox,
                    e -> e.isAlive() && e.isPickable() && !(e instanceof BulletEntity));

            for (Entity target : entities) {
                AABB targetBox = target.getBoundingBox().inflate(TARGET_INFLATE);
                Vec3 hit = targetBox.clip(stepStart, stepEnd).orElse(null);

                if (hit != null) {
                    float damage = this.entityData.get(DATA_DAMAGE);
                    DamageSource source = this.damageSources().mobProjectile(this, null);
                    target.hurt(source, damage);

                    hasHit = true;
                    return;
                }
            }
        }
    }

    private void updateRotation() {
        Vec3 motion = this.getDeltaMovement();
        double horizontalDist = motion.horizontalDistance();

        this.setYRot((float)(Mth.atan2(motion.x, motion.z) * (180.0 / Math.PI)));
        this.setXRot((float)(Mth.atan2(motion.y, horizontalDist) * (180.0 / Math.PI)));

        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    public void checkDespawn() {
        // Override to prevent any distance-based despawning
    }

    // Force the entity to always be considered "in range" for tracking
    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true; // Always render regardless of distance
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected float getEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dimensions) {
        return 0.0f;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.ticksAlive = tag.getInt("Age");
        if (tag.contains("Damage")) {
            this.entityData.set(DATA_DAMAGE, tag.getFloat("Damage"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.ticksAlive);
        tag.putFloat("Damage", this.entityData.get(DATA_DAMAGE));
    }
}