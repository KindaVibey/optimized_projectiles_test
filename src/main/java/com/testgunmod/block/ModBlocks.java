package com.testgunmod.block;

import com.testgunmod.TestGunMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, TestGunMod.MOD_ID);

    public static final RegistryObject<Block> GUN_TURRET = BLOCKS.register("gun_turret",
            () -> new GunTurretBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}