#!/usr/bin/env python3
"""Export review evidence from actual Maven reports using only Python's stdlib."""

import argparse
import hashlib
import html
import json
from pathlib import Path
import re
import shutil
import xml.etree.ElementTree as ET


def main():
    """Require successful results before exporting report snapshots."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--log", type=Path, required=True)
    parser.add_argument("--javadoc-log", type=Path)
    args = parser.parse_args()
    root = Path.cwd()
    target = root / "target"
    output = root / "docs/evidence"
    output.mkdir(parents=True, exist_ok=True)

    log = args.log.read_text()
    if "BUILD SUCCESS" not in log or "[ERROR]" in log:
        raise SystemExit("Expected a successful, error-free Maven verify log")

    # Parse Surefire's real XML results instead of hard-coding test totals.
    suites = []
    properties = {}
    for report in sorted((target / "surefire-reports").glob("TEST-*.xml")):
        suite = ET.parse(report).getroot()
        properties.update({p.attrib["name"]: p.attrib.get("value", "")
                           for p in suite.findall("properties/property")})
        item = {key: int(suite.attrib[key])
                for key in ("tests", "failures", "errors", "skipped")}
        item["name"] = suite.attrib["name"]
        item["cases"] = [case.attrib["name"]
                         for case in suite.findall("testcase")]
        suites.append(item)
    if not suites:
        raise SystemExit("No Surefire XML results found")
    totals = {key: sum(s[key] for s in suites)
              for key in ("tests", "failures", "errors", "skipped")}
    if totals["tests"] == 0 or any(totals[k] for k in ("failures", "errors", "skipped")):
        raise SystemExit("Expected passing tests with no skipped cases")

    # Bundle counters include production classes without coverage exclusions.
    jacoco = ET.parse(target / "site/jacoco/jacoco.xml").getroot()
    counters = {c.attrib["type"]: {k: int(c.attrib[k])
                                 for k in ("covered", "missed")}
                for c in jacoco.findall("counter")}
    for name in ("LINE", "BRANCH"):
        count = counters[name]
        count["percent"] = round(100 * count["covered"]
                                 / (count["covered"] + count["missed"]), 2)
        if count["percent"] < 80:
            raise SystemExit(f"Coverage below threshold: {name}")

    checkstyle = ET.parse(target / "checkstyle-result.xml").getroot()
    pmd = ET.parse(target / "pmd.xml").getroot()
    spotbugs = ET.parse(target / "spotbugsXml.xml").getroot()
    pmd_findings = [node for node in pmd.iter()
                    if node.tag.rsplit("}", 1)[-1] in ("violation", "error")]
    errors = spotbugs.find("Errors")
    metrics = spotbugs.find("FindBugsSummary")
    if (checkstyle.findall(".//error") or pmd_findings
            or spotbugs.findall("BugInstance") or errors is None
            or int(errors.attrib["errors"]) != 0
            or int(errors.attrib["missingClasses"]) != 0
            or metrics is None or int(metrics.attrib["total_classes"]) == 0):
        raise SystemExit("Static analysis must finish without findings or errors")

    def normalize(text):
        """Hide machine-specific paths while preserving report results."""
        text = text.replace(str(root), "<project>")
        # Agent paths also reveal the custom repository used in this session.
        text = re.sub(r'/Users/[^\s\"<>]+/connect-four-maven-repository',
                      '<local-maven-repository>', text)
        text = text.replace(str(Path.home()), "<user-home>")
        return text

    (output / "verify.log").write_text(normalize(log))
    if args.javadoc_log:
        (output / "javadoc.log").write_text(normalize(args.javadoc_log.read_text()))

    # Export the generated HTML with its local styles and image assets.
    # Keep all generated build output in target; commit only this evidence copy.
    shutil.copytree(target / "reports", output / "reports/maven",
                    dirs_exist_ok=True)
    shutil.copytree(target / "site/jacoco", output / "reports/jacoco",
                    dirs_exist_ok=True)
    shutil.copy2(target / "site/spotbugs.html", output / "reports/spotbugs.html")
    data = output / "data"
    data.mkdir(exist_ok=True)
    for source, name in [(target / "checkstyle-result.xml", "checkstyle.xml"),
                         (target / "pmd.xml", "pmd.xml"),
                         (target / "spotbugsXml.xml", "spotbugs.xml")]:
        # XML escaping keeps normalized path placeholders well formed.
        text = normalize(source.read_text())
        text = text.replace("<project>/", "&lt;project&gt;/")
        text = text.replace("<user-home>/", "&lt;user-home&gt;/")
        (data / name).write_text(text)
    # Normalize the single machine-specific path in SpotBugs' HTML report.
    report = output / "reports/spotbugs.html"
    report.write_text(normalize(report.read_text()).replace(
        "<project>/", "&lt;project&gt;/"))

    summary = {
        "verified_at": re.search(r'Finished at: (\S+)', log).group(1),
        "java_runtime": properties.get("java.runtime.version"),
        "os": properties.get("os.name"),
        "architecture": properties.get("os.arch"),
        "command": "./mvnw -o -B -ntp clean verify (custom local dependency cache)",
        "tests": totals,
        "coverage": {key: counters[key] for key in ("LINE", "BRANCH")},
        "checkstyle_violations": 0,
        "pmd_violations": 0,
        "spotbugs_bugs": 0,
        "spotbugs_errors": 0,
        "spotbugs_missing_classes": 0,
        "spotbugs_analyzed_classes": int(metrics.attrib["total_classes"]),
        "hyperskill": "Not executed in this verification session",
    }
    (output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    (data / "tests.json").write_text(json.dumps(suites, indent=2) + "\n")

    # Create an explicitly labeled summary from Surefire XML, not a fabricated
    # screenshot of an external test platform or the Surefire report plugin.
    rows = "".join("<tr>" + "".join(f"<td>{html.escape(str(s[k]))}</td>"
                   for k in ("name", "tests", "failures", "errors", "skipped"))
                   + "</tr>" for s in suites)
    total_row = "<tr><td><b>Total</b></td>" + "".join(
        f"<td><b>{totals[k]}</b></td>"
        for k in ("tests", "failures", "errors", "skipped")) + "</tr>"
    page = f"""<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8"><title>JUnit test results</title>
