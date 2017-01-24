package gd.izno.mc.muon;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderEnd;
import net.minecraft.world.gen.ChunkProviderFlat;
import net.minecraft.world.gen.ChunkProviderOverworld;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.fml.common.FMLLog;

/**
 * Created by TrinaryLogic on 2016-11-24.
 */
/*
This is a utility class to store terrain heightmaps over an arbitrary area.
It was created for use in terrain smoothing code for villages.
 */
public class MuonHeightMap {
    public int[][] heightmap;
    public StructureBoundingBox mapbb;
    public boolean isBlank;
    public int xSize;
    public int zSize;
    public int seaLevel;

    public MuonHeightMap(StructureBoundingBox bb) {
        mapbb = MuonUtils.chunksBoundingBox(bb);
        mapbb.minY = 0;
        mapbb.maxY = 4096;
        xSize = mapbb.getXSize();
        zSize = mapbb.getZSize();
        heightmap = new int[xSize][zSize];
        for (int i = 0; i < xSize; ++i) {
            for (int j = 0; j < zSize; ++j) {
                heightmap[i][j] = -1;
            }
        }
        seaLevel = -1; // Fix up at first opportunity.
        isBlank = true;
    }

    public static MuonHeightMap defaultHeights(World worldIn, StructureBoundingBox bb) {
        MuonHeightMap hm = new MuonHeightMap(bb);
        // try to load default map heights from terrain generator
        IChunkProvider provider = worldIn.getChunkProvider();
        IChunkGenerator gen = null;
        try {
            gen = ((ChunkProviderServer) provider).chunkGenerator;
        } catch (Exception e) { e.printStackTrace(); }
        hm.seaLevel =  worldIn.getSeaLevel() - 1; // set sea level.
        int defaultlevel = -1;
        if (gen instanceof ChunkProviderFlat) {
            defaultlevel = worldIn.getSeaLevel() - 1;
        }
        for (int chunkX = (hm.mapbb.minX>>4); chunkX <= (hm.mapbb.maxX>>4); ++chunkX) {
            for (int chunkZ = (hm.mapbb.minZ>>4); chunkZ <= (hm.mapbb.maxZ>>4); ++chunkZ) {
                ChunkPrimer primer = new ChunkPrimer();
                if (gen instanceof ChunkProviderOverworld) {
                    ((ChunkProviderOverworld) gen).setBlocksInChunk(chunkX, chunkZ, primer);
                } else if (gen instanceof ChunkProviderEnd) {
                    ((ChunkProviderEnd) gen).setBlocksInChunk(chunkX, chunkZ, primer);
                } else {
                    primer = null;
                }
                for (int i = 0; i < 16; ++i) {
                    for (int j = 0; j < 16; ++j) {
                        int groundlevel = defaultlevel;
                        if (primer != null) {
                            //BUGGY if we do it with groundlevel = primer.findGroundBlockIdx(i, j);
                            for (int k = 255; k >= 0; --k) {
                                IBlockState state = primer.getBlockState(i,k,j);
                                if (state != Blocks.AIR.getDefaultState()) {
                                    groundlevel = k;
                                    break;
                                }
                            }
                        }
                        hm.setHeight((chunkX<<4)+i, (chunkZ<<4)+j, groundlevel);
                    }
                }
            }
        }
        return hm;
    }

    int getXoffs(BlockPos bp) {
        int xpos = bp.getX();
        int zpos = bp.getZ();
        if (xpos >= mapbb.minX && xpos <= mapbb.maxX && zpos >= mapbb.minZ && zpos <= mapbb.maxZ) {
          return xpos - mapbb.minX;
        }
        return -1;
    }

    int getRealX(int xoffs) {
        return xoffs + mapbb.minX;
    }

    int getZoffs(BlockPos bp) {
        int xpos = bp.getX();
        int zpos = bp.getZ();
        if (xpos >= mapbb.minX && xpos <= mapbb.maxX && zpos >= mapbb.minZ && zpos <= mapbb.maxZ) {
            return zpos - mapbb.minZ;
        }
        return -1;
    }

    int getRealZ(int zoffs) {
        return zoffs + mapbb.minZ;
    }

    int getHeight(int xpos, int zpos) {
        if (xpos >= mapbb.minX && xpos <= mapbb.maxX && zpos >= mapbb.minZ && zpos <= mapbb.maxZ) {
            return heightmap[xpos - mapbb.minX][zpos - mapbb.minZ];
        }
        return -1;
    }

    int getHeight(BlockPos bp) {
        return getHeight(bp.getX(), bp.getZ());
    }

