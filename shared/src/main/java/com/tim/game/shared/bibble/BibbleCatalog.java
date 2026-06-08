package com.tim.game.shared.bibble;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Small default catalog until Bibble species, attacks and type matrix are moved into a database.
 */
public final class BibbleCatalog {
    private BibbleCatalog() {
    }

    public static BibbleStateDto createStarterBibble(String ownerClientId, int index, long seed) {
        Random random = new Random(seed ^ ownerClientId.hashCode() ^ (index * 31L));
        BibbleType[] types = {BibbleType.NATURE, BibbleType.FIRE, BibbleType.WATER};
        BibblePersonality[] personalities = {BibblePersonality.CURIOUS, BibblePersonality.LOYAL, BibblePersonality.TIMID};
        BibbleType type = types[Math.floorMod(index, types.length)];
        BibblePersonality personality = personalities[Math.floorMod(index, personalities.length)];
        float statMultiplier = 0.9f + random.nextFloat() * 0.2f;

        BibbleStateDto bibble = new BibbleStateDto();
        bibble.setBibbleId("bibble:" + UUID.randomUUID());
        bibble.setOwnerClientId(ownerClientId);
        bibble.setCustomName(defaultName(type, index));
        bibble.setSpeciesId(type.name().toLowerCase() + "_starter");
        bibble.setDescription("A domesticated starter Bibble. Real capture, breeding, mutation and training items are TODO systems.");
        bibble.setPrimaryType(type);
        bibble.setPersonality(personality);
        bibble.setStats(BibbleStats.baseFor(type, statMultiplier));
        bibble.setFearLove(35 + random.nextInt(25));
        bibble.setCaptureScore(30f);
        bibble.setAttacks(defaultAttacks(type));
        bibble.setLifecycleState(BibbleLifecycleState.OWNED_STORED_TERMINAL);
        return bibble;
    }


    public static BibbleStateDto createWildBibble(int index, long seed) {
        Random random = new Random(seed ^ 0xB1BB13L ^ (index * 97L));
        BibbleType[] types = {BibbleType.NATURE, BibbleType.FIRE, BibbleType.WATER, BibbleType.STONE, BibbleType.ELECTRIC};
        BibblePersonality[] personalities = {BibblePersonality.WILD, BibblePersonality.CURIOUS, BibblePersonality.TIMID, BibblePersonality.REBELLIOUS};
        BibbleType type = types[Math.floorMod(index, types.length)];
        BibblePersonality personality = personalities[Math.floorMod(index + random.nextInt(personalities.length), personalities.length)];
        float statMultiplier = 0.85f + random.nextFloat() * 0.45f;

        BibbleStateDto bibble = new BibbleStateDto();
        bibble.setBibbleId("wild-bibble:" + UUID.randomUUID());
        bibble.setOwnerClientId(null);
        bibble.setCustomName(defaultName(type, index));
        bibble.setSpeciesId(type.name().toLowerCase() + "_wild");
        bibble.setDescription("Wild Bibble. Capture chance depends on item modifiers, health, personality and relationship values.");
        bibble.setPrimaryType(type);
        bibble.setPersonality(personality);
        bibble.setStats(BibbleStats.baseFor(type, statMultiplier));
        bibble.setFearLove(-15 + random.nextInt(20));
        bibble.setCaptureScore(45f + random.nextFloat() * 35f);
        bibble.setAttacks(defaultAttacks(type));
        bibble.setLifecycleState(BibbleLifecycleState.WILD);
        return bibble;
    }

    private static String defaultName(BibbleType type, int index) {
        return switch (type) {
            case FIRE -> "Cinder Bibble";
            case WATER -> "Ripple Bibble";
            case NATURE -> "Moss Bibble";
            default -> "Bibble " + (index + 1);
        };
    }

    public static List<BibbleAttack> defaultAttacks(BibbleType type) {
        List<BibbleAttack> attacks = new ArrayList<>();
        attacks.add(new BibbleAttack("tackle", "Tackle", BibbleType.NORMAL, false, 8f, 8));
        switch (type == null ? BibbleType.NORMAL : type) {
            case FIRE -> attacks.add(new BibbleAttack("spark_burst", "Spark Burst", BibbleType.FIRE, true, 11f, 14));
            case WATER -> attacks.add(new BibbleAttack("water_push", "Water Push", BibbleType.WATER, true, 9f, 12));
            case NATURE -> attacks.add(new BibbleAttack("vine_snap", "Vine Snap", BibbleType.NATURE, false, 10f, 12));
            case ELECTRIC -> attacks.add(new BibbleAttack("charge", "Charge", BibbleType.ELECTRIC, true, 12f, 16));
            default -> attacks.add(new BibbleAttack("focus", "Focus", BibbleType.PSYCHIC, true, 7f, 10));
        }
        return attacks;
    }
}
