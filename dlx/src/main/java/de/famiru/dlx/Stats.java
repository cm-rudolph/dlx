package de.famiru.dlx;

import java.util.List;
import java.util.Objects;

public final class Stats {
    private final int numberOfChoices;
    private final int numberOfPrimaryConstraints;
    private final int numberOfSecondaryConstraints;
    private final int numberOfElements;
    private final int numberOfSolutions;
    private final List<Long> numberOfUpdates;
    private final List<Long> numberOfVisitedNodes;

    public Stats(int numberOfChoices, int numberOfPrimaryConstraints, int numberOfSecondaryConstraints,
                 int numberOfElements, int numberOfSolutions,
                 List<Long> numberOfUpdates, List<Long> numberOfVisitedNodes) {
        this.numberOfChoices = numberOfChoices;
        this.numberOfPrimaryConstraints = numberOfPrimaryConstraints;
        this.numberOfSecondaryConstraints = numberOfSecondaryConstraints;
        this.numberOfElements = numberOfElements;
        this.numberOfSolutions = numberOfSolutions;
        this.numberOfUpdates = numberOfUpdates;
        this.numberOfVisitedNodes = numberOfVisitedNodes;
    }

    public int numberOfChoices() {
        return numberOfChoices;
    }

    public int numberOfPrimaryConstraints() {
        return numberOfPrimaryConstraints;
    }

    public int numberOfSecondaryConstraints() {
        return numberOfSecondaryConstraints;
    }

    public int numberOfConstraints() {
        return numberOfPrimaryConstraints + numberOfSecondaryConstraints;
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
               this.numberOfPrimaryConstraints == that.numberOfPrimaryConstraints &&
               this.numberOfSecondaryConstraints == that.numberOfSecondaryConstraints &&
               this.numberOfElements == that.numberOfElements &&
               this.numberOfSolutions == that.numberOfSolutions &&
               Objects.equals(this.numberOfUpdates, that.numberOfUpdates) &&
               Objects.equals(this.numberOfVisitedNodes, that.numberOfVisitedNodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfChoices, numberOfPrimaryConstraints, numberOfSecondaryConstraints, numberOfElements,
                numberOfSolutions, numberOfUpdates, numberOfVisitedNodes);
    }

    @Override
    public String toString() {
        return "Stats[" +
               "numberOfChoices=" + numberOfChoices + ", " +
               "numberOfPrimaryConstraints=" + numberOfPrimaryConstraints + ", " +
               "numberOfSecondaryConstraints=" + numberOfSecondaryConstraints + ", " +
               "numberOfElements=" + numberOfElements + ", " +
               "numberOfSolutions=" + numberOfSolutions + ", " +
               "numberOfUpdates=" + numberOfUpdates + ", " +
               "numberOfVisitedNodes=" + numberOfVisitedNodes + ']';
    }
}
