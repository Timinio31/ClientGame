package com.tim.game.shared.bibble;

import java.util.HashMap;
import java.util.Map;

/**
 * Default type multiplier matrix. Later this should be loaded from the database/world preset.
 */
public final class BibbleTypeMatrix {
    private static final Map<String, Float> MULTIPLIERS = new HashMap<>();

    static {
        set(BibbleType.FIRE, BibbleType.NATURE, 1.5f);
        set(BibbleType.FIRE, BibbleType.ICE, 1.5f);
        set(BibbleType.FIRE, BibbleType.WATER, 0.65f);
        set(BibbleType.WATER, BibbleType.FIRE, 1.5f);
        set(BibbleType.WATER, BibbleType.STONE, 1.35f);
        set(BibbleType.NATURE, BibbleType.WATER, 1.4f);
        set(BibbleType.NATURE, BibbleType.STONE, 1.35f);
        set(BibbleType.ELECTRIC, BibbleType.WATER, 1.5f);
        set(BibbleType.STONE, BibbleType.ELECTRIC, 1.3f);
        set(BibbleType.METAL, BibbleType.ICE, 1.25f);
        set(BibbleType.PSYCHIC, BibbleType.NORMAL, 1.25f);
        set(BibbleType.SHADOW, BibbleType.LIGHT, 1.35f);
        set(BibbleType.LIGHT, BibbleType.SHADOW, 1.35f);
        set(BibbleType.TOXIC, BibbleType.NATURE, 1.4f);
        set(BibbleType.ICE, BibbleType.NATURE, 1.35f);
    }

    private BibbleTypeMatrix() {
    }

    public static float multiplier(BibbleType attacker, BibbleType defender) {
        if (attacker == null || defender == null) {
            return 1.0f;
        }
        return MULTIPLIERS.getOrDefault(key(attacker, defender), 1.0f);
    }

    private static void set(BibbleType attacker, BibbleType defender, float multiplier) {
        MULTIPLIERS.put(key(attacker, defender), multiplier);
    }

    private static String key(BibbleType attacker, BibbleType defender) {
        return attacker.name() + ">" + defender.name();
    }
}
