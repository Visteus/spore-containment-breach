package com.visteus.sporebreach.structuregrowth;

import com.Harbinger.Spore.core.Sblocks;
import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.mixin.StructureTemplateAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Shared structure-template plumbing for the goal #3 organoid structure growth system: resolving
 * templates, reading their full block list (bypassing vanilla's single-block-type {@link
 * StructureTemplate#filterBlocks}), sampling real terrain height for anchoring, gating underground
 * growth to genuine terrain, and swapping nearby water for crusted bile once a surface structure
 * completes.
 */
public final class OrganoidStructurePlacer {

    /**
     * Vanilla dirt/sand/stone/etc and Spore's own passively-infested terrain variants - deliberately
     * excludes the "grown structure" body blocks (biomass_block, membrane_block, fungal_stem, ...)
     * so an underground job can tell real ground apart from another organoid's structure poking
     * through the heightmap it anchored off of.
     */
    public static final TagKey<Block> NATURAL_GROUND =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(SporeContainmentBreach.MODID, "natural_ground"));

    private OrganoidStructurePlacer() {
    }

    public static StructureTemplate resolveTemplate(ServerLevel level, ResourceLocation id) {
        return level.getStructureManager().getOrCreate(id);
    }

    /**
     * Full block list for {@code template}, transformed to world-space positions/states as if
     * placed at {@code origin} with identity {@code settings} - the same transform vanilla's own
     * {@code placeInWorld}/{@code filterBlocks} apply, but for every block rather than one type.
     */
    public static List<StructureTemplate.StructureBlockInfo> worldBlocks(
            StructureTemplate template, BlockPos origin, StructurePlaceSettings settings
    ) {
        List<StructureTemplate.Palette> palettes = ((StructureTemplateAccessor) template).sporebreach$getPalettes();
        List<StructureTemplate.StructureBlockInfo> result = new ArrayList<>();
        if (palettes.isEmpty()) {
            return result;
        }
        for (StructureTemplate.StructureBlockInfo info : palettes.get(0).blocks()) {
            BlockPos worldPos = StructureTemplate.calculateRelativePosition(settings, info.pos()).offset(origin);
            BlockState worldState = info.state().mirror(settings.getMirror()).rotate(settings.getRotation());
            result.add(new StructureTemplate.StructureBlockInfo(worldPos, worldState, info.nbt()));
        }
        return result;
    }

    /** Real terrain surface height at the given column - the anchor Y for a new surface job. */
    public static int surfaceHeight(ServerLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
    }

    /**
     * True if {@code pos} is real terrain rather than another organoid's already-grown structure.
     * The heightmap an underground job anchors off of can't tell those apart on its own - it
     * happily reports the top of a neighboring Mound's tower as "surface" - so this is checked
     * before an underground job is allowed to start digging into whatever it lands on.
     */
    public static boolean isNaturalGround(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(NATURAL_GROUND);
    }

    /**
     * Swaps water source blocks for {@code spore:crusted_bile} in the area around a completed
     * surface structure's base - goal #3.6, keeping ponds/rivers from sitting untouched right next
     * to a freshly-grown tower.
     */
    public static void replaceNearbyWaterWithBile(ServerLevel level, StructureGrowthJob.Footprint footprint, int radius) {
        BlockPos min = footprint.min().offset(-radius, -1, -radius);
        BlockPos max = footprint.max().offset(radius, 2, radius);
        BlockState bile = Sblocks.CRUSTED_BILE.get().defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            FluidState fluid = level.getFluidState(pos);
            if (fluid.isSource() && fluid.is(Fluids.WATER)) {
                level.setBlock(pos, bile, 3);
            }
        }
    }
}
