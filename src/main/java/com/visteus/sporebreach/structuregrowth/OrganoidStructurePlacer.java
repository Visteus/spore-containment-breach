package com.visteus.sporebreach.structuregrowth;

import com.visteus.sporebreach.SporeContainmentBreach;
import com.visteus.sporebreach.mixin.StructureTemplateAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

/**
 * Shared structure-template plumbing for the goal #3 organoid structure growth system: resolving
 * templates, reading their full block list (bypassing vanilla's single-block-type {@link
 * StructureTemplate#filterBlocks}), sampling real terrain height for anchoring, and gating
 * underground growth to genuine terrain. Nearby-water crusting is no longer tied to individual
 * structures - see {@code com.visteus.sporebreach.biome.AreaWaterReplacementJob}, part of goal
 * #5's biome spread.
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
     */
    public static boolean isNaturalGround(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(NATURAL_GROUND);
    }

    /**
     * Fraction of {@code job}'s target positions that are currently natural terrain, checked
     * before an underground job is allowed to start. The heightmap an underground job anchors off
     * of can't tell real ground apart from a neighboring organoid's structure on its own - it
     * happily reports the top of a Mound's tower as "surface" too - and a single sample point at
     * the anchor can't tell a solid mountain apart from a thin floating overhang with open sky
     * just beneath it. Checking coverage across the whole job instead handles both: a foreign
     * structure or open sky under most of the footprint pulls this down, while ordinary cave
     * pockets inside otherwise-solid terrain barely move it.
     */
    public static double naturalGroundCoverage(ServerLevel level, StructureGrowthJob job) {
        Set<BlockPos> positions = job.targetPositions();
        if (positions.isEmpty()) {
            return 1.0;
        }
        long naturalCount = positions.stream().filter(pos -> isNaturalGround(level, pos)).count();
        return (double) naturalCount / positions.size();
    }
}
