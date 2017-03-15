package gd.izno.mc.muon;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.*;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraftforge.fml.common.FMLLog;

import java.util.*;

/**
 * Created by TrinaryLogic on 2016-11-12.
 */


public class MuonHooks {

    /**
     * Replacement for the same function in net.minecraft.world.gen.structure.StructureVillagePieces$Village
     *
     * We attempt to only check ground level on the side that the building faces.
     * This should mostly eliminate buildings with buried doors.
     */
    public static int getAverageGroundLevel(StructureComponent village, World worldIn, StructureBoundingBox structurebb) {
        int i = 0;
        int j = 0;
        int minY = -1;
        // structurebb is actually the bounding box of the currently generating chunk
        // village is actually the current village component being generated (i.e. building)
        StructureBoundingBox villagebb = village.getBoundingBox();
        // expand bounding box to building surrounds
        int minX = villagebb.minX - 1;
        int minZ = villagebb.minZ - 1;
        int maxX = villagebb.maxX + 1;
        int maxZ = villagebb.maxZ + 1;

        EnumFacing facing = village.getCoordBaseMode();
        // If possible, contract to only checking the height on the side the building faces
        // Village wells are 6x6 but we add one on each side above so they're now 8x8.
        if (facing != null && ((maxX - minX + 1) != 8 || (maxZ - minZ + 1) != 8)) {
            switch (facing) {
                case NORTH:
                    minZ = villagebb.maxZ;
                    break;
                case SOUTH:
                    maxZ = villagebb.minZ;
                    break;
                case WEST:
                    minX = villagebb.maxX;
                    break;
                case EAST:
                    maxX = villagebb.minX;
                    break;
                // UP | DOWN is do nothing
            }
        } else {
            // The main thing that ends up here should be the village wells.
            // Tweak position downwards (will mostly dissapear in averages)
            // Village wells should end up deciding based on only 4 points.
            // (Once borders are shrunk and only edges are checked)
            i = -4;
        }

        // Shrink borders if possible to omit corners
        if ((facing == EnumFacing.NORTH || facing == EnumFacing.SOUTH) /*&& (maxX - minX) > 6*/) {
            minX += Math.min(2, ((maxX-minX)/2)-1);
            maxX -= Math.min(2, ((maxX-minX)/2)-1);
        }
        if ((facing == EnumFacing.EAST || facing == EnumFacing.WEST) /*&& (maxZ - minZ) > 6*/) {
            minZ += Math.min(2, ((maxZ-minZ)/2)-1);
            maxZ -= Math.min(2, ((maxZ-minZ)/2)-1);
        }

        // Iterate over the (now hopefully reduced) set of blocks to establish an average
        BlockPos.MutableBlockPos currentBlock = new BlockPos.MutableBlockPos();
        for (int xpos = minX; xpos <= maxX; ++xpos) {
            for (int zpos = minZ; zpos <= maxZ; ++zpos) {
                // Only check the border we expanded out.
                int ypos = worldIn.provider.getAverageGroundLevel()-2;
                if (!(xpos >= villagebb.minX && xpos <= villagebb.maxX && zpos >= villagebb.minZ && zpos <= villagebb.maxZ)) {
                    StructureBoundingBox chunkbb = new StructureBoundingBox(xpos & ~15, zpos & ~15, xpos | 15, zpos | 15);
                    MuonHeightMap found = findTerrain(villagebb, chunkbb);
                    currentBlock.setPos(xpos, ypos, zpos);
                    if (found != null && found.getHeight(xpos, zpos) != -1) {
                        // use modified terrain heights for possibly unbuilt chunks
                        ypos = Math.max(ypos, found.getHeight(xpos, zpos));
                    } else {
                        BlockPos topBlock = MuonUtils.getTopSolidOrLiquidBlock(worldIn, currentBlock);
                        Block block = worldIn.getBlockState(topBlock).getBlock();
                        while (block == Blocks.AIR) {
                            topBlock = topBlock.down();
                            block = worldIn.getBlockState(topBlock).getBlock();
                        }
                        ypos = Math.max(ypos, topBlock.getY());
                    }
                    i += ypos;
                    ++j;
                    if (minY == -1 || ypos < minY) {
                        minY = ypos;
                    }
                }
            }
        }

        if (j > 0) {
            if ((i/j) >= (minY+1)) {
                return minY + 1;
            } else {
                return (i / j) + 1;
            }
        } else {
            return -1;
        }
    }


