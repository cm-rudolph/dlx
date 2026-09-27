package de.famiru.dlx;

import de.famiru.dlx.NQueensGenerator.Queen;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NQueensTest {
    /**
     * Expected numbers of solutions from <a href="https://oeis.org/A000170">OEIS A000170</a>.
     */
    @ParameterizedTest(name = "n = {0}")
    @CsvSource({
            "1, 1",
            "2, 0",
            "3, 0",
            "4, 2",
            "5, 10",
            "6, 4",
            "7, 40",
            "8, 92",
            "9, 352",
            "10, 724"
    })
    void nQueens_findsKnownNumberOfSolutions(int n, int expectedNumberOfSolutions) {
        Dlx<Queen> dlx = NQueensGenerator.createDlx(n, Dlx.builder().maxNumberOfSolutionsToStore(Integer.MAX_VALUE));

        List<List<Queen>> solutions = dlx.solve();

        assertThat(solutions).hasSize(expectedNumberOfSolutions);
    }

    @Test
    void nQueens10_multithreaded_findsSameSolutionsAsSingleThreaded() {
        List<Set<Queen>> singleThreaded = solve(Dlx.builder().disableMultithreading());
        List<Set<Queen>> multiThreaded = solve(Dlx.builder().enableMultithreading(2, 2));

        assertThat(multiThreaded).containsExactlyInAnyOrderElementsOf(singleThreaded);
    }

    @Test
    void nQueens10_singleThreaded_storesExactlyMaxNumberOfSolutions() {
        Dlx<Queen> dlx = NQueensGenerator.createDlx(10, Dlx.builder()
                .disableMultithreading()
                .maxNumberOfSolutionsToStore(5));

        List<List<Queen>> solutions = dlx.solve();

        assertThat(solutions).hasSize(5);
    }

    /**
     * Without synchronization between threads, more solutions than requested might be found, see README.
     */
    @Test
    void nQueens10_multithreaded_storesAtLeastMaxNumberOfSolutionsButNotAll() {
        Dlx<Queen> dlx = NQueensGenerator.createDlx(10, Dlx.builder()
                .enableMultithreading(0, 2)
                .maxNumberOfSolutionsToStore(5));

        List<List<Queen>> solutions = dlx.solve();

        assertThat(solutions).hasSizeBetween(5, 723);
    }

    private static List<Set<Queen>> solve(DlxBuilder.DlxConfig config) {
        return NQueensGenerator.createDlx(10, config.maxNumberOfSolutionsToStore(Integer.MAX_VALUE))
                .solve()
                .stream()
                .map(Set::copyOf)
                .toList();
    }
}
