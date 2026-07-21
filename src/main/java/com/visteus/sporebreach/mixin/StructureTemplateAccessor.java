package com.visteus.sporebreach.mixin;

import java.util.List;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code StructureTemplate}'s private {@code palettes} field. Vanilla's public
 * {@link StructureTemplate#filterBlocks} only returns blocks matching one given {@link
 * net.minecraft.world.level.block.Block}, which isn't enough to read a whole structure's block
 * list up front for the frontier-growth placement in {@code structuregrowth/} - everything else
 * needed ({@code Palette#blocks()}, {@code StructureTemplate#calculateRelativePosition}, the
 * {@code StructureBlockInfo} record accessors) is already public.
 */
@Mixin(StructureTemplate.class)
public interface StructureTemplateAccessor {

    @Accessor("palettes")
    List<StructureTemplate.Palette> sporebreach$getPalettes();
}
