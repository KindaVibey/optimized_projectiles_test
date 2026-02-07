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
 * Ultra-optimized bullet entity with CLIENT-SIDE PHYSICS PREDICTION
 *
 * Physics run on BOTH client and server with IDENTICAL code.
 * No desyncs because client spawns with server's exact initial state.
 */
public class BulletEntity extends Entity {

    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.FLOAT);

    // SYNC AGE SO CLIENT KNOWS IF IT MISSED TICKS
    private static final EntityDataAccessor<Integer> DATA_AGE =
            SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.INT);

    private int ticksAlive = 0;
    private AABB cachedSearchBox;

    // Track if this is the first tick after spawning on client
    private boolean firstClientTick = false;

    // Physics constants - MUST BE SAME ON CLIENT AND SERVER
    private static final double AIR_DRAG = 0.99;
    private static final double GRAVITY = 0.015;

    // Collision constants (server only)
    private static final double COLLISION_MARGIN = 0.10;

    // Lifetime
    private static final int MAX_LIFETIME_TICKS = 1200; // 60 seconds

    public BulletEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public BulletEntity(EntityType<?> type, Level level, Vec3 position, Vec3 velocity, float damage) {
        this(type, level);
        this.setPos(position.x, position.y, position.z);
        this.setDeltaMovement(velocity);
        this.updateRotation();
        this.entityData.set(DATA_DAMAGE, damage);
        this.entityData.set(DATA_AGE, 0);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_DAMAGE, 10.0f);
        this.entityData.define(DATA_AGE, 0);
    }

    @Override
    public void tick() {
        super.tick();

        // CLIENT: On first tick after receiving spawn packet, don't run physics yet
        // The position/velocity we received is ALREADY after server ran physics
        if (this.level().isClientSide && firstClientTick) {
            firstClientTick = false;
            ticksAlive++;
            this.entityData.set(DATA_AGE, ticksAlive);
            return; // Skip physics this tick - we're already at the correct position
        }

        // Quick despawn check
        if (++ticksAlive > MAX_LIFETIME_TICKS) {
            this.discard();
            return;
        }

        // Sync age to clients
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_AGE, ticksAlive);
        }

        Vec3 motion = this.getDeltaMovement();
        Vec3 currentPos = this.position();

        // Calculate next position using CURRENT velocity (before physics modification)
        Vec3 nextPos = currentPos.add(motion);

        // SERVER-SIDE: Full collision detection
        if (!this.level().isClientSide) {
            // Single block raytrace
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

            // Reuse cached AABB or create new one
            if (cachedSearchBox == null) {
                cachedSearchBox = new AABB(currentPos, nextPos).inflate(COLLISION_MARGIN);
            } else {
                // Update existing AABB (faster than creating new)
                double minX = Math.min(currentPos.x, nextPos.x) - COLLISION_MARGIN;
                double minY = Math.min(currentPos.y, nextPos.y) - COLLISION_MARGIN;
                double minZ = Math.min(currentPos.z, nextPos.z) - COLLISION_MARGIN;
                double maxX = Math.max(currentPos.x, nextPos.x) + COLLISION_MARGIN;
                double maxY = Math.max(currentPos.y, nextPos.y) + COLLISION_MARGIN;
                double maxZ = Math.max(currentPos.z, nextPos.z) + COLLISION_MARGIN;
                cachedSearchBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
            }

            // Single entity query with minimal predicate
            List<Entity> entities = this.level().getEntities(this, cachedSearchBox,
                    e -> e.isAlive() && e.isPickable());

            // Check only first entity (fastest)
            if (!entities.isEmpty()) {
                Entity target = entities.get(0);

                // Quick AABB intersection test
                if (cachedSearchBox.intersects(target.getBoundingBox())) {
                    float damage = this.entityData.get(DATA_DAMAGE);
                    target.hurt(this.damageSources().mobProjectile(this, null), damage);
                    this.discard();
                    return;
                }
            }
        }
        // CLIENT-SIDE: Physics prediction only (no collision)

        // Move bullet FIRST with current velocity
        this.setPos(nextPos.x, nextPos.y, nextPos.z);

        // THEN apply physics for NEXT tick's velocity
        Vec3 newMotion = new Vec3(
                motion.x * AIR_DRAG,
                motion.y - GRAVITY,
                motion.z * AIR_DRAG
        );
        this.setDeltaMovement(newMotion);

        // Update rotation for rendering
        this.updateRotation();
    }

    private void updateRotation() {
        Vec3 motion = this.getDeltaMovement();
        double horizontalDist = motion.horizontalDistance();

        if (horizontalDist > 0.001) {
            // Inlined constants for performance: 180/PI = 57.2957795
            this.setYRot((float)(Mth.atan2(motion.x, motion.z) * 57.2957795));
            this.setXRot((float)(Mth.atan2(motion.y, horizontalDist) * 57.2957795));

            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }
    }

    @Override
    public void checkDespawn() {
        // Override to prevent distance-based despawning
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);

        // Set velocity from packet
        double vx = packet.getXa();
        double vy = packet.getYa();
        double vz = packet.getZa();
        this.setDeltaMovement(vx, vy, vz);
        this.updateRotation();

        // CRITICAL: Mark that we just spawned on client
        // On the very first tick, we should NOT run physics because
        // the position we received has ALREADY had physics applied server-side
        if (this.level().isClientSide) {
            this.firstClientTick = true;
        }
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