    public static int offsetToAverageGroundLevel(StructureComponent village, World worldIn, StructureBoundingBox structurebb, int yOffset) {
        int newheight = getAverageGroundLevel(village, worldIn, structurebb);
        structurebb.offset(0, newheight - structurebb.minY + yOffset, 0);
        return newheight;
    }


    /**
     * Inserted in addComponentParts in net.minecraft.world.gen.structure.ComponentScatteredFeaturePieces$Igloo
     *
     * This modifies the blockpos passed to addBlocksToWorldChunk to keep the structure within the original bounding box.
     */
    public static BlockPos fixRotationBlockPos(BlockPos bp, PlacementSettings ps, StructureComponent structure) {
        StructureBoundingBox structurebb = structure.getBoundingBox();
        EnumFacing facing = structure.getCoordBaseMode();
        BlockPos newpos = bp;
        switch (facing) {
            case SOUTH:
                ps.setRotation(Rotation.NONE);
                newpos = new BlockPos(structurebb.minX, structurebb.minY, structurebb.minZ);
                break;
            case EAST:
                ps.setRotation(Rotation.COUNTERCLOCKWISE_90);
                newpos = new BlockPos(structurebb.minX, structurebb.minY, structurebb.maxZ);
                break;
            case NORTH:
                ps.setRotation(Rotation.CLOCKWISE_180);
                newpos = new BlockPos(structurebb.maxX, structurebb.minY, structurebb.maxZ);
                break;
            case WEST:
                ps.setRotation(Rotation.CLOCKWISE_90);
                newpos = new BlockPos(structurebb.maxX, structurebb.minY, structurebb.minZ);
                break;
        }
        return newpos;
    }


    /**
     * Inserted near the beginning of the same function in net.minecraft.world.gen.structure.StructureVillagePieces$Path
     *
     * This uses an alternate algorithm to render better paths.
     */
    public static boolean addComponentParts(StructureVillagePieces.Path path, World worldIn, Random rand, StructureBoundingBox chunkbb, IBlockState grass_path, IBlockState planks, IBlockState gravel, IBlockState cobblestone) {
        StructureBoundingBox pathbb = path.getBoundingBox();
        StructureBoundingBox mybb = MuonUtils.intersectionBoundingBox(pathbb, chunkbb);
        EnumFacing facing = mybb.getXSize() > mybb.getZSize() ? EnumFacing.EAST : EnumFacing.NORTH;
        int length = MuonUtils.getFacingLength(mybb, facing);
        int width = MuonUtils.getFacingWidth(mybb, facing);

        MuonHeightMap found = findTerrain(chunkbb, mybb); // smaller param as second arg. neither will match excatly.
        if (found != null) {
            BlockPos.MutableBlockPos currentBlock = new BlockPos.MutableBlockPos();
            IBlockState newbs = Blocks.AIR.getDefaultState(); // grass_path, planks, gravel, cobblestone (or something based on these variables)
            for (int l = 0; l < length; ++l) {
                if (l <= (length - width)) { // don't reset for final section, where paths branch off
                    newbs = Blocks.AIR.getDefaultState();
                }
                // assess current row
                for (int w = 0; w < width; ++w) {
                    BlockPos bp = MuonUtils.getFacingPos(mybb, facing, l, w, 0);
                    currentBlock.setPos(bp.getX(), found.getHeight(bp), bp.getZ());
                    IBlockState bs = worldIn.getBlockState(currentBlock);
                    Block b = bs.getBlock();
                    if (bs.getMaterial().isLiquid() || b == Blocks.ICE || b == Blocks.AIR) {
                        newbs = planks;
                    } else if (newbs != planks && (b == Blocks.SAND || b == Blocks.SANDSTONE || b == Blocks.RED_SANDSTONE || b == Blocks.GRAVEL)) {
                        newbs = gravel;
                    } else if (newbs != planks && newbs != gravel && (b == Blocks.GRASS || b == Blocks.DIRT)) {
                        newbs = grass_path;
                    } else if (newbs != planks && newbs != gravel && newbs != grass_path && (b == Blocks.STONE || b == Blocks.COBBLESTONE)) {
                        newbs = cobblestone;
                    }
                }
                if (newbs.getBlock() == Blocks.AIR) {
                    newbs = gravel;
                }
                if (l <= (length - width)) { // don't set final section yet, where other paths may branch off
                    // set current row with decided blocktype and height.
                    for (int w = 0; w < width; ++w) {
                        BlockPos bp = MuonUtils.getFacingPos(mybb, facing, l, w, 0);
                        currentBlock.setPos(bp.getX(), found.getHeight(bp), bp.getZ());
                        if (newbs == gravel) {
                            worldIn.setBlockState(currentBlock.down(), cobblestone, 2);
                        }
                        worldIn.setBlockState(new BlockPos(currentBlock), newbs, 2);
                    }
                }
            }
            // set end square to be uniform block type. this is where other paths may branch from.
            for (int l = (length-width); l < length; ++l) {
                for (int w = 0; w < width; ++w) {
                    BlockPos bp = MuonUtils.getFacingPos(mybb, facing, l, w, 0);
                    currentBlock.setPos(bp.getX(), found.getHeight(bp), bp.getZ());
                    if (newbs == gravel) {
                        worldIn.setBlockState(currentBlock.down(), cobblestone, 2);
                    }
                    worldIn.setBlockState(new BlockPos(currentBlock), newbs, 2);
                }
            }
            return false; // don't run vanilla code. we did it all!
        }
        return true; // fall back to vanilla code.
    }


