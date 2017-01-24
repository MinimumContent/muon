package gd.izno.mc.muon;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderEnd;
import net.minecraft.world.gen.ChunkProviderFlat;
import net.minecraft.world.gen.ChunkProviderOverworld;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by TrinaryLogic on 2016-11-23.
 */
public class MuonUtils {
    // Utility function to create simple string maps
    // http://stackoverflow.com/a/39510693
    public static Map<String, String> createMap(final String... args) {
        return new HashMap<String, String>() {{
            for (int i = 0; i < args.length - 1; i++) {
                put(args[i], args[++i]);
            }
        }};
    }

    public static StructureBoundingBox growBoundingBox(StructureBoundingBox in, int distance) {
        return new StructureBoundingBox(in.minX-distance,in.minY,in.minZ-distance,in.maxX+distance,in.maxY,in.maxZ+distance);
    }

    public static StructureBoundingBox chunksBoundingBox(StructureBoundingBox in) {
        return new StructureBoundingBox(in.minX&~15,in.minY&~15,in.minZ&~15,in.maxX|15,in.maxY|15,in.maxZ|15);
    }

    public static StructureBoundingBox biasBoundingBox(StructureComponent in, int distance) {
        StructureBoundingBox bb = in.getBoundingBox();
        switch (in.getCoordBaseMode()) {
            case NORTH:
                return new StructureBoundingBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ + distance);
            case SOUTH:
                return new StructureBoundingBox(bb.minX, bb.minY, bb.minZ - distance, bb.maxX, bb.maxY, bb.maxZ);
            case WEST:
                return new StructureBoundingBox(bb.minX, bb.minY, bb.minZ, bb.maxX + distance, bb.maxY, bb.maxZ);
            case EAST:
                return new StructureBoundingBox(bb.minX - distance, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
            case UP:
                return new StructureBoundingBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY + distance, bb.maxZ);
            case DOWN:
                return new StructureBoundingBox(bb.minX, bb.minY - distance, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
            default:
                return new StructureBoundingBox(bb.minX - distance, bb.minY - distance, bb.minZ - distance, bb.maxX + distance, bb.maxY + distance, bb.maxZ + distance);
        }
    }

    public static StructureBoundingBox facingBoundingBox(StructureComponent in, int distance) {
        StructureBoundingBox bb = in.getBoundingBox();
        int maxoffs = distance > 0 ? distance : 0;
        int minoffs = distance < 0 ? -distance : 0;
        switch (in.getCoordBaseMode()) {
            case NORTH:
                return new StructureBoundingBox(bb.minX, bb.minY, bb.maxZ - minoffs, bb.maxX, bb.maxY, bb.maxZ + maxoffs);
            case SOUTH:
                return new StructureBoundingBox(bb.minX, bb.minY, bb.minZ - maxoffs, bb.maxX, bb.maxY, bb.minZ + minoffs);
            case WEST:
                return new StructureBoundingBox(bb.maxX - minoffs, bb.minY, bb.minZ, bb.maxX + maxoffs, bb.maxY, bb.maxZ);
            case EAST:
                return new StructureBoundingBox(bb.minX - maxoffs, bb.minY, bb.minZ, bb.minX + minoffs, bb.maxY, bb.maxZ);
            case UP:
                return new StructureBoundingBox(bb.minX, bb.maxY - minoffs, bb.minZ, bb.maxX, bb.maxY + maxoffs, bb.maxZ);
            case DOWN:
                return new StructureBoundingBox(bb.minX, bb.minY - maxoffs, bb.minZ, bb.maxX, bb.minY + minoffs, bb.maxZ);
            default:
                return new StructureBoundingBox(bb.minX - distance, bb.minY - distance, bb.minZ - distance, bb.maxX + distance, bb.maxY + distance, bb.maxZ + distance);
        }
    }

    public static StructureBoundingBox intersectionBoundingBox(StructureBoundingBox a, StructureBoundingBox b) {
        if (a.minX > b.maxX || b.minX > a.maxX || a.minZ > b.maxZ || b.minZ > a.maxZ) {
            return null;
        }
        return new StructureBoundingBox(
                Math.max(a.minX,b.minX),
                Math.max(a.minY,b.minY),
                Math.max(a.minZ,b.minZ),
                Math.min(a.maxX,b.maxX),
                Math.min(a.maxY,b.maxY),
                Math.min(a.maxZ,b.maxZ)
        );
    }


    public static BlockPos getFacingPoint(StructureComponent in, int distance) {
        return getFacingPoint(in.getBoundingBox(), in.getCoordBaseMode(), distance);
    }

