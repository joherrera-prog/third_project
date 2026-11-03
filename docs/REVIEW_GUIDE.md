# Review and verification guide

## 1. Check the environment

```bash
./mvnw -v
```

The recorded build used Maven 3.9.16 on OpenJDK 27, with release 17 configured
for application and test bytecode. No runtime libraries are needed by the app.

## 2. Execute the complete build

```bash
./mvnw -B -ntp clean verify > verify.log 2>&1
```

Inspect the exit status and the end of `verify.log`. A successful build must
include the test totals, both JaCoCo checks, static-analysis checks, and
`BUILD SUCCESS`. Compilation or test success alone does not complete verify.

The recorded run used offline mode and a writable copy of the local Maven
dependency cache because this execution environment could not download from
Maven Central. It used the same pinned plugins and verify lifecycle.

## 3. Inspect the reports

Open the report paths listed in the root README in a browser. In particular,
check both the LINE and BRANCH counters in JaCoCo and the absence of analysis
errors or missing classes in SpotBugs. Coverage thresholds are aggregate.

The committed snapshots and images are in [evidence](evidence/README.md).
Their source manifest identifies the exact inputs associated with the run.

## 4. Generate Javadoc

```bash
javadoc -private -Xdoclint:all -encoding UTF-8 \
  -d target/apidocs src/main/java/four/*.java > javadoc.log 2>&1
```

## 5. Play the application

```bash
java -jar target/connect-four-1.0.0.jar
```

Verify gravity, the muted X/O colors, four-cell highlighting, rejected
full-column moves, blocked moves after victory, and reset behavior.
Automated tests cover these behaviors, but the actual JFrame still benefits
from a desktop smoke test. No interactive JFrame smoke test was performed
by the automated verification session.

## 6. Run the official Hyperskill checks separately

The local Maven tests are independent of the platform's official test suite.
No Hyperskill account session or platform result was available during this
verification. Add the platform result to the PR when available.

## 7. Refresh the committed evidence after code changes

Python 3 is needed only for this optional export step, not for the app build.
After successful verify and Javadoc commands:

```bash
python3 tools/export_evidence.py --log verify.log --javadoc-log javadoc.log
javac --release 17 -d target/evidence-tools tools/ReportCapture.java
java -Djava.awt.headless=true -cp target/evidence-tools ReportCapture \
  docs/evidence/reports/tests.html docs/evidence/screenshots/01-tests.png
java -Djava.awt.headless=true -cp target/evidence-tools ReportCapture \
  docs/evidence/reports/jacoco/index.html docs/evidence/screenshots/02-jacoco.png
java -Djava.awt.headless=true -cp target/evidence-tools ReportCapture \
  docs/evidence/reports/maven/checkstyle.html docs/evidence/screenshots/03-checkstyle.png
java -Djava.awt.headless=true -cp target/evidence-tools ReportCapture \
  docs/evidence/reports/maven/pmd.html docs/evidence/screenshots/04-pmd.png
java -Djava.awt.headless=true -cp target/evidence-tools ReportCapture \
  docs/evidence/reports/spotbugs.html docs/evidence/screenshots/05-spotbugs.png
```

`ReportCapture` renders existing HTML with Swing's HTML renderer. These are
off-screen report previews, not browser-window or Hyperskill screenshots.
The HTML snapshots retain their report content; browser styling can differ.
The JUnit summary is explicitly generated from Surefire XML by the exporter.
