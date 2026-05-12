package com.testgunmod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import java.util.List;

public class BulletEntity extends Projectile {

    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.FLOAT);

    public static final double AIR_DRAG  = 0.99;

    public static final double GRAVITY   = 0.015;

    public static final double COLLISION_MARGIN = 0.10;

    public static final int MAX_LIFETIME_TICKS = 1200;

    private int ticksAlive = 0;

    private AABB cachedSearchBox = null;

    private Vec3 spawnVelocity = null;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level,
                        Vec3 position, Vec3 velocity, float damage) {
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
        this.baseTick();

        if (++ticksAlive > MAX_LIFETIME_TICKS) {
            this.discard();
            return;
        }

        Vec3 motion     = this.getDeltaMovement();
        Vec3 currentPos = this.position();
        Vec3 nextPos    = currentPos.add(motion);

        if (!this.level().isClientSide) {
            BlockHitResult blockHit = this.level().clip(new ClipContext(
                    currentPos, nextPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    this
            ));

            if (blockHit.getType() != HitResult.Type.MISS) {
                this.discard();
                return;
            }

            updateCachedSearchBox(currentPos, nextPos);

            List<Entity> nearby = this.level().getEntities(
                    this, cachedSearchBox,
                    e -> e.isAlive() && e.isPickable()
            );

            if (!nearby.isEmpty()) {
                Entity target = nearby.get(0);
                if (cachedSearchBox.intersects(target.getBoundingBox())) {
                    float dmg = this.entityData.get(DATA_DAMAGE);
                    target.hurt(this.damageSources().mobProjectile(this, null), dmg);
                    this.discard();
                    return;
                }
            }
        }
        this.setPos(nextPos.x, nextPos.y, nextPos.z);

        this.setDeltaMovement(
                motion.x * AIR_DRAG,
                motion.y - GRAVITY,
                motion.z * AIR_DRAG
        );

        this.updateRotation();
    }

    @Override
    protected void updateRotation() {
        Vec3 motion = this.getDeltaMovement();
        double hDist = motion.horizontalDistance();
        if (hDist > 0.001) {
            this.setYRot((float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
            this.setXRot((float) (Mth.atan2(motion.y, hDist)   * Mth.RAD_TO_DEG));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        this.setPos(packet.getX(), packet.getY(), packet.getZ());

        double vx = packet.getXa();
        double vy = packet.getYa();
        double vz = packet.getZa();
        this.setDeltaMovement(vx, vy, vz);
        this.updateRotation();

        this.ticksAlive = 0;
        this.cachedSearchBox = null;
    }

    private void updateCachedSearchBox(Vec3 from, Vec3 to) {
        double minX = Math.min(from.x, to.x) - COLLISION_MARGIN;
        double minY = Math.min(from.y, to.y) - COLLISION_MARGIN;
        double minZ = Math.min(from.z, to.z) - COLLISION_MARGIN;
        double maxX = Math.max(from.x, to.x) + COLLISION_MARGIN;
        double maxY = Math.max(from.y, to.y) + COLLISION_MARGIN;
        double maxZ = Math.max(from.z, to.z) + COLLISION_MARGIN;

        if (cachedSearchBox == null) {
            cachedSearchBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        } else {
            cachedSearchBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    @Override
    public void checkDespawn() {}

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override public boolean isPickable()        { return false; }
    @Override public boolean isPushable()        { return false; }
    @Override public boolean canBeCollidedWith() { return false; }

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
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


    public float getDamage() {
        return this.entityData.get(DATA_DAMAGE);
    }

    public void setDamage(float damage) {
        this.entityData.set(DATA_DAMAGE, damage);
    }

    public int getTicksAlive() {
        return ticksAlive;
    }
}