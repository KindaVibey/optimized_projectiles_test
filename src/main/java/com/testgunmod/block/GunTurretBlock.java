package com.testgunmod.block;

import com.testgunmod.entity.BulletEntity;
import com.testgunmod.entity.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;

public class GunTurretBlock extends Block {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    // EVERY TICK FIRING for maximum stress testing
    // This will spawn 20 bullets per second per turret
    // With 9 turrets = 180 bullets/second
    private static final int FIRE_RATE_TICKS = 1;

    public GunTurretBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block block, BlockPos fromPos, boolean isMoving) {

        if (!level.isClientSide) {
            if (level.hasNeighborSignal(pos)) {
                // Start ticking immediately
                level.scheduleTick(pos, this, FIRE_RATE_TICKS);
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.hasNeighborSignal(pos)) {
            shootBullet(level, pos, state);

            // Schedule next shot immediately (every tick)
            level.scheduleTick(pos, this, FIRE_RATE_TICKS);
        }
    }

    private void shootBullet(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);

        // Calculate spawn position (center of block + offset in facing direction)
        Vec3 spawnPos = Vec3.atCenterOf(pos).add(
                facing.getStepX() * 0.6,
                facing.getStepY() * 0.6,
                facing.getStepZ() * 0.6
        );

        // Calculate velocity in facing direction
        Vec3 velocity = new Vec3(
                facing.getStepX() * 4.0,
                facing.getStepY() * 4.0,
                facing.getStepZ() * 4.0
        );

        // Create and spawn bullet
        BulletEntity bullet = new BulletEntity(
                ModEntityTypes.BULLET.get(),
                level,
                spawnPos,
                velocity,
                10.0f // damage
        );

        level.addFreshEntity(bullet);

        // Play sound (can comment out for even better performance)
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS, 0.5f, 1.5f);
    }
}