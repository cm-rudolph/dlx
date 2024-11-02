package de.famiru.dlx;

import java.util.List;

public record Stats(int numberOfChoices, int numberOfConstraints, int numberOfElements,
                    int numberOfSolutions, List<Long> numberOfUpdates, List<Long> numberOfVisitedNodes) {
}
