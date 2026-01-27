package com.testgunmod.item;

import com.testgunmod.entity.BulletEntity;
import com.testgunmod.entity.ModEntityTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GunItem extends Item {

    public GunItem(Properties properties) {
        super(properties.stacksTo(1).durability(500));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Get player's look direction
            Vec3 lookVec = player.getLookAngle();

            // Create bullet velocity (4 blocks per tick)
            Vec3 velocity = lookVec.scale(4.0);

            // Spawn bullet from player's eye position
            Vec3 spawnPos = player.getEyePosition(1.0f);

            BulletEntity bullet = new BulletEntity(
                    ModEntityTypes.BULLET.get(),
                    level,
                    spawnPos,
                    velocity,
                    10.0f // damage
            );

            level.addFreshEntity(bullet);

            // Play sound
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 1.5f);

            // Add cooldown (half second)
            player.getCooldowns().addCooldown(this, 10);

            // Damage item
            itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
        }

        return InteractionResultHolder.success(itemStack);
    }
}