# Desktop Connect Four

A two-player desktop Connect Four game written in Java with Swing for the
Grid Dynamics Java Core internship. The implementation follows the
Hyperskill **Desktop Connect Four** project requirements, including the
final **Players Ready** stage.

## Features

- A 6-by-7 board with gravity: clicking any cell selects its column.
- Alternating X and O turns, with muted red and blue piece backgrounds.
- Horizontal, vertical, and both diagonal winning combinations.
- Four winning positions highlighted with a distinct gold background.
- Full-column moves rejected without consuming the current player's turn.
- Further moves blocked after victory or a full-board draw.
- An enabled Reset button that restores the board, colors, and X's turn.
- Named Swing components: `ButtonA1` through `ButtonG6`, and `ButtonReset`.

## Build and run

The application targets **Java 17**. The complete verification was executed
on **OpenJDK 27**, macOS ARM64, using the included **Maven 3.9.16 Wrapper**.
No separate Maven installation is required.

```bash
git clone https://github.com/joherrera-prog/third_project.git
cd third_project
chmod +x mvnw
./mvnw -v
./mvnw clean verify
java -jar target/connect-four-1.0.0.jar
```

Until the review branch is merged, check out `feature/connect-four-review`
after cloning. On macOS, select the installed validation JDK if needed:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 27)
```

The first build needs network access to download Maven and dependencies.
Tests run with `java.awt.headless=true`; playing the application requires
a graphical desktop. Click a column to play, and press **Reset** to restart.

## Architecture

All application types belong to package `four`. The project is one Maven
module, with responsibilities separated into classes and an interface.

| Type | Responsibility |
| --- | --- |
| `Main` | Entry point and composition root; creates and connects dependencies. |
| `ConnectFour` | Configures the JFrame and requests initialization before showing it. |
| `GameManager` | Stores pieces and applies gravity, turns, victory, draw, and reset rules. |
| `GameView` | Defines the visual operations used by the controller. |
| `BoardPanel` | Creates real Swing components and updates their text and colors. |
| `CellButton` | Stores a cell's visual coordinates and required component name. |
| `CellActionListener` | Delegates actions to the model and requests visual updates. |
| `package-info.java` | Documents the package for generated Javadoc. |

The controller receives a `GameManager` and a `GameView` through its
constructor. This manual dependency injection allows controller tests to
replace the view with a Mockito mock. The model has no Swing dependency.
Component construction and interaction use Swing's event-dispatch thread.

Constructing `ConnectFour` also prepares its contents, allowing tests that
instantiate the window directly to find the required named buttons.

### Winning-line detection

The model uses zero-based coordinates, with row zero at the bottom.
Columns A through G correspond to indices zero through six.

After an accepted move, the model searches only lines containing the latest
piece. It checks four directions: `{0, 1}`, `{1, 0}`, `{1, 1}`, and `{1, -1}`.
For each direction, it tries four starting positions because the new piece
can occupy any of the four places in a winning line.

Each candidate position is calculated as `start + step * direction`.
All four positions must be inside the board and contain the player's piece.
The model then stores one verified line as four `{row, column}` pairs.
The view uses those coordinates for highlighting. Returned coordinates are
deep-copied to protect the model's state.

## Tests and quality gates

```bash
./mvnw test
./mvnw clean verify
```

`verify` compiles, runs the tests, builds the executable JAR, generates
reports, and enforces coverage and static-analysis gates.

| Check | Configuration | Recorded result |
| --- | --- | --- |
| JUnit 5 | 21 model cases, 6 controller tests, 7 panel tests | **34 passed; 0 failures, errors, or skips** |
| JaCoCo | Aggregate line and branch coverage each at least 80% | **90.38% lines; 100% branches** |
| Checkstyle | Unmodified `sun_checks.xml`, production sources | **0 violations** |
| PMD | Best practices, error-prone, and performance rules | **0 violations** |
| SpotBugs + Find Security Bugs | Maximum effort, low threshold; failures enabled | **0 bugs or analysis errors** |
| Javadoc | Private members included; `-Xdoclint:all` | **Passed** |

Recorded results are local verification evidence dated **2026-10-28**.
They are not GitHub Actions results or proof of a Hyperskill platform run.
Hyperskill verification must be performed separately in that platform.

### Test strategy

- `GameManagerTest` uses a real model and legal move sequences, including
  parameterized cases for invalid columns and winning directions.
- `CellActionListenerTest` uses a real model and controller with a mocked
  `GameView`; Mockito verifies the requested visual operations.
- `BoardPanelTest` inspects real components on the EDT, exercises connected
  button actions, and paints into images to verify background colors.

No production classes are excluded from coverage. `Main` and `ConnectFour`
remain in the aggregate even though the headless tests do not open a JFrame.
Static source analysis applies to production Java sources; it does not
lint tests or the optional evidence-export utilities.

PMD excludes `AvoidInstantiatingObjectsInLoops` because each grid position
requires its own Swing component. Other configured findings fail the build.
PMD's ASM dependency is pinned to 9.11 so it can read JDK 27 library classes.
SpotBugs analysis failures are not suppressed to work around bytecode errors.

## Reports and review evidence

Generated report locations after `verify`:

| Report | Local path |
| --- | --- |
| JUnit XML and text | `target/surefire-reports/` |
| JaCoCo | `target/site/jacoco/index.html` |
| Checkstyle | `target/reports/checkstyle.html` |
| PMD | `target/reports/pmd.html` |
| SpotBugs | `target/site/spotbugs.html` |

See [verification evidence](docs/evidence/README.md) for report images,
HTML snapshots, machine-readable results, and the successful build log.
See [the review guide](docs/REVIEW_GUIDE.md) for reproduction steps and
[the variable guide](docs/VARIABLE_GUIDE.md) for the model's terminology.

Generate API documentation separately:

```bash
javadoc -private -Xdoclint:all -encoding UTF-8 \
  -d target/apidocs src/main/java/four/*.java
```

## Project layout

| Location | Contents |
| --- | --- |
| `src/main/java/four/` | Application classes, interface, and package Javadoc. |
| `src/test/java/four/` | JUnit and Mockito tests. |
| `config/pmd-ruleset.xml` | PMD rules and the documented UI allocation exception. |
| `.mvn/wrapper/`, `mvnw`, `mvnw.cmd` | Maven Wrapper configuration and scripts. |
| `pom.xml` | Java target, test dependencies, plugins, gates, and JAR entry point. |
| `docs/` | Review documentation and committed verification snapshots. |
| `tools/` | Optional evidence export and off-screen HTML rendering utilities. |
| `target/` | Generated build output; excluded from Git. |
