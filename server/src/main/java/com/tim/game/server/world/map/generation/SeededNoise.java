package com.tim.game.server.world.map.generation;

public final class SeededNoise {

    private final long seed;

    public SeededNoise(long seed) {
        this.seed = seed;
    }

    public float value(float x, float y, float scale, long salt) {
        float nx = x * scale;
        float ny = y * scale;
        int x0 = fastFloor(nx);
        int y0 = fastFloor(ny);
        int x1 = x0 + 1;
        int y1 = y0 + 1;

        float sx = smooth(nx - x0);
        float sy = smooth(ny - y0);

        float n00 = randomUnit(x0, y0, salt);
        float n10 = randomUnit(x1, y0, salt);
        float n01 = randomUnit(x0, y1, salt);
        float n11 = randomUnit(x1, y1, salt);

        float ix0 = lerp(n00, n10, sx);
        float ix1 = lerp(n01, n11, sx);
        return lerp(ix0, ix1, sy);
    }

    public float fbm(float x, float y, float scale, int octaves, float persistence, float lacunarity, long salt) {
        float total = 0f;
        float amplitude = 1f;
        float frequency = 1f;
        float max = 0f;
        for (int i = 0; i < octaves; i++) {
            total += value(x, y, scale * frequency, salt + i * 31L) * amplitude;
            max += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        if (max <= 0f) {
            return 0f;
        }
        return clamp01(total / max);
    }

    public float cellular(float x, float y, float cellSize, long salt) {
        int cx = fastFloor(x / cellSize);
        int cy = fastFloor(y / cellSize);
        float bestDistance = Float.MAX_VALUE;
        long bestHash = 0L;

        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                int nx = cx + ox;
                int ny = cy + oy;
                float px = (nx + randomUnit(nx, ny, salt + 17L)) * cellSize;
                float py = (ny + randomUnit(nx, ny, salt + 43L)) * cellSize;
                float dx = px - x;
                float dy = py - y;
                float distance = dx * dx + dy * dy;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestHash = hash(nx, ny, salt + 91L);
                }
            }
        }

        long positive = bestHash & 0x7fffffffffffffffL;
        return (positive % 10000L) / 9999f;
    }

    public float randomUnit(int x, int y, long salt) {
        long h = hash(x, y, salt);
        long positive = h & 0x7fffffffffffffffL;
        return (positive % 1000000L) / 999999f;
    }

    public long hash(int x, int y, long salt) {
        long h = seed ^ salt;
        h ^= 0x9E3779B97F4A7C15L + (long) x * 0xBF58476D1CE4E5B9L;
        h = Long.rotateLeft(h, 27);
        h ^= 0x94D049BB133111EBL + (long) y * 0xD2B74407B1CE6E93L;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return h;
    }

    private int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private float smooth(float t) {
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float clamp01(float value) {
        if (value < 0f) {
            return 0f;
        }
        if (value > 1f) {
            return 1f;
        }
        return value;
    }
}
