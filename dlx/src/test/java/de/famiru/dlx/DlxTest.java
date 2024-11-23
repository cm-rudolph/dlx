package de.famiru.dlx;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DlxTest {
    @Test
    void matrixFromFigure3ofPaper_solvedCorrectly() {
        Dlx<String> dlx = new Dlx<>(7, Set.of(), -1, 1, 10, false, Integer.MAX_VALUE);
        dlx.addChoice("C E F", List.of(2, 4, 5));
        dlx.addChoice("A D G", List.of(0, 3, 6));
        dlx.addChoice("B C F", List.of(1, 2, 5));
        dlx.addChoice("A D", List.of(0, 3));
        dlx.addChoice("B G", List.of(1, 6));
        dlx.addChoice("D E G", List.of(3, 4, 6));

        List<List<String>> solutions = dlx.solve();

        assertThat(solutions)
                .hasSize(1)
                .first(InstanceOfAssertFactories.list(String.class))
                .containsExactlyInAnyOrder("A D", "C E F", "B G");
    }

    @Test
    void matrixWithSecondaryConstraint_constraintNotFulfillable_solvedCorrectly() {
        Dlx<String> dlx = new Dlx<>(8, Set.of(7), -1, 1, 10, false, Integer.MAX_VALUE);
        dlx.addChoice("C E F", List.of(2, 4, 5));
        dlx.addChoice("A D G H", List.of(0, 3, 6, 7));
        dlx.addChoice("B C F", List.of(1, 2, 5));
        dlx.addChoice("A D", List.of(0, 3));
        dlx.addChoice("B G", List.of(1, 6));
        dlx.addChoice("D E G", List.of(3, 4, 6));

        List<List<String>> solutions = dlx.solve();

        assertThat(solutions)
                .hasSize(1)
                .first(InstanceOfAssertFactories.list(String.class))
                .containsExactlyInAnyOrder("A D", "C E F", "B G");
    }

    @Test
    void matrixWithSecondaryConstraint_constraintFulfillable_solvedCorrectly() {
        Dlx<String> dlx = new Dlx<>(8, Set.of(7), -1, 1, 10, false, Integer.MAX_VALUE);
        dlx.addChoice("C E F", List.of(2, 4, 5));
        dlx.addChoice("A D G", List.of(0, 3, 6));
        dlx.addChoice("B C F", List.of(1, 2, 5));
        dlx.addChoice("A D H", List.of(0, 3, 7));
        dlx.addChoice("B G", List.of(1, 6));
        dlx.addChoice("D E G", List.of(3, 4, 6));

        List<List<String>> solutions = dlx.solve();

        assertThat(solutions)
                .hasSize(1)
                .first(InstanceOfAssertFactories.list(String.class))
                .containsExactlyInAnyOrder("A D H", "C E F", "B G");
    }
}