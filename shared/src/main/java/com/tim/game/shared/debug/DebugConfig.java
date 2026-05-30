package com.tim.game.shared.debug;

import java.util.EnumSet;
import java.util.Set;

public final class DebugConfig {

    private static boolean globalEnabled = true;

    private static final Set<DebugCategory> enabledCategories =
            EnumSet.noneOf(DebugCategory.class);

    private DebugConfig() {
    }

    public static void setGlobalEnabled(boolean enabled) {
        globalEnabled = enabled;
    }

    public static boolean isGlobalEnabled() {
        return globalEnabled;
    }

    public static void enable(DebugCategory category) {
        enabledCategories.add(category);
    }

    public static void disable(DebugCategory category) {
        enabledCategories.remove(category);
    }

    public static void enableAll() {
        enabledCategories.clear();
        enabledCategories.addAll(EnumSet.allOf(DebugCategory.class));
    }

    public static void disableAll() {
        enabledCategories.clear();
    }

    public static boolean isEnabled(DebugCategory category) {
        return globalEnabled && enabledCategories.contains(category);
    }

    public static void log(DebugCategory category, String message) {
        if (isEnabled(category)) {
            System.out.println("[" + category.name() + "] " + message);
        }
    }
}