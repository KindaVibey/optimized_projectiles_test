package com.testgunmod.entity;

import com.testgunmod.TestGunMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TestGunMod.MOD_ID);

    public static final RegistryObject<EntityType<BulletEntity>> BULLET =
            ENTITY_TYPES.register("bullet", () -> EntityType.Builder.<BulletEntity>of(
                            BulletEntity::new,
                            MobCategory.MISC
                    )
                    .sized(0.1f, 0.1f)         // Reduced from 0.15f for better performance
                    .clientTrackingRange(8)    // Increased to 8 chunks for long-range bullets
                    .updateInterval(20)  // Reasonable updates every 20 ticks for sync
                    .setShouldReceiveVelocityUpdates(true)   // Enable velocity sync for client prediction
                    .fireImmune()
                    .build("bullet"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
