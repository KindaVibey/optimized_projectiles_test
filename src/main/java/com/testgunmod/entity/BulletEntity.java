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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import java.util.List;

/**
 * Optimized bullet entity - smooth physics, high performance
 */
public class BulletEntity extends Entity {

    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.FLOAT);

    private int ticksAlive = 0;
    private boolean hasHit = false;

    public BulletEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = false;
    }

    public BulletEntity(EntityType<?> type, Level level, Vec3 position, Vec3 velocity, float damage) {
        this(type, level);
        this.setPos(position.x, position.y, position.z);
        this.setDeltaMovement(velocity);
        this.updateRotation();
        this.entityData.set(DATA_DAMAGE, damage);
        this.setNoGravity(true); // We handle gravity manually
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_DAMAGE, 10.0f);
    }

    @Override
    public void tick() {
        super.tick();

        ticksAlive++;

        // Despawn after 5 seconds or if already hit
        if (ticksAlive > 100 || hasHit) {
            this.discard();
            return;
        }

        // Server-side collision
        if (!this.level().isClientSide) {
            checkCollisions();
        }

        // Apply gravity
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x, motion.y - 0.03, motion.z);

        // Update position
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        // Update rotation to match velocity
        this.updateRotation();
    }

    private void checkCollisions() {
        Vec3 currentPos = this.position();
        Vec3 motion = this.getDeltaMovement();
        Vec3 nextPos = currentPos.add(motion);

        // Block collision
        BlockHitResult blockHit = this.level().clip(new ClipContext(
                currentPos, nextPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));

        if (blockHit.getType() != HitResult.Type.MISS) {
            hasHit = true;
            this.discard();
            return;
        }

        // Entity collision
        AABB searchBox = this.getBoundingBox().expandTowards(motion).inflate(0.5);
        List<Entity> entities = this.level().getEntities(this, searchBox,
                e -> e.isAlive() && e.isPickable() && !(e instanceof BulletEntity));

        for (Entity target : entities) {
            AABB targetBox = target.getBoundingBox().inflate(0.3);
            Vec3 hit = targetBox.clip(currentPos, nextPos).orElse(null);

            if (hit != null) {
                // Hit entity
                float damage = this.entityData.get(DATA_DAMAGE);
                DamageSource source = this.damageSources().mobProjectile(this, null);
                target.hurt(source, damage);

                hasHit = true;
                this.discard();
                return;
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
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.ticksAlive);
    }
}