    int getMaxHeight(StructureBoundingBox bb) {
        StructureBoundingBox ibb = MuonUtils.intersectionBoundingBox(bb, mapbb);
        int maxheight = -1;
        for (int xoffs = ibb.minX - mapbb.minX; xoffs <= ibb.maxX - mapbb.minX; ++xoffs) {
            for (int zoffs = ibb.minZ - mapbb.minZ; zoffs <= ibb.maxZ - mapbb.minZ; ++zoffs) {
                maxheight = Math.max(maxheight, heightmap[xoffs][zoffs]);
            }
        }
        return maxheight;
    }

    int getMinHeight(StructureBoundingBox bb) {
        if (bb.minX > mapbb.maxX || mapbb.minX > bb.maxX || bb.minZ > mapbb.maxZ || mapbb.minZ > bb.maxZ) {
            return -1;
        }
        StructureBoundingBox ibb = MuonUtils.intersectionBoundingBox(bb, mapbb);
        int minheight = -1;
        for (int xoffs = ibb.minX - mapbb.minX; xoffs <= ibb.maxX - mapbb.minX; ++xoffs) {
            for (int zoffs = ibb.minZ - mapbb.minZ; zoffs <= ibb.maxZ - mapbb.minZ; ++zoffs) {
                int here = heightmap[xoffs][zoffs];
                if (here >= 0 && (minheight == -1 || here < minheight)) {
                    minheight = here;
                }
            }
        }
        return minheight;
    }

    void setHeight(int xpos, int zpos, int value) {
        if (xpos >= mapbb.minX && xpos <= mapbb.maxX && zpos >= mapbb.minZ && zpos <= mapbb.maxZ) {
            heightmap[xpos - mapbb.minX][zpos - mapbb.minZ] = value;
            if (value != -1) {
                isBlank = false;
            }
        } else {
            FMLLog.warning("[Muon] %s", "Can't set height "+value+" outside boundingbox "+xpos+","+zpos+" "+mapbb);
        }
    }

    void setHeight(BlockPos bp, int value) {
        setHeight(bp.getX(), bp.getZ(), value);
    }

    void fill(StructureBoundingBox bb, int value) {
        if (bb.minX > mapbb.maxX || mapbb.minX > bb.maxX || bb.minZ > mapbb.maxZ || mapbb.minZ > bb.maxZ) {
            return;
        }
        StructureBoundingBox ibb = MuonUtils.intersectionBoundingBox(bb, mapbb);
        for (int xoffs = ibb.minX - mapbb.minX; xoffs <= ibb.maxX - mapbb.minX; ++xoffs) {
            for (int zoffs = ibb.minZ - mapbb.minZ; zoffs <= ibb.maxZ - mapbb.minZ; ++zoffs) {
                heightmap[xoffs][zoffs] = value;
                if (value != -1) {
                    isBlank = false;
                }
            }
        }
    }

    void fillEmpty(StructureBoundingBox bb, int value) {
        if (bb.minX > mapbb.maxX || mapbb.minX > bb.maxX || bb.minZ > mapbb.maxZ || mapbb.minZ > bb.maxZ) {
            return;
        }
        StructureBoundingBox ibb = MuonUtils.intersectionBoundingBox(bb, mapbb);
        for (int xoffs = ibb.minX - mapbb.minX; xoffs <= ibb.maxX - mapbb.minX; ++xoffs) {
            for (int zoffs = ibb.minZ - mapbb.minZ; zoffs <= ibb.maxZ - mapbb.minZ; ++zoffs) {
                if (heightmap[xoffs][zoffs] == -1) {
                    heightmap[xoffs][zoffs] = value;
                    if (value != -1) {
                        isBlank = false;
                    }
                }
            }
        }
    }