    // Storage for terrain modulation of villages.
    public static List<MuonHeightMap> terrainMods = new ArrayList<MuonHeightMap>();

    // search function
    public static MuonHeightMap findTerrain(StructureBoundingBox mybb, StructureBoundingBox chunkbb) {
        MuonHeightMap found = null;
        for (MuonHeightMap hm : terrainMods) {
            if (chunkbb.maxX >= hm.mapbb.minX && chunkbb.minX <= hm.mapbb.maxX && chunkbb.maxZ >= hm.mapbb.minZ && chunkbb.minZ <= hm.mapbb.maxZ) {
                if (found != null) {
                    FMLLog.warning("[Muon] %s", "ALERT! Found overlapping chunk in terraforming code!"+found.mapbb+" "+hm.mapbb);
                }
                found = hm;
                if (mybb.minX == hm.mapbb.minX && mybb.maxX == hm.mapbb.maxX && mybb.minZ == hm.mapbb.minZ && mybb.maxZ == hm.mapbb.maxZ) {
                    // exactly what we're looking for.
                    break;
                }
            }
        }
        return found;
    }


    /**
     * Inserted at the beginning of the same function in net.minecraft.world.gen.structure.StructureStart
     *
     * This does an initial step that reduces terrain steepness within complex structure boundaries
     */
    public static void generateStructure(net.minecraft.world.gen.structure.StructureStart structure, World worldIn, Random rand, StructureBoundingBox chunkbb) {
        // structure is the holder for the components of the village/etc. What we get here may not be a village.
        // chunkbb is actually the currently rendering chunk.
        StructureBoundingBox structbb = structure.getBoundingBox();
        MuonHeightMap found = findTerrain(structbb, chunkbb);
        if (found != null) {
            StructureBoundingBox mybb = MuonUtils.intersectionBoundingBox(found.mapbb, chunkbb);
            BlockPos.MutableBlockPos currentBlock = new BlockPos.MutableBlockPos();
            for (int i = mybb.minX; i <= mybb.maxX; ++i) {
                for (int j = mybb.minZ; j <= mybb.maxZ; ++j) {
                    currentBlock.setPos(i, 64, j);
                    int newheight = found.getHeight(currentBlock);
                    BlockPos topBlock = MuonUtils.getTopSolidOrLiquidBlock(worldIn, currentBlock).down();
                    IBlockState bsUpper = worldIn.getBlockState(topBlock);
                    IBlockState bsUnder = worldIn.getBlockState(topBlock.down());
                    int heightDiff = topBlock.getY() - newheight;
                    // Always leave water intact.
                    if (bsUpper.getMaterial().isLiquid()) {
                        newheight = -1;
                    }
                    // Ignore uncalculated points in heightmap and anything excluded above.
                    if (newheight != -1) {
                        newheight = Math.max(newheight, found.seaLevel);
                        currentBlock.setY(newheight+24);
                        if (bsUnder.getBlock() == Blocks.AIR) {
                            bsUnder = bsUpper;
                        }
                        for (int k = topBlock.down().getY(); k < newheight; ++k) {
                            currentBlock.setY(k);
                            worldIn.setBlockState(new BlockPos(currentBlock), bsUnder, 2);
                        }
                        currentBlock.setY(newheight);
                        worldIn.setBlockState(new BlockPos(currentBlock), bsUpper, 2);
                        for (int k = currentBlock.up().getY(); k <= topBlock.getY(); ++k){
                            currentBlock.setY(k);
                            worldIn.setBlockState(new BlockPos(currentBlock), Blocks.AIR.getDefaultState());
                        }
                    }
                }
            }
        }
    }

