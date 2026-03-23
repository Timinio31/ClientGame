package com.tim.game.client.net;

import com.tim.game.shared.DTOs.update.WorldSnapshotDto;

import java.util.concurrent.atomic.AtomicReference;

public class SnapshotBuffer {
    private final AtomicReference<WorldSnapshotDto> latest = new AtomicReference<>();

    public void set(WorldSnapshotDto snap) { latest.set(snap); }
    public WorldSnapshotDto get() { return latest.get(); }
}
