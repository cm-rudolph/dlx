package de.famiru.ctpuzzle;

import java.util.List;

public class Pieces {
    @SuppressWarnings("TextBlockMigration")
    public static final List<Piece> PIECES = List.of(
            new Piece(List.of("xx\n" +
                              "x\n" +
                              "xx")),
            new Piece(List.of("x\nx", "\nx", "\nx\nx"), true),
            new Piece(List.of(" x\n" +
                              " x\n" +
                              " x\n" +
                              "xx")),
            new Piece(List.of("x\n" +
                              "xx\n" +
                              "x\n" +
                              "x")),
            new Piece(List.of("x\nx", "x\nxx"), true),
            new Piece(List.of("xxx\n" +
                              " x\n" +
                              " x")),
            new Piece(List.of(" x\n" +
                              "xx\n" +
                              "xx")),
            new Piece(List.of("  x\n" +
                              " xx\n" +
                              "xx")),
            new Piece(List.of("x\nx", "xx"), true),
            new Piece(List.of("x\n" +
                              "xxx\n" +
                              " x")),
            new Piece(List.of(" x\n" +
                              "xxx\n" +
                              " x")),
            new Piece(List.of("x\n" +
                              "xx\n" +
                              "x\n" +
                              "xx"))
    );

    private Pieces() {
    }
}
