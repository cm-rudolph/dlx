package de.famiru.dlx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Dlx<T> {
    private static final Logger LOGGER = LogManager.getLogger(Dlx.class);

    private final Map<Future<Boolean>, Dlx<T>> forks = new HashMap<>();
    private final ExecutorService executor;
    private final int forkingLevel;

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

    Dlx(int numberOfConstraints, Set<Integer> indicesOfSecondaryConstraints, int forkingLevel, int numberOfThreads,
               int maxNumberOfSolutionsToStore, boolean countAllSolutions, int statusLogStepWidth) {
        this.forkingLevel = numberOfThreads > 1 ? forkingLevel : -1;
        this.executor = numberOfThreads > 1 ? Executors.newFixedThreadPool(numberOfThreads) : null;
        this.maxNumberOfSolutionsToStore = maxNumberOfSolutionsToStore;
        this.countAllSolutions = countAllSolutions;
        this.statusLogStepWidth = statusLogStepWidth;
        this.numberOfSecondaryConstraints = indicesOfSecondaryConstraints.size();
        head = new MatrixEntry<>();
        columnHeads = new ArrayList<>(numberOfConstraints);
        solution = new ArrayList<>();
        createColumnHeads(numberOfConstraints, indicesOfSecondaryConstraints);
    }

    // internal constructor for forking
    private Dlx(MatrixEntry<T> head, List<MatrixEntry<T>> solution, int maxNumberOfSolutionsToStore,
                boolean countAllSolutions, int statusLogStepWidth, int numberOfElements, int solutionsFound) {
        this.executor = null;
        this.forkingLevel = -1;
        this.head = head;
        this.columnHeads = List.of();
        this.solution = solution;
        this.maxNumberOfSolutionsToStore = maxNumberOfSolutionsToStore;
        this.countAllSolutions = countAllSolutions;
        this.statusLogStepWidth = statusLogStepWidth;
        this.numberOfSecondaryConstraints = 0;
        this.numberOfElements = numberOfElements;
        this.solutionsFound = solutionsFound;
    }

    /**
     * Start constructing a {@code Dlx} instance by using this method.
     */
    public static DlxBuilder.DlxConfig builder() {
        return new DlxBuilder.DlxConfig();
    }

    /**
     * Add a new choice (row) to the exact cover matrix.
     *
     * @param choiceData        the data that describes the choice. It gets returned by {@link #solve()}, if this choice
     *                          is part of an actual solution.
     * @param constraintIndices strictly increasing list of constraint (column) indices that are set to 1. A row of
     *                          {@code 1 0 0 1 0} would be described by a list of {@code 0, 3}.
     */
    void addChoice(T choiceData, List<Integer> constraintIndices) {
        if (constraintIndices == null || constraintIndices.isEmpty()) {
            return;
        }

        MatrixEntry<T> firstRowElement = null;
        for (int columnIndex : constraintIndices) {
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
    }

    private void createColumnHeads(int numberOfConstraints, Set<Integer> secondaryConstraints) {
        for (int i = 0; i < numberOfConstraints; i++) {
            MatrixEntry<T> columnHead = new MatrixEntry<>();
            columnHeads.add(columnHead);
            if (!secondaryConstraints.contains(i)) {
                head.insertBefore(columnHead);
            }
        }
    }

    /**
     * Solves the exact cover problem previously initialized using {@link #builder()} by executing
     * Donald E. Knuth's algorithm DLX. Executes only once and stores the result.
     *
     * @return All solutions up until {@code maxNumberOfSolutionsToStore} that have been found.
     */
    public List<List<T>> solve() {
        if (state.compareAndSet(State.INITIALIZING, State.SOLVING)) {
            try {
                LOGGER.info("Solving using DLX...");
                search(0);

                for (Map.Entry<Future<Boolean>, Dlx<T>> entry : forks.entrySet()) {
                    Future<Boolean> future = entry.getKey();
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                    Dlx<T> fork = entry.getValue();
                    join(fork);
                }

                LOGGER.info("Found {} solutions", solutionsFound);
            } finally {
                if (executor != null) {
                    executor.shutdown();
                }
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

        return Collections.unmodifiableList(solutions);
    }

    private Dlx<T> fork() {
        HashMap<MatrixEntry<T>, MatrixEntry<T>> mapping = new HashMap<>(numberOfElements);
        HashSet<MatrixEntry<T>> visited = new HashSet<>(numberOfElements);
        MatrixEntry<T> headCopy = head.copy(mapping, visited);

        List<MatrixEntry<T>> solutionCopy = new ArrayList<>(solution.size());
        for (MatrixEntry<T> entry : solution) {
            entry.copy(mapping, visited);
            solutionCopy.add(mapping.get(entry));
        }
        return new Dlx<>(headCopy, solutionCopy, maxNumberOfSolutionsToStore, countAllSolutions, statusLogStepWidth,
                numberOfElements, solutionsFound);
    }

    private void join(Dlx<T> fork) {
        Stats stats = fork.getStats();
        List<Long> numberOfUpdates = stats.numberOfUpdates();
        ensureStatsArraySize(numberOfUpdates.size());
        for (int i = 0; i < numberOfUpdates.size(); i++) {
            long updates = numberOfUpdates.get(i);
            this.updates[i] += updates;
        }

        List<Long> numberOfVisitedNodes = stats.numberOfVisitedNodes();
        for (int i = 0; i < numberOfVisitedNodes.size(); i++) {
            long visitedNodes = numberOfVisitedNodes.get(i);
            this.visitedNodes[i] += visitedNodes;
        }

        this.solutions.addAll(fork.solutions);
        this.solutionsFound += fork.solutionsFound;
    }

    private boolean search(int k) {
        if (head.getRight() == head) {
            return doSolutionBookkeeping();
        }
        ensureStatsArraySize(k + 1);

        MatrixEntry<T> c = selectNextColumn();
        updates[k] += c.coverColumn();
        MatrixEntry<T> r = c.getLower();
        boolean fork = k == forkingLevel;
        int solutionsCountCorrection = 0;
        while (r != c) {
            visitedNodes[k]++;
            solution.add(r);
            MatrixEntry<T> j = r.getRight();
            while (j != r) {
                updates[k] += j.coverColumn();
                j = j.getRight();
            }
            if (fork) {
                Dlx<T> forkedDlx = fork();
                solutionsCountCorrection += solutionsFound;
                Future<Boolean> future = executor.submit(() -> forkedDlx.search(k + 1));
                forks.put(future, forkedDlx);
            } else if (search(k + 1)) {
                return true;
            }
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
        solutionsFound -= solutionsCountCorrection;

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

    /**
     * Retrieve detailed statistics about the problem, the search tree and the solutions that have been found.
     */
    public Stats getStats() {
        return new Stats(numberOfChoices, columnHeads.size() - numberOfSecondaryConstraints,
                numberOfSecondaryConstraints, numberOfElements, solutionsFound,
                mapToList(updates), mapToList(visitedNodes)
        );
    }

    private List<Long> mapToList(long[] array) {
        return Arrays.stream(array).boxed().toList();
    }

    private enum State {
        INITIALIZING, SOLVING, SOLVED
    }
}
