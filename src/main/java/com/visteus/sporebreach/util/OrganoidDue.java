package com.visteus.sporebreach.util;

import com.Harbinger.Spore.Sentities.BaseEntities.Organoid;

/**
 * A candidate organoid in a growth sweep, paired with the earlier of its recheck/pass due stamps -
 * the key both {@code StructureGrowthDirector} and {@code OutpostWatcherDirector} sort their
 * overdue-first queue by.
 */
public record OrganoidDue(Organoid organoid, long dueAt) {
}
