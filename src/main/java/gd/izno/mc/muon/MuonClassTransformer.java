package gd.izno.mc.muon;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Arrays;

import static org.objectweb.asm.Opcodes.*;

/**
 * Created by TrinaryLogic on 2016-11-11.
 */
public class MuonClassTransformer implements IClassTransformer {
    private static final String[] classesBeingTransformed = {
            "net.minecraft.world.gen.structure.StructureVillagePieces$Village",
            "net.minecraft.world.gen.structure.StructureVillagePieces$Path",
            "net.minecraft.world.gen.structure.MapGenVillage$Start",
            "net.minecraft.world.gen.structure.StructureStart",
            "net.minecraft.world.gen.structure.MapGenScatteredFeature$Start",
            "net.minecraft.world.gen.structure.ComponentScatteredFeaturePieces$Feature",
            "net.minecraft.world.gen.structure.ComponentScatteredFeaturePieces$Igloo"
    };

    private static final String[] methodAltNames = {
            "func_75068_a", "generateStructure",
            "func_74875_a", "addComponentParts",
            "func_74889_b", "getAverageGroundLevel",
            "func_74935_a", "offsetToAverageGroundLevel"
    };

    private static String getMethodName(String context, String name, String desc) {
        String deobf = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(context, name, desc);
        // do further tweaks to the name for methods that aren't fully deobfuscated by FML
        for (int i = 0; i < methodAltNames.length; i++) {
            if (methodAltNames[i++].equals(deobf)) { // steps through list two at a time.
                deobf = methodAltNames[i]; // this is now the second of the pair.
                break;
            }
        }
        // may be the original name if neither FML nor we had any data.
        return deobf;
    }

    // returns a possibly re-obfuscated descriptor for a class parameter
    private static String descClass(String name) {
        if (isObfuscated) {
            return "L" + FMLDeobfuscatingRemapper.INSTANCE.unmap(name.replace(".", "/")) + ";";
        }
        return "L" + name.replace(".", "/") + ";";
    }

