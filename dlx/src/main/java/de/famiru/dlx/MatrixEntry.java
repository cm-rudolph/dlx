package de.famiru.dlx;

import java.util.Objects;

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

    int getRowCount() {
        return rowCount;
    }

    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return data != null ? data.toString() : "Head";
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