    public static BlockPos getFacingPoint(StructureBoundingBox bb, EnumFacing facing, int distance) {
        int midX = (bb.maxX + bb.minX) / 2;
        int midY = (bb.maxY + bb.minY) / 2;
        int midZ = (bb.maxZ + bb.minZ) / 2;
        switch (facing) {
            case NORTH:
                return new BlockPos(midX, midY, bb.maxZ + distance);
            case SOUTH:
                return new BlockPos(midX, midY, bb.minZ - distance);
            case WEST:
                return new BlockPos(bb.maxX + distance, midY, midZ);
            case EAST:
                return new BlockPos(bb.minX - distance, midY, midZ);
            case UP:
                return new BlockPos(midX, bb.maxY + distance, midZ);
            case DOWN:
                return new BlockPos(midX, bb.minY - distance, midZ);
            default:
                return new BlockPos(midX, midY, midZ);
        }
    }

    public static BlockPos getFacingPos(StructureComponent in, int along, int across, int up) {
        return getFacingPos(in.getBoundingBox(), in.getCoordBaseMode(), along, across, up);
    }

    public static BlockPos getFacingPos(StructureBoundingBox bb, EnumFacing facing, int along, int across, int up) {
        switch (facing) {
            case NORTH:
                return new BlockPos(bb.minX + across, bb.minY + up, bb.maxZ - along);
            case SOUTH:
                return new BlockPos(bb.minX + across, bb.minY + up, bb.minZ + along);
            case WEST:
                return new BlockPos(bb.maxX - along, bb.minY + up, bb.minZ + across);
            case EAST:
                return new BlockPos(bb.minX + along, bb.minY + up, bb.minZ + across);
            case UP:
                return new BlockPos(bb.minX + up, bb.maxY - along, bb.minZ + across);
            case DOWN:
                return new BlockPos(bb.minX + up, bb.minY + along, bb.minZ + across);
            default:
                return new BlockPos(bb.minX + along, bb.minY + up, bb.minZ + across);
        }
    }

    public static int getFacingLength(StructureComponent in) {
        return getFacingLength(in.getBoundingBox(), in.getCoordBaseMode());
    }

    public static int getFacingLength(StructureBoundingBox bb, EnumFacing facing) {
        switch (facing) {
            case NORTH:
            case SOUTH:
                return bb.maxZ - bb.minZ + 1;
            case WEST:
            case EAST:
                return bb.maxX - bb.minX + 1;
            case UP:
            case DOWN:
                return bb.maxY - bb.minY + 1;
            default:
                return bb.maxX - bb.minX + 1;
        }
    }

    public static int getFacingWidth(StructureComponent in) {
        return getFacingWidth(in.getBoundingBox(), in.getCoordBaseMode());
    }

    public static int getFacingWidth(StructureBoundingBox bb, EnumFacing facing) {
        switch (facing) {
            case NORTH:
            case SOUTH:
                return bb.maxX - bb.minX + 1;
            case WEST:
            case EAST:
            case UP:
            case DOWN:
            default:
                return bb.maxZ - bb.minZ + 1;
        }
    }

    public static int getFacingHeight(StructureComponent in) {
        return getFacingHeight(in.getBoundingBox(), in.getCoordBaseMode());
    }

    public static int getFacingHeight(StructureBoundingBox bb, EnumFacing facing) {
        switch (facing) {
            case NORTH:
            case SOUTH:
            case WEST:
            case EAST:
                return bb.maxY - bb.minY + 1;
            case UP:
            case DOWN:
                return bb.maxX - bb.minX + 1;
            default:
                return bb.maxY - bb.minY + 1;
        }
    }

    /**
     * Finds the highest block on the x and z coordinate that is solid or liquid, and returns its y coord.
     *
     * Unlike the similarly named function on the World object, this actually returns top liquid blocks.
     */
    public static BlockPos getTopSolidOrLiquidBlock(World world, BlockPos pos)
    {
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        BlockPos blockpos;
        BlockPos blockpos1;

        for (blockpos = new BlockPos(pos.getX(), chunk.getTopFilledSegment() + 16, pos.getZ()); blockpos.getY() >= 0; blockpos = blockpos1)
        {
            blockpos1 = blockpos.down();
            IBlockState state = chunk.getBlockState(blockpos1);

            if ((state.getMaterial().blocksMovement() || state.getMaterial().isLiquid()) && !state.getBlock().isLeaves(state, world, blockpos1) && !state.getBlock().isFoliage(world, blockpos1))
            {
                break;
            }
        }
        return blockpos;
    }


