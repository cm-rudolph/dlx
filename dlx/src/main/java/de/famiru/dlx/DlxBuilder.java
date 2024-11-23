package de.famiru.dlx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DlxBuilder<T> {
    private final Dlx<T> dlx;
    private final int numberOfConstraints;
    private boolean building = true;

    private DlxBuilder(Dlx<T> dlx, int numberOfConstraints) {
        this.dlx = dlx;
        this.numberOfConstraints = numberOfConstraints;
    }

    /**
     * Add a new choice (row) to the exact cover matrix.
     *
     * @param choiceData        the data that describes the choice. It gets returned by {@link Dlx#solve()}, if this
     *                          choice is part of an actual solution.
     * @param constraintIndices strictly increasing list of constraint (column) indices that are set to 1. A row of
     *                          {@code 1 0 0 1 0} would be described by a list of {@code 0, 3}.
     */
    public DlxBuilder<T> addChoice(T choiceData, List<Integer> constraintIndices) {
        verifyStillBuilding();
        if (constraintIndices == null || constraintIndices.isEmpty()) {
            throw new IllegalArgumentException("constraintIndices cannot be null or empty");
        }
        if (indicesUnordered(constraintIndices)) {
            constraintIndices = new ArrayList<>(constraintIndices);
            constraintIndices.sort(Integer::compareTo);
        }
        verifyNoDuplicates(constraintIndices);
        if (constraintIndices.get(0) < 0
            || constraintIndices.get(constraintIndices.size() - 1) >= numberOfConstraints) {
            throw new IllegalArgumentException("indices must be between 0 and " + (numberOfConstraints - 1));
        }
        dlx.addChoice(choiceData, constraintIndices);
        return this;
    }

    private void verifyStillBuilding() {
        if (!building) {
            throw new IllegalStateException("The Dlx instance has already been created");
        }
    }

    private boolean indicesUnordered(List<Integer> constraintIndices) {
        int lastIndex = Integer.MIN_VALUE;
        for (Integer index : constraintIndices) {
            if (index == null) {
                throw new IllegalArgumentException("indices must not contain null values");
            }
            if (lastIndex >= index) {
                return true;
            }
            lastIndex = index;
        }
        return false;
    }

    private void verifyNoDuplicates(List<Integer> constraintIndices) {
        int lastIndex = -1;
        for (int index : constraintIndices) {
            if (index == lastIndex) {
                throw new IllegalArgumentException("indices must not contain duplicate elements");
            }
            lastIndex = index;
        }
    }

    /**
     * Finally build the {@link Dlx} instance to solve the problem.
     */
    public Dlx<T> build() {
        building = false;
        return dlx;
    }

    public static class DlxConfig {
        private Integer numberOfConstraints = null;
        private Set<Integer> indicesOfSecondaryConstraints = null;
        private int forkingLevel = -1;
        private int numberOfThreads = 1;
        private int maxNumberOfSolutionsToStore = 1;
        private boolean countAllSolutions = false;
        private int statusLogStepWidth = Integer.MAX_VALUE;

        DlxConfig() {
        }

        /**
         * Prepare solving an exact cover problem that does not contain optional constraints.
         *
         * @param numberOfPrimaryConstraints the number of constraints that must be fulfilled exactly once
         */
        public DlxConfig numberOfConstraints(int numberOfPrimaryConstraints) {
            if (numberOfPrimaryConstraints < 1) {
                throw new IllegalArgumentException("Number of primary constraints must be greater than 0");
            }
            this.numberOfConstraints = numberOfPrimaryConstraints;
            this.indicesOfSecondaryConstraints = Set.of();
            return this;
        }

        /**
         * Prepare solving a generalized exact cover problem that contains optional constraints.
         *
         * @param numberOfPrimaryConstraints   the number of constraints that must be fulfilled exactly once
         * @param numberOfSecondaryConstraints the number of optional constraints that must be fulfilled at most once
         */
        public DlxConfig numberOfConstraints(int numberOfPrimaryConstraints, int numberOfSecondaryConstraints) {
            if (numberOfPrimaryConstraints < 0) {
                throw new IllegalArgumentException("Number of primary constraints cannot be negative");
            }
            if (numberOfSecondaryConstraints < 0) {
                throw new IllegalArgumentException("Number of secondary constraints cannot be negative");
            }
            if (numberOfPrimaryConstraints == 0 && numberOfSecondaryConstraints == 0) {
                throw new IllegalArgumentException("There must be at least one constraint");
            }
            this.numberOfConstraints = numberOfPrimaryConstraints + numberOfSecondaryConstraints;
            this.indicesOfSecondaryConstraints = generateSequence(numberOfPrimaryConstraints, numberOfSecondaryConstraints);
            return this;
        }

        /**
         * Prepare solving a generalized exact cover problem that contains optional constraints.
         *
         * @param numberOfConstraints           the total number of constraints that must be fulfilled including
         *                                      optional constraints
         * @param indicesOfSecondaryConstraints the indices of optional constraints that must be fulfilled at most once
         */
        public DlxConfig numberOfConstraints(int numberOfConstraints,
                                             Collection<Integer> indicesOfSecondaryConstraints) {
            if (numberOfConstraints < 1) {
                throw new IllegalArgumentException("Number of constraints must be greater than 0");
            }
            if (indicesOfSecondaryConstraints == null) {
                throw new IllegalArgumentException("indicesOfSecondaryConstraints cannot be null");
            }
            for (Integer index : indicesOfSecondaryConstraints) {
                if (index == null) {
                    throw new IllegalArgumentException("indicesOfSecondaryConstraints must not contain null values");
                }
                if (index < 0 || index >= numberOfConstraints) {
                    throw new IllegalArgumentException("All secondary indices must be between 0 and " +
                                                       (numberOfConstraints - 1));
                }
            }
            this.numberOfConstraints = numberOfConstraints;
            this.indicesOfSecondaryConstraints = Set.copyOf(indicesOfSecondaryConstraints);
            return this;
        }

        /**
         * Enable multithreading. Use as many threads as available processors. A lower {@code forkingLevel} reduces
         * the overhead of forking but might lead to inefficient CPU usage.
         * <p>
         *     By default, multithreading is disabled.
         * </p>
         *
         * @param forkingLevel the distance from the root of the search tree where {@code 0} is the root itself.
         * @see #enableMultithreading(int, int)
         * @see #disableMultithreading()
         */
        public DlxConfig enableMultithreading(int forkingLevel) {
            return enableMultithreading(forkingLevel, Runtime.getRuntime().availableProcessors());
        }

        /**
         * Enable multithreading. A lower {@code forkingLevel} reduces the overhead of forking but might lead to
         * inefficient CPU usage.
         * <p>
         *     By default, multithreading is disabled.
         * </p>
         *
         * @param forkingLevel    the distance from the root of the search tree where {@code 0} is the root itself.
         * @param numberOfThreads how many threads should run in parallel
         * @see #enableMultithreading(int)
         * @see #disableMultithreading()
         */
        public DlxConfig enableMultithreading(int forkingLevel, int numberOfThreads) {
            if (numberOfThreads < 1) {
                throw new IllegalArgumentException("Number of threads must be greater than 0");
            }
            this.numberOfThreads = numberOfThreads;
            this.forkingLevel = numberOfThreads > 1 ? forkingLevel : -1;
            return this;
        }

        /**
         * Disable multithreading. Prevents the overhead of forking. Especially suited for smaller problems.
         * <p>
         *     By default, multithreading is disabled already.
         * </p>
         * @see #enableMultithreading(int)
         * @see #enableMultithreading(int, int)
         */
        public DlxConfig disableMultithreading() {
            this.numberOfThreads = 1;
            this.forkingLevel = -1;
            return this;
        }

        /**
         * Define how many solutions should be stored and returned by {@link Dlx#solve()}.
         * <p>
         *     Defaults to {@code 1}.
         * </p>
         *
         * @param maxNumberOfSolutionsToStore the number of solutions to store
         */
        public DlxConfig maxNumberOfSolutionsToStore(int maxNumberOfSolutionsToStore) {
            if (maxNumberOfSolutionsToStore < 0) {
                throw new IllegalArgumentException("maxNumberOfSolutionsToStore must be at least 0");
            }
            this.maxNumberOfSolutionsToStore = maxNumberOfSolutionsToStore;
            return this;
        }

        /**
         * Continue to search after {@link #maxNumberOfSolutionsToStore(int)} have been found. Use if you are interested
         * in the total number of solutions, that can be retrieved using {@link Dlx#getStats()}.
         * <p>
         *     Defaults to {@code false}.
         * </p>
         *
         * @param countAllSolutions set to {@code true} if all solutions should be counted
         */
        public DlxConfig countAllSolutions(boolean countAllSolutions) {
            this.countAllSolutions = countAllSolutions;
            return this;
        }

        /**
         * Disable status logging messages during {@link Dlx#solve()}. This is the default.
         *
         * @see #statusLogStepWidth(int)
         */
        public DlxConfig disableStatusLog() {
            this.statusLogStepWidth = Integer.MAX_VALUE;
            return this;
        }

        /**
         * Output a status log line after every {@code statusLogStepWidth} solutions have been found while running
         * {@link Dlx#solve()}.
         * <p>
         *     By default, no status log lines will be printed.
         * </p>
         *
         * @param statusLogStepWidth afther that many solutions a log message should be printed.
         */
        public DlxConfig statusLogStepWidth(int statusLogStepWidth) {
            if (statusLogStepWidth < 0) {
                throw new IllegalArgumentException("Status log step width must be greater than 0");
            }
            this.statusLogStepWidth = statusLogStepWidth;
            return this;
        }

        /**
         * Call this method after the configuration is done.
         */
        public <T> DlxBuilder<T> createChoiceBuilder() {
            return new DlxBuilder<>(createDlx(), numberOfConstraints);
        }

        private <T> Dlx<T> createDlx() {
            if (numberOfConstraints == null) {
                throw new IllegalArgumentException("Number of constraints must be set");
            }
            return new Dlx<>(numberOfConstraints, indicesOfSecondaryConstraints, forkingLevel, numberOfThreads,
                    maxNumberOfSolutionsToStore, countAllSolutions, statusLogStepWidth);
        }

        private static Set<Integer> generateSequence(int numberOfPrimaryConstraints, int numberOfSecondaryConstraints) {
            return IntStream.range(numberOfPrimaryConstraints, numberOfSecondaryConstraints + numberOfPrimaryConstraints)
                    .boxed()
                    .collect(Collectors.toSet());
        }
    }
}
