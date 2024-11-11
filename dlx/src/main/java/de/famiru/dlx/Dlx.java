package de.famiru.dlx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    // fields for statistics
    private final int numberOfSecondaryConstraints;
    private int numberOfChoices = 0;
    private int numberOfElements = 0;
    private int solutionsFound = 0;
    private long[] updates = new long[0];
    private long[] visitedNodes = new long[0];

    /**
     * Create an empty instance of a (generalized) exact cover problem to be solved using Algorithm DLX. Insert choices
     * using {@link #addChoice(Object, List)} before solving it using {@link #solve()}.
     *
     * @param numberOfPrimaryConstraints   the number of constraints that must be fulfilled
     * @param numberOfSecondaryConstraints the number of optional constraints that must be fulfilled at most once
     * @param maxNumberOfSolutionsToStore  the maximum number of concrete solutions that should be returned by
     *                                     {@link #solve()}
     * @param countAllSolutions            whether to stop after {@code maxNumberOfSolutionsToStore} or to count all
     *                                     possible solutions. The result can be retrieved using {@link #getStats()}
     * @param statusLogStepWidth           print an informational log line after {@code statusLogStepWidth} solutions
     *                                     have been found
     */
    public Dlx(int numberOfPrimaryConstraints, int numberOfSecondaryConstraints, int maxNumberOfSolutionsToStore,
               boolean countAllSolutions, int statusLogStepWidth) {
        this(numberOfPrimaryConstraints + numberOfSecondaryConstraints,
                generateSequence(numberOfPrimaryConstraints, numberOfSecondaryConstraints),
                maxNumberOfSolutionsToStore, countAllSolutions, statusLogStepWidth);
    }

    /**
     * Create an empty instance of a (generalized) exact cover problem to be solved using Algorithm DLX. Insert choices
     * using {@link #addChoice(Object, List)} before solving it using {@link #solve()}.
     *
     * @param numberOfConstraints           the total number of constraints that must be fulfilled including optional
     *                                      constraints
     * @param indicesOfSecondaryConstraints the indices of optional constraints that must be fulfilled at most once
     * @param maxNumberOfSolutionsToStore   the maximum number of concrete solutions that should be returned by
     *                                      {@link #solve()}
     * @param countAllSolutions             whether to stop after {@code maxNumberOfSolutionsToStore} or to count all
     *                                      possible solutions. The result can be retrieved using {@link #getStats()}
     * @param statusLogStepWidth            print an informational log line after {@code statusLogStepWidth} solutions
     *                                      have been found
     */
    public Dlx(int numberOfConstraints, Collection<Integer> indicesOfSecondaryConstraints,
               int maxNumberOfSolutionsToStore, boolean countAllSolutions, int statusLogStepWidth) {
        this.maxNumberOfSolutionsToStore = maxNumberOfSolutionsToStore;
        this.countAllSolutions = countAllSolutions;
        this.statusLogStepWidth = statusLogStepWidth;
        this.numberOfSecondaryConstraints = indicesOfSecondaryConstraints.size();
        head = new MatrixEntry<>();
        columnHeads = new ArrayList<>(numberOfConstraints);
        solution = new ArrayList<>();
        createColumnHeads(numberOfConstraints, indicesOfSecondaryConstraints);
    }

    private static Set<Integer> generateSequence(int numberOfPrimaryConstraints, int numberOfSecondaryConstraints) {
        return IntStream.range(numberOfPrimaryConstraints, numberOfSecondaryConstraints + numberOfPrimaryConstraints)
                .boxed()
                .collect(Collectors.toSet());
    }

    /**
     * Add a new choice (row) to the exact cover matrix.
     *
     * @param choiceData        the data that describes the choice. It gets returned by {@link #solve()}, if this choice
     *                          is part of an actual solution.
     * @param constraintIndices strictly increasing list of constraint (column) indices that are set to 1. A row of
     *                          {@code 1 0 0 1 0} would be described by a list of {@code 0, 3}.
     */
    public void addChoice(T choiceData, List<Integer> constraintIndices) {
        checkBrokenState();
        if (state.get() != State.INITIALIZING) {
            throw new IllegalStateException("solve() has already been called.");
        }
        if (constraintIndices == null || constraintIndices.isEmpty()) {
            return;
        }

        MatrixEntry<T> firstRowElement = null;
        int lastIdx = -1;
        for (int columnIndex : constraintIndices) {
            if (lastIdx >= columnIndex) {
                markBroken();
                throw new IllegalArgumentException("Constraint indices must be >= 0 and strictly increasing.");
            }
            if (columnIndex >= columnHeads.size()) {
                markBroken();
                throw new IllegalArgumentException("Constraint indices must be < " + columnHeads.size() + ".");
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
        numberOfChoices++;
        numberOfElements += constraintIndices.size();
        if (state.get() != State.INITIALIZING) {
            markBroken();
            throw new IllegalStateException("solve() has already been called while adding an additional choice.");
        }
    }

    private void createColumnHeads(int numberOfConstraints, Collection<Integer> secondaryConstraints) {
        for (int i = 0; i < numberOfConstraints; i++) {
            MatrixEntry<T> columnHead = new MatrixEntry<>();
            columnHeads.add(columnHead);
            if (!secondaryConstraints.contains(i)) {
                head.insertBefore(columnHead);
            }
        }
    }

    /**
     * Solves the exact cover problem previously initialized using {@link #addChoice(Object, List)} by executing
     * Donald E. Knuth's algorithm DLX. Executes only once and stores the result.
     *
     * @return All solutions up until {@code maxNumberOfSolutionsToStore} that have been found.
     */
    public List<List<T>> solve() {
        checkBrokenState();
        if (state.compareAndSet(State.INITIALIZING, State.SOLVING)) {
            try {
                LOGGER.info("Solving using DLX...");
                search(0);
                LOGGER.info("Found {} solutions", solutionsFound);
            } finally {
                state.compareAndSet(State.SOLVING, State.SOLVED);
                solvedLatch.countDown();
            }
        }

        try {
            solvedLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        checkBrokenState();

        return Collections.unmodifiableList(solutions);
    }

    private boolean search(int k) {
        if (head.getRight() == head) {
            return doSolutionBookkeeping();
        }
        ensureStatsArraySize(k + 1);

        MatrixEntry<T> c = selectNextColumn();
        updates[k] += c.coverColumn();
        MatrixEntry<T> r = c.getLower();
        while (r != c) {
            visitedNodes[k]++;
            solution.add(r);
            MatrixEntry<T> j = r.getRight();
            while (j != r) {
                updates[k] += j.coverColumn();
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

    protected boolean doSolutionBookkeeping() {
        if (solutionsFound < maxNumberOfSolutionsToStore) {
            solutions.add(solution.stream()
                    .map(MatrixEntry::getData)
                    .collect(Collectors.toList()));
        }
        solutionsFound++;
        if (solutionsFound % statusLogStepWidth == 0) {
            LOGGER.info("Found {} solutions so far.", solutionsFound);
        }
        return !(countAllSolutions || solutionsFound < maxNumberOfSolutionsToStore);
    }

    private void ensureStatsArraySize(int size) {
        if (updates.length < size) {
            long[] newUpdates = new long[size];
            System.arraycopy(updates, 0, newUpdates, 0, updates.length);
            updates = newUpdates;

            long[] newVisitedNodes = new long[size];
            System.arraycopy(visitedNodes, 0, newVisitedNodes, 0, visitedNodes.length);
            visitedNodes = newVisitedNodes;
        }
    }

    private void markBroken() {
        state.set(State.BROKEN);
        solvedLatch.countDown();
    }

    private void checkBrokenState() {
        if (state.get() == State.BROKEN) {
            throw new IllegalStateException("This Dlx instance is broken due to invalid usage.");
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

    public Stats getStats() {
        checkBrokenState();
        return new Stats(numberOfChoices, columnHeads.size() - numberOfSecondaryConstraints,
                numberOfSecondaryConstraints, numberOfElements, solutionsFound,
                mapToList(updates), mapToList(visitedNodes)
        );
    }

    private List<Long> mapToList(long[] array) {
        return Arrays.stream(array).boxed().toList();
    }

    private enum State {
        INITIALIZING, SOLVING, SOLVED, BROKEN
    }
}
