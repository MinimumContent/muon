package gd.izno.mc.muon;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraftforge.fml.common.FMLLog;

import java.util.List;
import java.util.Random;

/**
 * Created by TrinaryLogic on 2017-01-28.
 */
public class MuonVillageGrove extends StructureVillagePieces.Village {
    public IBlockState sapling = null;

    public MuonVillageGrove() {}

    public MuonVillageGrove(StructureVillagePieces.Start start, int type, Random rand, StructureBoundingBox structurebb, EnumFacing facing)
    {
        super(start, type);
        this.boundingBox = structurebb;
        this.setCoordBaseMode(facing);
        this.sapling = Blocks.SAPLING.getDefaultState();
    }

    public static StructureBoundingBox findPieceBox(StructureVillagePieces.Start start, List<StructureComponent> structureComponents, Random rand, int structureMinX, int structureMinY, int structureMinZ, EnumFacing facing) {
        // for the Grove, find the largest unoccupied space in the village. we will later plant that with trees/saplings
        StructureBoundingBox villagebb = new StructureBoundingBox(start.getBoundingBox());
        MuonHeightMap found = MuonHooks.findTerrain(villagebb, villagebb);
        MuonHeightMap original = null;
        for (StructureComponent piece : structureComponents) {
            villagebb.expandTo(piece.getBoundingBox());
        }
        if (found != null) {
            original = MuonHeightMap.defaultHeights(found.world, found.mapbb);
        }
        // call recursive function to find best gap in bounding box
        StructureBoundingBox position = null;
        position = findPieceGap(villagebb, start, structureComponents, found, original);
        if (position == null) {
            // if we couldn't find a spot within the strict village boundary, look around the edges as well.
            if (found != null) {
                // use actual terraformed area.
                position = findPieceGap(MuonUtils.chunksBoundingBox(found.mapbb), start, structureComponents, found, original);
            } else {
                position = findPieceGap(MuonUtils.chunksBoundingBox(villagebb), start, structureComponents, found, original);
            }
        }
        return position;
    }

    private static StructureBoundingBox findPieceGap(StructureBoundingBox villagebb, StructureVillagePieces.Start start, List<StructureComponent> structureComponents, MuonHeightMap heightMap, MuonHeightMap original) {
        if ((villagebb.maxX-villagebb.minX) < 7 || (villagebb.maxZ-villagebb.minZ) < 7) {
            return null; // too small.
        }
//        FMLLog.info("[Muon] %s", " MuonVillageGrove findPieceGap("+villagebb+", ...)");
        for (StructureComponent piece : structureComponents) {
            StructureBoundingBox piecebb = piece.getBoundingBox();
            if (villagebb.intersectsWith(piecebb)) {
                // found an overlap, check each side of it for biggest segment.
                StructureBoundingBox best = null;
                int bestArea = 0;
                StructureBoundingBox current = new StructureBoundingBox(villagebb.minX, villagebb.minY, villagebb.minZ, piecebb.minX-1, villagebb.maxY, villagebb.maxZ);
                current = findPieceGap(current, start, structureComponents, heightMap, original);
                if (current != null) {
                    int area = areaMetric(current, start);
                    if (best == null || area > bestArea) {
                        best = current;
                        bestArea = area;
                    }
                }
                current = new StructureBoundingBox(piecebb.maxX+1, villagebb.minY, villagebb.minZ, villagebb.maxX, villagebb.maxY, villagebb.maxZ);
                current = findPieceGap(current, start, structureComponents, heightMap, original);
                if (current != null) {
                    int area = areaMetric(current, start);
                    if (best == null || area > bestArea) {
                        best = current;
                        bestArea = area;
                    }
                }
                current = new StructureBoundingBox(villagebb.minX, villagebb.minY, villagebb.minZ, villagebb.maxX, villagebb.maxY, piecebb.minZ-1);
                current = findPieceGap(current, start, structureComponents, heightMap, original);
                if (current != null) {
                    int area = areaMetric(current, start);
                    if (best == null || area > bestArea) {
                        best = current;
                        bestArea = area;
                    }
                }
                current = new StructureBoundingBox(villagebb.minX, villagebb.minY, piecebb.maxZ+1, villagebb.maxX, villagebb.maxY, villagebb.maxZ);
                current = findPieceGap(current, start, structureComponents, heightMap, original);
                if (current != null) {
                    int area = areaMetric(current, start);
                    if (best == null || area > bestArea) {
                        best = current;
                        bestArea = area;
                    }
                }
                return best;
            }
        }
        // if we got here we have no overlaps.
        return findPieceLevel(villagebb, start, structureComponents, heightMap, original);
    }

