package com.anjas.fuelrecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

final class WideAreaLightBlock extends Block {
    private static final int NODE_SPACING = 15;
    private static final int SEARCH_DISTANCE = 7;
    private static final int REFRESH_TICKS = 200;
    private final int radius;

    WideAreaLightBlock(int radius, BlockBehaviour.Properties properties) {
        super(properties);
        this.radius = radius;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        refreshNodes(level, pos);
        level.scheduleTick(pos, this, REFRESH_TICKS);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        cleanupLoadedNodes(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    private void refreshNodes(ServerLevel level, BlockPos origin) {
        BlockState nodeState = ModBlocks.LIGHT_NODE_A.defaultBlockState();
        int r2 = radius * radius;
        int extent = (radius / NODE_SPACING) * NODE_SPACING;
        for (int dx = -extent; dx <= extent; dx += NODE_SPACING) {
            for (int dy = -extent; dy <= extent; dy += NODE_SPACING) {
                for (int dz = -extent; dz <= extent; dz += NODE_SPACING) {
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    BlockPos target = findSafeAir(level, origin.offset(dx, dy, dz));
                    if (target == null || target.equals(origin)) continue;
                    BlockState current = level.getBlockState(target);
                    if (current.is(ModBlocks.LIGHT_NODE_A)) continue;
                    if (current.isAir() || ModBlocks.isLightNode(current)) {
                        level.setBlock(target, nodeState, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private void cleanupLoadedNodes(ServerLevel level, BlockPos origin) {
        int r2 = radius * radius;
        int extent = (radius / NODE_SPACING) * NODE_SPACING;
        for (int dx = -extent; dx <= extent; dx += NODE_SPACING) {
            for (int dy = -extent; dy <= extent; dy += NODE_SPACING) {
                for (int dz = -extent; dz <= extent; dz += NODE_SPACING) {
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    removeNodeNear(level, origin.offset(dx, dy, dz));
                }
            }
        }
    }

    private static BlockPos findSafeAir(ServerLevel level, BlockPos base) {
        if (isUsable(level, base)) return base;
        for (int d = 1; d <= SEARCH_DISTANCE; d++) {
            BlockPos candidate = base.offset(d, 0, 0);
            if (isUsable(level, candidate)) return candidate;
            candidate = base.offset(-d, 0, 0);
            if (isUsable(level, candidate)) return candidate;
            candidate = base.offset(0, d, 0);
            if (isUsable(level, candidate)) return candidate;
            candidate = base.offset(0, -d, 0);
            if (isUsable(level, candidate)) return candidate;
            candidate = base.offset(0, 0, d);
            if (isUsable(level, candidate)) return candidate;
            candidate = base.offset(0, 0, -d);
            if (isUsable(level, candidate)) return candidate;
        }
        return null;
    }

    private static boolean isUsable(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || ModBlocks.isLightNode(state);
    }

    private static void removeNodeNear(ServerLevel level, BlockPos base) {
        if (removeNodeIfPresent(level, base)) return;
        for (int d = 1; d <= SEARCH_DISTANCE; d++) {
            if (removeNodeIfPresent(level, base.offset(d, 0, 0))) return;
            if (removeNodeIfPresent(level, base.offset(-d, 0, 0))) return;
            if (removeNodeIfPresent(level, base.offset(0, d, 0))) return;
            if (removeNodeIfPresent(level, base.offset(0, -d, 0))) return;
            if (removeNodeIfPresent(level, base.offset(0, 0, d))) return;
            if (removeNodeIfPresent(level, base.offset(0, 0, -d))) return;
        }
    }

    private static boolean removeNodeIfPresent(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) return false;
        if (ModBlocks.isLightNode(level.getBlockState(pos))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            return true;
        }
        return false;
    }
}
