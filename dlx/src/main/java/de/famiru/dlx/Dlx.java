package de.famiru.dlx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Dlx<T> {
    private static final Logger LOGGER = LogManager.getLogger(Dlx.class);

    private final MatrixEntry<T> head;
    private final List<MatrixEntry<T>> columnHeads;
    private final List<MatrixEntry<T>> solution;
    private final int maxNumberOfSolutionsToStore;
    private final boolean countAllSolutions;
    private final int statusLogStepWidth;
    private final AtomicReference<State> state = new AtomicReference<>(State.INITIALIZING);
    private final CountDownLatch solvedLatch = new CountDownLatch(1);
    private final List<List<T>> solutions = new ArrayList<>();
    private int solutionsFound = 0;
    private long updates = 0;
    private long visitedNodes = 0;

    public Dlx(int maxNumberOfSolutionsToStore, boolean countAllSolutions, int statusLogStepWidth) {
        this.maxNumberOfSolutionsToStore = maxNumberOfSolutionsToStore;
        this.countAllSolutions = countAllSolutions;
        this.statusLogStepWidth = statusLogStepWidth;
        head = new MatrixEntry<>();
        columnHeads = new ArrayList<>();
        solution = new ArrayList<>();
    }

    /**
     * Add a new choice (row) to the exact cover matrix.
     *
     * @param choiceData    the data that describes the choice
     * @param columnIndices strictly increasing list of column indices that are set to 1. A row of {@code 1 0 0 1 0}
     *                      would be described by a list of {@code 0, 3}.
     */
    public void addChoice(T choiceData, List<Integer> columnIndices) {
        if (state.get() != State.INITIALIZING) {
            throw new IllegalStateException("solve() has already been called.");
        }
        if (columnIndices == null || columnIndices.isEmpty()) {
            return;
        }

        ensureColumnHeadSize(columnIndices.get(columnIndices.size() - 1) + 1);
        MatrixEntry<T> firstRowElement = null;
        int lastIdx = -1;
        for (Integer columnIndex : columnIndices) {
            if (lastIdx >= columnIndex) {
                throw new IllegalArgumentException("Column indices must be >= 0 and strictly increasing.");
            }
            lastIdx = columnIndex;

            MatrixEntry<T> columnHead = columnHeads.get(columnIndex);
            MatrixEntry<T> element = new MatrixEntry<>(choiceData, columnHead);
            columnHead.insertAbove(element);
            if (firstRowElement != null) {
                firstRowElement.insertBefore(element);
            } else {
                firstRowElement = element;
            }
        }
        if (state.get() != State.INITIALIZING) {
            throw new IllegalStateException("solve() has already been called while adding an additional choice.");
        }
    }

    public int getNumberOfSolutionsFound() {
        return solutionsFound;
    }

    public long getNumberOfUpdates() {
        return updates;
    }

    public long getVisitedNodes() {
        return visitedNodes;
    }

    private void ensureColumnHeadSize(int minSize) {
        while (columnHeads.size() < minSize) {
            MatrixEntry<T> columnHead = new MatrixEntry<>();
            columnHeads.add(columnHead);
            head.insertBefore(columnHead);
        }
    }

    /**
     * Solves the exact cover problem previously initialized using {@link #addChoice(Object, List)} by executing
     * Donald E. Knuth's algorithm DLX. Executes only once and stores the result.
     *
     * @return All solutions up until {@code maxNumberOfSolutionsToStore} that have been found.
     */
    public List<List<T>> solve() {
        if (state.compareAndSet(State.INITIALIZING, State.SOLVING)) {
            try {
                LOGGER.info("Solving using DLX...");
                search(0);
                LOGGER.info("Found {} solutions doing {} updates and visiting {} nodes.",
                        solutionsFound, updates, visitedNodes);
            } finally {
                state.set(State.SOLVED);
                solvedLatch.countDown();
            }
        }

        try {
            solvedLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        return Collections.unmodifiableList(solutions);
    }

    private boolean search(int k) {
        if (head.getRight() == head) {
            doSolutionBookkeeping();
            return !(countAllSolutions || solutionsFound < maxNumberOfSolutionsToStore);
        }
        MatrixEntry<T> c = selectNextColumn();
        updates += c.coverColumn();
        MatrixEntry<T> r = c.getLower();
        while (r != c) {
            visitedNodes++;
            solution.add(r);
            MatrixEntry<T> j = r.getRight();
            while (j != r) {
                updates += j.coverColumn();
                j = j.getRight();
            }
            if (search(k + 1)) return true;
            /*r = */solution.remove(solution.size() - 1);
            //c = r.getColumnHeader();
            j = r.getLeft();
            while (j != r) {
                j.uncoverColumn();
                j = j.getLeft();
            }
            r = r.getLower();
        }
        c.uncoverColumn();
        return false;
    }

    private void doSolutionBookkeeping() {
        if (solutionsFound < maxNumberOfSolutionsToStore) {
            solutions.add(solution.stream()
                    .map(MatrixEntry::getData)
                    .collect(Collectors.toList()));
        }
        solutionsFound++;
        if (solutionsFound % statusLogStepWidth == 0) {
            LOGGER.info("Found {} solutions doing {} updates and visiting {} nodes so far.",
                    solutionsFound, updates, visitedNodes);
        }
    }

    private MatrixEntry<T> selectNextColumn() {
        MatrixEntry<T> c = head.getRight();
        MatrixEntry<T> bestMatch = c;
        int bestRowCount = c.getRowCount();
        while (c != head) {
            if (c.getRowCount() < bestRowCount) {
                bestRowCount = c.getRowCount();
                bestMatch = c;
            }
            c = c.getRight();
        }
        return bestMatch;
    }

    private enum State {
        INITIALIZING, SOLVING, SOLVED
    }
}
