package de.famiru.ctpuzzle;

import java.util.*;

public class Piece {
    private final long cubicRepresentation;
    private final List<Integer> indexRepresentation;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final boolean readableApostrophe;

    public Piece(List<String> description) {
        this(description, false);
    }

    public Piece(List<String> description, boolean readableApostrophe) {
        long cubicRepresentation = 0;
        List<Integer> indexRepresentation = new ArrayList<>();
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;
        for (int z = 0; z < description.size(); z++) {
            String layer = description.get(z);
            String[] lines = layer.split("\n");
            for (int y = 0; y < lines.length; y++) {
                String line = lines[y];
                for (int x = 0; x < line.length(); x++) {
                    if (line.charAt(x) == 'x') {
                        indexRepresentation.add(z * 20 + y * 5 + x);
                        int cubicIdx = z * 16 + y * 4 + x;
                        cubicRepresentation |= 1L << cubicIdx;
                        if (maxX < x) {
                            maxX = x;
                        }
                        if (maxY < y) {
                            maxY = y;
                        }
                        if (maxZ < z) {
                            maxZ = z;
                        }
                    }
                }
            }
        }
        this.cubicRepresentation = cubicRepresentation;
        this.indexRepresentation = Collections.unmodifiableList(indexRepresentation);
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.readableApostrophe = readableApostrophe;
    }

    private Piece(long cubicRepresentation, int maxX, int maxY, int maxZ) {
        this.cubicRepresentation = cubicRepresentation;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        List<Integer> indexRepresentation = new ArrayList<>();
        for (int z = 0; z <= maxZ; z++) {
            for (int y = 0; y <= maxY; y++) {
                for (int x = 0; x <= maxX; x++) {
                    int cubicIdx = z * 16 + y * 4 + x;
                    if ((cubicRepresentation & 1L << cubicIdx) != 0) {
                        indexRepresentation.add(z * 20 + y * 5 + x);
                    }
                }
            }
        }
        this.indexRepresentation = Collections.unmodifiableList(indexRepresentation);
        this.readableApostrophe = false;
    }

    public Optional<Piece> rotateX90(int times) {
        int maxY = this.maxY;
        int maxZ = this.maxZ;
        long representation = cubicRepresentation;
        long rotated = 0;
        for (int i = 0; i < times; i++) {
            for (int z = 0; z <= maxZ; z++) {
                for (int y = 0; y <= maxY; y++) {
                    for (int x = 0; x <= maxX; x++) {
                        if ((representation & 1L << z * 16 + y * 4 + x) != 0) {
                            rotated |= 1L << (z * 60 + y * 16 + x + 12) % 64;
                        }
                    }
                }
            }
            int tmp = maxY;
            maxY = maxZ;
            maxZ = tmp;
            representation = rotated >> (3 - maxY) * 4;
            rotated = 0L;
        }

        if (representation >= (1L << 48) || representation < 0) {
            return Optional.empty();
        }
        return Optional.of(new Piece(representation, maxX, maxY, maxZ));
    }

    public Optional<Piece> rotateY90(int times) {
        int maxX = this.maxX;
        int maxZ = this.maxZ;
        long representation = cubicRepresentation;
        long rotated = 0;
        for (int i = 0; i < times; i++) {
            for (int z = 0; z <= maxZ; z++) {
                for (int y = 0; y <= maxY; y++) {
                    for (int x = 0; x <= maxX; x++) {
                        if ((representation & 1L << z * 16 + y * 4 + x) != 0) {
                            rotated |= 1L << (z + y * 4 + x * 48 + 48) % 64;
                        }
                    }
                }
            }
            int tmp = maxX;
            maxX = maxZ;
            maxZ = tmp;
            representation = rotated >> (3 - maxZ) * 16;
            rotated = 0L;
        }

        if (representation >= (1L << 48) || representation < 0) {
            return Optional.empty();
        }
        return Optional.of(new Piece(representation, maxX, maxY, maxZ));
    }

    public Optional<Piece> rotateZ90(int times) {
        int maxX = this.maxX;
        int maxY = this.maxY;
        long representation = cubicRepresentation;
        long rotated = 0;
        for (int i = 0; i < times; i++) {
            for (int z = 0; z <= maxZ; z++) {
                for (int y = 0; y <= maxY; y++) {
                    for (int x = 0; x <= maxX; x++) {
                        if ((representation & 1L << z * 16 + y * 4 + x) != 0) {
                            rotated |= 1L << (z * 16 + y * 63 + x * 4 + 3) % 64;
                        }
                    }
                }
            }
            int tmp = maxX;
            maxX = maxY;
            maxY = tmp;
            representation = rotated >> 3 - maxX;
            rotated = 0L;
        }

        if (representation >= (1L << 48) || representation < 0) {
            return Optional.empty();
        }
        return Optional.of(new Piece(representation, maxX, maxY, maxZ));
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public boolean hasReadableApostrophe() {
        return readableApostrophe;
    }

    public List<Integer> getIndexRepresentation() {
        return indexRepresentation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Piece piece = (Piece) o;
        return cubicRepresentation == piece.cubicRepresentation;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cubicRepresentation);
    }

    @Override
    public String toString() {
        return indexRepresentation.toString();
    }
}
