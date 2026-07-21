package com.visteus.sporebreach.structuregrowth;

import com.visteus.sporebreach.mixin.StructureTemplateAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Shared structure-template plumbing for the goal #3 organoid structure growth system: resolving
 * templates, reading their full block list (bypassing vanilla's single-block-type {@link
 * StructureTemplate#filterBlocks}), and sampling real terrain height for anchoring.
 */
public final class OrganoidStructurePlacer {

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
}
