package de.famiru.dlx;

import java.util.List;

/**
 * Generates the exact cover matrix of the n queens problem, following the model of Donald E. Knuth's generator
 * <a href="https://cs.stanford.edu/~knuth/programs/queens-dlx.w">QUEENS-DLX</a>
 * (see <a href="https://www-cs-faculty.stanford.edu/~knuth/programs.html">Knuth: Programs</a>).
 * <p>
 * Each of the n rows and n columns must contain exactly one queen, so they are primary constraints. Each of the
 * 2n-1 diagonals and 2n-1 anti-diagonals may contain at most one queen, so they are secondary constraints. Every
 * cell of the board is one choice.
 * </p>
 * <p>
 * Constraint indices: rows {@code [0, n)}, columns {@code [n, 2n)}, diagonals {@code [2n, 4n-1)},
 * anti-diagonals {@code [4n-1, 6n-2)}.
 * </p>
 */
final class NQueensGenerator {
    record Queen(int row, int column) {
    }

    private NQueensGenerator() {
    }

    /**
     * Creates the {@link Dlx} instance for the n&times;n board. The constraints are set by this method, all other
     * settings are taken from {@code config}.
     */
    static Dlx<Queen> createDlx(int n, DlxBuilder.DlxConfig config) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be greater than 0");
        }
        int numberOfDiagonals = numberOfDiagonals(n);
        DlxBuilder<Queen> builder = config
                .numberOfConstraints(2 * n, 2 * numberOfDiagonals)
                .createChoiceBuilder();
        for (int row = 0; row < n; row++) {
            for (int column = 0; column < n; column++) {
                int diagonal = row + column;
                int antiDiagonal = row - column + n - 1;
                builder.addChoice(new Queen(row, column), List.of(
                        row,
                        n + column,
                        2 * n + diagonal,
                        2 * n + numberOfDiagonals + antiDiagonal));
            }
        }
        return builder.build();
    }

    private static int numberOfDiagonals(int n) {
        return 2 * n - 1;
    }
}
