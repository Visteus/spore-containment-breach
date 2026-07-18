package com.visteus.sporebreach.spawning;

import com.visteus.sporebreach.SporeContainmentBreach;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class ForbiddenSpawnTags {

    public static final TagKey<EntityType<?>> CALAMITIES_AND_WOMBS = TagKey.create(
            Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(SporeContainmentBreach.MODID, "calamities_and_wombs")
    );

    private ForbiddenSpawnTags() {
    }
}