    /**
     * Inserted in MapGenScatteredFeature$Start after we've calculated structures and recalculated the bounding box.
     *
     * This terraforms the terrain around the feature to make terrain more pleasant.
     */
    public static void featureModTerrain(StructureStart start, World world) {
        List<StructureComponent> components = start.getComponents();
        StructureBoundingBox villagebb = start.getBoundingBox();
        StructureBoundingBox mybb = MuonUtils.growBoundingBox(villagebb, 8);
        mybb = MuonUtils.chunksBoundingBox(mybb);
        // now copy our calculated size back in so chunk generation uses it.
        villagebb.expandTo(mybb);
        // and start working out height mapping...
        MuonHeightMap defaultHeights = MuonHeightMap.defaultHeights(world, mybb);
        if (defaultHeights.isBlank) {
            FMLLog.warning("[Muon] %s", "WARNING! got blank default height map for Feature surrounds!");
            return; // Can't do it in this dimension?
        }
        MuonHeightMap myHeights = new MuonHeightMap(world, mybb);
        for (StructureComponent piece : components) {
            StructureBoundingBox piecebb = piece.getBoundingBox();
            StructureBoundingBox accessbb = MuonUtils.facingBoundingBox(piece, -2);
            int height = Math.max(defaultHeights.getMinHeight(MuonUtils.growBoundingBox(accessbb, 1)), world.getSeaLevel() + 1);
            myHeights.fillEmpty(accessbb, height);
            // and adjust floor of bounding box to match.
            // this works around bugs with igloos and desert temples.
            piecebb.offset(0, height - piecebb.minY, 0);
        }
        myHeights.smoothTo(defaultHeights);

        if (!myHeights.isBlank) {
            terrainMods.add(myHeights); // save for later
        } else {
            FMLLog.warning("[Muon] %s", "ERROR! Failed to generate height map for Feature surrounds!");
        }
    }

    /**
     * Inserted in MapgenVillage$Start after we've calculated structures and recalculated the bounding box.
     *
     * This terraforms the terrain around the village to eliminate untraversable paths, etc.
     */
    public static void villageModTerrain(MapGenVillage.Start start, World world, StructureVillagePieces.Start well) {
        List<StructureComponent> components = start.getComponents();
        StructureBoundingBox villagebb = start.getBoundingBox();
        StructureBoundingBox mybb = MuonUtils.growBoundingBox(villagebb, 16);
        for (StructureComponent piece : components) {
            mybb.expandTo(piece.getBoundingBox());
        }
        mybb = MuonUtils.chunksBoundingBox(mybb);
        // now copy our calculated size back in so chunk generation uses it.
        villagebb.expandTo(mybb);
        // and start working out height mapping...
        MuonHeightMap defaultHeights = MuonHeightMap.defaultHeights(world, mybb);
        if (defaultHeights.isBlank) {
            FMLLog.warning("[Muon] %s", "WARNING! got blank default height map for Village surrounds!");
            return; // Can't do it in this dimension?
        }
        MuonHeightMap myHeights = new MuonHeightMap(world, mybb);
        // set base height for well.
        int wellheight = defaultHeights.getMaxHeight(well.getBoundingBox());
        myHeights.fillEmpty(MuonUtils.growBoundingBox(well.getBoundingBox(),1),wellheight);
        // save reference to heightmap.
        terrainMods.add(myHeights);
        // recursively calculate path heights
        villageModPaths(myHeights, defaultHeights, well, components, wellheight, 0);
        // finally, smooth terrain between paths
        myHeights.smoothTo(defaultHeights);
    }

