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
    private static final int NODE_SPACING = 12;
    private static final int SEARCH_DISTANCE = 6;
    private static final int LOCAL_ESCAPE_DISTANCE = 12;
    private static final int PHASE_COUNT = 8;
    private static final int PHASE_INTERVAL_TICKS = 5;
    private final int radius;

    WideAreaLightBlock(int radius, BlockBehaviour.Properties properties) {
        super(properties);
        this.radius = radius;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel) {
            placeLocalEscapeNodes(serverLevel, pos);
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        placeLocalEscapeNodes(level, pos);
        int phase = Math.floorMod((int) (level.getGameTime() / PHASE_INTERVAL_TICKS), PHASE_COUNT);
        refreshNodes(level, pos, phase);
        level.scheduleTick(pos, this, PHASE_INTERVAL_TICKS);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        cleanupLoadedNodes(level, pos);
        cleanupLocalEscapeNodes(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    private void refreshNodes(ServerLevel level, BlockPos origin, int phase) {
        BlockState nodeState = ModBlocks.LIGHT_NODE_A.defaultBlockState();
        int r2 = radius * radius;
        int extent = (radius / NODE_SPACING) * NODE_SPACING;
        int ordinal = 0;
        for (int dx = -extent; dx <= extent; dx += NODE_SPACING) {
            for (int dy = -extent; dy <= extent; dy += NODE_SPACING) {
                for (int dz = -extent; dz <= extent; dz += NODE_SPACING) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    if ((ordinal++ & (PHASE_COUNT - 1)) != phase) continue;
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

    private void placeLocalEscapeNodes(ServerLevel level, BlockPos origin) {
        placeFirstAirOnRay(level, origin, 1, 0, 0);
        placeFirstAirOnRay(level, origin, -1, 0, 0);
        placeFirstAirOnRay(level, origin, 0, 1, 0);
        placeFirstAirOnRay(level, origin, 0, -1, 0);
        placeFirstAirOnRay(level, origin, 0, 0, 1);
        placeFirstAirOnRay(level, origin, 0, 0, -1);
    }

    private static void placeFirstAirOnRay(ServerLevel level, BlockPos origin, int sx, int sy, int sz) {
        for (int d = 1; d <= LOCAL_ESCAPE_DISTANCE; d++) {
            BlockPos pos = origin.offset(sx * d, sy * d, sz * d);
            if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) return;
            BlockState state = level.getBlockState(pos);
            if (ModBlocks.isLightNode(state)) return;
            if (state.isAir()) {
                level.setBlock(pos, ModBlocks.LIGHT_NODE_A.defaultBlockState(), Block.UPDATE_CLIENTS);
                return;
            }
        }
    }

    private void cleanupLoadedNodes(ServerLevel level, BlockPos origin) {
        int r2 = radius * radius;
        int extent = (radius / NODE_SPACING) * NODE_SPACING;
        for (int dx = -extent; dx <= extent; dx += NODE_SPACING) {
            for (int dy = -extent; dy <= extent; dy += NODE_SPACING) {
                for (int dz = -extent; dz <= extent; dz += NODE_SPACING) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    removeNodeNear(level, origin.offset(dx, dy, dz));
                }
            }
        }
    }

    private static void cleanupLocalEscapeNodes(ServerLevel level, BlockPos origin) {
        cleanupRay(level, origin, 1, 0, 0);
        cleanupRay(level, origin, -1, 0, 0);
        cleanupRay(level, origin, 0, 1, 0);
        cleanupRay(level, origin, 0, -1, 0);
        cleanupRay(level, origin, 0, 0, 1);
        cleanupRay(level, origin, 0, 0, -1);
    }

    private static void cleanupRay(ServerLevel level, BlockPos origin, int sx, int sy, int sz) {
        for (int d = 1; d <= LOCAL_ESCAPE_DISTANCE; d++) {
            BlockPos pos = origin.offset(sx * d, sy * d, sz * d);
            if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) return;
            if (removeNodeIfPresent(level, pos)) return;
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