    static boolean isObfuscated = false; // this should only change once.
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!name.equals(transformedName)) {
            isObfuscated = true;
        }
        int index = Arrays.asList(classesBeingTransformed).indexOf(transformedName);
        return index != -1 ? transform(index, name, transformedName, basicClass) : basicClass;
    }

    private static byte[] transform(int index, String name, String transformedName, byte[] basicClass) {
        FMLLog.info("[Muon ASM] %s", "Transforming "+transformedName);
        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);

            switch (index) {
                case 0:
                    transformVillage(name, transformedName, classNode);
                    break;
                case 1:
                    transformPath(name, transformedName, classNode);
                    break;
                case 2:
                    transformGenVillage(name, transformedName, classNode);
                    break;
                case 3:
                    transformStructureStart(name, transformedName, classNode);
                    break;
                case 4:
                    transformScatteredFeatureStart(name, transformedName, classNode);
                    break;
                case 5:
                    transformScatteredFeaturePieces(name, transformedName, classNode);
                    break;
                case 6:
                    transformIgloo(name, transformedName, classNode);
                    break;
            }

            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            return classWriter.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return basicClass;
    }

    private static void transformVillage(String name, String transformedName, ClassNode villageClass) {
        final String AVERAGE_LEVEL_DESC = "(" +
                descClass("net.minecraft.world.World") +
                descClass("net.minecraft.world.gen.structure.StructureBoundingBox") +
                ")I";
        final String AVERAGE_LEVEL_REPL_DESC = "(" +
                descClass("net.minecraft.world.gen.structure.StructureComponent") +
                descClass("net.minecraft.world.World") +
                descClass("net.minecraft.world.gen.structure.StructureBoundingBox") +
                ")I";

        Boolean found = false;
        for (MethodNode method : villageClass.methods) {
            String realname = getMethodName(name, method.name, method.desc);
            if (realname.equals("getAverageGroundLevel") && method.desc.equals(AVERAGE_LEVEL_DESC)) {
                found = true;
                // this is the method for getAverageGroundLevel()
                // Insert a redirect at the start of the function to override it completely

                FMLLog.info("[Muon ASM] %s", "Patching Village Method getAverageGroundLevel(): " + method.name.toString() + " " + method.desc.toString());
                LabelNode newLabelNode = new LabelNode();
                InsnList toInsert = new InsnList();
                // Check if hook is enabled
                toInsert.add(new LdcInsnNode("fix_buried_doors"));
                toInsert.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MuonConfig.class), "getBoolean", "(Ljava/lang/String;)Z", false));
                toInsert.add(new JumpInsnNode(IFEQ, newLabelNode)); // skip if equal to zero (false)
                // conditionally run hook
                toInsert.add(new VarInsnNode(ALOAD, 0));
                toInsert.add(new VarInsnNode(ALOAD, 1));
                toInsert.add(new VarInsnNode(ALOAD, 2));
                toInsert.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MuonHooks.class), "getAverageGroundLevel", AVERAGE_LEVEL_REPL_DESC, false));
                toInsert.add(new InsnNode(IRETURN));
                toInsert.add(newLabelNode);
                // Shove all that at the beginning to redirect
                method.instructions.insertBefore(method.instructions.getFirst(), toInsert);
            }
        }
        if (found == false) {
            FMLLog.warning("[Muon ASM] %s", "Unpatched Village Method getAverageGroundLevel()!");
        }
    }

    private static void transformPath(String name, String transformedName, ClassNode pathClass) {
        final String ADD_PARTS_DESC = "(" +
                descClass("net.minecraft.world.World") +
                descClass("java.util.Random") +
                descClass("net.minecraft.world.gen.structure.StructureBoundingBox") +
                ")Z";
        final String ADD_PARTS_PRFX_DESC = "(" +
                descClass("net.minecraft.world.gen.structure.StructureVillagePieces$Path") +
                descClass("net.minecraft.world.World") +
                descClass("java.util.Random") +
                descClass("net.minecraft.world.gen.structure.StructureBoundingBox") +
                descClass("net.minecraft.block.state.IBlockState") +
                descClass("net.minecraft.block.state.IBlockState") +
                descClass("net.minecraft.block.state.IBlockState") +
                descClass("net.minecraft.block.state.IBlockState") +
                ")Z";

        Boolean found = false;
        for (MethodNode method : pathClass.methods) {
            String realname = getMethodName(name, method.name, method.desc);
            if (realname.equals("addComponentParts") && method.desc.equals(ADD_PARTS_DESC)) {
                // this is the method for addComponentParts()
                // we add a hook at the beginning of the function so we can do extra prepratory stuff.
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (instruction.getOpcode() == ASTORE &&
                            ((VarInsnNode) instruction).var == 7) {
                        found = true;
                        /*
                        IBlockState iblockstate = this.getBiomeSpecificBlockState(Blocks.GRASS_PATH.getDefaultState());
                        IBlockState iblockstate1 = this.getBiomeSpecificBlockState(Blocks.PLANKS.getDefaultState());
                        IBlockState iblockstate2 = this.getBiomeSpecificBlockState(Blocks.GRAVEL.getDefaultState());
                        IBlockState iblockstate3 = this.getBiomeSpecificBlockState(Blocks.COBBLESTONE.getDefaultState());
                        <WE ARE HERE>
                         */

                        FMLLog.info("[Muon ASM] %s", "Patching Well Method addComponentParts(): " + method.name.toString() + " " + method.desc.toString());
                        LabelNode newLabelNode = new LabelNode();
                        InsnList toInsert = new InsnList();
                        // Check if hook is enabled
                        toInsert.add(new LdcInsnNode("better_paths"));
                        toInsert.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MuonConfig.class), "getBoolean", "(Ljava/lang/String;)Z", false));
                        toInsert.add(new JumpInsnNode(IFEQ, newLabelNode)); // skip if equal to zero (false)
                        // conditionally run hook
                        toInsert.add(new VarInsnNode(ALOAD, 0)); // Path
                        toInsert.add(new VarInsnNode(ALOAD, 1)); // World
                        toInsert.add(new VarInsnNode(ALOAD, 2)); // Random
                        toInsert.add(new VarInsnNode(ALOAD, 3)); // chunkbb
                        toInsert.add(new VarInsnNode(ALOAD, 4)); // GRASS_PATH (or replacement)
                        toInsert.add(new VarInsnNode(ALOAD, 5)); // PLANKS (or replacement)
                        toInsert.add(new VarInsnNode(ALOAD, 6)); // GRAVEL (or replacement)
                        toInsert.add(new VarInsnNode(ALOAD, 7)); // COBBLESTONE (or replacement)
                        toInsert.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MuonHooks.class), "addComponentParts", ADD_PARTS_PRFX_DESC, false));
                        toInsert.add(new JumpInsnNode(IFNE, newLabelNode)); // if hook returned true (not zero/false), skip to vanilla code
                        toInsert.add(new InsnNode(ICONST_1));
                        toInsert.add(new InsnNode(IRETURN));
                        toInsert.add(newLabelNode);
                        // Shove function call in to optionally redirect.
                        method.instructions.insert(instruction, toInsert);
                        break;
                    }
                }
            }
        }
        if (found == false) {
            FMLLog.warning("[Muon ASM] %s", "Unpatched Well Method addComponentParts()!");
        }
    }

    private static void transformIgloo(String name, String transformedName, ClassNode wellClass) {
        final String ADD_PARTS_DESC = "(" +
                descClass("net.minecraft.world.World") +
                descClass("java.util.Random") +
                descClass("net.minecraft.world.gen.structure.StructureBoundingBox") +
                ")Z";
        final String FIX_ROTATION_HOOK_DESC = "(" +
                descClass("net.minecraft.util.math.BlockPos") +
                descClass("net.minecraft.world.gen.structure.template.PlacementSettings") +
                descClass("net.minecraft.world.gen.structure.StructureComponent") +
                ")" +
                descClass("net.minecraft.util.math.BlockPos");

        Boolean found = false;
        for (MethodNode method : wellClass.methods) {
            String realname = getMethodName(name, method.name, method.desc);
            if (realname.equals("addComponentParts") && method.desc.equals(ADD_PARTS_DESC)) {
                // this is the method for addComponentParts()
                // patch the blockpos passed to addBlocksToWorldChunk to keep structure within the original box.
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if ((instruction.getPrevious() != null) &&
                            (instruction.getPrevious().getOpcode() == ALOAD) &&
                            (((VarInsnNode) instruction.getPrevious()).var == 5) &&
                            (instruction.getOpcode() == ALOAD) &&
                            (((VarInsnNode) instruction).var == 9) &&
                            (instruction.getNext().getOpcode() == INVOKEVIRTUAL)) {
                        found = true;
                        /*
                        template.addBlocksToWorldChunk(worldIn, blockpos, <WE ARE HERE> placementsettings);
                         */
                        FMLLog.info("[Muon ASM] %s", "Patching Igloo Method addComponentParts(): " + method.name.toString() + " " + method.desc.toString());
                        LabelNode newLabelNode = new LabelNode();
                        InsnList toInsert = new InsnList();
                        // Check if hook is enabled
                        toInsert.add(new LdcInsnNode("fix_scattered_features"));
                        toInsert.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MuonConfig.class), "getBoolean", "(Ljava/lang/String;)Z", false));
                        toInsert.add(new JumpInsnNode(IFEQ, newLabelNode)); // skip if equal to zero (false)
                        // conditionally run hook
                        // ALOAD 5 (BlockPos) already on stack
                        // ALOAD 9 (PlacementSettings) already on stack, but we'll need to replace it later.
                        toInsert.add(new VarInsnNode(ALOAD, 0)); // igloo Structure
                        toInsert.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MuonHooks.class), "fixRotationBlockPos", FIX_ROTATION_HOOK_DESC, false));
                        // now we have a new BlockPos on the stack as a return, replacing the old one.
                        toInsert.add(new VarInsnNode(ALOAD, 9)); // replace PlacementSettings
                        toInsert.add(newLabelNode);

                        // insert after the original ALOAD 9 (placementsettings)
                        method.instructions.insert(instruction, toInsert);
                    }
                }
            }
        }
        if (found == false) {
            FMLLog.warning("[Muon ASM] %s", "Unpatched Igloo Method addComponentParts()!");
        }
    }

    private static void transformGenVillage(String name, String transformedName, ClassNode startClass) {
        final String START_INIT_DESC = "(" +
                descClass("net.minecraft.world.World") +
                descClass("java.util.Random") +
                "III)V";
        final String START_INIT_TERRAIN_DESC = "(" +
                descClass("net.minecraft.world.gen.structure.MapGenVillage$Start") +
                descClass("net.minecraft.world.World") +
                descClass("net.minecraft.world.gen.structure.StructureVillagePieces$Start") +
                ")V";

        Boolean found = false;
        for (MethodNode method : startClass.methods) {
            String realname = getMethodName(name, method.name, method.desc);
            if (realname.equals("<init>") && method.desc.equals(START_INIT_DESC)) {
                // this is the method for Start(World worldIn, Random rand, int x, int z, int size)
                AbstractInsnNode targetNode = null;
                int line = 0;
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if ((instruction.getOpcode() == ICONST_0) &&
                            (instruction.getNext().getOpcode() == ISTORE &&
                                    ((VarInsnNode) instruction.getNext()).var == 10)) {
                        found = true;
                        /*
                        this.updateBoundingBox();
                        <WE ARE HERE> int k = 0;
                         */
                        FMLLog.info("[Muon ASM] %s", "Patching MapGenVillage.Start(): " + method.name.toString() + " " + method.desc.toString());
                        InsnList toInsert = new InsnList();

                        LabelNode newLabelNode = new LabelNode();
                        // Check if hook is enabled
                        toInsert.add(new LdcInsnNode("smooth_village_terrain"));
                        toInsert.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MuonConfig.class), "getBoolean", "(Ljava/lang/String;)Z", false));
                        toInsert.add(new JumpInsnNode(IFEQ, newLabelNode)); // skip if equal to zero (false)
                        // conditionally run hook
                        toInsert.add(new VarInsnNode(ALOAD, 0));
                        toInsert.add(new VarInsnNode(ALOAD, 1));
                        toInsert.add(new VarInsnNode(ALOAD, 7));
                        toInsert.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MuonHooks.class), "villageModTerrain", START_INIT_TERRAIN_DESC, false));
                        toInsert.add(newLabelNode);

                        LabelNode newLabelNode2 = new LabelNode();
                        // Check if next hook is enabled
                        toInsert.add(new LdcInsnNode("terrain_dependent_structures"));
                        toInsert.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MuonConfig.class), "getBoolean", "(Ljava/lang/String;)Z", false));
                        toInsert.add(new JumpInsnNode(IFEQ, newLabelNode2)); // skip if equal to zero (false)
                        // conditionally run hook
                        toInsert.add(new VarInsnNode(ALOAD, 0));
                        toInsert.add(new VarInsnNode(ALOAD, 1));
                        toInsert.add(new VarInsnNode(ALOAD, 7));
                        toInsert.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MuonHooks.class), "terrainDependentStructures", START_INIT_TERRAIN_DESC, false));
                        toInsert.add(newLabelNode2);

                        // this goes before k=0
                        method.instructions.insertBefore(instruction, toInsert);
                        break;
                    }
                }
            }
        }
        if (found == false) {
            FMLLog.warning("[Muon ASM] %s", "Unpatched MapGenVillage.Start()!");
        }
    }

    private static void transformStructureStart(String name, String transformedName, ClassNode startClass) {
        final String GEN_STRUCTURE_DESC = "(" +
                descClass("net.minecraft.world.World") +
                descClass("java.util.Random") +
                descClass("net.minecraft.world.gen.structure.StructureBoundingBox") +
                ")V";
        final String GEN_STRUCTURE_PRFX_DESC = "(" +
                descClass("net.minecraft.world.gen.structure.StructureStart") +
                descClass("net.minecraft.world.World") +
                descClass("java.util.Random") +
                descClass("net.minecraft.world.gen.structure.StructureBoundingBox") +
                ")V";

        Boolean found = false;
        for (MethodNode method : startClass.methods) {
            String realname = getMethodName(name, method.name, method.desc);
            if (realname.equals("generateStructure") && method.desc.equals(GEN_STRUCTURE_DESC)) {
                found = true;
                // this is the method for generateStructure()
                // we add a hook at the beginning of the function so we can do extra prepratory stuff.

                FMLLog.info("[Muon ASM] %s", "Patching StructureStart Method generateStructure(): " + method.name.toString() + " " + method.desc.toString());
                LabelNode newLabelNode = new LabelNode();
                InsnList toInsert = new InsnList();
                // Check if hook is enabled
                toInsert.add(new LdcInsnNode("using_heightmaps"));
                toInsert.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MuonConfig.class), "getBoolean", "(Ljava/lang/String;)Z", false));
                toInsert.add(new JumpInsnNode(IFEQ, newLabelNode)); // skip if equal to zero (false)
                // conditionally run hook
                toInsert.add(new VarInsnNode(ALOAD, 0)); // StructureStart
                toInsert.add(new VarInsnNode(ALOAD, 1)); // World
                toInsert.add(new VarInsnNode(ALOAD, 2)); // Random
                toInsert.add(new VarInsnNode(ALOAD, 3)); // chunk StructureBoundingBox
                toInsert.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MuonHooks.class), "generateStructure", GEN_STRUCTURE_PRFX_DESC, false));
                toInsert.add(newLabelNode);
                // Shove all that at the beginning to redirect
                method.instructions.insertBefore(method.instructions.getFirst(), toInsert);
            }
        }
        if (found == false) {
            FMLLog.warning("[Muon ASM] %s", "Unpatched StructureStart Method generateStructure()!");
        }
    }

    private static void transformScatteredFeatureStart(String name, String transformedName, ClassNode startClass) {
        final String START_INIT_DESC = "(" +
                descClass("net.minecraft.world.World") +
                descClass("java.util.Random") +
                "II" +
                descClass("net.minecraft.world.biome.Biome") +
                ")V";
        final String START_INIT_TERRAIN_DESC = "(" +
                descClass("net.minecraft.world.gen.structure.StructureStart") +
                descClass("net.minecraft.world.World") +
                ")V";

        Boolean found = false;
        for (MethodNode method : startClass.methods) {
            String realname = getMethodName(name, method.name, method.desc);
            if (realname.equals("<init>") && method.desc.equals(START_INIT_DESC)) {
                // this is the method for Start(World worldIn, Random rand, int x, int z, Biome biome)
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (instruction.getOpcode() == RETURN) {
                        found = true;
                        /*
                         * presumed to be after recalculating bounding box *
                         <WE ARE HERE> RETURN;
                         */
                        FMLLog.info("[Muon ASM] %s", "Patching MapGenScatteredFeature.Start(): " + method.name.toString() + " " + method.desc.toString());
                        LabelNode newLabelNode = new LabelNode();
                        InsnList toInsert = new InsnList();
                        // Check if hook is enabled
                        toInsert.add(new LdcInsnNode("fix_scattered_features"));
                        toInsert.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MuonConfig.class), "getBoolean", "(Ljava/lang/String;)Z", false));
                        toInsert.add(new JumpInsnNode(IFEQ, newLabelNode)); // skip if equal to zero (false)
                        // conditionally run hook
                        toInsert.add(new VarInsnNode(ALOAD, 0));
                        toInsert.add(new VarInsnNode(ALOAD, 1));
                        toInsert.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MuonHooks.class), "featureModTerrain", START_INIT_TERRAIN_DESC, false));
                        toInsert.add(newLabelNode);
                        // this goes before the return
                        method.instructions.insertBefore(instruction, toInsert);
                        break;
                    }
                }
            }
        }
        if (found == false) {
            FMLLog.warning("[Muon ASM] %s", "Unpatched MapGenScatteredFeature.Start()!");
        }
    }

    private static void transformScatteredFeaturePieces(String name, String transformedName, ClassNode villageClass) {
        final String OFFSET_LEVEL_DESC = "(" +
                descClass("net.minecraft.world.World") +
                descClass("net.minecraft.world.gen.structure.StructureBoundingBox") +
                "I)Z";
        final String OFFSET_LEVEL_REPL_DESC = "(" +
                descClass("net.minecraft.world.gen.structure.StructureComponent") +
                descClass("net.minecraft.world.World") +
                descClass("net.minecraft.world.gen.structure.StructureBoundingBox") +
                "I)I";

        Boolean found = false;
        for (MethodNode method : villageClass.methods) {
            String realname = getMethodName(name, method.name, method.desc);
            if (realname.equals("offsetToAverageGroundLevel") && method.desc.equals(OFFSET_LEVEL_DESC)) {
                // this is the method for offsetToAverageGroundLevel()
                FieldInsnNode targetNode = null;
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if ((instruction.getPrevious() != null) &&
                            (instruction.getPrevious().getOpcode() == IDIV) &&
                            (instruction.getOpcode() == PUTFIELD) /*&&
                                    (((FieldInsnNode) instruction).desc == "I")*/) {
                        targetNode = (FieldInsnNode)instruction;
                        break;
                    }
                }
                if (targetNode != null) {
                    found = true;
                    // Insert a hook immediately after calculation to override value
                    FMLLog.info("[Muon ASM] %s", "Patching Feature Method offsetToAverageGroundLevel(): " + method.name.toString() + " (" + realname + ") " + method.desc.toString());
                    LabelNode newLabelNode = new LabelNode();
                    InsnList toInsert = new InsnList();
                    // Check if hook is enabled
                    toInsert.add(new LdcInsnNode("fix_scattered_features"));
                    toInsert.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MuonConfig.class), "getBoolean", "(Ljava/lang/String;)Z", false));
                    toInsert.add(new JumpInsnNode(IFEQ, newLabelNode)); // skip if equal to zero (false)
                    // conditionally run hook
                    toInsert.add(new VarInsnNode(ALOAD, 0));
                    toInsert.add(new VarInsnNode(ALOAD, 0));
                    toInsert.add(new VarInsnNode(ALOAD, 1));
                    toInsert.add(new VarInsnNode(ALOAD, 2));
                    toInsert.add(new VarInsnNode(ILOAD, 3));
                    toInsert.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MuonHooks.class), "offsetToAverageGroundLevel", OFFSET_LEVEL_REPL_DESC, false));
                    toInsert.add(new FieldInsnNode(PUTFIELD, targetNode.owner, targetNode.name, targetNode.desc));
                    toInsert.add(new InsnNode(ICONST_1));
                    toInsert.add(new InsnNode(IRETURN));
                    toInsert.add(newLabelNode);
                    // Shove all that at the beginning to redirect
                    method.instructions.insertBefore(method.instructions.getFirst(), toInsert);
                }
            }
        }
        if (found == false) {
            FMLLog.warning("[Muon ASM] %s", "Unpatched Feature Method offsetToAverageGroundLevel()!");
        }
    }

}
