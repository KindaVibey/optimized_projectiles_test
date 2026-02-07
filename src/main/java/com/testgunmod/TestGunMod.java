package com.testgunmod;

import com.testgunmod.block.ModBlocks;
import com.testgunmod.entity.ModEntityTypes;
import com.testgunmod.item.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TestGunMod.MOD_ID)
public class TestGunMod {
    public static final String MOD_ID = "testgunmod";

    public TestGunMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntityTypes.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
    }
}