    private static StructureBoundingBox findPieceLevel(StructureBoundingBox villagebb, StructureVillagePieces.Start start, List<StructureComponent> structureComponents, MuonHeightMap heightMap, MuonHeightMap original) {
        if ((villagebb.maxX-villagebb.minX) < 10 || (villagebb.maxZ-villagebb.minZ) < 10) {
            return null; // too small.
        }
//        FMLLog.info("[Muon] %s", " MuonVillageGrove findPieceLevel("+villagebb+", ...)");
        if (heightMap != null && original != null) {
            // check if terrain is suitable before returning?
            StructureBoundingBox best = null;
            int bestArea = 0;
            int xSize = villagebb.maxX - villagebb.minX;
            int zSize = villagebb.maxZ - villagebb.minZ;
            // check average incline across Z and then split X or Z at deviations
            int lastX = villagebb.minX;
            for (int xPos = villagebb.minX; xPos < villagebb.maxX; ++xPos) {
                int oHeight = original.getHeight(xPos, villagebb.minZ);
                int oHeight2 = original.getHeight(xPos, villagebb.maxZ);
                int height = heightMap.getHeight(xPos, villagebb.minZ);
                int height2 = heightMap.getHeight(xPos, villagebb.maxZ);
                if (height < 0) height = oHeight;
                if (height2 < 0) height2 = oHeight2;
                int heightdiff = (int)(1.5 * (height - height2));
                if (height >= 0 && height2 >= 0 && (heightdiff > zSize || heightdiff < -zSize || oHeight <= original.seaLevel || oHeight2 <= original.seaLevel || (xPos - lastX) >= 16)) {
                    if (bestArea == 0) {
                        bestArea = -1; // indicate starting box has issues
                    }
                    if ((xPos - lastX) >= 10) {
                        //try splitting here.
                        StructureBoundingBox current = new StructureBoundingBox(lastX, villagebb.minY, villagebb.minZ, xPos, villagebb.maxY, villagebb.maxZ);
                        current = findPieceLevel(current, start, structureComponents, heightMap, original);
                        if (current != null) {
                            int area = areaMetric(current, start);
                            if (best == null || area > bestArea) {
                                best = current;
                                bestArea = area;
                            }
                        }
                    }
                    lastX = xPos;
                }
            }
            // check average incline across X and then split Z at deviations
            int lastZ = villagebb.minZ;
            for (int zPos = villagebb.minZ; zPos < villagebb.maxZ; ++zPos) {
                int oHeight = original.getHeight(villagebb.minX, zPos);
                int oHeight2 = original.getHeight(villagebb.maxX, zPos);
                int height = heightMap.getHeight(villagebb.minX, zPos);
                int height2 = heightMap.getHeight(villagebb.maxX, zPos);
                if (height < 0) height = oHeight;
                if (height2 < 0) height2 = oHeight2;
                int heightdiff = (int)(1.5 * (height - height2));
                if (height >= 0 && height2 >= 0 && (heightdiff > xSize || heightdiff < -xSize || oHeight < original.seaLevel || oHeight2 < original.seaLevel || (zPos - lastZ) >= 16)) {
                    if (bestArea == 0) {
                        bestArea = -1; // indicate starting box has issues}
                    }
                    if ((zPos - lastZ) >= 10) {
                        //try splitting here.
                        StructureBoundingBox current = new StructureBoundingBox(villagebb.minX, villagebb.minY, lastZ, villagebb.maxX, villagebb.maxY, zPos);
                        current = findPieceLevel(current, start, structureComponents, heightMap, original);
                        if (current != null) {
                            int area = areaMetric(current, start);
                            if (best == null || area > bestArea) {
                                best = current;
                                bestArea = area;
                            }
                        }
                    }
                    lastZ = zPos;
                }
            }
            if (bestArea != 0) {
                // it is possible for bestArea to be set to non-zero but for best to be null, indicating no suitable areas.
                return best;
            }
        }
        return villagebb;
    }

