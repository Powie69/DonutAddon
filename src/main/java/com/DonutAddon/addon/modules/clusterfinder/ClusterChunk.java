/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package com.DonutAddon.addon.modules.clusterfinder;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import com.DonutAddon.addon.modules.*;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getRenderDistance;

public class ClusterChunk {

    private final int x, z;
    public Long2ObjectMap<ClusterBlock> blocks;

    public ClusterChunk(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public ClusterBlock get(int x, int y, int z) {
        return blocks == null ? null : blocks.get(ClusterBlock.getKey(x, y, z));
    }

    public void add(BlockPos blockPos, boolean update) {
        ClusterBlock block = new ClusterBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ());

        if (blocks == null) blocks = new Long2ObjectOpenHashMap<>(64);
        blocks.put(ClusterBlock.getKey(blockPos), block);

        if (update) block.update();
    }

    public void add(BlockPos blockPos) {
        add(blockPos, true);
    }

    public void remove(BlockPos blockPos) {
        if (blocks != null) {
            ClusterBlock block = blocks.remove(ClusterBlock.getKey(blockPos));
            if (block != null) block.group.remove(block);
        }
    }

    public void update() {
        if (blocks != null) {
            for (ClusterBlock block : blocks.values()) block.update();
        }
    }

    public void update(int x, int y, int z) {
        if (blocks != null) {
            ClusterBlock block = blocks.get(ClusterBlock.getKey(x, y, z));
            if (block != null) block.update();
        }
    }

    public int size() {
        return blocks == null ? 0 : blocks.size();
    }

    public boolean shouldBeDeleted() {
        int viewDist = getRenderDistance() + 1;
        int chunkX = ChunkSectionPos.getSectionCoord(mc.player.getBlockPos().getX());
        int chunkZ = ChunkSectionPos.getSectionCoord(mc.player.getBlockPos().getZ());

        return x > chunkX + viewDist || x < chunkX - viewDist || z > chunkZ + viewDist || z < chunkZ - viewDist;
    }

    public void render(Render3DEvent event) {
        if (blocks != null) {
            for (ClusterBlock block : blocks.values()) block.render(event);
        }
    }


    public static ClusterChunk searchChunk(Chunk chunk, List<Block> blocks) {
        ClusterChunk schunk = new ClusterChunk(chunk.getPos().x, chunk.getPos().z);
        if (schunk.shouldBeDeleted()) return schunk;

        BlockPos.Mutable blockPos = new BlockPos.Mutable();

        for (int x = chunk.getPos().getStartX(); x <= chunk.getPos().getEndX(); x++) {
            for (int z = chunk.getPos().getStartZ(); z <= chunk.getPos().getEndZ(); z++) {
                int height = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(x - chunk.getPos().getStartX(), z - chunk.getPos().getStartZ());

                for (int y = mc.world.getBottomY(); y < height; y++) {
                    blockPos.set(x, y, z);
                    BlockState bs = chunk.getBlockState(blockPos);

                    if (blocks.contains(bs.getBlock())) schunk.add(blockPos, false);
                }
            }
        }

        return schunk;
    }
}
