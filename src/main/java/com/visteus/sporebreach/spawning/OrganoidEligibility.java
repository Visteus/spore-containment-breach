package com.visteus.sporebreach.spawning;

/**
 * Reason an organoid was or wasn't included in a director cycle's eligible list - shared by
 * MoundDefenseSpawner and ProtoRaidDirector so OrganoidSpawnDirector can log exclusions
 * without duplicating either class's gate logic.
 */
public enum OrganoidEligibility {
    ELIGIBLE,
    INVALID_LEVEL,
    PROTECTED_SPAWN_RADIUS,
    ON_COOLDOWN
}