    void smoothTo(MuonHeightMap hm) {
        StructureBoundingBox smoothbb = MuonUtils.intersectionBoundingBox(mapbb, hm.mapbb);
        int smStartX = smoothbb.minX;
        int smStartZ = smoothbb.minZ;
        int smEndX = smoothbb.maxX;
        int smEndZ = smoothbb.maxZ;
        int[][] pointlist = new int[((smoothbb.getXSize()+2)*(smoothbb.getZSize()+2))/*/2*/+1][3];
        int numpoints = 0;
        if (seaLevel == -1) {
            seaLevel = hm.seaLevel; // propogate meaningful value, if possible.
        }
        if (isBlank || hm.isBlank) {
            return; // Can't do something with nothing!
        }
        // copy edges of intersection box into point list.
        for (int xPos = smStartX; xPos <= smEndX; xPos++) {
            // xPos, smStartZ
            int height = getHeight(xPos, smStartZ);
            if (height == -1) {
                height = hm.getHeight(xPos, smStartZ);
            }
            if (height != -1) {
                pointlist[numpoints][0] = xPos;
                pointlist[numpoints][1] = smStartZ;
                pointlist[numpoints][2] = height;
                ++numpoints;
            }
            // xPos, smEndZ
            height = getHeight(xPos, smEndZ);
            if (height == -1) {
                height = hm.getHeight(xPos, smEndZ);
            }
            if (height != -1) {
                pointlist[numpoints][0] = xPos;
                pointlist[numpoints][1] = smEndZ;
                pointlist[numpoints][2] = height;
                ++numpoints;
            }
        }
        for (int zPos = smStartZ; zPos <= smEndZ; zPos++) {
            // smStartX, zPos
            int height = getHeight(smStartX, zPos);
            if (height == -1) {
                height = hm.getHeight(smStartX, zPos);
            }
            if (height != -1) {
                pointlist[numpoints][0] = smStartX;
                pointlist[numpoints][1] = zPos;
                pointlist[numpoints][2] = height;
                ++numpoints;
            }
            // smEndX, zPos
            height = getHeight(smEndX, zPos);
            if (height == -1) {
                height = hm.getHeight(smEndX, zPos);
            }
            if (height != -1) {
                pointlist[numpoints][0] = smEndX;
                pointlist[numpoints][1] = zPos;
                pointlist[numpoints][2] = height;
                ++numpoints;
            }
        }
        // find all internal "edge" points and add to list
        for (int xPos = smStartX+1; xPos < smEndX; xPos++) {
            for (int zPos = smStartZ+1; zPos < smEndZ; zPos++) {
                int height = getHeight(xPos, zPos);
                if (height != -1 && (getHeight(xPos-1, zPos) == -1 || getHeight(xPos+1, zPos) == -1 || getHeight(xPos, zPos-1) == -1 || getHeight(xPos, zPos+1) == -1)) {
                    pointlist[numpoints][0] = xPos;
                    pointlist[numpoints][1] = zPos;
                    pointlist[numpoints][2] = height;
                    ++numpoints;
                }
            }
        }
        if (numpoints <= 2) {
            // not enough points?!
            return;
        }
        // For each blank point in intersection box, iterate point list and calculate new height.
        for (int xPos = smStartX; xPos <= smEndX; xPos++) {
            for (int zPos = smStartZ; zPos <= smEndZ; zPos++) {
                if (getHeight(xPos, zPos) == -1) {
                    double minDist = Math.max(smoothbb.getXSize(), smoothbb.getZSize());
                    double totInfluence = 0.0;
                    double totHeight = 0.0;
                    // Add influence from all known points
                    for (int pointNum = 0; pointNum < numpoints; pointNum++) {
                        // calculate inverse square of distance to point.
                        int xDist = xPos - pointlist[pointNum][0];
                        int zDist = zPos - pointlist[pointNum][1];
                        double distSquared = (xDist*xDist)+(zDist*zDist);
                        double dist = Math.sqrt(distSquared);
                        double influence = 1.0d/((dist-1)*(dist-1)*(dist-1)+2);
                        if (pointlist[pointNum][0] == smStartX || pointlist[pointNum][0] == smEndX || pointlist[pointNum][1] == smStartZ || pointlist[pointNum][1] == smEndZ) {
                            // less influence for edge points
                            influence /= 8;
                        } else if (dist <= minDist) {
                            // closest non-edge point.
                            minDist = dist;
                        }
                        // Add influenced point to totals
                        totInfluence += influence;
                        totHeight += influence * pointlist[pointNum][2];
                    }
                    // Add some influence from original height.
                    int orig = hm.getHeight(xPos, zPos);
                    if (orig != -1 && minDist > 0) {
                        double edgedist = Math.max(1,Math.min(Math.min(xPos-smStartX, smEndX-xPos), Math.min(zPos-smStartZ, smEndZ-zPos)));
                        double edgeRatio = Math.min(edgedist, 6*Math.abs(orig-(totHeight/totInfluence)))/minDist;
                        double edgeInfluence = 1.0d/(edgeRatio*edgeRatio);
                        totInfluence += edgeInfluence;
                        totHeight += edgeInfluence * orig;
                    }
                    int finalHeight = (int)((totHeight/totInfluence)+0.5);
                    if (finalHeight != orig && orig > seaLevel) {
                        setHeight(xPos, zPos, (int)((totHeight/totInfluence)));
                    }
                }
            }
        }
        // all done.
        return;
    }
}
