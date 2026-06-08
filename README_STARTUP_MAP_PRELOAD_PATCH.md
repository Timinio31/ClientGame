# Startup Map Preload Patch

This patch changes the map loading behavior after the layered-map/chunk-streaming update.

## Goal

The server still generates the complete finite world at room start. The client no longer streams chunks based on camera/player movement. Instead, the client preloads the complete map once at startup. During that preload, a loading/progress screen is shown. After preload completion, movement does not trigger additional chunk loading, so no empty chunk border should become visible while walking.

## Main changes

- `WorldSettings` default map size is now `1024x1024` instead of `32x32`.
- `chunkStreamingEnabled` remains enabled as chunked startup transfer for large worlds.
- Runtime/camera-based chunk prefetching was removed from `GameScreen`.
- Startup chunk preload was added to `GameScreen`.
- The renderer now uses a spatial lookup (`x,y -> TileStateDto`) and iterates only visible tile coordinates instead of scanning all loaded tiles every frame.
- Full map DTO tile lists are indexed and then cleared client-side to avoid duplicate memory and per-frame iteration.
- Biome noise scales were reduced to create larger, less noisy biome regions.
- Server event serialization now omits `null` values to reduce map/chunk payload size.

## Practical size note

`1024x1024` is a much larger playable world than the original map and should be a reasonable first target for this architecture.
Values around `2048x2048` may work depending on RAM and RabbitMQ throughput.
Values close to `8048x8048` are not recommended yet with object-per-tile + DTO-per-tile storage. For that scale, the next architectural step should be packed tile storage and/or client-side deterministic map reconstruction from seed.
