# Library to solve exact cover problems using Algorithm DLX

This project is a simple library to solve generalized
[exact cover problems](https://en.wikipedia.org/wiki/Exact_cover) using Donald E. Knuth's Algorithm DLX. The algorithm
was published in [arXiv:cs/0011047](https://arxiv.org/abs/cs/0011047).

## Technical foundation

The library is compatible with Java 17 or newer.

It has a dependency to log4j2-api. If you want logging, provide a log4j2 implementation at runtime. This can be either
log4j2 or some bridge to another logging framework.

## Usage

First, include the library as a dependency within your project.

maven:
```xml
<dependency>
    <groupId>de.famiru.dlx</groupId>
    <artifactId>dlx</artifactId>
    <version>0.6.1</version>
</dependency>
```

gradle:
```groovy
implementation "de.famiru.dlx:dlx:0.6.1"
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
Dlx<String> dlx = new Dlx<>(7, 0, 1, false, 1);
dlx.addChoice("C E F", List.of(2, 4, 5));
dlx.addChoice("A D G", List.of(0, 3, 6));
dlx.addChoice("B C F", List.of(1, 2, 5));
dlx.addChoice("A D", List.of(0, 3));
dlx.addChoice("B G", List.of(1, 6));
dlx.addChoice("D E G", List.of(3, 4, 6));

List<List<String>> solutions = dlx.solve();
```

Each choice carries some identifying information. `Dlx` is a generic class and the identifying information can be of any
type. It gets returned by `Dlx#solve()` and can then be further processed, e.g. for printing some human-readable
message.

For further information, please have a look into the JavaDocs of `Dlx`.

## Open tasks before 1.0.0

* Consider multithreading

## License

Licensed under the MIT License.