    static int calcPathHeight(MuonHeightMap heightMap, MuonHeightMap groundMap, StructureComponent component, List<StructureComponent> listIn, int wellheight, int totlen) {
        // recurse paths to work out the end height for this path segment.
        StructureBoundingBox componentbb = component.getBoundingBox();
        int length = MuonUtils.getFacingLength(component);
        int width = MuonUtils.getFacingWidth(component);
        BlockPos startPos = MuonUtils.getFacingPos(component, -1, width/2, 0);
        int startheight = heightMap.getHeight(startPos);
        if (startheight == -1) {
            startheight = groundMap.getHeight(startPos);
        }
        BlockPos b1 = MuonUtils.getFacingPos(component, length - 1, width - 1, 0);
        BlockPos b2 = MuonUtils.getFacingPos(component, length - width, 0, 0);
        StructureBoundingBox endBox = new StructureBoundingBox(b1, b2);
        int endheight = groundMap.getMinHeight(endBox);
        // iterate over subsequent paths and work out a worst-case height for the path end.
        int enddist = -1;
        for (StructureComponent piece : listIn) {
            StructureBoundingBox piecebb = piece.getBoundingBox();
            int pieceratio = Math.max(piecebb.getXSize(),piecebb.getZSize()) / Math.min(piecebb.getXSize(),piecebb.getZSize());
            if (pieceratio >= 4 && !componentbb.intersectsWith(piecebb) && componentbb.isVecInside(MuonUtils.getFacingPoint(piece,1))) {
                // looks llike another path/road piece branching off this one...
                int newheight = calcPathHeight(heightMap, groundMap, piece, listIn, wellheight, totlen+length);
                int newdist = Math.abs(newheight-wellheight);
                if (newheight > 0 && newdist > enddist) {
                    enddist = newdist;
                    endheight = newheight;
                }
            }
        }
        double incline = 3; // number of horizontal blocks to each vertical
        if (endheight < wellheight) {
            incline = -incline;
        }
        if ((int) ((endheight - wellheight) * incline) >= (totlen+length)) {
            if (enddist < 0) {
                return -1; // doesn't go anywhere interesting, so ignore in calculations
            }
            endheight = wellheight + (int) (((double) (totlen+length) - 0.1) / incline);
        }
        // now tweak our path start accordingly and return it.
        if ((int) ((endheight - startheight) * incline) >= length) {
            if (enddist < 0) {
                return -1; // doesn't go anywhere interesting, so ignore in calculations
            }
            startheight = endheight - (int) (((double) (length) - 0.1) / incline);
        }
        return startheight;
    }

