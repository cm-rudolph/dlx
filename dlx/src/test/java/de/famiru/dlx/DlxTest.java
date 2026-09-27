package de.famiru.dlx;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The example matrix is taken from Figure 3 of Donald E. Knuth's paper
 * <a href="https://arxiv.org/abs/cs/0011047">Dancing Links</a>. The tests with secondary constraints extend it by an
 * optional constraint H.
 */
class DlxTest {
    @Test
    void matrixFromFigure3ofPaper_solvedCorrectly() {
        Dlx<String> dlx = Dlx.builder()
                .numberOfConstraints(7)
                .maxNumberOfSolutionsToStore(10)
                .<String>createChoiceBuilder()
                .addChoice("C E F", List.of(2, 4, 5))
                .addChoice("A D G", List.of(0, 3, 6))
                .addChoice("B C F", List.of(1, 2, 5))
                .addChoice("A D", List.of(0, 3))
                .addChoice("B G", List.of(1, 6))
                .addChoice("D E G", List.of(3, 4, 6))
                .build();

        List<List<String>> solutions = dlx.solve();

        assertThat(solutions)
                .hasSize(1)
                .first(InstanceOfAssertFactories.list(String.class))
                .containsExactlyInAnyOrder("A D", "C E F", "B G");
    }

    /**
     * Expected values traced by hand. Updates count the removed column header plus every node removed from other
     * columns, visited nodes count the rows tried per level.
     * <ul>
     *     <li>Level 0: cover A (4), try "A D G" covering D (3) and G (2), try "A D" covering D (3).</li>
     *     <li>Level 1: after "A D G" cover B (3), try "B C F" covering C (3) and F (1); after "A D" cover E (3),
     *     try "C E F" covering F (3) and C (1).</li>
     *     <li>Level 2: after "B C F" cover the empty column E (1); after "C E F" cover B (2), try "B G" covering
     *     G (1), which yields the solution.</li>
     * </ul>
     */
    @Test
    void matrixFromFigure3ofPaper_statsCorrect() {
        Dlx<String> dlx = Dlx.builder()
                .numberOfConstraints(7)
                .maxNumberOfSolutionsToStore(10)
                .<String>createChoiceBuilder()
                .addChoice("C E F", List.of(2, 4, 5))
                .addChoice("A D G", List.of(0, 3, 6))
                .addChoice("B C F", List.of(1, 2, 5))
                .addChoice("A D", List.of(0, 3))
                .addChoice("B G", List.of(1, 6))
                .addChoice("D E G", List.of(3, 4, 6))
                .build();

        dlx.solve();

        assertThat(dlx.getStats()).isEqualTo(new Stats(6, 7, 0, 16, 1,
                List.of(12L, 14L, 4L), List.of(2L, 2L, 1L)));
    }

    @Test
    void matrixWithSecondaryConstraint_constraintNotFulfillable_solvedCorrectly() {
        Dlx<String> dlx = Dlx.builder()
                .numberOfConstraints(7, 1)
                .maxNumberOfSolutionsToStore(10)
                .<String>createChoiceBuilder()
                .addChoice("C E F", List.of(2, 4, 5))
                .addChoice("A D G H", List.of(0, 3, 6, 7))
                .addChoice("B C F", List.of(1, 2, 5))
                .addChoice("A D", List.of(0, 3))
                .addChoice("B G", List.of(1, 6))
                .addChoice("D E G", List.of(3, 4, 6))
                .build();

        List<List<String>> solutions = dlx.solve();

        assertThat(solutions)
                .hasSize(1)
                .first(InstanceOfAssertFactories.list(String.class))
                .containsExactlyInAnyOrder("A D", "C E F", "B G");
    }

    @Test
    void matrixWithSecondaryConstraint_constraintFulfillable_solvedCorrectly() {
        Dlx<String> dlx = Dlx.builder()
                .numberOfConstraints(7, 1)
                .maxNumberOfSolutionsToStore(10)
                .<String>createChoiceBuilder()
                .addChoice("C E F", List.of(2, 4, 5))
                .addChoice("A D G", List.of(0, 3, 6))
                .addChoice("B C F", List.of(1, 2, 5))
                .addChoice("A D H", List.of(0, 3, 7))
                .addChoice("B G", List.of(1, 6))
                .addChoice("D E G", List.of(3, 4, 6))
                .build();

        List<List<String>> solutions = dlx.solve();

        assertThat(solutions)
                .hasSize(1)
                .first(InstanceOfAssertFactories.list(String.class))
                .containsExactlyInAnyOrder("A D H", "C E F", "B G");
    }

    @Test
    void matrixWithUncoveredConstraint_noSolution() {
        Dlx<String> dlx = Dlx.builder()
                .numberOfConstraints(2)
                .maxNumberOfSolutionsToStore(10)
                .<String>createChoiceBuilder()
                .addChoice("A", List.of(0))
                .addChoice("A'", List.of(0))
                .build();

        List<List<String>> solutions = dlx.solve();

        assertThat(solutions).isEmpty();
    }
}