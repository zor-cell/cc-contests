# Cloudflight Coding Contests

A collection of past [Cloudflight Coding Contest (CCC)](https://codingcontest.org) challenges: the level
statements, the input files, my Java solutions and the outputs they produce.

The framework around the solutions (file discovery, input/output handling, visualizer check) comes from
[cc-input-reader](https://github.com/zor-cell/cc-input-reader). That repository is the clean template for a
single contest; this one collects everything that has been solved with it.

## Contests

| Contest | Levels | Task | Solutions |
| --- | --- | --- | --- |
| `lawnmower` (39th CCC) | 1–7 | Program a robotic lawn mower: evaluate given paths, then find a path that mows a lawn with one or more trees on it. | [`solver/lawnmower`](src/main/java/solver/lawnmower) |
| `roomplanner` (40th CCC) | 1–7 | Fill storage rooms with desks: count how many fit, then place them — later with other and mixed desk sizes. | [`solver/roomplanner`](src/main/java/solver/roomplanner) |
| `spaceship` (41st CCC) | 1–8 | Steer a spaceship with sequences of paces to a space station within a time limit, later around stationary and moving asteroids. | [`solver/spaceship`](src/main/java/solver/spaceship) |

Every level builds on the previous one, so later levels reuse the helper classes of their contest
(`PathFinder`, `Asteroid`, ...).

## Running a level

Requires JDK 21 and Maven. Always run from the repository root.

1. Point `inputPath` in [`config.xml`](src/main/resources/config.xml) at the level folder, without a trailing
   slash:
   ```xml
   <inputPath>./input/spaceship/level8</inputPath>
   ```
   The solution class is derived from this path: `./input/spaceship/level8` runs `solver.spaceship.Level8`.
2. Optionally restrict the files with `targetSpecificLevel`: `e` runs only the example, `-1` every file in the
   folder.
3. Run:
   ```bash
   mvn -q compile exec:java
   ```

Each input file produces one file in `output/`. Every non-empty output is also copied to
`solutions/<contest>/levelN/`, where nothing is ever deleted and only files of the same name are replaced.
Since that happens on every run, look at `git diff` before committing: a run with a half-finished solver
overwrites the stored solution as well.

To compare with the expected example output:

```bash
diff --strip-trailing-cr output/out_level-8_0-example.txt input/spaceship/level8/out_level-8_0-example.txt
```

Many later levels accept any valid answer, so a difference there does not mean the solution is wrong. That is
what the visualizer is for.

## Checking a solution with the visualizer

Levels that accept several answers ship an HTML page (`visualizer.html` or `sandbox-level-N.html`) that
validates a solution. The framework can feed every test case into it with headless Chrome:

1. Set `<visualizerPath>` to the HTML file of the level and `<enabled>true</enabled>` in the `verifier` block.
2. Adapt `verify()` in [`Solver.java`](src/main/java/solver/Solver.java) so that it reads one test case from
   the input and the matching lines from the output. Unlike `solve()`, it is not picked per level; it
   currently matches the later spaceship levels.
3. Run as above. Each output file gets a `.ver` file next to it in `output/` with one line per test case.

Chrome has to be installed. See [`CLAUDE.md`](CLAUDE.md) for the details and limits of the verifier.

## Adding a level

1. Unzip the level into `input/<contest>/levelN/`.
2. Create `src/main/java/solver/<contest>/LevelN.java` with a
   `public static void solve(Scanner reader, FileWriter writer) throws IOException`.
3. Set `inputPath` to the new folder, run and upload the files from `output/`.
4. Once the level is accepted, commit the solver together with `solutions/<contest>/levelN/`.