    public boolean addComponentParts(World worldIn, Random myrand, StructureBoundingBox chunkbb) {
        // plant trees... (this is earlier than minecraft would normally do it, but it's the only chance we've got)
//        FMLLog.info("[Muon] %s", " MuonVillageGrove addComponentParts("+worldIn+", "+myrand+", "+chunkbb+")");
        StructureBoundingBox allbb = this.boundingBox;
        StructureBoundingBox fullbb = MuonUtils.intersectionBoundingBox(allbb, chunkbb);
        StructureBoundingBox growbb = MuonUtils.growBoundingBox(allbb, -3);
        if (growbb == null) {
            growbb = allbb; // shouldn't happen. if it's too small plant all of it.
        }
        MuonHeightMap found = MuonHooks.findTerrain(this.boundingBox, chunkbb);
        MuonHeightMap built = new MuonHeightMap(worldIn, fullbb);
        MuonHeightMap grown = new MuonHeightMap(worldIn, fullbb);
        Biome thisbiome = worldIn.getBiome(new BlockPos(fullbb.minX, fullbb.minY, fullbb.minZ));
        // Place dirt etc
        for (int xPos = fullbb.minX; xPos <= fullbb.maxX; ++xPos) {
            for (int zPos = fullbb.minZ; zPos <= fullbb.maxZ; ++zPos) {
                int yPos = -1;
                if (found != null) {
                    yPos = found.getHeight(xPos, zPos);
                }
                if (yPos < 0) {
                    yPos = MuonUtils.getTopSolidOrLiquidBlock(worldIn, new BlockPos(xPos, fullbb.maxY, zPos)).getY();
                }
                if (yPos < 0) {
                    yPos = fullbb.maxY;
                }
                BlockPos here = new BlockPos(xPos, yPos, zPos);
                IBlockState oldState = worldIn.getBlockState(here);
                Block oldBlock = oldState.getBlock();
                while (here.getY() > 2 && ((!oldState.isFullCube()) || oldBlock.isFoliage(worldIn, here) || oldBlock.canSustainLeaves(oldState, worldIn, here.up())) && !oldState.getMaterial().isLiquid()) {
                    here = here.down();
                    oldState = worldIn.getBlockState(here);
                    oldBlock = oldState.getBlock();
                }
                if (!oldState.getMaterial().isLiquid()) {
                    int dist = Math.min(Math.min(Math.abs(allbb.minX - xPos), Math.abs(xPos - allbb.maxX)), Math.min(Math.abs(allbb.minZ - zPos), Math.abs(zPos - allbb.maxZ)));
                    if (dist >= 5 || dist > myrand.nextInt(5)) {
                        IBlockState dirt = Blocks.DIRT.getDefaultState();
                        worldIn.setBlockState(here.down(), dirt, 2);
                        int dirt_selector = ((myrand.nextInt(32) ^ xPos ^ yPos) & 31);
                        // do dirt.
                        if (dirt_selector < 2) {
                            // Sometimes GRASS
                            dirt = Blocks.GRASS.getDefaultState();
                        } else if (dirt_selector < 3) {
                            // Rarely PODZOL
                            dirt = dirt.withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.PODZOL);
                        } else if (dirt_selector < 12) {
                            // Maybe COARSE DIRT
                            dirt = dirt.withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT);
                        }
                        worldIn.setBlockState(here, dirt, 2);
                        built.setHeight(here, here.getY()); // mark the level we filled at.
                    }
                }
            }
        }
        // Grow trees
        for (int xPos = growbb.minX; xPos <= growbb.maxX; ++xPos) {
            for (int zPos = growbb.minZ; zPos <= growbb.maxZ; ++zPos) {
                int yPos = built.getHeight(xPos, zPos);
                if (yPos > 0) {
                    BlockPos here = new BlockPos(xPos, yPos+1, zPos);
                    WorldGenAbstractTree treegen = thisbiome.getRandomTreeFeature(myrand);
                    int tree_selector = myrand.nextInt(32);
                    int dist = Math.min(Math.min(Math.abs(growbb.minX - xPos), Math.abs(xPos - growbb.maxX)), Math.min(Math.abs(growbb.minZ - zPos), Math.abs(zPos - growbb.maxZ)));
                    // maybe tree
                    if (grown.getHeight(xPos,zPos-1) == -1 &&
                            grown.getHeight(xPos-1,zPos-1) == -1 &&
                            grown.getHeight(xPos-1,zPos) == -1 &&
                            grown.getHeight(xPos-1,zPos+1) == -1 &&
                            tree_selector < Math.min((dist*2)+5, 12) ) {
                        // if we have sky view, grow tree.
                        grown.setHeight(here, yPos+1); // mark where we grew a tree
                        if (!treegen.generate(worldIn, myrand, here)) {
                            // otherwise, plant sapling
                            // to get the right sapling type we need to detect the tree type from a private field...
                            // so we're going to guess vanilla trees and use the stored result or default to oak.
                            if (this.sapling != null) {
                                worldIn.setBlockState(here, this.sapling, 2);
                            }
                        }
                        IBlockState trunk = worldIn.getBlockState(here);
                        Block trunkblock = trunk.getBlock();
                        // get vanilla saplings
                        if (trunkblock == Blocks.LOG) {
                            // set sapling
                            this.sapling = Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.TYPE, trunk.getValue(BlockOldLog.VARIANT));
                        } else if (trunkblock == Blocks.LOG2) {
                            // set sapling
                            this.sapling = Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.TYPE, trunk.getValue(BlockNewLog.VARIANT));
                        }
                    }
                }
            }
        }
        // Place ground cover.
        for (int xPos = growbb.minX; xPos <= growbb.maxX; ++xPos) {
            for (int zPos = growbb.minZ; zPos <= growbb.maxZ; ++zPos) {
                int yPos = built.getHeight(xPos, zPos);
                if (yPos > 0) {
                    BlockPos here = new BlockPos(xPos, yPos+1, zPos);
                    if (worldIn.getBlockState(here).getBlock() == Blocks.AIR) {
                        IBlockState plant = Blocks.AIR.getDefaultState();
                        int grass_selector = ((myrand.nextInt(32) ^ xPos ^ yPos) & 31);
                        if (grass_selector < 2) {
                            plant = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.DEAD_BUSH);
                        } else if (grass_selector < 6) {
                            plant = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.FERN);
                        } else if (grass_selector < 24) {
                            plant = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS);
                        }
                        worldIn.setBlockState(here, plant, 2);
                    }
                }
            }
        }
        return true;
    }

    private static int areaMetric(StructureBoundingBox current, StructureVillagePieces.Start start) {
        // int area = (current.maxX-current.minX)*(current.maxZ-current.minZ);
        int xSize = current.maxX-current.minX;
        int zSize = current.maxZ-current.minZ;
        int shortSide = Math.min(xSize, zSize);
        int longSide = Math.max(xSize, zSize);
        int smallArea = (shortSide * shortSide);
        if (shortSide > 12) {
            smallArea = shortSide * (24-shortSide);
        }
        int dist = Math.abs(current.maxX - start.getBoundingBox().maxX) + Math.abs(current.maxZ - start.getBoundingBox().maxZ);
        int area = smallArea + longSide - dist;
        return area;
    }

    static {
        MapGenStructureIO.registerStructureComponent(MuonVillageGrove.class,"ViMuonGrove");
    }
}
