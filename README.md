# Library to solve exact cover problems using Algorithm DLX

This project is a simple library to solve generalized
[exact cover problems](https://en.wikipedia.org/wiki/Exact_cover) using Donald E. Knuth's Algorithm DLX. The algorithm
was published in [arXiv:cs/0011047](https://arxiv.org/abs/cs/0011047).

## Features

The library supports

* solving exact cover problems like sudoku
* solving generalized exact cover problems like the N queens problem
* statistics about the search tree
* multithreading for larger problems

## Technical foundation

The library is compatible with Java 17 or newer.

It has a dependency to log4j2-api. If you want logging, provide a log4j2 implementation at runtime. This can be either
log4j2 or some bridge to another logging framework.

A Java 8 compatible release can be found using the version `0.7.0-jdk8`.

## Usage

First, include the library as a dependency within your project.

maven:
```xml
<dependency>
    <groupId>de.famiru.dlx</groupId>
    <artifactId>dlx</artifactId>
    <version>0.7.0</version>
</dependency>
```

gradle:
```groovy
implementation "de.famiru.dlx:dlx:0.7.0"
```

Then create an instance of `Dlx` and add all possible choices (rows) of the exact cover problem. For a matrix like

```text
0 0 1 0 1 1 0
1 0 0 1 0 0 1
0 1 1 0 0 1 0
1 0 0 1 0 0 0
0 1 0 0 0 0 1
0 0 0 1 1 0 1
```

it might look like that:

```java
Dlx<String> dlx = Dlx.builder()
        .numberOfConstraints(7)
        .<String>createChoiceBuilder()
        .addChoice("C E F", List.of(2, 4, 5))
        .addChoice("A D G", List.of(0, 3, 6))
        .addChoice("B C F", List.of(1, 2, 5))
        .addChoice("A D", List.of(0, 3))
        .addChoice("B G", List.of(1, 6))
        .addChoice("D E G", List.of(3, 4, 6))
        .build();

List<List<String>> solutions = dlx.solve();
```

Each choice carries some identifying information. `Dlx` is a generic class and the identifying information can be of any
type. It gets returned by `Dlx#solve()` and can then be further processed, e.g. for printing some human-readable
message.

For further information, please have a look into the JavaDocs of `Dlx` and `DlxBuilder`.

## Known limitations

* To improve performance in multithreading, there is no synchronization between threads. Therefore, the solver might run
  significantly longer than necessary to find the given number of solutions. In addition, it might find much more
  solutions than requested.

## License

Licensed under the MIT License.
