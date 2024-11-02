package de.famiru.ctpuzzle;

import de.famiru.dlx.Dlx;
import de.famiru.dlx.Stats;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class App {
    private static final Logger LOGGER = LogManager.getLogger(App.class);
    private final boolean findSolutionWithReadableText;
    private int choices = 0;

    public App(boolean findSolutionWithReadableText) {
        this.findSolutionWithReadableText = findSolutionWithReadableText;
    }

    public static void main(String[] args) {
        new App(false).run();
    }

    private void run() {
        Dlx<String> dlx = new Dlx<>(0, true, 500000);
        if (findSolutionWithReadableText) {
            dlx.addChoice("c", List.of(5, 6, 10, 15, 16, 60));
            dlx.addChoice("t", List.of(3, 8, 9, 13, 18, 19, 71));
        }
        for (int i = 0; i < Pieces.PIECES.size(); i++) {
            if (findSolutionWithReadableText && (i == 0 || i == 11)) {
                continue;
            }
            Piece piece = Pieces.PIECES.get(i);
            Set<Piece> allRotations = new HashSet<>();
            allRotations.add(piece);
            createChoices(dlx, "de.famiru.ctpuzzle.Piece " + (i + 1), piece, i);

            Optional<Piece> pieceY90 = piece.rotateY90(1);
            Optional<Piece> pieceX90 = piece.rotateX90(1);
            if (i == 6) {
                Optional<Piece> pieceZ90 = piece.rotateZ90(1);
                if (pieceZ90.map(allRotations::add).orElse(false)) {
                    createChoices(dlx, "de.famiru.ctpuzzle.Piece " + (i + 1) + " Z1", pieceZ90.get(), i);
                }
                if (pieceY90.map(allRotations::add).orElse(false)) {
                    createChoices(dlx, "de.famiru.ctpuzzle.Piece " + (i + 1) + " Y1", pieceY90.get(), i);
                }
                if (pieceX90.map(allRotations::add).orElse(false)) {
                    createChoices(dlx, "de.famiru.ctpuzzle.Piece " + (i + 1) + " X1", pieceX90.get(), i);
                }
                Optional<Piece> pieceY90X90 = pieceY90.flatMap(p -> p.rotateX90(1));
                if (pieceY90X90.map(allRotations::add).orElse(false)) {
                    createChoices(dlx, "de.famiru.ctpuzzle.Piece " + (i + 1) + " Y1X1", pieceY90X90.get(), i);
                }
                Optional<Piece> pieceX90Y90 = pieceX90.flatMap(p -> p.rotateY90(1));
                if (pieceX90Y90.map(allRotations::add).orElse(false)) {
                    createChoices(dlx, "de.famiru.ctpuzzle.Piece " + (i + 1) + " X1Y1", pieceX90Y90.get(), i);
                }
            } else {
                rotateZ(allRotations, piece, dlx, "de.famiru.ctpuzzle.Piece " + (i + 1) + " ", i);

                Optional<Piece> pieceY180 = piece.rotateY90(2);
                if (pieceY180.map(allRotations::add).orElse(false)) {
                    String prefix = "de.famiru.ctpuzzle.Piece " + (i + 1) + " Y2";
                    createChoices(dlx, prefix, pieceY180.get(), i);
                    rotateZ(allRotations, pieceY180.get(), dlx, prefix, i);
                }

                if (pieceY90.map(allRotations::add).orElse(false)) {
                    String prefix = "de.famiru.ctpuzzle.Piece " + (i + 1) + " Y1";
                    createChoices(dlx, prefix, pieceY90.get(), i);
                    rotateX(allRotations, pieceY90.get(), dlx, prefix, i);
                }

                Optional<Piece> pieceY270 = piece.rotateY90(3);
                if (pieceY270.map(allRotations::add).orElse(false)) {
                    String prefix = "de.famiru.ctpuzzle.Piece " + (i + 1) + " Y3";
                    createChoices(dlx, prefix, pieceY270.get(), i);
                    rotateX(allRotations, pieceY270.get(), dlx, prefix, i);
                }
                String x90Prefix = "de.famiru.ctpuzzle.Piece " + (i + 1) + " X1";
                if (pieceX90.isEmpty()) {
                    x90Prefix = "de.famiru.ctpuzzle.Piece " + (i + 1) + " Z1X1";
                    pieceX90 = piece.rotateZ90(1).flatMap(p -> p.rotateX90(1));
                }
                if (pieceX90.map(allRotations::add).orElse(false)) {
                    createChoices(dlx, x90Prefix, pieceX90.get(), i);
                    rotateY(allRotations, pieceX90.get(), dlx, x90Prefix, i);
                }

                Optional<Piece> pieceX270 = piece.rotateX90(3);
                String x270Prefix = "de.famiru.ctpuzzle.Piece " + (i + 1) + " X3";
                if (pieceX270.isEmpty()) {
                    x270Prefix = "de.famiru.ctpuzzle.Piece " + (i + 1) + " Z1X3";
                    pieceX270 = piece.rotateZ90(1).flatMap(p -> p.rotateX90(3));
                }
                if (pieceX270.map(allRotations::add).orElse(false)) {
                    createChoices(dlx, x270Prefix, pieceX270.get(), i);
                    rotateY(allRotations, pieceX270.get(), dlx, x270Prefix, i);
                }
            }

            LOGGER.debug("{}: ({}) {}", i + 1, allRotations.size(), allRotations);
        }
        LOGGER.info("{} choices.", choices);
        Thread.currentThread().setPriority(5);
        Instant start = Instant.now();
        LOGGER.info("Solutions: {}", dlx.solve());
        Instant end = Instant.now();
        LOGGER.info("Solving took {}ms", Duration.between(start, end).toMillis());
        Stats stats = dlx.getStats();
        LOGGER.info("Stats: {}", stats);
    }

    private void createChoices(Dlx<String> dlx, String prefix, Piece piece, int i) {
        List<Integer> pieceIndexRepresentation = piece.getIndexRepresentation();
        for (int z = 0; z < 3 - piece.getMaxZ(); z++) {
            for (int y = 0; y < 4 - piece.getMaxY(); y++) {
                for (int x = 0; x < 5 - piece.getMaxX(); x++) {
                    List<Integer> choice = new ArrayList<>(pieceIndexRepresentation.size() + 1);
                    for (Integer idx : pieceIndexRepresentation) {
                        choice.add(idx + x + y * 5 + z * 20);
                    }
                    choice.add(i + 60);
                    if (findSolutionWithReadableText && piece.hasReadableApostrophe() && x == 2 && y == 0 && z == 0) {
                        choice.add(72);
                    }
                    String rowName = prefix + " Shift X" + x + "Y" + y + "Z" + z + ": " + choice;
                    dlx.addChoice(rowName, choice);
                    LOGGER.debug("{}", rowName);
                    choices++;
                }
            }
        }
    }

    private void rotateX(Set<Piece> allRotations, Piece piece, Dlx<String> dlx, String prefix, int i) {
        for (int j = 1; j < 4; j++) {
            Optional<Piece> pieceRotated = piece.rotateX90(j);
            if (pieceRotated.map(allRotations::add).orElse(false)) {
                createChoices(dlx, prefix + "X" + j, pieceRotated.get(), i);
            }
        }
    }

    private void rotateY(Set<Piece> allRotations, Piece piece, Dlx<String> dlx, String prefix, int i) {
        for (int j = 1; j < 4; j++) {
            Optional<Piece> pieceRotated = piece.rotateY90(j);
            if (pieceRotated.map(allRotations::add).orElse(false)) {
                createChoices(dlx, prefix + "Y" + j, pieceRotated.get(), i);
            }
        }
    }

    private void rotateZ(Set<Piece> allRotations, Piece piece, Dlx<String> dlx, String prefix, int i) {
        for (int j = 1; j < 4; j++) {
            Optional<Piece> pieceRotated = piece.rotateZ90(j);
            if (pieceRotated.map(allRotations::add).orElse(false)) {
                createChoices(dlx, prefix + "Z" + j, pieceRotated.get(), i);
            }
        }
    }
}
