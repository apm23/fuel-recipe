package com.anjas.fuelrecipe;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class ModBlocks {
    private static final ResourceKey<Block> WIDE_LIGHT_KEY = blockKey("wide_light");
    private static final ResourceKey<Block> GREATER_LIGHT_KEY = blockKey("greater_light");
    private static final ResourceKey<Block> SUPER_LIGHT_KEY = blockKey("super_light");
    private static final ResourceKey<Block> LIGHT_NODE_A_KEY = blockKey("light_node_a");
    private static final ResourceKey<Block> LIGHT_NODE_B_KEY = blockKey("light_node_b");

    public static final Block LIGHT_NODE_A = registerNode(LIGHT_NODE_A_KEY);
    public static final Block LIGHT_NODE_B = registerNode(LIGHT_NODE_B_KEY);

    public static final Block WIDE_LIGHT = registerSource(WIDE_LIGHT_KEY, 20);
    public static final Block GREATER_LIGHT = registerSource(GREATER_LIGHT_KEY, 40);
    public static final Block SUPER_LIGHT = registerSource(SUPER_LIGHT_KEY, 70);

    private static Block registerSource(ResourceKey<Block> key, int radius) {
        Block block = new WideAreaLightBlock(radius,
            BlockBehaviour.Properties.of()
                .strength(1.5f, 6.0f)
                .sound(SoundType.COPPER)
                .lightLevel(state -> 15)
                .setId(key)
        );
        Registry.register(BuiltInRegistries.BLOCK, key, block);
        registerBlockItem(key, block);
        return block;
    }

    private static Block registerNode(ResourceKey<Block> key) {
        Block block = new LightNodeBlock(
            BlockBehaviour.Properties.of()
                .noCollision()
                .noOcclusion()
                .replaceable()
                .instabreak()
                .noLootTable()
                .lightLevel(state -> 15)
                .setId(key)
        );
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    private static void registerBlockItem(ResourceKey<Block> blockKey, Block block) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, blockKey.identifier());
        BlockItem item = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
    }

    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(FuelRecipeMod.MOD_ID, path));
    }

    public static boolean isLightNode(BlockState state) {
        return state.is(LIGHT_NODE_A) || state.is(LIGHT_NODE_B);
    }

    public static void initialize() {}
    private ModBlocks() {}
}
