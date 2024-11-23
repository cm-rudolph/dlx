package de.famiru.dlx;

import java.util.*;

import static java.util.Objects.requireNonNull;

class MatrixEntry<T> {
    private final T data;
    private final MatrixEntry<T> columnHead;
    private MatrixEntry<T> left;
    private MatrixEntry<T> right;
    private MatrixEntry<T> upper;
    private MatrixEntry<T> lower;
    private int rowCount;

    // constructor for column header entries
    MatrixEntry() {
        rowCount = 0;
        left = this;
        right = this;
        upper = this;
        lower = this;
        columnHead = this;
        this.data = null;
    }

    // constructor for regular entries
    MatrixEntry(T data, MatrixEntry<T> columnHead) {
        left = this;
        right = this;
        upper = this;
        lower = this;
        this.columnHead = requireNonNull(columnHead);
        this.data = requireNonNull(data);
    }

    MatrixEntry<T> copy(Map<MatrixEntry<T>, MatrixEntry<T>> mapping, Set<MatrixEntry<T>> visited) {
        if (visited.contains(this)) {
            return mapping.get(this);
        }

        Deque<MatrixEntry<T>> queue = new LinkedList<>();
        queue.add(this);

        while (!queue.isEmpty()) {
            MatrixEntry<T> current = queue.remove();
            MatrixEntry<T> copy;
            if (current.isColumnHead()) {
                copy = mapping.computeIfAbsent(current, k -> new MatrixEntry<>());
            } else {
                MatrixEntry<T> columnHead = mapping.get(current.columnHead);
                if (columnHead == null) {
                    columnHead = new MatrixEntry<>();
                    mapping.put(current.columnHead, columnHead);
                    queue.addFirst(columnHead);
                }
                copy = new MatrixEntry<>(current.data, columnHead);
            }
            mapping.put(current, copy);

            if (visited.add(current.upper)) {
                queue.add(current.upper);
            }
            if (visited.add(current.lower)) {
                queue.add(current.lower);
            }
            if (visited.add(current.right)) {
                queue.add(current.right);
            }
            if (visited.add(current.left)) {
                queue.add(current.left);
            }
        }

        for (Map.Entry<MatrixEntry<T>, MatrixEntry<T>> entry : mapping.entrySet()) {
            MatrixEntry<T> from = entry.getKey();
            MatrixEntry<T> to = entry.getValue();
            to.upper = mapping.get(from.upper);
            to.lower = mapping.get(from.lower);
            to.right = mapping.get(from.right);
            to.left = mapping.get(from.left);
            to.rowCount = from.rowCount;
        }
        return mapping.get(this);
    }

    void insertBefore(MatrixEntry<T> entry) {
        entry.right = this;
        entry.left = left;
        left.right = entry;
        left = entry;
    }

    void insertAbove(MatrixEntry<T> entry) {
        entry.lower = this;
        entry.upper = upper;
        upper.lower = entry;
        upper = entry;
        columnHead.rowCount++;
    }

    MatrixEntry<T> getLeft() {
        return left;
    }

    MatrixEntry<T> getRight() {
        return right;
    }

    MatrixEntry<T> getLower() {
        return lower;
    }

    private boolean isColumnHead() {
        return columnHead == this;
    }

    int getRowCount() {
        return rowCount;
    }

    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return Objects.requireNonNullElse(data, "Head").toString();
    }

    int coverColumn() {
        int updates = 1;
        columnHead.right.left = columnHead.left;
        columnHead.left.right = columnHead.right;
        MatrixEntry<T> i = columnHead.lower;
        while (i != columnHead) {
            MatrixEntry<T> j = i.right;
            while (j != i) {
                updates++;
                j.lower.upper = j.upper;
                j.upper.lower = j.lower;
                j.columnHead.rowCount--;
                j = j.right;
            }
            i = i.lower;
        }
        return updates;
    }

    void uncoverColumn() {
        MatrixEntry<T> i = columnHead.upper;
        while (i != columnHead) {
            MatrixEntry<T> j = i.left;
            while (j != i) {
                j.columnHead.rowCount++;
                j.lower.upper = j;
                j.upper.lower = j;
                j = j.left;
            }
            i = i.upper;
        }
        columnHead.right.left = columnHead;
        columnHead.left.right = columnHead;
    }
}