    static void villageModPaths(MuonHeightMap heightMap, MuonHeightMap groundMap, StructureComponent component, List<StructureComponent> listIn, int wellheight, int totlen) {
        // component is the current village piece being examined (i.e. road or building)
        StructureBoundingBox componentbb = component.getBoundingBox();
        // get height in middle of the point the path spawns from
        // get height at point opposite end.
        // if height difference is greater than length-3, opposite point is moderated to maximum incline
        // traverse path, setting smooth gradient from start to end.
        int length = MuonUtils.getFacingLength(component);
        int width = MuonUtils.getFacingWidth(component);
        int startheight = heightMap.getHeight(MuonUtils.getFacingPos(component, -1, width/2, 0));
        if (startheight == -1) {
            startheight = groundMap.getHeight(MuonUtils.getFacingPos(component, -1, width/2, 0));
        }

        BlockPos b1 = MuonUtils.getFacingPos(component, length - 1, width - 1, 0);
        BlockPos b2 = MuonUtils.getFacingPos(component, length - width, 0, 0);
        StructureBoundingBox endBox = new StructureBoundingBox(b1, b2);
        int endheight = groundMap.getMaxHeight(endBox);
        // iterate over subsequent paths and work out a worst-case height for the path end.
        int enddist = -1;
        for (StructureComponent piece : listIn) {
            StructureBoundingBox piecebb = piece.getBoundingBox();
            int pieceratio = Math.max(piecebb.getXSize(),piecebb.getZSize()) / Math.min(piecebb.getXSize(),piecebb.getZSize());
            if (pieceratio >= 4 && !componentbb.intersectsWith(piecebb) && componentbb.isVecInside(MuonUtils.getFacingPoint(piece,1))) {
                // looks llike another path/road piece branching off this one...
                int newheight = calcPathHeight(heightMap, groundMap, piece, listIn, wellheight, totlen+length);
                int newdist = Math.abs(newheight-wellheight);
                if (newheight > 0 && newdist > enddist) {
                    enddist = newdist;
                    endheight = newheight;
                }
            }
        }
        // tweak our end height
        int inclineDir = 1; // +1 or -1
        double incline = 3; // number of horizontal blocks to each vertical
        if (endheight < startheight) {
            inclineDir = -inclineDir;
            incline = -incline;
        }
        if ((int) ((endheight - startheight) * incline) >= (length)) {
            if (enddist < 0 && endheight <= groundMap.seaLevel) {
                endheight = startheight; // uninteresting paths may remain level if over water?
            } else {
                endheight = startheight + (int) (((double) (length) - 0.1) / incline);
            }
        }
        // ... and generate height map.
        if (endheight != startheight) {
            incline = (double) (length + width) / (double) (endheight - startheight + inclineDir);
        } else {
            incline = 0.0;
        }
        for (int k = 0; k < length; ++k) {
            BlockPos bp = MuonUtils.getFacingPos(component, k, -1, 0);
            BlockPos bp2 = MuonUtils.getFacingPos(component, k, width, 0);
            StructureBoundingBox pathRow = new StructureBoundingBox(bp, bp2);
            pathRow.minY = 0;
            pathRow.maxY = 1024;
            int minHeight = -1;
            if (incline == 0) {
                minHeight = endheight;
            } else {
                minHeight = startheight + (int) (((double) (k + width) - 0.1) / incline);
            }
            if (minHeight > 0) {
                heightMap.fillEmpty(pathRow, minHeight);
            }
        }

        for (StructureComponent piece : listIn) {
            StructureBoundingBox piecebb = piece.getBoundingBox();
            int pieceratio = Math.max(piecebb.getXSize(),piecebb.getZSize()) / Math.min(piecebb.getXSize(),piecebb.getZSize());
            // reshape adjoining path
            if (pieceratio >= 4 && !componentbb.intersectsWith(piecebb) && componentbb.isVecInside(MuonUtils.getFacingPoint(piece,1))) {
                villageModPaths(heightMap, groundMap, piece, listIn, wellheight, totlen+length+1);
            }
            // reshape terrain for adjoining building
            if (pieceratio < 4 && !componentbb.intersectsWith(piecebb) && componentbb.isVecInside(MuonUtils.getFacingPoint(piece,1))) {
                StructureBoundingBox accessbb = MuonUtils.facingBoundingBox(piece, -((MuonUtils.getFacingLength(piece))/2));
                int height = heightMap.getHeight(MuonUtils.getFacingPoint(piece,1));
                if (height > 0) {
                    heightMap.fillEmpty(accessbb, height);
                }
            }
        }
    }

    /**
     * Inserted in MapgenVillage$Start after villageModTerrain (which is after recalculating the bounding box).
     *
     * This enables terrain-dependent structures to insert themselves in a village at the last minute.
     * the bounding box *shouldn't* change, but we recalculate again if necessary.
     */
    public static void terrainDependentStructures(MapGenVillage.Start start, World world, StructureVillagePieces.Start well) {
        List<StructureComponent> components = start.getComponents();
        StructureBoundingBox villagebb = start.getBoundingBox();
        StructureBoundingBox wellbb = well.getBoundingBox();
        // create our own pseudorandom to feed to various functions without using up seed.
        Random myrand = new Random((long)(wellbb.maxX/16) ^ (wellbb.maxY/16) ^ (wellbb.maxZ/16));

        if (MuonConfig.getInt("village_grove_frequency") > myrand.nextInt(100)) {
            StructureBoundingBox grove = MuonVillageGrove.findPieceBox(well, components, myrand, villagebb.minX, villagebb.minY, villagebb.minZ, EnumFacing.NORTH);
            if (grove != null) {
                // work out actual grove facing? should face path, but facing well is good enough for now.
                EnumFacing facing = EnumFacing.getFacingFromVector((float)(grove.maxX-wellbb.maxX), 0, (float)(grove.maxZ-wellbb.maxZ));
                // do stuff to actually create grove and add it to the structure list
                FMLLog.info("[Muon] %s", " Creating Grove "+grove+" facing "+facing);
                components.add(new MuonVillageGrove(well, well.getComponentType(), myrand, grove, facing));
                // expand bounding box if necessary so chunk generation uses it.
                villagebb.expandTo(grove);
            }
        }
    }


}
