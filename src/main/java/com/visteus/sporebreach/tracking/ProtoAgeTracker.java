package com.visteus.sporebreach.tracking;

import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.visteus.sporebreach.config.SporeBreachServerConfig;

/**
 * Base Spore gives {@link Proto} no age of its own (unlike {@link com.Harbinger.Spore.Sentities.
 * Organoids.Mound}, which has a real synced {@code AGE} field) - this is the addon-side
 * equivalent, a first-class tracked property usable by any consumer (World Corruption today,
 * goal #4 chunkloading later), not owned by any one of them.
 */
public final class ProtoAgeTracker {

    private static final String CREATED_AT_KEY = "sporebreach_proto_created_game_time";

    private ProtoAgeTracker() {
    }

    /**
     * Stamps the Proto's creation time the first time this is called for it. Safe to call on
     * every join - a no-op once the stamp already exists.
     */
    public static void markCreatedIfAbsent(Proto proto, long gameTime) {
        if (!proto.getPersistentData().contains(CREATED_AT_KEY)) {
            proto.getPersistentData().putLong(CREATED_AT_KEY, gameTime);
        }
    }

    /**
     * Derives the Proto's current age from elapsed time since creation, so any consumer always
     * gets an up-to-date value without a separate polling director keeping it fresh. Protos
     * already alive when this tracking was added have no stamp yet; such a Proto reports age 0
     * until its next join stamps it (see {@link OrganoidTrackingEvents}).
     */
    public static int getAge(Proto proto, long currentGameTime) {
        if (!proto.getPersistentData().contains(CREATED_AT_KEY)) {
            return 0;
        }
        long createdAt = proto.getPersistentData().getLong(CREATED_AT_KEY);
        int interval = SporeBreachServerConfig.PROTO_AGE_UP_INTERVAL_TICKS.get();
        int maxAge = SporeBreachServerConfig.PROTO_MAX_AGE.get();
        long elapsed = Math.max(0, currentGameTime - createdAt);
        long age = interval <= 0 ? 0 : elapsed / interval;
        return (int) Math.min(age, maxAge);
    }
}
