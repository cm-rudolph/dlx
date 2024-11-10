package de.famiru.dlx;

import java.util.List;

public record Stats(int numberOfChoices, int numberOfPrimaryConstraints, int numberOfSecondaryConstraints,
                    int numberOfElements, int numberOfSolutions,
                    List<Long> numberOfUpdates, List<Long> numberOfVisitedNodes) {
    public int numberOfConstraints() {
        return numberOfPrimaryConstraints + numberOfSecondaryConstraints;
    }
}