    public static int getMinimumGroundLevel(World worldIn, StructureBoundingBox bb, boolean actual) {
        return getMinimumGroundLevel(worldIn, bb.minX, bb.minZ, bb.maxX, bb.maxZ, actual);
    }

    public static int getMinimumGroundLevel(World worldIn, int minX, int minZ, int maxX, int maxZ, boolean actual) {
        int minHeight = 65535;
        BlockPos.MutableBlockPos currentBlock = new BlockPos.MutableBlockPos();
        for (int k = minZ; k <= maxZ; ++k) {
            for (int l = minX; l <= maxX; ++l) {
                currentBlock.setPos(l, 64, k);
                BlockPos topBlock;
                if (actual) {
                    topBlock = worldIn.getTopSolidOrLiquidBlock(currentBlock);
                } else {
                    int groundlevel = getGroundLevel(worldIn, currentBlock);
                    if (groundlevel == -1) {
                        return -1;
                    } else {
                        currentBlock.setY(groundlevel);
                        topBlock = new BlockPos(l, groundlevel, k);
                    }
                }
                Block block = worldIn.getBlockState(topBlock).getBlock();
                while (block == Blocks.AIR) {
                    topBlock = topBlock.down();
                    block = worldIn.getBlockState(topBlock).getBlock();
                }
                int seaLevel = worldIn.provider.getAverageGroundLevel() - 1;
                int ypos = Math.max(topBlock.getY(), seaLevel);
                minHeight = Math.min(minHeight, ypos);
            }
        }
        return minHeight;
    }

    public static int getGroundLevel(World worldin, BlockPos pos) {
        return getGroundLevel(worldin, pos.getX(), pos.getZ());

    }

    public static ChunkPrimer cachedChunk = null;
    public static int cachedChunkX = 0;
    public static int cachedChunkZ = 0;
    // Get original ground level of world. This only works for certain known Dimensions.
    public static int getGroundLevel(World worldin, int xpos, int zpos) {
        int chunkX = xpos >> 4;
        int chunkZ = zpos >> 4;
        IChunkProvider provider = worldin.getChunkProvider();
        ChunkPrimer primer = new ChunkPrimer();
        if (chunkX == cachedChunkX && chunkZ == cachedChunkZ && cachedChunk != null) {
            primer = cachedChunk;
        } else {
            if (provider instanceof ChunkProviderOverworld) {
                ((ChunkProviderOverworld)provider).setBlocksInChunk(chunkX, chunkZ, primer);
            } else if (provider instanceof ChunkProviderEnd) {
                ((ChunkProviderEnd) provider).setBlocksInChunk(chunkX, chunkZ, primer);
            } else if (provider instanceof ChunkProviderFlat) {
                return worldin.getSeaLevel();
            } else {
                return -1; // unhandled dimension.
            }
            cachedChunk = primer;
            cachedChunkX = chunkX;
            cachedChunkZ = chunkZ;
        }
        return primer.findGroundBlockIdx(xpos & 15, zpos & 15);
    }

    static double interpolateNums(double percent, double p1, double p2) {
        return p1 + (percent * (p2 - p1));
    }

    static Vec3d interpolatePoints(double percent, Vec3d p1, Vec3d p2) {
        return new Vec3d(
                interpolateNums(percent, p1.xCoord, p2.xCoord),
                interpolateNums(percent, p1.yCoord, p2.yCoord),
                interpolateNums(percent, p1.zCoord, p2.zCoord)
        );
    }

    static Vec3d interpolateBezier(double percent, Vec3d p1, Vec3d p2, Vec3d p3) {
        return interpolatePoints(
                percent,
                interpolatePoints(percent, p1, p2),
                interpolatePoints(percent, p2, p3)
        );
    }

    static Vec3d interpolateBezier(double percent, Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4) {
        return interpolateBezier(
                percent,
                interpolatePoints(percent, p1, p2),
                interpolatePoints(percent, p2, p3),
                interpolatePoints(percent, p3, p4)
        );
    }

    static BlockPos interpolateBezier(double percent, BlockPos p1, BlockPos p2, BlockPos p3, BlockPos p4) {
        Vec3d next = interpolateBezier(percent, new Vec3d(p1), new Vec3d(p2), new Vec3d(p3), new Vec3d(p4));
        return new BlockPos(Math.round(next.xCoord), Math.round(next.yCoord), Math.round(next.zCoord));
    }
}
