package de.famiru.dlx;

import java.util.List;
import java.util.Objects;

public final class Stats {
    private final int numberOfChoices;
    private final int numberOfConstraints;
    private final int numberOfElements;
    private final int numberOfSolutions;
    private final List<Long> numberOfUpdates;
    private final List<Long> numberOfVisitedNodes;

    public Stats(int numberOfChoices, int numberOfConstraints, int numberOfElements,
                 int numberOfSolutions, List<Long> numberOfUpdates, List<Long> numberOfVisitedNodes) {
        this.numberOfChoices = numberOfChoices;
        this.numberOfConstraints = numberOfConstraints;
        this.numberOfElements = numberOfElements;
        this.numberOfSolutions = numberOfSolutions;
        this.numberOfUpdates = numberOfUpdates;
        this.numberOfVisitedNodes = numberOfVisitedNodes;
    }

    public int numberOfChoices() {
        return numberOfChoices;
    }

    public int numberOfConstraints() {
        return numberOfConstraints;
    }

    public int numberOfElements() {
        return numberOfElements;
    }

    public int numberOfSolutions() {
        return numberOfSolutions;
    }

    public List<Long> numberOfUpdates() {
        return numberOfUpdates;
    }

    public List<Long> numberOfVisitedNodes() {
        return numberOfVisitedNodes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        Stats that = (Stats) obj;
        return this.numberOfChoices == that.numberOfChoices &&
               this.numberOfConstraints == that.numberOfConstraints &&
               this.numberOfElements == that.numberOfElements &&
               this.numberOfSolutions == that.numberOfSolutions &&
               Objects.equals(this.numberOfUpdates, that.numberOfUpdates) &&
               Objects.equals(this.numberOfVisitedNodes, that.numberOfVisitedNodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfChoices, numberOfConstraints, numberOfElements, numberOfSolutions, numberOfUpdates, numberOfVisitedNodes);
    }

    @Override
    public String toString() {
        return "Stats[" +
               "numberOfChoices=" + numberOfChoices + ", " +
               "numberOfConstraints=" + numberOfConstraints + ", " +
               "numberOfElements=" + numberOfElements + ", " +
               "numberOfSolutions=" + numberOfSolutions + ", " +
               "numberOfUpdates=" + numberOfUpdates + ", " +
               "numberOfVisitedNodes=" + numberOfVisitedNodes + ']';
    }
}
