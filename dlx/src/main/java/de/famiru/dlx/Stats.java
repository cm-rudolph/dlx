package de.famiru.dlx;

import java.util.List;

public record Stats(int numberOfSolutions, List<Long> numberOfUpdates, List<Long> numberOfVisitedNodes) {
}
