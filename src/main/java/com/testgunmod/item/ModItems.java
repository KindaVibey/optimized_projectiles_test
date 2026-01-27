package com.testgunmod.item;

import com.testgunmod.TestGunMod;
import com.testgunmod.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TestGunMod.MOD_ID);

    // Gun item
    public static final RegistryObject<Item> GUN = ITEMS.register("gun",
            () -> new GunItem(new Item.Properties()));

    // Gun turret block item
    public static final RegistryObject<Item> GUN_TURRET = ITEMS.register("gun_turret",
            () -> new BlockItem(ModBlocks.GUN_TURRET.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}