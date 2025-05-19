package org.example;

public class Tile {
    private TileType type;
    private boolean isWalkable;

    public Tile(TileType type) {
        this.type = type;
        this.isWalkable = type == TileType.EMPTY;
    }

    public TileType getType() {
        return type;
    }
    public boolean isWalkable() {
        return isWalkable;
    }
}