<style>body{{font-family:Arial,sans-serif;color:#17263c;background:white}}
h1{{color:#233c63}} th{{background:#e5edf6;text-align:left}}
td,th{{padding:12px}} table{{border-collapse:collapse;width:100%}}
td{{border-bottom:1px solid #c9d4e1}}</style></head><body>
<h1>Desktop Connect Four - JUnit 5 test results</h1>
<p><b>PASS: {totals['tests']} tests; zero failures, errors, or skipped tests.</b></p>
<p>Generated from Maven Surefire XML by tools/export_evidence.py.</p>
<p>Verified: {html.escape(summary['verified_at'])}<br>
Java: {html.escape(str(summary['java_runtime']))}; target release: 17.</p>
<table border="0" cellspacing="0" cellpadding="10"><tr>
<th>Test suite</th><th>Tests</th><th>Failures</th><th>Errors</th><th>Skipped</th>
</tr>{rows}{total_row}</table>
<h2>Verification scope</h2>
<p>Real model tests, controller tests with a Mockito view, and Swing panel
tests on the event-dispatch thread. No desktop window is required.</p>
<p>This is local project evidence. Hyperskill tests are separate.</p>
</body></html>
"""
    (output / "reports/tests.html").write_text(page)

    # Tie the snapshots to the exact build inputs, independently of screenshots.
    inputs = [root / "pom.xml", root / "mvnw", root / "mvnw.cmd"]
    for folder in ("src", "config", ".mvn"):
        inputs.extend(p for p in (root / folder).rglob("*") if p.is_file())
    manifest = "".join(hashlib.sha256(p.read_bytes()).hexdigest()
                       + "  " + str(p.relative_to(root)) + "\n"
                       for p in sorted(inputs))
    (output / "source-sha256.txt").write_text(manifest)
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
